package btClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;

import javax.swing.JProgressBar;

import btClient.BtUtils.Status;

public class ActiveTorrent implements Runnable {
	private int unchoked_peers;
	/**
	 *  worstPeer extends timer to select a pere to choke and then pick
	 *  a peer randomly to unchoke
	 */
	GetWorstPeer worstPeer;
	/**
	 * Timer for choke/unchoke algo
	 */
	private Timer timer;
	/**
	 * The {@link TorrentInfo} file that is associated with this
	 * {@link ActiveTorrent}
	 */
	private final TorrentInfo torrent;
	/**
	 * The {@link CommunicationTracker} that links this {@link ActiveTorrent} to
	 * the BitTorrent Tracker
	 */
	private final CommunicationTracker communicationTracker;
	/**
	 * The list of {@link Peer} objects that this {@link ActiveTorrent} is
	 * connected to
	 */
	private ArrayList<Peer> peers;
	/**
	 * The {@link Piece} objects that comprise the download for this
	 * {@link ActiveTorrent}
	 */
	private ArrayList<Piece> pieces;
	/**
	 * The list of threads associated with this {@link ActiveTorrent}
	 */
	private ArrayList<Thread> threads;
	/**
	 * The location to where downloaded bytes are written
	 */
	private File file;
	/**
	 * Indicates whether this torrent currently has threads running for
	 * upload/download with peers
	 */
	private boolean active;
	/**
	 * A Progress bar indicating the percentage of the file that has been
	 * downloaded
	 */
	private JProgressBar progressBar;
	/**
	 * The row number that corresponds to this torrent in the GUI table
	 */
	private int gui_index;
	/**
	 * A string describing the status of this torrent
	 */
	public static Status status;

	/**
	 * Creates a new ActiveTorrent Object for the given torrent file
	 * 
	 * @param torrent
	 *            Torrent file with download information
	 * @param {@link#file}
	 * @throws FileNotFoundException
	 */
	public ActiveTorrent(TorrentInfo torrent, File file)
			throws FileNotFoundException {
		super();
		this.torrent = torrent;
		unchoked_peers = 0;
		this.communicationTracker = new CommunicationTracker(torrent);
		this.file = file;
		peers = new ArrayList<Peer>();
		threads = new ArrayList<Thread>();
		active = false;
		createPieces();
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setValue(getPercentComplete());
		progressBar.setVisible(true);
		gui_index = -1;
		status = Status.Stopped;
	}

	public int getNumPieces() {
		return pieces.size();
	}

	/**
	 * Checks if this torrent is currently uploading/downloading
	 * 
	 * @return True if torrent active, otherwise false
	 */
	public boolean isActive() {
		active = false;
		for (Thread thread : threads) {
			if (thread.isAlive()) {
				active = true;
			}
		}
		return active;
	}

	public String getFileName() {
		return file.getName();
	}

	/**
	 * Creates a new piece object for the total number of pieces in the file to
	 * be downloaded and adds them to the pieces array list
	 * 
	 * @param pieces
	 *            ArrayList of piece objects
	 * @param torrent
	 *            TorrentInfo object for the active download
	 * @return
	 * @throws FileNotFoundException
	 */
	private void createPieces() throws FileNotFoundException {
		pieces = new ArrayList<Piece>();
		int leftover = torrent.file_length % torrent.piece_length;

		for (int i = 0; i < torrent.piece_hashes.length; i++) {
			if ((i == (torrent.piece_hashes.length - 1)) && leftover != 0) {
				pieces.add(new Piece(i, leftover, i * torrent.piece_length,
						file, torrent.piece_hashes[i].array()));
			} else {
				pieces.add(new Piece(i, torrent.piece_length, i
						* torrent.piece_length, file, torrent.piece_hashes[i]
						.array()));
			}
		}
	}

