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
	public ArrayList<Peer> peersList;
	
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
	
	final protected static String hexArray = "0123456789ABCDEF";
	public static String bytesToHex(byte[] bytes) {
	    StringBuilder finalHex=new StringBuilder(bytes.length *2);
	    for (byte b : bytes) {
	        finalHex.append('%').append(hexArray.charAt((b & 0xF0) >> 4))
	        .append(hexArray.charAt((b & 0x0F)));
	    }
	    
	    
	    return finalHex.toString();
	}
	
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
	public String escape(String blah){
		String result=null;
		try{
			result=URLEncoder.encode(blah, "ISO-8859-1");
			
		}
		catch(Exception e){
			
		}
		return result;
				
		
	}
	
	@SuppressWarnings("unchecked")
	public void establishConnection(){
		System.out.println("checking something: "+torrentInfoRU.info_hash.array());
		HttpURLConnection connection = null;
	
		
		byte [] random_peer=getRandomID();
		
		System.out.println(torrentInfoRU.info_hash);
		/*making the string fullUrl*/
		String fullUrl="";
		try {
			fullUrl = torrentInfoRU.announce_url.toString() +
					"?info_hash=" + escape(new String(torrentInfoRU.info_hash.array(), "ISO-8859-1"))+ 
					"&peer_id="+escape(new String(random_peer))+"&port="+ 6681
					+"&uploaded="+uploaded +"&downloaded="+downloaded+"&left="+torrentInfoRU.file_length
					+"&event="+"started";
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	//	String fullUrl="http://128.6.171.130:6969/announce?info_hash=%A2%B9%AAWU%A5z*%AD%7BSE%DE%C1%06%DE%AD%93%3AK&peer_id=GvdxnngWwbBpHRpCkrNP&port=6881&uploaded=0&downloaded=0&left=1246427&event=started";
		System.out.println("Full URL: "+ fullUrl);
		try {
			urlAddress = new URL(fullUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			/*http connection*/
			
			connection=(HttpURLConnection) urlAddress.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			String message=connection.getResponseMessage();
			System.out.println("Response message is: "+message);
			responseCode=connection.getResponseCode();
			System.out.println("Response code is: "+responseCode+"Url address: "+fullUrl+
					"\nContent length is: "+connection.getContentLength());
			DataInputStream reader=new DataInputStream(connection.getInputStream());
	
			byte [] response=new byte[connection.getContentLength()];
			System.out.println(response);
			reader.readFully(response);
			reader.close();
			/*end of http connection*/
			
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
			
			peers=(ArrayList<Map<ByteBuffer, Object>>)responseMap.get(ByteBuffer.wrap(new byte[]
					{'p','e','e','r','s'}));
			
			
			
			//String pieces=new String(peer_id.array(), "ASCII");
			if(peers==null){
				//real error message later
				System.out.println("Wtf am I null?");
				return;
			}
			if(peers.isEmpty()){
				System.out.println("No peers");
			}
			if(!peers.isEmpty()){
				System.out.println(" am I not empty?");
			}
			peersList=new ArrayList<Peer>(peers.size());
			System.out.println("Size of the peers: "+peers.size());
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
				
				/*(int interval, int complete, int incomplete,
			String IP, String peer_id, int port)*/
				Peer temp_peer=new Peer(interval, complete, incomplete, ipS, peerID, peer_port);
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