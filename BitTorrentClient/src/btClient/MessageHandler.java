/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import btClient.BtUtils.Status;

/**
 * This class is tasked with deciding which piece to download next and handling
 * the messages received from the peer. This class also handles the logic for
 * deciding when to send a message to the peer and which message to send (the
 * actual sending/receiving is handled by the peer class)
 * 
 * 
 */
public class MessageHandler implements Runnable {
	/**
	 * An array list of {@link Piece} objects that make up the file to be
	 * downloaded
	 */
	private ArrayList<Piece> pieces;
	/**
	 * The {@link Peer} that this MessageHandler is responsible for handling
	 */
	private final Peer peer;
	/**
	 * The {@link TorrentInfo#info_hash} for the current torrent file
	 */
	private final ByteBuffer info_hash;
	/**
	 * The local peer_ID for this client
	 */
	private final ByteBuffer clientID;
	/**
	 * Indicates whether or not the client is choked by the {@link Peer} that
	 * this MessageHandler is responsible for
	 */
	private boolean choked;
	/**
	 * All general info relating to this torrent
	 * 
	 * @see TorrentInfo
	 */
	private TorrentInfo torrentInfo;
	/**
	 * The {@link ActiveTorrent} object responsible for managing this
	 * {@link MessageHandler}
	 */
	private ActiveTorrent torrent;
	/**
	 * The number of downloaded bytes that this MessageHandler has discarded due
	 * to errors or other issues
	 */
	private int wasted;
	/**
	 * The current piece to be downloaded
	 */
	private Piece piece;
	/**
	 * Current block to be requested
	 */
	private Block block;
	/**
	 * boolean array to indicate which pieces the client has
	 */
	private boolean[] has_piece;

	private Status status;
	/**
	 * If true, indicates that this thread has been requested to be killed by
	 * the GUI
	 */
	private boolean killme;

	/**
	 * Returns a new message handler object, created with the given parameters
	 * 
	 * @param {@link#pieces}
	 * @param {@link#peer}
	 * @param {@link#info_hash}
	 * @param {@link#clientID}
	 * @param {@link#torr}
	 */
	public MessageHandler(ActiveTorrent torrent, Peer peer) {
		this.torrent = torrent;
		this.torrentInfo = torrent.getTorrentInfo();
		this.pieces = torrent.getPieces();
		this.peer = peer;
		this.info_hash = torrentInfo.info_hash;
		this.clientID = torrent.getClientID();
		choked = true;
		wasted = 0;
		// initialize peer has_piece array
		peer.setHasPieces(new boolean[pieces.size()]);
		for (int i = 0; i < pieces.size(); i++) {
			peer.setHasPiece(i, false);
		}
		// initialize client has_piece array
		has_piece = new boolean[pieces.size()];
		for (Piece piece : pieces) {
			if (piece.isComplete()) {
				has_piece[piece.getIndex()] = true;
			} else {
				has_piece[piece.getIndex()] = false;
			}
		}
		status = Status.Active;
	}

