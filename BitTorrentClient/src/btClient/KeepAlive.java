package btClient;

import java.io.IOException;

public class KeepAlive extends Thread{
	private Peer peer;
	public KeepAlive(Peer p){
		this.peer=p;
	}
	
	public void run(){
		while(this.peer.isConnected()){
			try{
				this.sleep(120000);
			}
			catch(InterruptedException e){
				continue;
			}
			
			try {
				//make sure peer is still connected
				if(peer.isConnected()){
					System.out.println("Sending a keep alive message");
					this.peer.sendKeepAlive();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("An error has occured");
			}
		}
	}
	
	
	

}
