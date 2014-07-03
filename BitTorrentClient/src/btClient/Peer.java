package btClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Peer {
	private int interval, complete, incomplete, port;
	private String IP, peer_id;
	private Socket connection;
	private InputStream inputStream;
	private OutputStream outputStream;

	public Peer(int interval, int complete, int incomplete, String IP,
			String peer_id, int port) {
		this.interval = interval;
		this.complete = complete;
		this.incomplete = incomplete;
		this.IP = IP;
		this.peer_id = peer_id;
		this.port = port;
		this.connection = null;
	}

	/* ================= Getters =================== */
	public int getInterval() {
		return interval;
	}

	public int getComplete() {
		return complete;
	}

	public int getIncomplete() {
		return incomplete;
	}

	public int getPort() {
		return port;
	}

	public String getIP() {
		return IP;
	}

	public String getPeer_id() {
		return peer_id;
	}

	public Socket getSocket() {
		return connection;
	}

	public Socket getConnection() {
		return connection;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	/* =============== Setters ================ */
	public void setInterval(int interval) {
		this.interval = interval;
	}

	public void setComplete(int complete) {
		this.complete = complete;
	}

	public void setIncomplete(int incomplete) {
		this.incomplete = incomplete;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setIP(String iP) {
		IP = iP;
	}

	public void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}

	/* =============== Methods ==================== */
	/**
	 * Establishes a connection with the peer by creating a socket to the peer's
	 * IP address and port number then creates a handshake message from the
	 * given info_hash and clientID
	 * 
	 * @param info_hash
	 *            info_hash of from the torrent file (must match that of the
	 *            peer's)
	 * @param clientID
	 *            psuedorandom peer_ID identifying the client (not this peer)
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void establishConnection(ByteBuffer info_hash, ByteBuffer clientID)
			throws UnknownHostException, IOException {
		connection = new Socket(IP, port);
		inputStream = connection.getInputStream();
		outputStream = connection.getOutputStream();

		ByteBuffer handshake = ByteBuffer.allocate(48);
		handshake.put(BtUtils.p2pHandshakeHeader);
		System.err.println(handshake.position());
		info_hash.rewind();
		clientID.rewind();
		while (info_hash.position() < info_hash.capacity()) {
			System.err.println(handshake.position());
			handshake.put(info_hash.get());
		}
		while (handshake.position() < handshake.capacity()) {
			System.err.println(handshake.position());
			handshake.put(clientID.get());
		}
		if (handshake.position() == handshake.capacity()) {
			System.err.println("position == capacity");
		} else {
			System.err.println("not ==");
		}

		handshake.rewind();
		byte[] bytes = new byte[BtUtils.p2pHandshakeLength];
		handshake.get(bytes, 0, handshake.capacity());

		outputStream.write(bytes);

	}
	
	public void sendKeepAlive() throws IOException{
		outputStream.write(BtUtils.KEEP_ALIVE);
	}
	
	public void sendChoke(){
		
	}
	
	public void sendunchoke(){
		
	}

	public void sendInterested(){
		
	}
	
	public void sendUninterested(){
		
	}
}
