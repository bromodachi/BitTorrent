package btClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;

import btClient.sendMessages.sendChoke;
import btClient.sendMessages.sendUnchoke;

public class GetWorstPeer extends TimerTask{

	ArrayList<Peer> peers;
	public GetWorstPeer(ArrayList<Peer> p){
		this.peers=p;
	}
	public void run() {
		// TODO Auto-generated method stub
		int worstUploadOrDownload=0;
		Peer theWorstLeecher=null;
		for(Peer peer: peers){
			if(RUBTClient.clientIsDownloading && peer.getIsDownloading()){
				System.out.println("In if before the next if while downloading");
				if(peer.getDownloaded()<=worstUploadOrDownload){
					System.out.println("In if");
					worstUploadOrDownload=peer.getUploaded();
					theWorstLeecher=peer;
				}
			}
			else if(!(RUBTClient.clientIsDownloading) && peer.getIsDownloading()){
				//If the peer is not choked.
					if(peer.getUploaded()<=worstUploadOrDownload){
						worstUploadOrDownload=peer.getUploaded();
						theWorstLeecher=peer;
					}
				
			}
		}
		System.err.println("Sending a choke message!!!"+ theWorstLeecher.getIP()+theWorstLeecher.getUploaded());
		sendMessages sendMe=new sendChoke(theWorstLeecher.getOutputStream());
		theWorstLeecher.setIsDownloading(false);
		theWorstLeecher.handleSendMessages(sendMe);;
		RUBTClient.listOfChokers(theWorstLeecher);
		
		if(RUBTClient.chokers.size()==3){
			Random random=new Random();
			int i=random.nextInt(RUBTClient.chokers.size());
			Peer p=RUBTClient.getAChokePeer(i);
			System.err.println("Unchoking a peer!!!!");
			sendMessages sendUnchoke=new sendUnchoke(p.getOutputStream());
			p.setIsDownloading(true);
		}
		
		
	}
	
	

}
