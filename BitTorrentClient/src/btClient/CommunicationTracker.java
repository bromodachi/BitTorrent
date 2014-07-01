package btClient;
import java.io.IOException;
import java.net.*;

public class CommunicationTracker {
	TorrentInfoRU torrentInfoRU;
	String IPAddress;
	int port;
	URL urlAddress;
	int responseCode;
	
	public CommunicationTracker(TorrentInfoRU passTheTorrentFile){
		this.torrentInfoRU=passTheTorrentFile;
		this.urlAddress=torrentInfoRU.announce_url;
		this.IPAddress=urlAddress.getHost();
		this.port=urlAddress.getPort();
		
	}
	
	public void establishConnection(){
		HttpURLConnection connection;
		try {
			connection=(HttpURLConnection) urlAddress.openConnection();
			connection.setRequestMethod("GET");
			responseCode=connection.getResponseCode();
			System.out.println(responseCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't open the connection :c");
		}
		
		
	}

}
