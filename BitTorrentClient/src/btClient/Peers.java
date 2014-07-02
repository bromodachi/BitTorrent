package btClient;

public class Peers {
	int interval;
	int complete;
	int incomplete;
	String IP;
	String peer_id;
	int port;
	
	public Peers(int interval, int complete, int incomplete,
			String IP, String peer_id, int port){
		this.interval=interval;
		this.complete=complete;
		this.incomplete=incomplete;
		this.IP=IP;
		this.peer_id=peer_id;
		this.port=port;
	}
	

}
