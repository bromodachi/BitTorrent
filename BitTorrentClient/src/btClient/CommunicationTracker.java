package btClient;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.lang.*;

/*TO DO:
 * PORT MUST BE RANDOM
 * wireshark for testing*/

public class CommunicationTracker {
	TorrentInfoRU torrentInfoRU;
	String IPAddress;
	int port;
	URL urlAddress;
	int responseCode;
	Map<ByteBuffer, Object> responseMap;
	ByteBuffer peer_bytes;
	ByteBuffer failure_bytes;
	/*perhaps change the one later*/
	public static ArrayList<Peers> peersList;
	
	/**/
	int uploaded=0;
	int downloaded=0;
	int left=0;
	/**/
	
	int interval;
	int complete;
	int incomplete;
	ArrayList <Map<ByteBuffer, Object>>peers;
	
	public CommunicationTracker(TorrentInfoRU passTheTorrentFile){
		this.torrentInfoRU=passTheTorrentFile;
		this.urlAddress=torrentInfoRU.announce_url;
		this.IPAddress=urlAddress.getHost();
		this.port=urlAddress.getPort();
		
	}
	/*below is stolen from stackoverflow...*/
	final protected static String hexArray = "0123456789ABCDEF";
	public static String bytesToHex(byte[] bytes) {
	    StringBuilder finalHex=new StringBuilder(bytes.length *2);
	    for ( final byte b : bytes) {
	        finalHex.append('%').append(hexArray.charAt((b & 0xF0) >> 4))
	        .append(hexArray.charAt((b & 0x0F)));
	    }
	    
	    
	    return finalHex.toString();
	}
	/*end from stackoverflow*/
	/**
	 * generates a  random peer to be used for the tracker
	 * @return
	 */
	
	
	public byte[] getRandomID(){
		byte [] random_peer=new byte[20];
		Random random=new Random();
		random.nextBytes(random_peer);
		return random_peer;
	}
	public void establishConnection(){
		System.out.println(torrentInfoRU.file_name);
		HttpURLConnection connection = null;
		String line;
		
		byte [] random_peer=getRandomID();
		
		String fullUrl = torrentInfoRU.announce_url.toString() +
				"?info_hash=" + bytesToHex(torrentInfoRU.info_hash.array())+ 
				"&peer_id="+bytesToHex(random_peer)+"&port="+ 6681
				+"&uploaded="+uploaded +"&downloaded="+downloaded+"&left="+left
				+"&event="+"started";
		
		try {
			urlAddress = new URL(fullUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			connection=(HttpURLConnection) urlAddress.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			responseCode=connection.getResponseCode();
			System.out.println("Response code is: "+responseCode+"Url address: "+fullUrl);
			DataInputStream reader=new DataInputStream(connection.getInputStream());
			byte [] response=new byte[connection.getContentLength()];
			
			reader.readFully(response);
			reader.close();
			System.out.println(response+" "+connection.getContentLength());
			try {
				responseMap=(Map<ByteBuffer, Object>)Bencoder2.decode(response);
			} catch (BencodingException e) {
				// TODO Auto-generated catch block
				System.out.println("Bencoder couldn't get the map");
			}
			if(responseMap.containsKey(ByteBuffer.wrap(new byte[]
					{'f','a','i','l','u','r','e',' ', 'r','e','a','s','o','n'}))){
				failure_bytes=(ByteBuffer)responseMap.get(ByteBuffer.wrap(new byte[]
						{'f','a','i','l','u','r','e',' ', 'r','e','a','s','o','n'}));
				String errorMessage=new String(failure_bytes.array(), "ASCII");
				System.out.println("Failure: "+ errorMessage);
				return;
			}
			
			interval=(int)responseMap.get(ByteBuffer.wrap(new byte[]
					{'i','n','t','e','r','v','a','l'}));
			complete=(int)responseMap.get(ByteBuffer.wrap(new byte[]
					{'c','o','m','p','l','e','t','e'}));
			incomplete=(int)responseMap.get(ByteBuffer.wrap(new byte[]
					{'i','n','c','o','m','p','l','e','t','e'}));
			ByteBuffer peer_id=(ByteBuffer)responseMap.get(ByteBuffer.wrap(new byte[]
					{'p','e','e','r','_','i','d'}));
			System.out.println("Interval is: "+interval +"\n"+"Complete: "+complete+"\nIncomplete: "+incomplete);
			
			peers=(ArrayList<Map<ByteBuffer, Object>>)responseMap.get(ByteBuffer.wrap(new byte[]
					{'p','e','e','r','s'}));
			
			//String pieces=new String(peer_id.array(), "ASCII");
			if(peers==null){
				System.out.println("Wtf am I null?");
				return;
			}
			if(peers.isEmpty()){
				System.out.println("No peers");
			}
			if(!peers.isEmpty()){
				System.out.println("Wtf am I not empty?");
			}
			for(Map<ByteBuffer, Object> temp:peers){
				ByteBuffer peer_id2=(ByteBuffer) temp.get(ByteBuffer.wrap(new byte[]
						{'p', 'e','e','r',' ','i','d'}));
				int peer_port=(int) temp.get(ByteBuffer.wrap(new byte[]
						{'p', 'o','r','t'}));
				
				String peerID=new String(peer_id2.array(), "ASCII");
				ByteBuffer ip=(ByteBuffer) temp.get(ByteBuffer.wrap(new byte[]
						{'i', 'p'}));
				String ipS=new String(ip.array(), "ASCII");
				System.out.println("peer id is: "+peerID+"\nIP is: "+ipS+ "\nport: "+peer_port);
				peersList=new ArrayList<Peers>(peers.size());
				/*(int interval, int complete, int incomplete,
			String IP, String peer_id, int port)*/
				Peers temp_peer=new Peers(interval, complete, incomplete, ipS, peerID, peer_port);
				peersList.add(temp_peer);
			}
			
			/*peer_bytes=(ByteBuffer) responseMap.get(ByteBuffer.wrap(new byte[]
					{'p', 'e','e','r','s'}));
			String peerID=new String(peer_bytes.array(), "ASCII");
			/*BufferedReader read=new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer response = new StringBuffer();
			while(((line=read.readLine())!=null)){
				response.append(line);
			}
			read.close();*/
		//	System.out.println(interval);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't open the connection :c");
		}
		
		
		
	}

}