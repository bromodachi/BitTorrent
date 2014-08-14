package btClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;

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

	/**
	 * The port that this active torrent listens for new connections on
	 */
	//private int server_port;
	/**
	 * 
	 */
	//private PeerConnectionListener peerConnectionListener = null;

	/* =================== CONSTRUCTOR ========================== */
	/**
	 * Creates a new ActiveTorrent Object for the given torrent file
	 * 
	 * @param torrentInfo
	 *            Torrent file with tracker and torrent information
	 * @param file
	 *            The save file location for the downloaded file associated with
	 *            this torrent
	 * @throws FileNotFoundException
	 */
	public ActiveTorrent(TorrentInfo torrentInfo, File file, int listeningPort) throws FileNotFoundException {
		super();
		this.torrentInfo = torrentInfo;
		this.communicationTracker = new CommunicationTracker(torrentInfo);
		this.file = file;
		createPieces();
		gui_index = -1;
		unchoked_peer_count = 0;
		updateStatus();
		//this.server_port = listeningPort;
	}

	/* =================== GETTERS ========================= */
	/**
	 * Gets the number of pieces that this torrent has
	 * 
	 * @return integer number of pieces
	 */
	public int getNumPieces() {
		return pieces.size();
	}

	/**
	 * Gets the name of the torrent's save file
	 * 
	 * @return file name
	 */
	public String getFileName() {
		return file.getName();
	}

	/**
	 * 
	 * @return The integer index of this active torrent in the GUI torrent table
	 */
	public int getGuiIndex() {
		return gui_index;
	}

	/**
	 * Gets the current status of this {@link ActiveTorrent}
	 * 
	 * @return a {@link Status} enum
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Returns a list of peers associated with this ActiveTorrent
	 * 
	 * @return ArrayList of {@link Peer} objects
	 */
	public ArrayList<Peer> getPeerList() {
		return peers;
	}

	/**
	 * Gets the current number of unchoked {@link Peer} objects connected to the
	 * client and associated with this {@link ActiveTorrent}
	 * 
	 * @return integer number of unchoked peers
	 */
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

	public ArrayList<Piece> getPieces() {
		return pieces;
	}

	public TorrentInfo getTorrentInfo() {
		return torrentInfo;
	}

	public ByteBuffer getClientID() {
		return communicationTracker.getClientID();
	}

	public CommunicationTracker getCommunicarionTracker() {
		return communicationTracker;
	}

	/**
	 * Checks if this torrent is currently uploading/downloading
	 * 
	 * @return True if torrent active, otherwise false
	 */
	public boolean isAlive() {
		boolean isalive = false;
		if (threads == null) {
			return false;
		}
		synchronized (threads) {
			for (Thread thread : threads) {
				if (thread.isAlive()) {
					isalive = true;
				}
			}
		}
		return isalive;
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

	/* ========================== METHODS =========================== */

	/**
	 * Initializes and runs this {@link ActiveTorrent} object. Sends "started"
	 * message to the tracker. Gets the peer list from the
	 * {@link CommuncationTracker}. Creates and runs a new
	 * {@link MessageHandler} thread for each {@link Peer}. Creates a new
	 * {@link Timer}. Creates and schedules timer tasks for the
	 * {@link ActiveTorrent}: {@link ChokeHandler} timer task {@link KeepAlive}
	 * timer task. Creates and runs a {@link PeerConnectionListener} thread.
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
		communicationTracker.CommunicateWithTracker("started", getBytesCompleted());
		// Initialize array lists
		peers = communicationTracker.getPeersList();
		threads = new ArrayList<Thread>(peers.size());
		message_handlers = new ArrayList<MessageHandler>(peers.size());

		// create a new MessageHandler and thread for each peer
		synchronized (peers) {
			for (Peer peer : peers) {
				// Connect only to the specified IP addresses as for the assignment specification
				if (peer.getIP().equals("128.6.171.130") || peer.getIP().equals("128.6.171.131")) {

					// Create MessageHandler and add to message_handlers array list
					MessageHandler messageHandler = new MessageHandler(this, peer);
					synchronized (message_handlers) {
						message_handlers.add(messageHandler);
					}

					// create thread from message handler and add it to the array list
					synchronized (threads) {
						Thread thread = new Thread(messageHandler);
						threads.add(thread);
						thread.start();
					}
				}
			}
		}
		// Create connection listener thread (not fully working yet)
		/*
		 * peerConnectionListener = new PeerConnectionListener(this,
		 * server_port); new Thread(peerConnectionListener).start();
		 */

		// create and start timer tasks
		timer = new Timer();
		timer.schedule(new ChokeHandler(this), 5000, BtUtils.CHOKE_INTERVAL);
		timer.schedule(new KeepAlive(this), BtUtils.KEEP_ALIVE_INTERVAL, BtUtils.KEEP_ALIVE_INTERVAL);
		if (communicationTracker.getMinInterval() != 0) {
			timer.schedule(new UpdateTracker(this), 10000, communicationTracker.getMinInterval() * 1000);
		} else if ((communicationTracker.getInterval() / 2) > BtUtils.MAX_UPDATE_INTERVAL) {
			timer.schedule(new UpdateTracker(this), BtUtils.MAX_UPDATE_INTERVAL * 1000, BtUtils.MAX_UPDATE_INTERVAL * 1000);
		} else {
			timer.schedule(new UpdateTracker(this), communicationTracker.getInterval() * 1000, communicationTracker.getInterval() * 1000);
		}

	}

	/**
	 * Calls {@link ActiveTorrent#start()} then continuously updates this
	 * {@link ActiveTorrent}'s status until it is stopped. Cancels all future
	 * scheduled tasks when stopped.
	 */
	@Override
	public void run() {
		try {
			start();
		} catch (IOException | BtException e1) {
			e1.printStackTrace();
		}

		updateStatus();
		while (status == Status.Seeding || status == Status.Active) {
			updateStatus();
		}
		updateStatus();

		// clear timer tasks
		timer.cancel();
		// Kill peerConnectionListener
		//peerConnectionListener.setListening(false);
	}

	/**
	 * Stops the {@link ActiveTorrent} by calling {@link MessageHandler#kill()}
	 * for each {@link MessageHandler} associated with this torrent. Also
	 * disconnects all associated {@link Peer} objects by calling
	 * {@link Peer#disconnect()} and joins all MessageHandler threads then sends
	 * a stopped message to the tracker and updates that ActiveTorrent's status.
	 * 
	 * @throws BtException
	 * @throws InterruptedException
	 */
	public void stop() throws BtException, InterruptedException {
		// Kill MessageHandlers
		if (message_handlers != null) {
			synchronized (message_handlers) {
				for (MessageHandler handler : message_handlers) {
					handler.kill();
				}
				message_handlers = null;
			}
		}
		// Disconnect Peers
		if (peers != null) {
			synchronized (peers) {
				for (Peer peer : peers) {
					peer.disconnect();
				}
				peers = null;
			}
		}
		// Join threads
		if (threads != null) {
			synchronized (threads) {
				for (Thread thread : threads) {
					thread.join();
				}
				threads = null;
			}
		}
		// Send "stopped" to tracker
		communicationTracker.CommunicateWithTracker("stopped", getBytesCompleted());
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
	 * @throws FileNotFoundException
	 */
	private void createPieces() throws FileNotFoundException {
		pieces = new ArrayList<Piece>();
		int leftover = torrentInfo.file_length % torrentInfo.piece_length;

		for (int i = 0; i < torrentInfo.piece_hashes.length; i++) {
			if ((i == (torrentInfo.piece_hashes.length - 1)) && leftover != 0) {
				pieces.add(new Piece(i, leftover, i * torrentInfo.piece_length, file, torrentInfo.piece_hashes[i].array()));
			} else {
				pieces.add(new Piece(i, torrentInfo.piece_length, i * torrentInfo.piece_length, file, torrentInfo.piece_hashes[i].array()));
			}
		}
	}

	/**
	 * Updates the status of this ActiveTorrent
	 * 
	 * @return
	 */
	private Status updateStatus() {
		if (!isAlive()) {
			if (isComplete()) {
				status = Status.Complete;
			} else {
				status = Status.Stopped;
			}
		} else {
			if (isComplete()) {
				status = Status.Seeding;
			} else {
				status = Status.Active;
			}
		}
		return status;
	}

	/**
	 * Adds a new peer to the peer list in a synchronized manner
	 * 
	 * @param peer
	 *            new peer to be added
	 */
	public synchronized void addPeerToList(Peer peer) {
		System.err.println("added peer " + peer.getPeer_id());
		MessageHandler handler = new MessageHandler(this, peer);
		synchronized (message_handlers) {
			message_handlers.add(handler);
		}
		synchronized (threads) {
			Thread thread = new Thread(handler);
			threads.add(thread);
			thread.start();
		}
		synchronized (peers) {
			this.peers.add(peer);
		}
	}

}
