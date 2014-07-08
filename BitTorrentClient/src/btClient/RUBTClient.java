package btClient;

import java.io.File;
import java.io.IOException;
import java.net.URL; //not sure if we need this one yet but let's see --Conrado, see my note below, -CW
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;

//Use ctrl-shift-o or cmd-shift-o in eclipse to automatically import the necessary classes -CW

public class RUBTClient {
	static ArrayList<Piece>  pieces = null;
	/**
	 * @param args
	 * @throws IOException
	 * @throws BencodingException
	 */
	public static void main(String[] args) throws IOException,
			BencodingException {

		TorrentInfo activeTorrent;

		// Validate args
		validateArgs(args);

		// Get all the bytes from the torrent file
		byte[] torrentBytes = getFileBytes(args[0]);

		// Decode the torrent to produce it's TorrentInfo object.
		// activeTorrent=decodeTorrent(torrentBytes); //This is for our
		// TorrentInfo class
		activeTorrent = new TorrentInfo(torrentBytes); // This is for Rutgers'
															// TorrentInfoRU
															// class

		// Output for testing

		// Step 3 - Send an HTTP GET request to the tracker
		CommunicationTracker communicationTracker = new CommunicationTracker(
				activeTorrent);
		communicationTracker.CommunicateWithTracker();

		// Step 4 - Connect with the Peer.
		
		Thread thread = new Thread(new MessageHandler(pieces, communicationTracker.getPeersList().get(0), activeTorrent.info_hash, communicationTracker.getClientID()));
		thread.start();

	}// END MAIN

	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad. We need to change the image
	 * file validation later in the project.
	 * 
	 * @param args
	 *            the command line arguments from the main method.
	 */
	public static void validateArgs(String[] args) {
		// The main method requires two command line arguments:
		// The name of the torrent file to load, and the name to the resulting
		// file to save-as.
		if (args.length != 2) {
			System.err
					.println("Incorrect arguments. Need 'somefile.torrent' 'picture.jpg'");
			System.exit(1);
		}

		// Validating if arg[0] is a correct .torrent file extension
		String torrentChecker = args[0].substring(args[0].lastIndexOf(".") + 1,
				args[0].length());
		if (!(torrentChecker.equals("torrent"))) {
			System.err.println("Not a valid .torrent file, exiting program.");
		}

		// TODO - Modify this later for general file types
		// Validating if arg[1] has correct file extension .JPG, .PNG, .GIF,
		// .jpg, .gif, .png;
		String extChecker = args[1].substring(args[1].lastIndexOf(".") + 1,
				args[1].length());
		if (!(extChecker.toLowerCase().equals("gif")
				|| extChecker.toLowerCase().equals("png") || extChecker
				.toLowerCase().equals("jpg"))) {
			System.err.println("Not a valid image file type, exiting program.");
			System.exit(1);
		}
		// System.out.println("Valid args.\n");
	}// END validateArgs

	/**
	 * Return the bytes of a torrent file to be used with Bencoder2.java
	 * Requires Java7
	 * 
	 * @param file
	 *            file to be converted to a byte array
	 * @return byte torrent file represented as a byte array
	 * @throws IOException
	 */
	public static byte[] getFileBytes(String fileName) throws IOException,
			BencodingException {

		// Create a file with the first argument
		File torrentFile = new File(fileName);

		// Make sure the file exists and it's not a directory
		if (!torrentFile.exists() || torrentFile.isDirectory()) {
			System.err
					.println("Couldn't load the torrent file.  Exiting program.");
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
