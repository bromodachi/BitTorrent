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
import java.util.Arrays;
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
	 private int []pieceLowest;
	/**
	 * The local peer_ID for this client
	 */
	private final ByteBuffer clientID;
	/**
	 * Indicates whether or not the client is choked by the {@link Peer} that
	 * this MessageHandler is responsible for
	 */
	private boolean choked;
	private ArrayList<Peer> peers;
	/**
	 * All general info relating to this torrent
	 * 
	 * @see TorrentInfo
	 */
	private TorrentInfo torrent;
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
	/**
	 * boolean indicated whether or not the client has finished donwloading
	 */
	private boolean complete;
	
	private Status status;
	

	/**
	 * Returns a new message handler object, created with the given parameters
	 * 
	 * @param {@link#pieces}
	 * @param {@link#peer}
	 * @param {@link#info_hash}
	 * @param {@link#clientID}
	 * @param {@link#torr}
	 */
	public MessageHandler(ActiveTorrent activeTorrent, Peer peer, ArrayList<Peer> peers) {
		this.torrent = activeTorrent.getTorrentInfo();
		this.pieces = activeTorrent.getPieces();
		pieceLowest=new int [pieces.size()];
		this.peers=peers;
		this.info_hash = torrent.info_hash;
		this.clientID = activeTorrent.getClientId();
		this.peer = peer;
		choked = true;
		wasted = 0;
		// initialize peer has_piece array
		peer.setHasPieces(new boolean[pieces.size()]);
		for (int i = 0; i < pieces.size(); i++) {
			peer.setHasPiece(i, false);
		}
		// initialize client has_piece array
		has_piece = new boolean[pieces.size()];
		complete = true;
		for (Piece piece : pieces) {
			if (piece.isComplete()) {
				has_piece[piece.getIndex()] = true;
			} else {
				has_piece[piece.getIndex()] = false;
				complete = false;
			}
		}
		status = Status.Active;
	}

	@Override
	public void run() {
		try {
			connect();
		} catch (IOException e2) {
			e2.printStackTrace();
			System.out.println(Thread.currentThread().getName()
					+ "Failed to connect");
		}
		// While connected : communication loop
		while (peer.isConnected() && !peer.isClosed()) {
			updateHasPiece();
			// debug();
			if (!choked) {

				// if piece is completed set it to null to get next piece
				if (piece != null) {
					if (piece.isComplete()) {
						piece = null;
					}
				}
				// If the is no active piece try to get one
				// If block is null get next block
				// get the next block to download
				if (piece == null && status == Status.Active) {
					piece = getNextPiece();
					if (piece == null) {
						System.out
								.println(Thread.currentThread().getName()
										+ " Selected null piece: Probably means none available");
						// if the peer has no pieces we want and we are not seeding then make sure peer is choked
						if(!peer.isChoked()){
							try {
								peer.sendUninterested();
								peer.sendChoke();
								peer.setChoked(true);
								System.out.println(Thread.currentThread().getName() + " Sent choke and uninterested");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				// if piece is not null then the peer has a piece we are interested in
				if (piece != null) {
					System.out.println(Thread.currentThread().getName()
							+ " Set next piece " + piece.getIndex());
					// get next non-downloaded block of the piece
					block = piece.getNextBlock();
					// if block is null then the piece is complete
					if (block == null) {
						System.err.println(Thread.currentThread().getName()
								+ " Received null Block");
						piece.checkComplete();
					}
				}
				// Only attempt request if block is not null
				if (block != null) {
					try {
						System.out.println(Thread.currentThread().getName()
								+ " Requesting piece " + block.getPieceIndex()
								+ " offset " + block.getOffset());
						peer.sendRequest(block);
					} catch (IOException e) {
						System.err.println(Thread.currentThread().getName()
								+ " Failed to send request");
					}
					block = null;
				}
			}

			// check if any pieces have been completed by other threads
			System.out.println(Thread.currentThread().getName()
					+ " Getting message from peer");

			try {
				handleMessage(peer.getMessage());
			} catch (EOFException e) {
				System.out.println(Thread.currentThread().getName()
						+ " Received EOF disconnecting... ");
				try {
					peer.disconnect();
					peer.closeEverything();
					peer.setChoked(true);
				} catch (IOException e1) {
					return;
				}
				return;
			} catch (IOException | InterruptedException | BtException e) {
				e.printStackTrace();
				return;
			}

		}
		System.err.println(Thread.currentThread().getName()
				+ " connection is lost");
		// make sure all streams are closed
		peer.closeEverything();
		// decrement peer counters to update rarity of pieces 
		peer.decrementPeerCounters(pieces);
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
		if (message == null) {
			System.err.println(Thread.currentThread().getName()
					+ " Received null message");
			return;
		}
		if (message.length == 0) {
			System.out.println(Thread.currentThread().getName()
					+ " Received message of length 0");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return;
		}
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			System.out.println(Thread.currentThread().getName()
					+ " Received choke");
			choked = true;
			break;
		case BtUtils.UNCHOKE_ID:
			System.out.println(Thread.currentThread().getName()
					+ " Received unchoke");
			choked = false;
		
			break;
		case BtUtils.INTERESTED_ID:
			System.out.println(Thread.currentThread().getName()
					+ " Received Interested");
			peer.sendUnchoke();
			peer.setChoked(false);
			System.out.println(Thread.currentThread().getName()
					+ " Sent Unchoke");
			break;
		case BtUtils.UNINTERESTED_ID:
			System.out.println(Thread.currentThread().getName()
					+ " Received Uninterested");
			break;
		case BtUtils.HAVE_ID:
			receiveHave(message);
			break;
		case BtUtils.BITFIELD_ID:
			System.out.println(Thread.currentThread().getName()
					+ " Received Bitfield");
			receiveBitfield(message);
			break;
		case BtUtils.REQUEST_ID:
			receiveRequest(message);
			break;
		case BtUtils.PIECE_ID:
			receivePiece(message);
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
	 * @throws IOException 
	 */
	private void receiveBitfield(byte[] message) throws IOException {
		// remove message id from message
		ByteBuffer bytes = ByteBuffer.wrap(new byte[message.length - 1]);
		bytes.put(message, 1, message.length - 1);
		peer.setHasPieces(Peer.ConvertBitfieldToArray(bytes.array(),
				pieces.size()));
		peer.incrementPeerCounters(pieces);
		for(int i = 0; i < has_piece.length; i++){
			if(!has_piece[i] && peer.has_piece(i)){
				peer.sendInterested();
				System.out.println(Thread.currentThread().getName()
						+ " Sent Interested");
				return;
			}
		}
		peer.sendUninterested();
		System.out.println(Thread.currentThread().getName()
				+ " Sent Uninterested");
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
	 * Handles an incoming piece message from the peer. Checks and validates the
	 * piece and writes it to the file
	 * 
	 * @param message
	 * @throws IOException
	 * @throws BtException
	 */
	private void receivePiece(byte[] message) throws IOException, BtException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		Piece currPiece = pieces.get(parser.getInt(BtUtils.REQUEST_INDEX));
		System.out.println(Thread.currentThread().getName() + " Piece Message "
				+ " Received " + parser.getInt(1) + " offset "
				+ parser.getInt(5));

		currPiece.writeBlock(message);
		System.err.println(Thread.currentThread().getName() + " Wrote Piece "
				+ parser.getInt(1) + " offset " + parser.getInt(5));

		// Add number of bytes to peer's uploaded counter
		peer.uploaded(message.length
				- (BtUtils.PIECE_HEADER_SIZE + BtUtils.PREFIX_LENGTH));

		/* Check for piece completeness and hash correctness */
		currPiece.checkComplete();
		if (currPiece.isComplete()) {
			if (!currPiece.checkHash(torrent.piece_hashes[currPiece.getIndex()]
					.array())) {
				System.err.println(Thread.currentThread().getName()
						+ "Hash mismatch piece: " + currPiece.getIndex());
				// If hash mismatch, clear piece and try again
				currPiece.clearBlocks();
				currPiece.incrementAttempts();
				// If piece has reach the max attempts print error and
				if (currPiece.getDownloadAttempts() >= BtUtils.MAX_DOWNLOAD_ATTEMPTS) {
					System.err
							.println("ERROR: Max download attempts reached, hash mismatch for piece #"
									+ piece.getIndex());
					return;
				}
			} else {
				peer.sendHave(currPiece.getIndex());
				System.err.println(Thread.currentThread().getName()
						+ " sent have piece" + currPiece.getIndex());
			}
		}
	}

	/**
	 * Handles an incoming request message, sends the request piece to the peer
	 * if the piece is available
	 * 
	 * @param message
	 *            A request message from the peer
	 * @throws IOException
	 */
	private void receiveRequest(byte[] message) throws IOException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		System.out.println(Thread.currentThread().getName()
				+ " Received Request for Piece "
				+ ByteBuffer.wrap(message).getInt(1));
		if(parser.getInt(BtUtils.REQUEST_INDEX) < 0 || parser.getInt(BtUtils.REQUEST_INDEX) >= pieces.size()){
			System.err.println(Thread.currentThread().getName() + " ERROR: Peer Requested invalid piece disconnecting...");
			peer.disconnect();
			peer.closeEverything();
			return;
		}
		if (!has_piece[parser.getInt(BtUtils.REQUEST_INDEX)]) {
			System.err.println(Thread.currentThread().getName()
					+ " Peer requested piece that client does not have");
			return;
		}
		peer.sendPiece(pieces.get(parser.getInt(BtUtils.REQUEST_INDEX)),
				parser.getInt(BtUtils.REQUEST_OFFSET), BtUtils.REQUEST_SIZE);

		System.out.println(Thread.currentThread().getName() + " Sent piece"
				+ parser.getInt(BtUtils.REQUEST_INDEX));
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
		System.out.println(Thread.currentThread().getName()
				+ " Received Have for Piece " + parser.getInt(1));
		int index = parser.getInt(1);
		if (index >= 0 && index < pieces.size()) {
			if (!pieces.get(index).isComplete()) {
				System.out.println(Thread.currentThread().getName()
						+ " Sending interested message for piece " + index);
				peer.sendInterested();
				peer.setHasPiece(index);
				pieces.get(index).incrementPeerCount();
			}
		} else {
			System.err
					.println("Received have message with invalid piece index");
			peer.disconnect();
			peer.closeEverything();
			wasted += message.length;
		}
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded;
	 * Implements Rarest-Piece-First Algorithm
	 * 
	 * @return the next piece to download
	 */
	private synchronized  Piece getNextPiece() {
        updateGetLowest();
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < this.pieceLowest.length; i++){
        	
        	if(this.has_piece==null){
        		System.err.println("======================NULLLLLL for has piece at i: "+i);
        	}
        	if(peer.getHasPieceArray()==null){
        		System.out.println("NULLLLLL for has peerHas");
        	}
            if(this.has_piece[i] == false && peer.getHasPieceArray()[i] == true){
                if(min > this.pieceLowest[i]){
                    min = pieceLowest[i];
                }
            }
        }
        if(min==Integer.MAX_VALUE){
            return null;
        }
        ArrayList<Integer> indices = new ArrayList<Integer>();

        for(int i = 0; i < this.pieceLowest.length; i++){
            if(this.has_piece[i] == false && peer.getHasPieceArray()[i] == true){
                if(min == this.pieceLowest[i]){
                    indices.add(i);
                }
            }
        }
         
        Random random = new Random();
        int n = random.nextInt(indices.size());
        Piece piece =pieces.get(indices.get(n));
        System.out.println("I have this piece"+peer.getHasPieceArray()[indices.get(n)]);
        System.out.println("Does the host have it? "+has_piece[indices.get(n)]);
        return piece;
       
    }
   
    private synchronized void updateGetLowest(){
        Arrays.fill(this.pieceLowest, 0);
        for(Peer peer : this.peers){
            for(int i = 0; i < this.pieceLowest.length; i++){
            		if(peer.getHasPieceArray()==null){
            			continue;
            		}
                    if(peer.getHasPieceArray()[i] == true){
                        this.pieceLowest[i] += 1;
                        if(has_piece[i]){
                            this.pieceLowest[i] += 10;
                        }
                    }
                }
            }
    }

	/**
	 * updates the has_piece boolean array for this thread, and checks
	 * completeness / updates complete
	 */
	private void updateHasPiece() {
		// keep complete set to true if all pieces are complete
		status = Status.Seeding;
		complete = true;
		for (Piece piece : pieces) {
			// If a piece has been completed send have message to peer then
			// update has_piece array
			if (!piece.isComplete()) {
				status = Status.Active;
				complete = false;
			}
			if (piece.isComplete() && !has_piece[piece.getIndex()]) {
				try {
					System.out.println(Thread.currentThread().getName()
							+ " Sent have piece " + piece.getIndex());
					peer.sendHave(piece.getIndex());
				} catch (IOException e) {
					// if sendHave fails don't update array so it will be resent
					// on next pass
					continue;
				}
				has_piece[piece.getIndex()] = true;
			}
		}
	}

	private void connect() throws IOException {
		if (peer == null) {
			System.err.println(Thread.currentThread().getName()
					+ " recieved null peer");
			return;
		} else {
			System.out.println(Thread.currentThread().getName()
					+ "recieved peer: " + peer.getPeer_id());
		}
		try {
			if (!peer.establishConnection(info_hash, clientID)) {
				System.err.println(Thread.currentThread().getName()
						+ "ERROR: Failed to establish connection with peer "
						+ peer.getPeer_id());
				return;
			}
			System.out.println(Thread.currentThread().getName()
					+ " Connected to peer " + peer.getPeer_id());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}
		// send have message to peer
		for (Piece piece : pieces) {
			if (piece.isComplete()) {
				peer.sendHave(piece.getIndex());
			}
		}

		// Send bitfield message to peer after handshake
		/*
		 * try { peer.sendBitfield(has_piece);
		 * System.out.println(Thread.currentThread().getName() +
		 * " Sent bitfield"); } catch (IOException e2) { e2.printStackTrace(); }
		 */
	}
}
