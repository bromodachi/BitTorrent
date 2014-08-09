package btClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

public class KeepAlive extends TimerTask {
	private ActiveTorrent torrent;

	public KeepAlive(ActiveTorrent torrent) {
		this.torrent = torrent;
	}

	public void run() {
		ArrayList<Peer> peers = torrent.getPeerList();
		synchronized (peers) {
			for (Peer peer : peers) {
				try {
					peer.sendKeepAlive();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}