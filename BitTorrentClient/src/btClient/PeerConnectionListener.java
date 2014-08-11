package btClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class PeerConnectionListener implements Runnable {

	private final int port;

	private ArrayList<Peer> peers;
	private final ByteBuffer info_hash;
	private final ByteBuffer clientID;
	private ServerSocket serverSocket = null;
	private ActiveTorrent torrent = null;

	private boolean listening = false;

	public PeerConnectionListener(ActiveTorrent torrent, int port) {
		this.torrent = torrent;
		this.port = port;
		this.peers = torrent.getPeerList();
		this.info_hash = torrent.getTorrentInfo().info_hash;
		this.clientID = torrent.getClientID();
	}

	@Override
	public void run() {
		listening = true;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		while (listening) {
			try {
				listenForConnection();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listenForConnection() throws IOException {

		Socket connection = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;

		connection = serverSocket.accept();
		inputStream = new DataInputStream(connection.getInputStream());
		outputStream = new DataOutputStream(connection.getOutputStream());

		// get handshake message from peer
		byte[] message = new byte[BtUtils.p2pHandshakeLength];
		inputStream.readFully(message);

		// Ensure message received is a proper BitTorrent Protocol handshake
		if (!isBTProtocol(message)) {
			closeEverything(connection, inputStream, outputStream);
		}

		// Ensure that correct info hash has been received
		if (!isSameHash(info_hash.array(), message)) {
			closeEverything(connection, inputStream, outputStream);
			return;
		}
		// Create reply message
		ByteBuffer handshake = ByteBuffer.wrap(new byte[BtUtils.p2pHandshakeLength]);
		handshake.put(BtUtils.p2pHandshakeHeader);
		handshake.put(info_hash.array());
		handshake.put(clientID.array());
		// Send reply message
		outputStream.write(handshake.array());
		outputStream.flush();

		// Create peer and add it to the peer list
		String peerIP = connection.getInetAddress().toString();
		// If peer is from an ip specified in the assignment: add to peer list
		if (peerIP.equals("128.6.171.130") || peerIP.equals("128.6.171.131")) {
			synchronized (peers) {
				torrent.addPeerToList(new Peer(peerIP, getPeerID(message), connection));
			}
		}
	}

	/**
	 * Checks if the begining of a byte array matches the BitTorrent protocol
	 * handshake header
	 * 
	 * @param message
	 *            byte array to be checked
	 * @return True if message is in BitTorrent format, otherwise false
	 */
	private boolean isBTProtocol(byte[] message) {
		for (int i = 0; i < BtUtils.p2pHandshakeHeader.length; i++) {
			if (message[i] != BtUtils.p2pHandshakeHeader[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Verifies that two hashes match
	 * 
	 * @param info_hash
	 * @param response_info_hash
	 * @return
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
	 * Closes all peer connection
	 * 
	 * @param connection
	 * @param inputStream
	 * @param outputStream
	 */
	public void closeEverything(Socket connection, DataInputStream inputStream, DataOutputStream outputStream) {
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
			e.printStackTrace();
		}

	}

	public String getPeerID(byte[] message) {
		return Arrays.copyOfRange(message, BtUtils.HANDSHAKE_PEER_ID_OFFSET, BtUtils.HANDSHAKE_PEER_ID_OFFSET + BtUtils.PEER_ID_LENGTH).toString();
	}

	public void setListening(boolean listening) {
		this.listening = listening;
	}
}
