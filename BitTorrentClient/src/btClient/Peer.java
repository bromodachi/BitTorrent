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

	public void establishConnection(String info_hash, String clientID) throws UnknownHostException, IOException {
		connection = new Socket(IP, port);
		inputStream = connection.getInputStream();
		outputStream = connection.getOutputStream();
		
		ByteBuffer handshake = ByteBuffer.allocate(48);
		handshake.put(BtUtils.p2pHandshakeHeader);	
		handshake.put(info_hash.getBytes("UTF-8"));
		handshake.put(clientID.getBytes("UTF-8"));
		
		outputStream.write(handshake.array());

	}

}
