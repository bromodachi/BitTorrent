package btClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;

import btClient.BtUtils.Status;

public class GetWorstPeer extends TimerTask{

	ArrayList<Peer> peers;
	ActiveTorrent torrent;
	public GetWorstPeer(ArrayList<Peer> p, ActiveTorrent t){
		this.peers=p;
		this.torrent=t;
	}
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("=============COming through, move aside ======");
		int worstUploadOrDownload=Integer.MAX_VALUE;
		Peer theWorstLeecher=null;
		for(Peer peer: peers){
			if(!peer.isChoked()){
				if(torrent.getStatus()==Status.Active && peer.isConnected()){
					System.out.println("======downloading atm=========== "+ peer.getDownloaded());
					if(worstUploadOrDownload>peer.getDownloaded()){
						System.out.println("In if");
						worstUploadOrDownload=peer.getDownloaded();
						theWorstLeecher=peer;
					}
				}
				else if(torrent.getStatus()==Status.Seeding && peer.isConnected()){
					System.out.println("===========Seding atm=========");
					//If the peer is not choked.
						if(worstUploadOrDownload>peer.getUploaded()){
							worstUploadOrDownload=peer.getUploaded();
							theWorstLeecher=peer;
						}
					
				}
			}
		}
		//I need to pick a random peer to unchoke before i send a choke
		ArrayList<Peer> chokedPeers= new ArrayList<Peer> ();
		for(Peer p: peers){
			if(p.isChoked()){
				chokedPeers.add(p);
			}
		}
		Random random=new Random();
		Peer unChokeMe=chokedPeers.get(random.nextInt(chokedPeers.size()));
		System.err.println("Sending a choke message!!!"+ theWorstLeecher.getIP()+theWorstLeecher.getUploaded());
		
		try {
			theWorstLeecher.sendChoke();
			theWorstLeecher.setChoked(true);
			//will probably remove this since we will only connect to see 6 peers
			if(chokedPeers.size()<3){
				return;
			}
			System.err.println("Sending an unchoke message!!!"+ unChokeMe.getIP()+unChokeMe.getUploaded());
			unChokeMe.sendUnchoke();
			unChokeMe.setChoked(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		//	System.exit(0);
		}
		
		
	}
	
	

}