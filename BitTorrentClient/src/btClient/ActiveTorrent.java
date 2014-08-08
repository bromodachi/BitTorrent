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
	/**
	 * The {@link TorrentInfo} file that is associated with this
	 * {@link ActiveTorrent}
	 */
	private final TorrentInfo torrentInfo;
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
	 * All the {@link MessageHandler} objects related to this torrent
	 */
	private ArrayList<MessageHandler> message_handlers;
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
	private Status status;
	/**
	 * Number of peers connected to the client and related to this torrent that
	 * are unchoked
	 */
	private int unchoked_peer_count;

	private Timer timer;

	private ChokeHandler chokeHandler;

	/* =================== CONSTRUCTOR ========================== */
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
		this.torrentInfo = torrent;
		this.communicationTracker = new CommunicationTracker(torrent);
		this.file = file;
		peers = new ArrayList<Peer>();
		threads = new ArrayList<Thread>();
		message_handlers = new ArrayList<MessageHandler>();
		active = false;
		createPieces();
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setValue(getPercentComplete());
		progressBar.setVisible(true);
		gui_index = -1;
		status = Status.Stopped;
		unchoked_peer_count = 0;
	}

	/* =================== GETTERS ========================= */
	public int getNumPieces() {
		return pieces.size();
	}

	public String getFileName() {
		return file.getName();
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public int getGuiIndex() {
		return gui_index;
	}

	public Status getStatus() {
		return status;
	}

	public ArrayList<Peer> getPeerList() {
		return peers;
	}

	public int getUnchokedPeerCount() {
		return unchoked_peer_count;
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
	 * Checks if this torrent is currently uploading/downloading
	 * 
	 * @return True if torrent active, otherwise false
	 */
	public boolean isAlive() {
		for(Thread thread : threads){
			if(thread.isAlive()){
				System.err.println(thread.getName() + " is Alive");
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the completeness of the file by calling {@link Piece#isComplete()}
	 * for each {@link Piece}
	 * 
	 * @param pieces
	 *            ArrayList of pieces that make up the file
	 * @return True if all pieces are complete, otherwise false
	 */
	public boolean isComplete() {
		for (Piece piece : pieces) {
			if (!piece.isComplete()) {
				return false;
			}
		}
		return true;
	}

	/* ==================== SETTERS ======================== */
	public void setGuiIndex(int gui_index) {
		this.gui_index = gui_index;
	}

	public synchronized void incrementUnchokedPeerCount() {
		unchoked_peer_count++;
	}

	public synchronized void decrementUnchokedPeerCount() {
		unchoked_peer_count--;
	}

	public synchronized void setUnchokedPeerCount(int unchoked_peer_count) {
		this.unchoked_peer_count = unchoked_peer_count;
	}

	/* ======================== METHODS ========================= */

	/**
	 * Creates threads and connects to peers to begin uploading/downloading,
	 * sets {@link#active} to true;
	 * 
	 * @throws BtException
	 * @throws IOException
	 */
	public void start() throws BtException, IOException {
		unchoked_peer_count = 0;
		// Verify file exists or create a new one
		if (!file.createNewFile()) {
			if (!file.exists()) {
				throw new BtException("Failed to create file");
			}
		}

		// Send tracker started message
		communicationTracker.CommunicateWithTracker("started",
				getBytesCompleted());
		peers = communicationTracker.getPeersList();

		// create a new MessageHandler and thread for each peer
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getIP().equals("128.6.171.130")
						|| peer.getIP().equals("128.6.171.130")) {
					// Create MessageHandler and add to message_handlers array
					// list
					MessageHandler messageHandler = new MessageHandler(this, peer);
					message_handlers.add(messageHandler);
					// create thread from message handler and add it to the
					// array
					// list
					Thread thread = new Thread(messageHandler);
					threads.add(thread);
					thread.start();
				}
			}
		}
		// setup timer tasks
		chokeHandler = new ChokeHandler(this);
		timer = new Timer();

		// create listener for connecting peers

		 //Thread thread = new Thread(this); thread.start();
		 
	}

	@Override
	public void run() {
		try {
			start();
		} catch (IOException | BtException e1) {
			e1.printStackTrace();
		}
		// start timer tasks
		timer.schedule(chokeHandler, BtUtils.CHOKE_INTERVAL, BtUtils.CHOKE_INTERVAL);
		updateStatus();
		while (status == Status.Seeding || status == Status.Active) {
			progressBar.setValue(getPercentComplete());
			updateStatus();
			System.err.println("Current number of unchoked peers" + getUnchokedPeerCount());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		updateStatus();
		
		
		//clear timer tasks
		timer.cancel();
	}

	public void stop() throws BtException, InterruptedException {
		for (MessageHandler handler : message_handlers) {
			handler.kill();
		}
		for (Thread thread : threads) {
			thread.join();
			System.out.println(thread.getName() + "joined");
		}
		threads.clear();

		communicationTracker.CommunicateWithTracker("stopped",
				getBytesCompleted());
		System.err.println("Should be dead");
		updateStatus();
	}

	/**
	 * Creates a new piece object for the total number of pieces in the file to
	 * be downloaded and adds them to the pieces array list
	 * 
	 * @param pieces
	 *            ArrayList of piece objects
	 * @param torrentInfo
	 *            TorrentInfo object for the active download
	 * @return
	 * @throws FileNotFoundException
	 */
	private void createPieces() throws FileNotFoundException {
		pieces = new ArrayList<Piece>();
		int leftover = torrentInfo.file_length % torrentInfo.piece_length;

		for (int i = 0; i < torrentInfo.piece_hashes.length; i++) {
			if ((i == (torrentInfo.piece_hashes.length - 1)) && leftover != 0) {
				pieces.add(new Piece(i, leftover, i * torrentInfo.piece_length,
						file, torrentInfo.piece_hashes[i].array()));
			} else {
				pieces.add(new Piece(i, torrentInfo.piece_length, i
						* torrentInfo.piece_length, file, torrentInfo.piece_hashes[i]
						.array()));
			}
		}
	}
	
	private Status updateStatus(){
		if(!isAlive()){
			if(isComplete()){
				status = Status.Complete;
			}else{
				status = Status.Stopped;
			}
		}else{
			if(isComplete()){
				status = Status.Seeding;
			}else{
				status = Status.Active;
			}
		}
		return status;
	}

	public ArrayList<Piece> getPieces() {
		return pieces;
	}

	public TorrentInfo getTorrentInfo() {
		return torrentInfo;
	}

	public ByteBuffer getClientID() {
		return communicationTracker.getClientID();
	}

}