	/**
	 * Creates threads and connects to peers to begin uploading/downloading,
	 * sets {@link#active} to true;
	 * 
	 * @throws BtException
	 * @throws IOException
	 */
	public void start() throws BtException, IOException {
		if (!file.createNewFile()) {
			if (!file.exists()) {
				throw new BtException("Failed to create file");
			}
		}
		System.out.println("Communicating with tracker");
		// Send tracker started message
		communicationTracker.CommunicateWithTracker("started",
				getBytesCompleted());
		peers = communicationTracker.getPeersList();
		System.out.println("making threads");
		// create a new MessageHandler and thread for each peer
		int total = 0, connected = 0;
		
		for (Peer peer : peers) {
			if ( peer.getIP().equals("128.6.171.130")||peer.getIP().equals("128.6.171.131")) {
				Thread thread = new Thread(new MessageHandler(this, peer, peers));
				threads.add(thread);
				thread.start();
				System.out.println("ADDED PEER " + peer.getPeer_id());
				connected++;
			}
			total++;
		}
		System.err.println("Total: " + total + " connected: " + connected);

		// create listener for connecting peers

		// indicate this torrent is active
		System.out.println("starting main thread");
		worstPeer=new GetWorstPeer(peers, this);
		timer = new Timer();
		timer.schedule(worstPeer, 30000, 30000);
		status = Status.Active;
		
		/*
		 * Thread thread = new Thread(this); thread.start();
		 */
	}

	@Override
	public void run() {
		status = Status.Active;
		
		while (isActive()) {
			progressBar.setValue(getPercentComplete());
			if (checkCompleteness()) {
				status = Status.Seeding;
			} else {
				status = Status.Active;
			}
		}
		if (checkCompleteness()) {
			status = Status.Complete;
		} else {
			status = Status.Stopped;
		}
	}

	public void stop() throws BtException, InterruptedException {
		for (Peer peer : peers) {
			try {
				peer.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (Thread thread : threads) {
			thread.join();
		}
		threads.clear();

		communicationTracker.CommunicateWithTracker("stopped",
				getBytesCompleted());
	}

	/**
	 * Computes the percentage of this {@link ActiveTorrent} that has been
	 * completed
	 * 
	 * @return int percent download complete
	 */
	public int getPercentComplete() {
		int completed = 0;
		for (Piece curr : pieces) {
			if (curr.isComplete()) {
				completed++;
			}
		}
		return (int) (((float) completed / (float) pieces.size()) * 100);
	}

	/**
	 * calculates the number of bytes that have been completed so far
	 * 
	 * @return The number of bytes completed
	 */
	public int getBytesCompleted() {
		int completed = 0;
		for (Piece piece : pieces) {
			if (piece.isComplete()) {
				completed += piece.getSize();
			}
		}
		return completed;
	}

	/**
	 * Checks the completeness of the file by calling {@link Piece#isComplete()}
	 * for each {@link Piece}
	 * 
	 * @param pieces
	 *            ArrayList of pieces that make up the file
	 * @return True if all pieces are complete, otherwise false
	 */
	public boolean checkCompleteness() {
		for (Piece piece : pieces) {
			if (!piece.isComplete()) {
				return false;
			}
		}
		return true;
	}

	 
 	public ArrayList<Piece> getPieces() {
 		return pieces;
 	}
 
 	public TorrentInfo getTorrentInfo() {
 		return torrent;
 	}
 
 	public ByteBuffer getClientId() {
 		return communicationTracker.getClientID();
 	}
 
 	public synchronized int getUnchokedPeers() {
 		return unchoked_peers;
 	}
 
 	public synchronized void incrementUnchokedPeers() {
 		unchoked_peers++ ;
 	}
 
 	public synchronized void decrementUnchokedPeers() {
 		unchoked_peers--;
 	}
 
 	public synchronized void setUnchokedPeers(int unchoked_peers) {
 		this.unchoked_peers = unchoked_peers;
 	}
 	
	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public int getGuiIndex() {
		return gui_index;
	}

	public void setGuiIndex(int gui_index) {
		this.gui_index = gui_index;
	}

	public Status getStatus() {
		return status;
	}
}
