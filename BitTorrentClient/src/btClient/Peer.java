/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * This class is tasked with managing the connection with a peer and sending
 * receiving messages between that peer. Handling of messages should take place
 * elsewhere
 * 
 * @author Cody
 * 
 */
public class Peer {
	private int interval, port;
	private String IP, peer_id;
	private Socket connection;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	/**
	 * The number of bytes that this peer has downloaded from the client
	 */
	private int downloaded;
	/**
	 * The number of bytes this peer has uploaded to the client
	 */
	private int uploaded;
	/**
	 * boolean array indicating whether or not this peer has each piece
	 */
	private boolean[] has_piece = null;
	/**
	 * indicates whether or not this piece is choked
	 */
	private boolean choked;
	/**
	 * indicates whether or not this peer is interested in pieces that we have
	 */
	private boolean interested;
	/**
	 * Indicates whether or not we are interested in downloading from this peer
	 */
	private boolean interesting;

	/**
	 * Creats a new Peer object with the given parameters
	 * 
	 * @param {@link#IP}
	 * @param {@link#peer_id}
	 * @param {@link#port}
	 */
	public Peer(String IP, String peer_id, int port) {
		this.IP = IP;
		this.peer_id = peer_id;
		this.port = port;
		this.connection = null;
		uploaded = 0;
		downloaded = 0;
		choked = true;
		interested = false;
		interesting = false;

	}

	@Override
	public boolean equals(Object object) {
		if (object.getClass() == this.getClass()) {
			if (((Peer) object).getPeer_id().equals(this.getPeer_id()) && ((Peer) object).getIP().equals(this.getIP())) {
				return true;
			}
		}
		return false;
	}

