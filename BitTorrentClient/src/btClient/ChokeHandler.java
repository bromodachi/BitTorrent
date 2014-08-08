package btClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;

import btClient.BtUtils.Status;

public class ChokeHandler extends TimerTask {

	private ArrayList<Peer> peers;
	private ActiveTorrent torrent;

	public ChokeHandler(ActiveTorrent torrent) {
		this.peers = torrent.getPeerList();
		this.torrent = torrent;
	}

	public void run() {
		System.err.println("Running ChokeHandler task ");
		/* Check status of torrent */
		switch (torrent.getStatus()) {
		// If torrent is active (downloading) choke worst uploader
		case Active:
			if (torrent.getUnchokedPeerCount() >= BtUtils.MAX_UNCHOKED_PEERS) {
				try {
					chokeWorstUploader();
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
			}
			break;
		// if torrent is seeding then choke worst downloader
		case Seeding:
			if (torrent.getUnchokedPeerCount() >= BtUtils.MAX_UNCHOKED_PEERS) {
				try {
					chokeWorstDownloader();
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
			}
			break;
		// all other cases do nothing
		default:
			return;

		}
		// If less than the maximum number of peers are unchoked, unchoke peers
		// until the max is hit
		while (torrent.getUnchokedPeerCount() < BtUtils.MAX_UNCHOKED_PEERS) {
			try {
				unchokeRandomPeer();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Finds the peer with the worst uploaded counter and chokes it, also resets
	 * the downloaded and uploaded counters for each peer
	 * 
	 * @throws IOException
	 */
	private void chokeWorstUploader() throws IOException {
		int worst_upload = Integer.MAX_VALUE;
		Peer worst_peer = null;
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getUploaded() < worst_upload && !peer.isChoked()
						&& peer.isConnected() && !peer.isClosed()) {
					worst_upload = peer.getUploaded();
					worst_peer = peer;
				}
				peer.resetDownloaded();
				peer.resetUploaded();
			}
		}
		worst_peer.sendChoke();
		worst_peer.setChoked(true);
		torrent.decrementUnchokedPeerCount();
		System.err.println("Choked worst uploader peer " + worst_peer.getPeer_id());

	}

	/**
	 * Finds the peer with the worst downloaded counter and chokes it, also
	 * resets the downloaded and uploaded counters for each peer
	 * 
	 * @throws IOException
	 */
	private void chokeWorstDownloader() throws IOException {
		int worst_download = Integer.MAX_VALUE;
		Peer worst_peer = null;
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getDownloaded() < worst_download && !peer.isChoked()
						&& peer.isConnected() && !peer.isClosed()) {
					worst_download = peer.getDownloaded();
					worst_peer = peer;
				}
				peer.resetDownloaded();
				peer.resetUploaded();
			}
		}
		worst_peer.sendChoke();
		worst_peer.setChoked(true);
		torrent.decrementUnchokedPeerCount();
		
		System.err.println("Choked worst downloader peer " + worst_peer.getPeer_id());
	}

	/**
	 * Picks a random choked and connected peer to unchoked (optimistic
	 * unchoking)
	 * 
	 * @throws IOException
	 */
	private void unchokeRandomPeer() throws IOException {
		ArrayList<Peer> choked_peers = new ArrayList<Peer>();
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.isChoked() && peer.isConnected() && !peer.isClosed() && peer.isInterested()) {
					choked_peers.add(peer);
				}
			}
		}
		Random random = new Random();
		Peer peer = choked_peers.get(random.nextInt(choked_peers.size()));
		peer.sendUnchoke();
		peer.setChoked(false);
		torrent.incrementUnchokedPeerCount();
		System.err.println("Unchoked peer " + peer.getPeer_id());
	}

}