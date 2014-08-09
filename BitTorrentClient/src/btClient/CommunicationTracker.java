/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * This class is tasked with handling communications with the torrent tracker,
 * such as establishing a connection, finding peers, and alerting the tracker to
 * the current state of the download
 * 
 */
public class CommunicationTracker {
	private TorrentInfo torrentInfo;
	private URL urlAddress;
	int responseCode;
	Map<ByteBuffer, Object> responseMap;
	ByteBuffer peer_bytes;
	ByteBuffer failure_bytes;
	/* perhaps change the one later */
	public ArrayList<Peer> peersList;

	/**/
	String event = "";
	int uploaded = 0;
	int downloaded = 0;
	int left = 0;
	/**/

	ArrayList<Map<ByteBuffer, Object>> peers;
	/* Below is client information */
	private final ByteBuffer clientID;
	int min_interval;
	int interval;
	int complete;
	int incomplete;
	private int connectPort;

	/* ================= Constructor =================== */

	public CommunicationTracker(TorrentInfo passTheTorrentFile) {
		this.torrentInfo = passTheTorrentFile;
		this.urlAddress = torrentInfo.announce_url;
		urlAddress.getHost();
		urlAddress.getPort();
		this.clientID = ByteBuffer.wrap(getRandomID());

	}

	/* ================= Getters ======================== */

	public ByteBuffer getClientID() {
		return clientID;
	}

	public TorrentInfo getTorrent() {
		return torrentInfo;
	}

	public ArrayList<Peer> getPeersList() {
		return peersList;
	}

	public int getInterval() {
		return interval;
	}

	public int complete() {
		return complete;
	}

	public int incomplete() {
		return incomplete;
	}
	
	public int getMinInterval(){
		return min_interval;
	}

	/**
	 * generates a random peer to be used for the tracker
	 * 
	 * @return an array of bytes
	 */

	public byte[] getRandomID() {
		byte[] random_id = new byte[20];
		Random random = new Random();
		random.nextBytes(random_id);
		random_id[0] = (byte)'C';
		random_id[1] = (byte)'G';
		random_id[2] = (byte)'C';
		random_id[3] = (byte)'U';
		return random_id;
	}

	/**
	 * Formats the string into the correct format for the torrent file
	 * 
	 * @param blah
	 * @return the formatted string with the escape
	 */

	public String escape(String blah) {
		String result = null;
		try {
			result = URLEncoder.encode(blah, "ISO-8859-1");

		} catch (Exception e) {

		}
		return result;

	}

	/**
	 * Common behavior is for a downloader to try to listen on port 6881 and if
	 * that port is taken try 6882, then 6883, etc. and give up after 6889.
	 * 
	 * @return a valid port.
	 */
	public int getPort() {
		for (int i = 6881; i <= 6889; i++) {
			try {
				@SuppressWarnings({ "unused", "resource" })
				ServerSocket testing = new ServerSocket(i);
				return i;
			} catch (IOException e) {
				// try next port
			}

		}
		return -1;

	}

	/**
	 * Communicates with the tracker to get the list of peers. 
	 * @param event -> This takes an event to properly format the url and
	 * communicate with the tracker.
	 * Once we get the tracker, it extracts all the information in order to start
	 * exchanging messages like getting the list of peers.
	 * @throws BtException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void CommunicateWithTracker(String event, int downloaded) throws BtException {
		if (event.equals("completed")) {
			this.event = "completed";
			this.downloaded = torrentInfo.file_length;
			this.left = 0;
		} else if (event.equals("started")) {
			this.event = "started";
			this.downloaded = 0;
			this.left = torrentInfo.file_length;
		}
		else if(event.equals("stopped")){
			this.event = "stopped";
			this.downloaded = downloaded;
			this.left =torrentInfo.file_length -downloaded;
		}
		else if(event.equals(" ")){
			this.event="";
			this.downloaded=downloaded;
			this.left=torrentInfo.file_length-downloaded;
		}
		HttpURLConnection connection = null;
		/* get connection port. */
		connectPort = getPort();
		if (connectPort == -1) {
			System.err.println("Couldn't connect to a port Exiting...");
			throw new BtException("could not connect to a port");
		}
		/* making the string fullUrl */
		String fullUrl = "";
		try {
			// change length later
			fullUrl = torrentInfo.announce_url.toString()
					+ "?info_hash="
					+ escape(new String(torrentInfo.info_hash.array(),
							"ISO-8859-1")) + "&peer_id="
					+ escape(new String(clientID.array())) + "&port="
					+ connectPort + "&uploaded=" + uploaded + "&downloaded="
					+ this.downloaded + "&left=" + this.left  + "&event="
					+ this.event;
			System.err.println(fullUrl);
			System.err.println(event);
		} catch (UnsupportedEncodingException e2) {
			throw new BtException(e2.getMessage());
		}
	
