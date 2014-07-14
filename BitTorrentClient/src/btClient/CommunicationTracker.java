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

/*TO DO:
 * PORT MUST BE RANDOM(as in checking for 6681-6689
 * wireshark for testing*/

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
	String event="";
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
	private int connectPort;
	private boolean errors; //false if no errors.

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
	public boolean getError(){
		return errors;
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
	
	/**
	 * Formats the string into the correct format for the torrent file
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
	 *  Common behavior is for a downloader to try to listen on port 6881 
	 *  and if that port is taken try 6882, then 6883, etc. 
	 *  and give up after 6889.
	 * @return a valid port.
	 */
	public int getPort(){
		for(int i=6881;i<=6889;i++){
			try {
				@SuppressWarnings({ "unused", "resource" })
				ServerSocket testing=new ServerSocket(i);
				return i;
			} catch (IOException e) {
				//try next port
			}
			
		}
		return -1;
		
	}
	/**
	 * Communicates with the tracker to get the list of peers
	 */
	@SuppressWarnings("unchecked")
	public void CommunicateWithTracker(String event) {
		if(event.equals("completed")){
			this.event="completed";
			this.downloaded=torrentInfo.file_length;
			this.left=0;
		}
		else if(event.equals("started")){
			this.event="started";
			this.downloaded=0;
			this.left=torrentInfo.file_length;
		}
		HttpURLConnection connection = null;
		/*get connection port. */
		connectPort=getPort();
		if(connectPort==-1){
			System.err.println("Couldn't connect to a port\nExiting...");
			this.errors=true;
			return;
		}
		/* making the string fullUrl */
		String fullUrl = "";
		try {
			// change length later
			fullUrl = torrentInfo.announce_url.toString()
					+ "?info_hash="
					+ escape(new String(torrentInfo.info_hash.array(),
							"ISO-8859-1")) + "&peer_id="
					+ escape(new String(clientID.array())) + "&port=" + connectPort
					+ "&uploaded=" + uploaded + "&downloaded=" + this.downloaded
					+ "&left=" + this.left + "&event="
					+ this.event;
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			errors=true;
			return;
		}
		try {
			urlAddress = new URL(fullUrl);
		} catch (MalformedURLException e1) {
			//we should create
			// TODO Auto-generated catch block
			e1.printStackTrace();
			errors=true;
			return;
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
				System.err.println("Bencoder couldn't get the map");
				errors=true;
				return;
			}
			if (responseMap.containsKey(ByteBuffer
					.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ',
							'r', 'e', 'a', 's', 'o', 'n' }))) {
				failure_bytes = (ByteBuffer) responseMap.get(ByteBuffer
						.wrap(new byte[] { 'f', 'a', 'i', 'l', 'u', 'r', 'e',
								' ', 'r', 'e', 'a', 's', 'o', 'n' }));
				String errorMessage = new String(failure_bytes.array(), "ASCII");
				System.err.println("Failure: " + errorMessage);
				errors=true;
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
				System.err.println("Peers were not extracted");
				errors=true;
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
			System.err.println("Can't open the connection :c");
			errors=true;
			return;
		}
	}

}
