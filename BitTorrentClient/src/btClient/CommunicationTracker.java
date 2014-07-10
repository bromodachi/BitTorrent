package btClient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.lang.*;

/*TO DO:
 * PORT MUST BE RANDOM(as in checking for 6681-6689
 * wireshark for testing*/

public class CommunicationTracker {
	private TorrentInfo torrentInfo;
	private final String IPAddress;
	private int port;
	private URL urlAddress;
	int responseCode;
	Map<ByteBuffer, Object> responseMap;
	ByteBuffer peer_bytes;
	ByteBuffer failure_bytes;
	/* perhaps change the one later */
	public ArrayList<Peer> peersList;

	/**/
	int uploaded = 0;
	int downloaded = 0;
	int left = 0;
	/**/

	ArrayList<Map<ByteBuffer, Object>> peers;
	/* Below is client information */
	private final ByteBuffer clientID;
	int interval;
	int complete;
	int incomplete;

	public CommunicationTracker(TorrentInfo passTheTorrentFile) {
		this.torrentInfo = passTheTorrentFile;
		this.urlAddress = torrentInfo.announce_url;
		this.IPAddress = urlAddress.getHost();
		this.port = urlAddress.getPort();
		this.clientID = ByteBuffer.wrap(getRandomID());

	}

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

	/**
	 * generates a random peer to be used for the tracker
	 * 
	 * @return an array of bytes
	 */

	public byte[] getRandomID() {
		byte[] random_id = new byte[20];
		Random random = new Random();
		random.nextBytes(random_id);
		return random_id;
	}

	public String escape(String blah) {
		String result = null;
		try {
			result = URLEncoder.encode(blah, "ISO-8859-1");

		} catch (Exception e) {

		}
		return result;

	}

	@SuppressWarnings("unchecked")
	public void CommunicateWithTracker() {

		HttpURLConnection connection = null;

		/* making the string fullUrl */
		String fullUrl = "";
		try {
			// change length later
			fullUrl = torrentInfo.announce_url.toString()
					+ "?info_hash="
					+ escape(new String(torrentInfo.info_hash.array(),
							"ISO-8859-1")) + "&peer_id="
					+ escape(new String(clientID.array())) + "&port=" + 6681
					+ "&uploaded=" + uploaded + "&downloaded=" + downloaded
					+ "&left=" + torrentInfo.file_length + "&event="
					+ "started";
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		// String
		// fullUrl="http://128.6.171.130:6969/announce?info_hash=%A2%B9%AAWU%A5z*%AD%7BSE%DE%C1%06%DE%AD%93%3AK&peer_id=GvdxnngWwbBpHRpCkrNP&port=6881&uploaded=0&downloaded=0&left=1246427&event=started";
		System.out.println("Full URL: " + fullUrl);
		try {
			urlAddress = new URL(fullUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
				// TODO Auto-generated catch block
				System.out.println("Bencoder couldn't get the map");
			}
			if (responseMap.containsKey(ByteBuffer
					.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ',
							'r', 'e', 'a', 's', 'o', 'n' }))) {
				failure_bytes = (ByteBuffer) responseMap.get(ByteBuffer
						.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e',
								' ', 'r', 'e', 'a', 's', 'o', 'n' }));
				String errorMessage = new String(failure_bytes.array(), "ASCII");
				System.out.println("Failure: " + errorMessage);
				return;
			}

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
				System.out.println("Peers were not extracted");
				return;
			}
			peersList = new ArrayList<Peer>(peers.size());
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
				peersList.add(temp_peer);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't open the connection :c");
		}
	}

}