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
import java.util.Comparator;
import java.util.PriorityQueue;

import btClient.sendMessages.sendHave;
import btClient.sendMessages.sendInterested;
import btClient.sendMessages.sendBlock;
 

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
	 * Array list of peers
	 */
	private ArrayList<Peer> peers;
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
			ByteBuffer info_hash, ByteBuffer clientID, TorrentInfo torr, ArrayList<Peer> peers) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
		choked = true;
		this.torrent = torr;
		wasted = 0;
		this.peers=peers;
	}

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
			System.err.println(e.getMessage());
			return;
		}
		// need to add send bitfield
		mainLoop:
		while (peer.isConnected()) {
			while (choked) {
				System.out.println("choked " + Thread.currentThread().getId());
				if(checkCompleteness()){
					try {
						System.out.println("disconnecting " + "Thread ID " + Thread.currentThread().getId() + Thread.currentThread().getName());
						peer.disconnect();
					} catch (IOException e) {
						
					}
					return;
				}
				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					System.err.println("An error has encountered. Exiting...");
					break mainLoop;
				}
			}
			while (!choked) {
				//System.out.println("unchoked " + "Thread_ID:" + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
				// If the is no active piece try to get one
				if (piece == null) {
					piece = getNextPiece();
					
				}
				// If piece is no longer null we got a new active piece
				if (piece != null) {
					// get the next block to download
					Block block = piece.getNextBlock();
					// should not happen
					if (block == null) {
						System.err.println("set null block");
						piece.unlock();
						piece = null;
						continue;
					}

					sendMessages message=new sendBlock(block, peer.getOutputStream());
					//peer.sendRequest(block);
					peer.handleSendMessages(message);
				} else {
					if (checkCompleteness()) {
						break mainLoop;
					}
				}

				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					System.err.println("Fatal error.... disconnecting");
					break mainLoop;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// If we become choked unlock piece so other threads can have a
			// chance to lock it
			if (piece != null) {
				piece.unlock();
				piece = null;
			}
		} // If disconnected make sure piece is unlocked so other threads can
			// acquire it
		if (piece != null) {
			piece.unlock();
			piece = null;
		}
		peer.closeEverything();
		peer.setIsDownloading(false);
		peer.decrementPeerCounters(pieces);
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded;
	 * Implements Rarest-Piece-First Algorithm
	 * 
	 * @return the next piece to download
	 */
	private Piece getNextPiece() {
		Comparator<Piece> comparator = new PieceRarityComparator();
		PriorityQueue<Piece> queue = new PriorityQueue<Piece>(250, comparator);
		// Push available pieces into queue that is sorted by piece rarity
		for (Piece piece : pieces) {
			if (!piece.isComplete() && peer.has_piece(piece.getIndex())
					&& !piece.isLocked()) {
				queue.add(piece);
			}
		}
		Piece piece;
		// Try to acquire rarest piece, if unavailable try next rarist etc
		while (!queue.isEmpty()) {
			piece = queue.poll();
			if (piece.tryLock()) {
				return piece;
			}
		}
		// return null if unable to acquire piece
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
	private synchronized  void handleMessage(byte[] message) throws IOException, BtException {
		ByteBuffer parser;
		if(message.length==0){
			peer.keepAlive();
			return;
		}
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			choked = true;
			if (piece != null) {
				piece.unlock();
				piece = null;
			}
			break;
		case BtUtils.UNCHOKE_ID:
			System.out.println("Unchoked Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			choked = false;
			break;
		case BtUtils.INTERESTED_ID:
			System.out.println("Interested Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			break;
		case BtUtils.UNINTERESTED_ID:
			System.out.println("Uninterested Message  " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			break;
		case BtUtils.HAVE_ID:
			System.out.println("Have Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
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
			System.out.println("Bitfield Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			bitField(message);
			sendMessages inter=new sendInterested(peer.getOutputStream());
			peer.handleSendMessages(inter);
			//peer.sendInterested();
			break;
		case BtUtils.REQUEST_ID:
			System.out.println("Request Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			break;
		case BtUtils.PIECE_ID:
			System.out.println("Piece Message " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
			if (piece == null) {
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
			// Add number of bytes to downloaded counter
			peer.uploaded(message.length
					- (BtUtils.PIECE_HEADER_SIZE + BtUtils.PREFIX_LENGTH));

			/* Check for piece completeness and hash correctness */
			piece.checkComplete();
			if (piece.isComplete()) {
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
					for(Peer peerFor: this.peers) {
						sendMessages passMess=new sendHave(piece.getIndex(), peer.getOutputStream());
						peer.handleSendMessages(passMess);
//	peerFor.sendHave(piece.getIndex());
					}
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

	private boolean checkCompleteness() {
		for (Piece piece : pieces) {
			if (!piece.isComplete()) {
				return false;
			}
		}
		return true;
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
	
}