	/* ================= Getters =================== */
	public int getInterval() {
		return interval;
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

	public int getDownloaded() {
		return downloaded;
	}

	public int getUploaded() {
		return uploaded;
	}

	public boolean isChoked() {
		return choked;
	}

	public boolean isClosed() {
		if (connection == null) {
			return true;
		}
		return connection.isClosed();
	}

	/**
	 * Checks if connection to peer is still valid
	 * 
	 * @return True if connection is open, otherwise false
	 */
	public boolean isConnected() {
		if (connection == null) {
			return false;
		}
		return connection.isConnected();
	}

	public boolean isInterested() {
		return interested;
	}

	/* =============== SETTERS ================ */
	public void setInterval(int interval) {
		this.interval = interval;
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

	public void setHasPieces(boolean[] has_piece) {
		this.has_piece = has_piece;
	}

	public void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}

	public void setInterested(boolean interested) {
		this.interested = interested;
	}

	/**
	 * Adds the given number of uploaded bytes to the uploaded counter
	 * 
	 * @param uploaded
	 *            number of bytes uploaded
	 */
	public void uploaded(int uploaded) {
		this.uploaded = uploaded + this.uploaded;
	}

	public void setDownloaded(int downloaded) {
		this.downloaded = downloaded;
	}

	/**
	 * Adds the given number of bytes downloaded to the download counter
	 * 
	 * @param downloaded
	 *            number of bytes downloaded
	 */
	public void downloaded(int downloaded) {
		this.downloaded += this.downloaded + downloaded;
	}

	/**
	 * Resets (@link#downloaded} byte counter to 0
	 */
	public void resetDownloaded() {
		downloaded = 0;
	}

	/**
	 * Resets {@link#uploaded} byte counter to 0
	 */
	public void resetUploaded() {
		uploaded = 0;
	}

	/* =============== GETTERS ============== */
	/**
	 * Checks if a this peer has the piece indicated by the piece index
	 * 
	 * @param pieceIndex
	 *            the zero based index of the piece to be checked
	 * @return True if this peer has the indicated piece, False if peer does not
	 *         have piece or if {@link#has_piece} has not yet been set
	 */
	public boolean has_piece(int pieceIndex) {
		if (has_piece != null) {
			return has_piece[pieceIndex];
		}
		return false;
	}

	/**
	 * Sets {@link#has_piece} at the given index to be true, indicating that
	 * this peer now has the piece with the given index
	 * 
	 * @param index
	 *            the zero based index of the piece
	 */
	public void setHasPiece(int index) {
		has_piece[index] = true;
	}

	/**
	 * Sets the value of {@link#has_piece} to the given boolean value at the
	 * given index
	 * 
	 * @param index
	 *            zero based index
	 * @param bool
	 */
	public void setHasPiece(int index, boolean bool) {
		has_piece[index] = bool;
	}

	/* =============== Methods ==================== */
	/**
	 * Disconnects client from this peer and closes relevant streams
	 * 
	 * @throws IOException
	 */
	public void disconnect() {

		try {
			inputStream.close();
			outputStream.close();
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
	public boolean establishConnection(ByteBuffer info_hash, ByteBuffer clientID) throws UnknownHostException, IOException {
		connection = new Socket(IP, port);
		inputStream = new DataInputStream(connection.getInputStream());
		outputStream = new DataOutputStream(connection.getOutputStream());
		// connection.setSoTimeout(BtUtils.MAX_TIME);

		ByteBuffer handshake = ByteBuffer.wrap(new byte[BtUtils.p2pHandshakeLength + info_hash.array().length + clientID.array().length]);
		handshake.put(BtUtils.p2pHandshakeHeader);
		handshake.put(info_hash.array());
		handshake.put(clientID.array());

		outputStream.write(handshake.array());
		outputStream.flush();
		/* get the response */
		byte[] response = new byte[BtUtils.p2pHandshakeLength];
		inputStream.read(response);
		/* verify that it's the same info_hash */
		// connection.setKeepAlive(true);

		if (!isSameHash(info_hash.array(), response)) {
			closeEverything();
			System.err.println("FATAL ERROR: Tracker info_hash did not match file info_hash");
			return false;
		}
		return true;
	}

	public void reconnect() {

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
		System.err.println("Verfication fail (info_hash mismatch)...connection will drop now");
		return false;
	}

	/**
	 * Closes all streams and connections
	 */
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	public static boolean[] ConvertBitfieldToArray(byte[] bitfield, int numPieces) {
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

	/**
	 * Increments the peerCount for each {@link Piece} in the list of pieces by
	 * calling {@link Piece#incrementPeerCount()}
	 * 
	 * @param pieces
	 *            An array list of {@link Piece} objects
	 */
	public void incrementPeerCounters(ArrayList<Piece> pieces) {
		for (Piece piece : pieces) {
			if (has_piece[piece.getIndex()]) {
				piece.incrementPeerCount();
			}
		}
	}

	/**
	 * Decrements the peerCount for each {@link Piece} by calling
	 * {@link Piece#decrementPeerCount()}
	 * 
	 * @param pieces
	 *            An array list of {@link Piece} objects
	 */
	public void decrementPeerCounters(ArrayList<Piece> pieces) {
		if (pieces == null) {
			return;
		}
		for (Piece piece : pieces) {
			if (has_piece[piece.getIndex()]) {
				piece.decrementPeerCount();
			}
		}
	}

	/* ================ SEND MESSAGE METHODS ==================== */
	/**
	 * Sends an all zero keep alive message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendKeepAlive() throws IOException {
		synchronized (outputStream) {
			outputStream.write(BtUtils.KEEP_ALIVE);
			outputStream.flush();
		}
	}

	/**
	 * Sends a choke message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendChoke() throws IOException {
		byte[] bytes = new byte[BtUtils.CHOKE_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.CHOKE_LENGTH_PREFIX);
		message.put((BtUtils.CHOKE_ID));
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends and unchoke message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendUnchoke() throws IOException {
		byte[] bytes = new byte[BtUtils.UNCHOKE_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.UNCHOKE_LENGTH_PREFIX);
		message.put((BtUtils.UNCHOKE_ID));
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends an interested message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendInterested() throws IOException {
		byte[] bytes = new byte[BtUtils.INTERESTED_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.INTERESTED_LENGTH_PREFIX);
		message.put((BtUtils.INTERESTED_ID));
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends uninterested message to the peer
	 * 
	 * @throws IOException
	 */
	public void sendUninterested() throws IOException {
		byte[] bytes = new byte[BtUtils.UNINTERESTED_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.UNINTERESTED_LENGTH_PREFIX);
		message.put((BtUtils.UNINTERESTED_ID));
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends a have message to the peer
	 * 
	 * @param index
	 *            zero based index of the piece referenced in the have message
	 * @throws IOException
	 */
	public void sendHave(int index) throws IOException {
		byte[] bytes = new byte[BtUtils.HAVE_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.HAVE_LENGTH_PREFIX);
		message.put((BtUtils.HAVE_ID));
		message.putInt(index);
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends a request message to the peer
	 * 
	 * @param index
	 *            zero based index of the piece being requested
	 * @param block_offset
	 *            offset of block being requested
	 * @param block_length
	 *            number of bytes in a typical block in the piece
	 * @throws IOException
	 */
	public void sendRequest(int index, int block_offset, int block_length) throws IOException {
		byte[] bytes = new byte[BtUtils.REQUEST_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.REQUEST_LENGTH_PREFIX);
		message.put((BtUtils.REQUEST_ID));
		message.putInt(index);
		message.putInt(block_offset);
		message.putInt(block_length);
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends a request message to the peer
	 * 
	 * @param block
	 *            the {@link Block} to be requested
	 * @throws IOException
	 */
	public void sendRequest(Block block) throws IOException {
		byte[] bytes = new byte[BtUtils.REQUEST_LENGTH_PREFIX + BtUtils.PREFIX_LENGTH];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.REQUEST_LENGTH_PREFIX);
		message.put((BtUtils.REQUEST_ID));
		message.putInt(block.getPieceIndex());
		message.putInt(block.getOffset());
		message.putInt(block.getSize());
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Sends a piece message to the peer
	 * 
	 * @param piece
	 *            the {@link Piece} being sent to the peer
	 * @param offset
	 *            the block offset to send to the peer
	 * @param size
	 *            the size of the block to send to the peer
	 * @throws IOException
	 */
	public void sendPiece(Piece piece, int offset, int payload_size) throws IOException {
		byte[] bytes = new byte[BtUtils.SIZE_OF_INT + BtUtils.PIECE_HEADER_SIZE + payload_size];
		ByteBuffer message = ByteBuffer.wrap(bytes);
		message.putInt(BtUtils.PIECE_HEADER_SIZE + payload_size);
		message.put((BtUtils.PIECE_ID));
		message.putInt(piece.getIndex());
		message.putInt(offset);
		message.put(piece.getBytes(offset, payload_size));
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
		downloaded += payload_size;
	}

	/**
	 * Converts a given boolean array into a bitfield and sends the bitfield to
	 * the connected peer
	 * 
	 * @param has_piece
	 *            boolean array to send as a bitfield
	 * @throws IOException
	 */
	public void sendBitfield(boolean[] has_piece) throws IOException {
		BitSet bitset = new BitSet(has_piece.length);
		bitset.clear();
		for (int i = 0; i < has_piece.length; i++) {
			if (has_piece[i]) {
				bitset.set(i);
			}
		}
		byte[] bitfield = bitset.toByteArray();
		ByteBuffer message = ByteBuffer.wrap(new byte[BtUtils.PREFIX_LENGTH + bitfield.length]);
		message.put(BtUtils.BITFIELD_ID);
		message.put(bitfield);
		synchronized (outputStream) {
			outputStream.write(message.array());
			outputStream.flush();
		}
	}

	/**
	 * Reads a message form the peer
	 * 
	 * @return null if no bytes were read or if there were an incorrect number
	 *         of byte representing the length_prefix. Otherwise returns the
	 *         message (without its length prefix, message_id is in index = 0)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public byte[] getMessage() throws IOException, InterruptedException {
		connection.setSoTimeout(120000);
		int length = inputStream.readInt();
		byte[] message = new byte[length];
		synchronized (inputStream) {
			inputStream.readFully(message, 0, length);
		}
		connection.setSoTimeout(0);
		return message;
	}

	public void setInteresting(boolean interesting) {
		this.interesting = interesting;
	}

	public boolean isInteresting() {
		return interesting;
	}
}