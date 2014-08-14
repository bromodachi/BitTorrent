package btClient;

import java.util.ArrayList;
import java.util.TimerTask;

public class UpdateTracker extends TimerTask{
	ActiveTorrent activeTorrent;
	ArrayList<Peer> peers;
	
	public UpdateTracker(ActiveTorrent torrent){
		this.activeTorrent=torrent;
		this.peers=activeTorrent.getPeerList();
	}
	
	public void run(){
		ArrayList<Peer> updatesPeers;
		try {
			activeTorrent.getCommunicarionTracker().CommunicateWithTracker(" ", activeTorrent.getBytesCompleted());
		} catch (BtException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updatesPeers=activeTorrent.getCommunicarionTracker().getPeersList();
		boolean addMe=false;
		for(Peer p: updatesPeers){
			for( Peer q: this. activeTorrent.getPeerList()){
				//if we find a matching peer class, break out of the 2nd for loop don't add
				if(p.equals(q)){
					addMe=true;
					break;
				}
			}
			//if it's still false, we can safely add the peer
			if(!addMe){
				//only add rutgers IP addresses. 
				if(p.getIP().equals("128.6.171.130")||p.getIP().equals("128.6.171.131")){
					this.activeTorrent.addPeerToList(p);
				}
			}
			//set addMe to false before we go back to the 2nd loop
			addMe=false;
		}
		
	}

}