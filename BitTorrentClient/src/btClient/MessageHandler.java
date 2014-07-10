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

	public MessageHandler(ArrayList<Piece> pieces, Peer peer,
			ByteBuffer info_hash, ByteBuffer clientID) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
		this.peer_has_piece = new boolean[pieces.size()];
		for (int i = 0; i < peer_has_piece.length; i++) {
			peer_has_piece[i] = false;
		}
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

		while (peer.isConnected()) {
			int i = 0;
			while (peer.isChoked()) {
				try {
					handleMessage(peer.getMessage());
				} catch (IOException | BtException e) {
					e.printStackTrace();
				}
			}
			while (!peer.isChoked()) {
				System.out.println("in unchoked loop");
				Piece curr = getNextPiece();
				try {
					peer.sendRequest(curr.getIndex(), curr.getNextBlockIndex()
							* BtUtils.BLOCK_SIZE, BtUtils.BLOCK_SIZE);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				System.out.println("here");
				try {
					handleMessage(peer.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (BtException e) {
					e.printStackTrace();
				}
				System.out.println("here");

			}
			System.out.println("connection closed");
			return;

		}
	}

	private Piece getNextPiece() {
		for (Piece curr : pieces) {
			if (!curr.isComplete()) {
				return curr;
			}
		}
		return null;
	}

	private void handleMessage(byte[] message) throws IOException, BtException {
		System.out.println("handling " + message[0]);
		switch (message[0]) {
		case BtUtils.CHOKE_ID:
			System.out.println("Choke id");
			peer.setChoked(true);
			break;
		case BtUtils.UNCHOKE_ID:
			System.out.println("Unchoke id");
			peer.setChoked(false);
			break;
		case BtUtils.INTERESTED_ID:
			System.out.println("interest id");
			break;
		case BtUtils.UNINTERESTED_ID:
			System.out.println("uninterested id");
			break;
		case BtUtils.HAVE_ID:
			System.out.println("Have id");
			break;
		case BtUtils.BITFIELD_ID:
			System.out.println("bitfield");
			bitField(message);
			try {
				peer.sendInterested();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("sent interested");
			break;
		case BtUtils.REQUEST_ID:
			// request
			System.out.println("request id");
			break;
		case BtUtils.PIECE_ID:
			// piece
			System.out.println("piece id");
			pieces.get(ByteBuffer.wrap(message).getInt(1)).addBlock(message);
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
		//remove message id from message
		ByteBuffer bytes = ByteBuffer.wrap(new byte[message.length - 1]);
		bytes.put(message, 1, message.length-1);
		
		//create bitset and set boolean values
		BitSet bitSet = BitSet.valueOf(bytes);
		for (int i = 0; i < bitSet.length(); i++) {
			peer_has_piece[i] = bitSet.get(i);
		}
	}
}
