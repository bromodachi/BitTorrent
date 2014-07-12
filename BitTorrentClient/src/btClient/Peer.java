package btClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class is tasked with managing the connection with a peer and sending
 * receiving messages between that peer. Handling of messages should take place
 * elsewhere
 * 
 * @author Cody
 * 
 */
public class Peer {
	private int interval, complete, incomplete, port;
	private String IP, peer_id;
	private Socket connection;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private boolean choked;

	public Peer(String IP, String peer_id, int port) {
		this.IP = IP;
		this.peer_id = peer_id;
		this.port = port;
		this.connection = null;
		choked = true;
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

	public DataInputStream getInputStream() {
		return inputStream;
	}

	public DataOutputStream getOutputStream() {
		return outputStream;
	}

	public boolean isChoked() {
		return choked;
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

	public void setChoked(boolean choked) {
		this.choked = choked;
	}
	
	public void disconnect() throws IOException{
		inputStream.close();
		outputStream.close();
		connection.close();
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
		inputStream = new DataInputStream(connection.getInputStream());
		outputStream = new DataOutputStream(connection.getOutputStream());

		ByteBuffer handshake = ByteBuffer.allocate(BtUtils.p2pHandshakeLength);
		handshake.put(BtUtils.p2pHandshakeHeader);
		info_hash.rewind();
		clientID.rewind();
		while (info_hash.position() < info_hash.capacity()) {
			handshake.put(info_hash.get());
		}
		while (clientID.position() < clientID.capacity()) {
			handshake.put(clientID.get());
		}

		handshake.rewind();
		byte[] bytes = new byte[BtUtils.p2pHandshakeLength];
		handshake.get(bytes, 0, handshake.capacity());

		outputStream.write(bytes);
		outputStream.flush();
		/* get the response */
		byte[] response = new byte[BtUtils.p2pHandshakeLength];
		inputStream.read(response);
		/* verify that it's the same info_hash */

		if (isSameHash(info_hash.array(), response)) {
			System.out.println("info hash verified");
		}
	}

	/**
	 * Just verifies that the info hash are the same. If not, we should drop the
	 * connection
	 * 
	 * @return boolean True if remote and local peer have the same info_hash,
	 *         otherwise false
	 */
	public boolean isSameHash(byte[] info_hash, byte[] response_info_hash) {
		int index = BtUtils.INFO_HASH_OFFSET;
		int indexHash = 0;
		while (info_hash[indexHash++] == response_info_hash[index++]) {
			if (indexHash == BtUtils.INFO_HASH_LENGTH) {
				return true;
			}
		}
		System.err
				.println("Verfication fail (info_hash mismatch)...connection will drop now");
		return false;
	}

	/**
	 * Sends an all zero keep alive message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendKeepAlive() throws IOException {
		outputStream.write(BtUtils.KEEP_ALIVE);
	}

	/**
	 * Sends a choke message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendChoke() throws IOException {
		byte[] bytes = new byte[BtUtils.CHOKE_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.CHOKE_LENGTH_PREFIX);
		message.put((byte) (BtUtils.CHOKE_ID));
		outputStream.write(message.array());
		outputStream.flush();
	}

	/**
	 * Sends and unchoke message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendUnchoke() throws IOException {
		byte[] bytes = new byte[BtUtils.UNCHOKE_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.UNCHOKE_LENGTH_PREFIX);
		message.put((byte) (BtUtils.UNCHOKE_ID));
		outputStream.write(message.array());
		outputStream.flush();
	}

	/**
	 * Sends an interested message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendInterested() throws IOException {
		byte[] bytes = new byte[BtUtils.INTERESTED_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.INTERESTED_LENGTH_PREFIX);
		message.put((byte) (BtUtils.INTERESTED_ID));
		outputStream.write(message.array());
	}

	/**
	 * Sends uninterested message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendUninterested() throws IOException {
		byte[] bytes = new byte[BtUtils.UNINTERESTED_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.UNINTERESTED_LENGTH_PREFIX);
		message.put((byte) (BtUtils.UNINTERESTED_ID));
		outputStream.write(message.array());
	}

	/**
	 * Sends a have message to the peer
	 * 
	 * @param index
	 *            zero based index of the piece referenced in the have message
	 * @throws IOException
	 */
	public void sendHave(int index) throws IOException {
		byte[] bytes = new byte[BtUtils.HAVE_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.HAVE_LENGTH_PREFIX);
		message.put((byte) (BtUtils.HAVE_ID));
		message.putInt(index);
		outputStream.write(message.array());
	}

	/**
	 * Sends a request message to the peer
	 * 
	 * @param index
	 *            zero based index of the piece being requested
	 * @param block_offset
	 *            offset of block being requested
	 * @param block_length
	 *           number of bytes in a typical block in the piece 
	 * @throws IOException
	 */
	public void sendRequest(int index, int block_offset, int block_length)
			throws IOException {
		byte[] bytes = new byte[BtUtils.REQUEST_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.REQUEST_LENGTH_PREFIX);
		message.put((byte) (BtUtils.REQUEST_ID));
		message.putInt(index);
		message.putInt(block_offset);
		message.putInt(block_length);
		outputStream.write(message.array());
	}

	/**
	 * Sends a piece message to the peer
	 * 
	 * @param index
	 *            zero based index of the piece
	 * @param offset
	 *            block offset of the piece
	 * @throws IOException
	 */
	public void sendPiece(int index, int offset) throws IOException {
		int payloadLength = 0; // need to figure out code for adding payload
		byte[] bytes = new byte[BtUtils.PIECE_LENGTH_PREFIX
				+ BtUtils.PREFIX_LENGTH + payloadLength];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.PIECE_LENGTH_PREFIX);
		message.put((byte) (BtUtils.PIECE_ID));
		message.putInt(index);
		message.putInt(offset);
		// add payload
		outputStream.write(message.array());
		outputStream.flush();
	}

	/**
	 * Reads a message form the peer
	 * 
	 * @return null if no bytes were read or if there were an incorrect number
	 *         of byte representing the length_prefix. Otherwise returns the
	 *         message (without its length prefix, message_id is in index = 0)
	 * @throws IOException
	 */
	public byte[] getMessage() throws IOException {
		int length = inputStream.readInt();
		System.out.println("retrieving: "+ length);
		byte[] message = new byte[length];
		inputStream.readFully(message, 0, length);
		System.out.println("returning message id " + message[0]);
		return message;
	}
	
	public boolean isConnected(){
		return !connection.isClosed();
	}
}