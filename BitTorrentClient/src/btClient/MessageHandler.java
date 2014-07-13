package btClient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * This class is tasked with deciding which piece to download next and handling
 * the messages recieved from the peer
 * 
 * @author Cody
 * 
 */
public class MessageHandler implements Runnable {

	private ArrayList<Piece> pieces;
	private final Peer peer;
	private final ByteBuffer info_hash;
	private final ByteBuffer clientID;
	private boolean[] peer_has_piece;
	private boolean choked;
	private boolean errors;
	private TorrentInfo torrent;
	
	public boolean getErrors(){
		return this.errors;
	}

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
		this.torrent=torr;
	}

	public void run() {
		if (peer == null) {
			System.err.println("recieved null peer");
			return;
		}
		try {
			peer.establishConnection(info_hash, clientID);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			return;
		}
		if(peer.getExchangeMessage()){
		while (peer.isConnected()) {
			int i = 0;
			while (choked) {
		//		System.out.println("top choked loop");
				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					e.printStackTrace();
				}
			//	System.out.println("bottom choked loop");
			}
			while (!choked) {
			//	System.out.println("in unchoked loop");
				Piece curr = getNextPiece();
				if (curr == null) {
					// send completed
					try {
						peer.disconnect();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}
				try {
					System.out.println("sending request: Index:"
							+ curr.getIndex() + " Offset:"
							+ curr.getNextBlockOffest() + " length:"
							+ curr.getNextBlockSize());
					peer.sendRequest(curr.getIndex(),
							curr.getNextBlockOffest(), curr.getNextBlockSize());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				try {
					handleMessage(peer.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (BtException e) {
					e.printStackTrace();
				}

			//	System.out.println("bottom unchoked loop");
			}
			peer.closeEverything();
			System.out.println("connection closed");
			return;

		}
		}
		else{
			this.errors=true;
		}
	}

	/**
	 * Returns the next piece that the peer has and has not yet been downloaded
	 * 
	 * @return the next piece to download
	 */
	private Piece getNextPiece() {
		for (Piece curr : pieces) {
			if (!curr.isComplete() && peer_has_piece[curr.getIndex()]) {
				return curr;
			}
		}
		return null;
	}

	private void handleMessage(byte[] message) throws IOException, BtException {
		//System.out.println("handling " + message[0]);
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
		//	System.out.println("Choke id");
			choked = true;
			break;
		case BtUtils.UNCHOKE_ID:
		//	System.out.println("Unchoke id");
			choked = false;
			break;
		case BtUtils.INTERESTED_ID:
		//	System.out.println("interest id");
			break;
		case BtUtils.UNINTERESTED_ID:
		//	System.out.println("uninterested id");
			break;
		case BtUtils.HAVE_ID:
		//	System.out.println("Have id");
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
			System.out.println("request id");
			break;
		case BtUtils.PIECE_ID:
			Piece piece = pieces.get(ByteBuffer.wrap(message).getInt(1));
			piece.addBlock(message);
			if (piece.isComplete()) {
				peer.sendHave(piece.getIndex());
				if(!piece.compareTo(torrent.piece_hashes[piece.getIndex()].array())){
					//what do you want to do if there's an error
				}
			//	System.out.println("Comparing pieces: "+piece.compareTo(torrent.piece_hashes[piece.getIndex()].array()));
			}
			break;

		default:
			System.out.println("You fucked up big time ");
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
		/*for (int i = 0; i < bitSet.length; i++) {
			System.out.println("bitset " + i + " " + bitSet[i]);
		}*/
		for (int i = 0; i < peer_has_piece.length; i++) {

			peer_has_piece[i] = bitSet[i];
		}
		/*for (int i = 0; i < peer_has_piece.length; i++) {
			System.out.println("peer has " + i + ":" + peer_has_piece[i]);
		}*/
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
