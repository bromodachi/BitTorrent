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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.lang.*;

public class CommunicationTracker {
	TorrentInfoRU torrentInfoRU;
	String IPAddress;
	int port;
	URL urlAddress;
	int responseCode;
	Map<ByteBuffer, Object> responseMap;
	ByteBuffer peer_bytes;
	ByteBuffer failure_bytes;
	
	/**/
	int uploaded=0;
	int downloaded=0;
	int left=0;
	/**/
	
	int interval;
	int complete;
	int incomplete;
	List <Map<ByteBuffer, Object>>peers=null;
	
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
	public byte[] getRandomPeer(){
		byte [] random_peer=new byte[20];
		Random random=new Random();
		random.nextBytes(random_peer);
		return random_peer;
	}
	public void establishConnection(){
		HttpURLConnection connection = null;
		String line;
		
		byte [] random_peer=getRandomPeer();
		
		String fullUrl = torrentInfoRU.announce_url.toString() +
				"?info_hash=" + bytesToHex(torrentInfoRU.info_hash.array())+ 
				"&peer_id="+bytesToHex(random_peer)+"&port="+port
				+"&uploaded="+uploaded +"&downloaded="+downloaded+"&left="+left;
		
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
			System.out.println("Response code is: "+responseCode);
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
			System.out.println("Interval is: "+interval +"\n"+"Complete: "+complete+"\nIncomplete: "+incomplete);
			peers=(List<Map<ByteBuffer, Object>>)responseMap.get(ByteBuffer.wrap(new byte[]
					{'p','e','e','r','s'}));
			if(peers==null){
				System.out.println("Wtf am I null?");
				return;
			}
			if(peers.isEmpty()){
				System.out.println("Wtf am I empty?");
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
		
		try {
			InputStream input =  connection.getInputStream();
			System.out.println("success??");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("failure");
		}
		
	}

}