	@Override
	public void run() {
		try {
			connect();
		} catch (IOException e2) {
			return;
		}
		// While connected : communication loop
		while (peer.isConnected() && !peer.isClosed()) {
			if (killme) {
				peer.disconnect();
				return;
			}
			updateHasPiece();
			if (!choked) {
				// if piece is completed set it to null to get next piece
				if (piece != null) {
					if (piece.isComplete()) {
						piece = null;
					}
				}
				/*
				 * If the is no active piece try to get one If block is null get
				 * next block get the next block to download
				 */
				if (piece == null && status == Status.Active) {
					piece = getNextPiece();
					if (piece == null) {
						/*
						 * if the peer has no pieces we want and we are not
						 * seeding then make sure peer is choked
						 */
						if (!peer.isChoked()) {
							try {
								peer.sendUninterested();
								peer.sendChoke();
								peer.setChoked(true);
								peer.setInteresting(false);
								torrent.decrementUnchokedPeerCount();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				// if piece is not null then the peer has a piece we are interested in
				if (piece != null) {
					// get next non-downloaded block of the piece
					block = piece.getNextBlock();
					// if block is null then the piece is complete
					if (block == null) {
						piece.checkComplete();
					}
				}
				// Only attempt request if block is not null
				if (block != null) {
					try {
						peer.sendRequest(block);
					} catch (IOException e) {
						// do nothing just move on
					}
					block = null;
				}
			}

			// check if any pieces have been completed by other threads
			try {
				handleMessage(peer.getMessage());
			} catch (EOFException e) {
				peer.disconnect();
				return;
			} catch (IOException | InterruptedException | BtException e) {
				// This can occur on purpose when GUI forces all peers to disconnect
				return;
			}

		}
		// make sure all streams are closed
		peer.disconnect();
		// decrement peer counters to update rarity of pieces
		peer.decrementPeerCounters(pieces);
	}

	/**
	 * Identifies the given message and calls the appropriate method to handle
	 * it
	 * 
	 * @param message
	 *            The message to be handled
	 * @throws IOException
	 * @throws BtException
	 */
	private void handleMessage(byte[] message) throws IOException, BtException {
		if (message == null) {
			return;
		}
		if (message.length == 0) {
			// Keep alive message
			return;
		}
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			receiveChoke(message);
			break;
		case BtUtils.UNCHOKE_ID:
			receiveUnchoke(message);
			break;
		case BtUtils.INTERESTED_ID:
			receiveInterested(message);
			break;
		case BtUtils.UNINTERESTED_ID:
			receiveUninterested(message);
			break;
		case BtUtils.HAVE_ID:
			receiveHave(message);
			break;
		case BtUtils.BITFIELD_ID:
			receiveBitfield(message);
			break;
		case BtUtils.REQUEST_ID:
			receiveRequest(message);
			break;
		case BtUtils.PIECE_ID:
			receivePiece(message);
			break;

		default:
			wasted += message.length;
		}
	}

	/* ================ RECEIVE MESSAGE METHODS ================== */
	/**
	 * Handles an incoming unchoke message, sets the client choke flag to false.
	 * Given its own method in case the need arises to add more functionality.
	 * 
	 * @param message
	 */
	private void receiveUnchoke(byte[] message) {
		choked = false;
	}

	/**
	 * Handles an incoming choke message, sets the client choke flag to true.
	 * Given its own method in case the need arises to add more functionality.
	 * 
	 * @param message
	 */
	private void receiveChoke(byte[] message) {
		choked = true;
	}

	/**
	 * Handles an incoming uninterested message form the peer, simply sets the
	 * peer interested flag to false. Given its own method in case the need
	 * arises to add more functionality
	 * 
	 * @param message
	 */
	private void receiveUninterested(byte[] message) {
		peer.setInterested(false);
	}

	/**
	 * Handles an incoming interested message, sets the peer's interested field
	 * to true. Given its own method in case the need arises to add more
	 * functionality.
	 */
	private void receiveInterested(byte[] message) {
		peer.setInterested(true);
	}

	/**
	 * Reads a bitfield message and sets the boolean values of peer_has_piece
	 * accordingly.
	 * 
	 * @param message
	 *            byte array representing the bitfield message from the peer
	 * @throws IOException
	 */
	private void receiveBitfield(byte[] message) throws IOException {
		// remove message id from message
		ByteBuffer bytes = ByteBuffer.wrap(new byte[message.length - 1]);
		bytes.put(message, 1, message.length - 1);
		peer.setHasPieces(Peer.ConvertBitfieldToArray(bytes.array(), pieces.size()));
		peer.incrementPeerCounters(pieces);
		for (int i = 0; i < has_piece.length; i++) {
			if (!has_piece[i] && peer.has_piece(i)) {
				peer.sendInterested();
				peer.setInteresting(false);
				return;
			}
		}
		peer.sendUninterested();
	}

	/**
	 * Handles an incoming piece message from the peer. Checks and validates the
	 * piece and writes it to the file
	 * 
	 * @param message
	 *            the byte array message from the peer
	 * @throws IOException
	 * @throws BtException
	 */
	private void receivePiece(byte[] message) throws IOException, BtException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		Piece currPiece = pieces.get(parser.getInt(BtUtils.REQUEST_INDEX));

		currPiece.writeBlock(message);

		// Add number of bytes to peer's uploaded counter
		peer.uploaded(message.length - (BtUtils.PIECE_HEADER_SIZE + BtUtils.PREFIX_LENGTH));

		/* Check for piece completeness and hash correctness */
		currPiece.checkComplete();
		if (currPiece.isComplete()) {
			if (!currPiece.checkHash(torrentInfo.piece_hashes[currPiece.getIndex()].array())) {
				// If hash mismatch, clear piece and try again
				currPiece.clearBlocks();
				currPiece.incrementAttempts();
				// If piece has reach the max attempts print error and return
				if (currPiece.getDownloadAttempts() >= BtUtils.MAX_DOWNLOAD_ATTEMPTS) {
					System.err.println("ERROR: Max download attempts reached, hash mismatch for piece #" + piece.getIndex());
					return;
				}
			}
		}
	}

	/**
	 * Handles an incoming request message, sends the request piece to the peer
	 * if the piece is available and the peer is not choked
	 * 
	 * @param message
	 *            A request message from the peer
	 * @throws IOException
	 */
	private void receiveRequest(byte[] message) throws IOException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		// Check for request errors
		if (parser.getInt(BtUtils.REQUEST_INDEX) < 0 || parser.getInt(BtUtils.REQUEST_INDEX) >= pieces.size()) {
			System.err.println(Thread.currentThread().getName() + " ERROR: Peer Requested invalid piece disconnecting...");
			peer.disconnect();
			return;
		}
		if (!has_piece[parser.getInt(BtUtils.REQUEST_INDEX)]) {
			System.err.println(Thread.currentThread().getName() + " ERROR: Peer requested piece that client does not have");
			return;
		}
		if (peer.isChoked()) {
			System.err.println(Thread.currentThread().getName() + " ERROR: choked peer requested piece");
			return;
		}
		peer.sendPiece(pieces.get(parser.getInt(BtUtils.REQUEST_INDEX)), parser.getInt(BtUtils.REQUEST_OFFSET), BtUtils.REQUEST_SIZE);

	}

	/**
	 * Handles an incoming have message, sends an interested request to the peer
	 * if client doesn't have the piece indicated in the message
	 * 
	 * @param message
	 *            A have message from the peer
	 * @throws IOException
	 */
	private void receiveHave(byte[] message) throws IOException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		int index = parser.getInt(1);
		// check if have message is for a valid piece
		if (index >= 0 && index < pieces.size()) {
			if (!pieces.get(index).isComplete()) {
				peer.sendInterested();
				peer.setInteresting(true);
				peer.setHasPiece(index);
				pieces.get(index).incrementPeerCount();
			}
		} else {
			peer.disconnect();
			wasted += message.length;
		}
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded;
	 * Implements Rarest-Piece-First Algorithm
	 * 
	 * @return the next piece to download
	 */
	private Piece getNextPiece() {
		int rarity = 0;
		ArrayList<Piece> available = new ArrayList<Piece>();
		// check rarity of each piece
		for (Piece piece : pieces) {
			// if rarity is still 0 set rarity to the current piece's peer count
			if (rarity == 0 && peer.has_piece(piece.getIndex()) && !piece.isComplete()) {
				rarity = piece.getPeerCount();
			}
			/*
			 * if peer has current piece and it is more rare, set rarity to that
			 * of the current piece
			 */
			if (piece.getPeerCount() < rarity && peer.has_piece(piece.getIndex()) && !piece.isComplete()) {
				rarity = piece.getPeerCount();
				// if rarity has been lowered clear array list of any pieces
				available.clear();
			}
			// if current piece matches rarity then add it to array list
			if (piece.getPeerCount() == rarity && !piece.isComplete() && peer.has_piece(piece.getIndex())) {
				available.add(piece);
			}
		}
		// if rarity == 0 then no peers have any pieces, if size == 0 connected peer doesn't have pieces we need
		if (rarity == 0 || available.size() == 0) {
			return null;
		}
		// generate random number for tie breaker
		Random rand = new Random();
		return available.get(rand.nextInt(available.size()));
	}

	/**
	 * updates the has_piece boolean array for this thread, and checks
	 * completeness / updates complete
	 */
	private void updateHasPiece() {
		// keep complete set to true if all pieces are complete
		status = Status.Seeding;
		for (Piece piece : pieces) {
			// If there are any incomplete pieces set status to active
			if (!piece.isComplete()) {
				status = Status.Active;
			}
			// If a piece has been completed send have message to peer then update has_piece array
			if (piece.isComplete() && !has_piece[piece.getIndex()]) {
				try {
					peer.sendHave(piece.getIndex());
				} catch (IOException e) {
					// if sendHave fails don't update array so it will be resent on next pass
					continue;
				}
				has_piece[piece.getIndex()] = true;
			}
		}
	}

	/**
	 * Attempts to connect to the given peer, if connection is successful or if
	 * the peer is already connected, send appropriate have messages to the peer
	 * 
	 * @throws IOException
	 */
	private void connect() throws IOException {
		// make sure peer is not null (shouldn't happen but just in case)
		if (peer == null) {
			return;
		}
		// If not already connected, attempt to establish connection with the peer
		if (!peer.isConnected()) {
			if (!peer.establishConnection(info_hash, clientID)) {
				System.err.println(Thread.currentThread().getName() + "ERROR: Failed to establish connection with peer " + peer.getPeer_id());
				return;
			}
		}

		// send have messages to peer
		for (Piece piece : pieces) {
			if (piece.isComplete()) {
				peer.sendHave(piece.getIndex());
			}
		}
	}

	/**
	 * Goes to the bar and drinks heavily
	 * 
	 * @return The number of downloaded bytes that were discarded or ignored by
	 *         this MessageHandler
	 */
	public int getWasted() {
		return wasted;
	}

	/**
	 * Sets the killme field to true to indicate this MessageHandler should die
	 * when possible
	 */
	public void kill() {
		killme = true;
	}

	/**
	 * Gets the current status of this {@link MessageHandler}
	 * 
	 * @return
	 */
	public Status getStatus() {
		return status;
	}
}
