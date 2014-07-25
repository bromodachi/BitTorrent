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

	private ArrayList<Piece> pieces;
	private final Peer peer;
	private final ByteBuffer info_hash;
	private final ByteBuffer clientID;
	private boolean[] peer_has_piece;
	private boolean choked;
	private TorrentInfo torrent;
	private Block currBlock = null;

	/**
	 * Returns a new message handler object, created with the given parameters
	 * 
	 * @param pieces
	 *            ArrayList of pieces to be downloaded for the file
	 * @param peer
	 *            The peer for which this MessageHandler is responsible for
	 *            communicating with
	 * @param info_hash
	 *            The info_hash given in the torrent file
	 * @param clientID
	 *            The local peer_id
	 * @param torr
	 *            The relevent TorrentInfo object for this download
	 */
	public MessageHandler(ArrayList<Piece> pieces, Peer peer,
			ByteBuffer info_hash, ByteBuffer clientID, TorrentInfo torr) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
		this.peer_has_piece = new boolean[pieces.size()];
		for (int i = 0; i < peer_has_piece.length; i++) {
			peer_has_piece[i] = false;
		}
		choked = true;
		this.torrent = torr;
	}

	/**
	 * 
	 */
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
					return;
				}
			}
			while (!choked) {
				if (currBlock == null) {
					Piece piece = getNextPiece();
					if (piece == null) {
						// no more pieces
						try {
							peer.disconnect();
						} catch (IOException e) {
							System.err
									.println("An error has encountered. Exiting...");
							return;
						}
						return;
					}
					// get the next block to download
					currBlock = piece.getNextBlock();
					if (currBlock == null) {
						continue;
					}
				}

				try {
					peer.sendRequest(currBlock);
				} catch (IOException e) {
					System.err.println("An error has encountered. Exiting...");

					return;
				}
				try {
					handleMessage(peer.getMessage());
				} catch (IOException e) {
					System.err.println("An error has encountered. Exiting...");
					return;
				} catch (BtException e) {
					System.err.println("An error has encountered. Exiting...");
					return;
				}

			}
			peer.closeEverything();
			return;
		}
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded
	 * 
	 * @return the next piece to download
	 */
	private Piece getNextPiece() {
		for (Piece piece : pieces) {
			if (!piece.isComplete() && peer_has_piece[piece.getIndex()]) {
				return piece;
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
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			choked = true;
			if (currBlock != null) {
				currBlock.unlock();
				currBlock = null;
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
			if (currBlock == null) {
				System.err.println("Received unexpected block");
				return;
			}
			ByteBuffer parser = ByteBuffer.wrap(message, 1, message.length - 1);
			Piece piece = pieces.get(parser.getInt());
			// Check block offset, size, and piece index to ensure correct block
			// was received
			parser.get();
			/*
			 * if ((currBlock.getOffset() != parser.getInt()) ||
			 * (piece.getIndex() != currBlock.getPieceIndex())) {
			 * System.err.println("received invalid block:");
			 * System.err.println("Requested: Piece: " +
			 * currBlock.getPieceIndex() + " offset: " + currBlock.getOffset() +
			 * " size: " + currBlock.getSize());
			 * System.err.println("Received: Piece:" + parser.getInt(1) +
			 * " offset: " + parser.getInt(5) + " size: " + parser.remaining());
			 * return; }
			 */
			piece.writeBlock(message);
			/*System.out.println("wrote Piece:" + currBlock.getPieceIndex()
					+ " block:" + currBlock.getIndex());*/
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
			}
			currBlock.unlock();
			currBlock = null;
			break;

		default:
			System.err.println("ERROR: received invalid message from peer");
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
		boolean[] bitSet = ConvertBitfieldToArray(bytes.array(), pieces.size());
		for (int i = 0; i < peer_has_piece.length; i++) {

			peer_has_piece[i] = bitSet[i];
		}
	}

	/**
	 * Converts the bytes of a bitfield to a boolean array. If we get a 1 the
	 * peer has the piece.
	 * 
	 * @param bitfield
	 *            bitfield message received from the peer
	 * @param numPieces
	 *            number of pieces to the downloading file
	 * @return boolean array of boolean values representing the peers possesion
	 *         of the peice corresponding to the array's index
	 */
	public boolean[] ConvertBitfieldToArray(byte[] bitfield, int numPieces) {
		boolean[] bool = new boolean[numPieces];
		for (int i = 0; i < bool.length; i++) {
			// from the java docs of a bitset : ((bb.get(bb.position()+n/8) &
			// (1<<(n%8))) != 0)
			// uses bit wise &:
			if (((bitfield[i / 8] >> (7 - i % 8) & 1) == 1)) {
				bool[i] = true;
			}
			// don't need else, default value is false.
		}
		return bool;
	}
}
