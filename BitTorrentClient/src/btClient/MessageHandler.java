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
	
	public MessageHandler(ArrayList<Piece> pieces, Peer peer, ByteBuffer info_hash, ByteBuffer clientID ) {
		this.pieces = pieces;
		this.peer = peer;
		this.info_hash = info_hash;
		this.clientID = clientID;
	}
	
	public void run() {
			try {
				peer.establishConnection(info_hash, clientID);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
				return;
			}

		while(true){
			//handle messages and such
		}
	}

}
