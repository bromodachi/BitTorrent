/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Timer;

/**
 * This is the main client class for CS352, BitTorrent project 1 The program is
 * designed to load a .torrent file, interface with a tracker and a single peer
 * and will download a single file (a JPEG) from that peer. The file is then
 * saved to a hard disk. All communication is done over TCP.
 * 
 * @author Cody Goodman & Conrado Uraga
 *
 */
public class RUBTClient {
	static File file = null;
	static ArrayList<Peer> peers = null;
	static CommunicationTracker communicationTracker;
	static boolean clientIsDownloading=false;
	static ArrayList<Peer> chokers=new ArrayList<Peer>();//

	/**
	 * This is the main method that is called upon program startup, this method
	 * is tasked with validating the program's arguments and then instantiating
	 * the other classes need to run the program. When the download is finished
	 * this method verifies that it has completed successfully
	 * 
	 * @param args
	 *            The name of the .torrent file to be loaded and the name of the
	 *            file to save the data to, using the proper path and file
	 *            extensions.
	 * @throws IOException
	 * @throws BencodingException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			BencodingException, InterruptedException {
		RUBTClientThread t=new RUBTClientThread(args);
		Thread clientThread=new Thread(t);
		t.start=true;
		clientThread.start();
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String reader;
		
		
		do{
			reader=br.readLine();
			
			if(reader.equalsIgnoreCase("end")){
				System.out.println("here");
				t.end=true;
				clientThread.join();
				break;
			}
		}while(true);
		

	}// END MAIN

	public static void start(){
		
	}
	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad.
	 * 
	 * @param args
	 *            The command line arguments from the main method.
	 * @return True if both arguments are valid, otherwise false
	 * @throws IOException
	 */
	public static boolean validateArgs(String[] args) throws IOException {
		// The main method requires two command line arguments:
		// The name of the torrent file to load, and the name to the resulting
		// file to save-as.
		if (args.length != 2) {
			System.err.println("Incorrect arguments. "
					+ "Need 'somefile.torrent' 'picture.jpg'");
			return false;
		}

		// Validating if arg[0] is a correct .torrent file extension
		String torrentChecker = args[0].substring(args[0].lastIndexOf(".") + 1,
				args[0].length());
		if (!(torrentChecker.equals("torrent"))) {
			System.err.println("Not a valid .torrent file, exiting program.");
			return false;
		}
		// check that a new file can be created with the second argument
		return true;
	}// END validateArgs

	/**
	 * Returns the byte array of a file to be used with Bencoder2.java. The byte
	 * array format is required as input to the Bencoder2 class. Requires jre7
	 * or greater.
	 * 
	 * 
	 * @param file
	 *            The file to be converted to a byte array
	 * @return byte The torrent file represented as a byte array
	 * @throws IOException
	 */
	public static byte[] getFileBytes(String fileName) throws IOException,
			BencodingException {

		// Create a file with the first argument
		File torrentFile = new File(fileName);

		// Make sure the file exists and it's not a directory
		if (!torrentFile.exists() || torrentFile.isDirectory()) {
			System.err.println("Couldn't load the torrent file.  "
					+ "Exiting program.");
			System.exit(1);
		}

		Path filePath = torrentFile.toPath();
		byte[] torrentBytes = Files.readAllBytes(filePath);
		if (torrentBytes == null) {
			System.err.println("Torrent file is empty.  Exiting program.");
			System.exit(1);
		}
		return torrentBytes;
	}// END getFileBytes

	/**
	 * Returns the specified peer for testing the RUBTClient
	 * 
	 * @param peers
	 *            the list of availible peers
	 * @return peer object for the test peer
	 */
	public static Peer getTestPeer(ArrayList<Peer> peers) {
		for (Peer curr : peers) {
			if (curr.getPeer_id().startsWith(BtUtils.RU_PEER_PREFIX_STRING)) {
				return curr;
			}
		}
		return null;
	}
	/**
	 * Adds a peer to list of chokers. This list will later be used to unchoke a peer
	 * @param p
	 */
	public static void listOfChokers(Peer p){
		chokers.add(p);
	}
	
	public static Peer getAChokePeer (int i){
		return chokers.get(i);
		
	}

	/**
	 * Creates a new piece object for the total number of pieces in the file to
	 * be downloaded and adds them to the pieces array list
	 * 
	 * @param pieces
	 *            ArrayList of piece objects
	 * @param activeTorrent
	 *            TorrentInfo object for the active download
	 * @return
	 * @throws FileNotFoundException
	 */
	private static boolean createPieces(ArrayList<Piece> pieces,
			TorrentInfo activeTorrent) throws FileNotFoundException {
		int leftover = activeTorrent.file_length % activeTorrent.piece_length;

		for (int i = 0; i < activeTorrent.piece_hashes.length; i++) {
			if (i == (activeTorrent.piece_hashes.length - 1)) {
				pieces.add(new Piece(i, leftover, i
						* activeTorrent.piece_length, file,
						activeTorrent.piece_hashes[i].array()));
			} else {
				pieces.add(new Piece(i, activeTorrent.piece_length, i
						* activeTorrent.piece_length, file,
						activeTorrent.piece_hashes[i].array()));
			}
		}
		return checkCompleteness(pieces);
	}

