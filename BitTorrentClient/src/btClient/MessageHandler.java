/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
	private TorrentInfo torrent;
	/**
	 * The current {@link Piece} that this MessageHandler is downloading from
	 * the peer
	 */
	private Piece piece = null;
	/**
	 * The number of downloaded bytes that this MessageHandler has discarded due
	 * to errors or other issues
	 */
	private int wasted;

	/**
	 * Returns a new message handler object, created with the given parameters
	 * 
	 * @param {@link#pieces}
	 * @param {@link#peer}
	 * @param {@link#info_hash}
	 * @param {@link#clientID}
	 * @param {@link#torr}
	 */
	public MessageHandler(ArrayList<Piece> pieces, Peer peer,
			ByteBuffer info_hash, ByteBuffer clientID, TorrentInfo torr) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
		choked = true;
		this.torrent = torr;
	}

	// blah
	@Override
	public void run() {
		if (peer == null) {
			System.err.println("recieved null peer");
			return;
		} else {
			System.out.println("recieved peer: " + peer.getPeer_id());
		}
		try {
			if (!peer.establishConnection(info_hash, clientID)) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			return;
		}
		// need to add send bitfield
		while (peer.isConnected()) {
			while (choked) {
				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					System.err.println("An error has encountered. Exiting...");
					peer.decrementPeerCounters(pieces);
					return;
				}
			}
			while (!choked) {
				piece = getNextPiece();
				if (piece != null) {
					// get the next block to download
					Block block = piece.getNextBlock();
					// should not happen
					if (block == null) {
						System.err.println("set null block");
						continue;
					}

					try {
						peer.sendRequest(block);
					} catch (IOException e) {
						System.err
								.println("An error has encountered. Exiting...");
						piece.unlock();
						peer.decrementPeerCounters(pieces);
						return;
					}
				}

				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					System.err.println("Fatal error.... disconnecting");
					if (piece != null) {
						piece.unlock();
					}
					peer.decrementPeerCounters(pieces);
					return;
				}

			}
			if (piece != null) {
				piece.unlock();
				piece = null;
			}
			peer.decrementPeerCounters(pieces);
			return;
		}
		peer.closeEverything();
		peer.decrementPeerCounters(pieces);
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded
	 * 
	 * @return the next piece to download
	 */
	private Piece getNextPiece() {
		for (Piece piece : pieces) {
			if (!piece.isComplete() && peer.has_piece(piece.getIndex())) {
				// attempt to acquire lock on piece
				if (piece.tryLock()) {
					return piece;
				}
			}
		}
		return null;
	}

	/**
	 * Identifies the given message and decides how it should be handled
	 * 
	 * @param message
	 *            The message to be handled
	 * @throws IOException
	 * @throws BtException
	 */
	private void handleMessage(byte[] message) throws IOException, BtException {
		ByteBuffer parser;
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			choked = true;
			if (piece != null) {
				piece.unlock();
			}
			break;
		case BtUtils.UNCHOKE_ID:
			choked = false;
			break;
		case BtUtils.INTERESTED_ID:
			break;
		case BtUtils.UNINTERESTED_ID:
			break;
		case BtUtils.HAVE_ID:
			parser = ByteBuffer.wrap(message);
			int index = parser.getInt(1);
			if (index >= 0 && index < pieces.size()) {
				peer.setHasPiece(index);
			} else {
				System.err
						.println("Received have message with invalid piece index");
				wasted += message.length;
			}
			break;
		case BtUtils.BITFIELD_ID:
			bitField(message);
			try {
				peer.sendInterested();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case BtUtils.REQUEST_ID:
			break;
		case BtUtils.PIECE_ID:
			if(piece == null){
				System.err.println("ERROR: Received unexpected piece message");
				wasted += message.length;
				return;
			}
			parser = ByteBuffer.wrap(message, 1, message.length - 1);
			if (piece.getIndex() != parser.getInt()) {
				System.err.println("ERROR: Received non-requested piece");
				wasted += message.length;
				return;
			}
			piece.writeBlock(message);
			peer.downloaded(message.length
					- (BtUtils.PIECE_HEADER_SIZE + BtUtils.PREFIX_LENGTH));
			/*
			 * System.out.println("wrote Piece:" + currBlock.getPieceIndex() +
			 * " block:" + currBlock.getIndex());
			 */
			if (piece.isComplete()) {
				// check that piece has downloaded correctly (check for correct
				// hash)
				if (!piece.checkHash(torrent.piece_hashes[piece.getIndex()]
						.array())) {
					// If hash mismatch, clear piece and try again
					piece.clearBlocks();
					piece.incrementAttempts();
					// If piece has reach the max attempts print error and
					// return
					if (piece.getDownloadAttempts() >= BtUtils.MAX_DOWNLOAD_ATTEMPTS) {
						System.err
								.println("ERROR: Max download attempts reached, hash mismatch for piece #"
										+ piece.getIndex());
						return;
					}
				} else {
					peer.sendHave(piece.getIndex());
				}
				piece.unlock();
				piece = null;
			}

			break;

		default:
			System.err.println("ERROR: received invalid message from peer");
			wasted += message.length;
		}
	}

	/**
	 * Reads a bitfield message and sets the boolean values of peer_has_piece
	 * accordingly
	 * 
	 * @param message
	 */
	private void bitField(byte[] message) {
		// remove message id from message
		ByteBuffer bytes = ByteBuffer.wrap(new byte[message.length - 1]);
		bytes.put(message, 1, message.length - 1);
		peer.setHasPieces(Peer.ConvertBitfieldToArray(bytes.array(),
				pieces.size()));
		peer.incrementPeerCounters(pieces);
	}
}
