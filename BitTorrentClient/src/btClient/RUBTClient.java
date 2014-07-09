package btClient;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


public class RUBTClient {
	
	static ArrayList<Piece>  pieces = null;
	
	/**
	 * This is the main client class for CS352, BitTorrent project 1
	 * The program is designed to load a .torrent file, interface with a 
	 * tracker and a single peer and will download a single file (a JPEG) from 
	 * that peer. The file is then saved to a hard disk. All communication is 
	 * done over TCP.
	 * 
	 * @param args The name of the .torrent file to be loaded and the name of 
	 * the file to save the data to, using the proper path and file extensions.
	 * @throws IOException
	 * @throws BencodingException
	 */
	public static void main(String[] args) throws IOException,
			BencodingException {

		TorrentInfo activeTorrent;

		// Step 1 - Take the command line arguments
		validateArgs(args);

		// Step 2 - Open the .torrent file and parse the data inside
		byte[] torrentBytes = getFileBytes(args[0]);
		activeTorrent = new TorrentInfo(torrentBytes);

		// Step 3 - Send an HTTP GET request to the tracker
		CommunicationTracker communicationTracker = new CommunicationTracker(
				activeTorrent);
		communicationTracker.CommunicateWithTracker();

		// Step 4 - Connect with the Peer.	
		Thread thread = new Thread(new MessageHandler(pieces, 
				communicationTracker.getPeersList().get(0), 
				activeTorrent.info_hash, communicationTracker.getClientID()));
		thread.start();

	}// END MAIN

	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad.
	 * 
	 * @param args The command line arguments from the main method.
	 */
	public static void validateArgs(String[] args) {
		// The main method requires two command line arguments:
		// The name of the torrent file to load, and the name to the resulting
		// file to save-as.
		if (args.length != 2) {
			System.err.println("Incorrect arguments. "
					+ "Need 'somefile.torrent' 'picture.jpg'");
			System.exit(1);
		}

		// Validating if arg[0] is a correct .torrent file extension
		String torrentChecker = args[0].substring(args[0].lastIndexOf(".") + 1,
				args[0].length());
		if (!(torrentChecker.equals("torrent"))) {
			System.err.println("Not a valid .torrent file, exiting program.");
		}
	}// END validateArgs

	/**
	 * Returns the byte array of a file to be used with Bencoder2.java. 
	 * The byte array format is required as input to the Bencoder2 class.
	 * Requires jre7 or greater.
	 * 
	 * 
	 * @param file The file to be converted to a byte array
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

	public Peer getTestPeer(ArrayList<Peer> peers) {
		ByteBuffer prefix;
		for (Peer curr : peers) {
			prefix = ByteBuffer.wrap(curr.getPeer_id().getBytes(), 0,
					BtUtils.RU_PEER_PREFIX.length);
		}
		return null;
	}
}