	/**
	 * Checks the completeness of the file by calling {@link Piece#isComplete()}
	 * for each {@link Piece}
	 * 
	 * @param pieces
	 *            ArrayList of pieces that make up the file
	 * @return True if all pieces are complete, otherwise false
	 */
	public static boolean checkCompleteness(ArrayList<Piece> pieces) {
		for (Piece piece : pieces) {
			if (!piece.isComplete()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes the percentage of the file that has been downloaded at a certain
	 * moment of time
	 * 
	 * @param pieces
	 *            ArrayList of all the piece objects for a file
	 * @return int percent download complete
	 */
	private static int getPercentComplete(ArrayList<Piece> pieces) {
		int completed = 0;
		for (Piece curr : pieces) {
			if (curr.isComplete()) {
				completed++;
			}
		}
		return (int) (((float) completed / (float) pieces.size()) * 100);
	}
	
	
	public static class RUBTClientThread implements Runnable{
	//	private boolean start=false;
		public boolean end=false;
		Worker w;
		private boolean start=false;
		String [] arg;
		public RUBTClientThread(String [] args){
			arg=args;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(start){
				w=new Worker(arg);
				try {
					w.main();
				} catch (IOException | BencodingException
						| InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			while(true){
				
				if(end){
					try {
						communicationTracker.CommunicateWithTracker("stopped", (int) file.length());
						break;
					} catch (BtException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
			
			
		}
	}
	public static class Worker {
		String [] args;
		public Worker(String [] arg){
			this.args=arg;
		}
		// Step 1 - Take the command line arguments
		public void main() throws IOException,
		BencodingException, InterruptedException{
				if (!validateArgs(args)) {
					return;
				}
				ArrayList<Piece> pieces = new ArrayList<Piece>();

				// Step 2 - Open the .torrent file and parse the data inside
				byte[] torrentBytes = getFileBytes(args[0]);
				TorrentInfo activeTorrent = new TorrentInfo(torrentBytes);
				// Create Piece objects based on activeTorrent info and add them to
				// pieces list
				file = new File(args[1]);
				if (!file.createNewFile()) {
					if (!file.exists()) {
						System.err.println("ERROR: Failed to create download file");
						return;
					}
				}

				// Step 3 - Send an HTTP GET request to the tracker
				communicationTracker = new CommunicationTracker(
						activeTorrent);
				try {
					communicationTracker.CommunicateWithTracker("started", (int) file.length());
				} catch (BtException e) {
					e.printStackTrace();
					System.err.println("Failed to send started message");
					return;
				}
				// Any errors in the communication tracker, we shouldn't proceed.
				if (createPieces(pieces, activeTorrent)) {
					System.out.println("File is already complete");
					return;
				}
				// Step 4 - Connect with the Peer.
				// Create new message handler and give it its own thread to run in
				peers = communicationTracker.getPeersList();
				ArrayList<Thread> threadList=new ArrayList<Thread>(peers.size());
				clientIsDownloading=true;
				for (int i=0; i<peers.size();i++){
					
					Thread thread = new Thread(new MessageHandler(pieces,
							peers.get(i), activeTorrent.info_hash,
							communicationTracker.getClientID(), activeTorrent, peers));
					threadList.add(thread);
					threadList.get(i).start();
				}
				
				
				GetWorstPeer test=new GetWorstPeer(peers);
				Timer timer = new Timer();

				while (getPercentComplete(pieces) != 100) {
					/*System.out.print("\rdownloading: " + getPercentComplete(pieces)
							+ "%");
					Thread.sleep(1000);*/
			//		timer.scheduleAtFixedRate(test, 30000, 30000);
					
				}
				for (Thread thread : threadList){
					thread.join();
				}
				System.out.print("\rdownloading: " + getPercentComplete(pieces) + "%");
				System.out.println();
				// Check download for completeness
				for (Piece curr : pieces) {
					if (!curr.isComplete()) {
						System.err
								.println("Disconnected before downloading all pieces");
						return;
					}
				}
				try {
					communicationTracker.CommunicateWithTracker("completed", (int) file.length());
					
				} catch (BtException e) {
					e.printStackTrace();
					System.err.println("Failed to send completed/stopped");
				}
				System.out.println("Download successful");
	}
	}
}