		try {
			urlAddress = new URL(fullUrl);
		} catch (MalformedURLException e1) {
			throw new BtException(e1.getMessage());
		}
		try {
			/* http connection */

			connection = (HttpURLConnection) urlAddress.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			responseCode = connection.getResponseCode();
			DataInputStream reader = new DataInputStream(
					connection.getInputStream());

			byte[] response = new byte[connection.getContentLength()];
			reader.readFully(response);
			reader.close();
			/* end of http connection */

			try {
				responseMap = (Map<ByteBuffer, Object>) Bencoder2
						.decode(response);
			} catch (BencodingException e) {
				System.err.println("Bencoder couldn't get the map");
				throw new BtException(e.getMessage());
			}
			if (responseMap.containsKey(ByteBuffer
					.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ',
							'r', 'e', 'a', 's', 'o', 'n' }))) {
				failure_bytes = (ByteBuffer) responseMap.get(ByteBuffer
						.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e',
								' ', 'r', 'e', 'a', 's', 'o', 'n' }));
				String errorMessage = new String(failure_bytes.array(), "ASCII");
				System.err.println("Failure: " + errorMessage);
				throw new BtException(errorMessage);
			}
			min_interval=interval = (int) responseMap.get(ByteBuffer.wrap(new byte[] {
					'm', 'i','n', ' ','i','n', 't', 'e', 'r', 'v', 'a', 'l' }));

			interval = (int) responseMap.get(ByteBuffer.wrap(new byte[] { 'i',
					'n', 't', 'e', 'r', 'v', 'a', 'l' }));
			complete = (int) responseMap.get(ByteBuffer.wrap(new byte[] { 'c',
					'o', 'm', 'p', 'l', 'e', 't', 'e' }));
			incomplete = (int) responseMap.get(ByteBuffer.wrap(new byte[] {
					'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' }));

			peers = (ArrayList<Map<ByteBuffer, Object>>) responseMap
					.get(ByteBuffer
							.wrap(new byte[] { 'p', 'e', 'e', 'r', 's' }));

			if (peers == null) {
				// real error message later
				System.err.println("Peers were not extracted");
				throw new BtException("Peers were not extracted");
			}
			peersList = new ArrayList<Peer>(peers.size());
			System.err.println("Communication tracker got peer list of size: " + peers.size());
			int counter = 0;
			for (Map<ByteBuffer, Object> temp : peers) {
				ByteBuffer peer_id2 = (ByteBuffer) temp
						.get(ByteBuffer.wrap(new byte[] { 'p', 'e', 'e', 'r',
								' ', 'i', 'd' }));
				int peer_port = (int) temp.get(ByteBuffer.wrap(new byte[] {
						'p', 'o', 'r', 't' }));

				String peerID = new String(peer_id2.array(), "ASCII");
				ByteBuffer ip = (ByteBuffer) temp.get(ByteBuffer
						.wrap(new byte[] { 'i', 'p' }));
				String ipS = new String(ip.array(), "ASCII");

				Peer temp_peer = new Peer(ipS, peerID, peer_port);
				System.out.println("Id: "+peerID+ " ip: "+ipS+ " peer port: "+peer_port);
				peersList.add(temp_peer);
				counter++;
			}
			System.err.println("Created peers " + counter);
		} catch (IOException e) {
			System.err.println("Can't open the connection :c");
			throw new BtException("Could not open connection");
		}
	}

}