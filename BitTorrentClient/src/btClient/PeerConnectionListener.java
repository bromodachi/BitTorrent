package btClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class PeerConnectionListener implements Runnable{
	
	
	private int port;
	private Thread thread;
	
	private ServerSocket serverSocket;
	private Socket connection;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private ArrayList<Peer> peer_list;
	public ByteBuffer info_hash;
	public ByteBuffer clientID;
	
	public boolean listening = false;
	
	public PeerConnectionListener(int port, ArrayList<Peer> peer_list, ByteBuffer info_hash, ByteBuffer clientID){
		
		this.port = port;
		this.peer_list = peer_list;
		this.info_hash = info_hash;
		this.clientID = clientID;
		this.listening = true;
	}

	@Override
	public void run() {
		
		try {
			listenForConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void listenForConnection() throws IOException{
		
		
		while(listening){
			
			serverSocket = new ServerSocket(port); 
			inputStream = new DataInputStream(connection.getInputStream());
			outputStream = new DataOutputStream(connection.getOutputStream());
			
			try{
				connection = serverSocket.accept();
			}catch(IOException e){
				e.printStackTrace();
			}
			
			byte[] message = new byte[BtUtils.p2pHandshakeLength];
			inputStream.read(message);
			
			if(!isSameHash(info_hash.array(), message)){
				closeEverything();
			}else{
				
				ByteBuffer handshake = ByteBuffer.wrap(new byte[BtUtils.p2pHandshakeLength + info_hash.array().length + clientID.array().length]);
				handshake.put(BtUtils.p2pHandshakeHeader);
				handshake.put(info_hash.array());
				handshake.put(clientID.array());

				outputStream.write(handshake.array());
				outputStream.flush();
				
				String peerIP = getIPaddress(connection);
				if (peerIP.equals("128.6.171.130") || peerIP.equals("128.6.171.131")){
					Peer newPeer = new Peer(getIPaddress(connection), getPeerID(message), port);
					peer_list.add(newPeer);
				}
			}
			
			closeEverything();
			
		}
	}
	
	public boolean isSameHash(byte[] info_hash, byte[] response_info_hash) {
		int index = BtUtils.INFO_HASH_OFFSET;
		int indexHash = 0;
		while (info_hash[indexHash++] == response_info_hash[index++]) {
			if (indexHash == BtUtils.INFO_HASH_LENGTH) {
				return true;
			}
		}
		System.err.println("Verfication fail (info_hash mismatch)...connection will drop now");
		return false;
	}
	
	public void closeEverything() {
		try {
			
			
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
			if (connection != null) {
				connection.close();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public String getIPaddress(Socket connection){
		
		return connection.getInetAddress().toString();
		
	}
	
	public String getPeerID(byte[] message){
		
		return Arrays.copyOfRange(message, 48, 67).toString();
		
	}
}
