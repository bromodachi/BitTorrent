package btClient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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

	public MessageHandler(ArrayList<Piece> pieces, Peer peer,
			ByteBuffer info_hash, ByteBuffer clientID) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
	}

	public void run() {
		if(peer == null){
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

		while (true) {
			while(peer.isChoked()){
				System.out.println("in choked loop");
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

					try {
						handleMessage(peer.getMessage());
					} catch (IOException e) {
						e.printStackTrace();
					} catch (BtException e) {
						e.printStackTrace();
					}
				
			}

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
			// interested
			System.out.println("interest id");
			break;
		case BtUtils.UNINTERESTED_ID:
			System.out.println("uninterested id");
			break;
		case BtUtils.HAVE_ID:
			// have
			System.out.println("Have id");
			break;
		case BtUtils.BITFIELD_ID:
			System.out.println("bitfield");
			try {
				peer.sendInterested();
			} catch (IOException e) {
				e.printStackTrace();
			}

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
}
