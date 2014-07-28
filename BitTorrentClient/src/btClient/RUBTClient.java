/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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

		// Step 1 - Take the command line arguments
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
		CommunicationTracker communicationTracker = new CommunicationTracker(
				activeTorrent);
		try {
			communicationTracker.CommunicateWithTracker("started");
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
		Thread thread = new Thread(new MessageHandler(pieces,
				getTestPeer(peers), activeTorrent.info_hash,
				communicationTracker.getClientID(), activeTorrent));
		thread.start();
		while (getPercentComplete(pieces) != 100) {
			System.out.print("\rdownloading: " + getPercentComplete(pieces)
					+ "%");
			Thread.sleep(1000);
		}
		System.out.print("\rdownloading: " + getPercentComplete(pieces) + "%");
		System.out.println();
		thread.join();
		// Check download for completeness
		for (Piece curr : pieces) {
			if (!curr.isComplete()) {
				System.err
						.println("Disconnected before downloading all pieces");
				return;
			}
		}
		try {
			communicationTracker.CommunicateWithTracker("completed");
			communicationTracker.CommunicateWithTracker("stopped");
		} catch (BtException e) {
			e.printStackTrace();
			System.err.println("Failed to send completed/stopped");
		}
		System.out.println("Download successful");

	}// END MAIN

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
}