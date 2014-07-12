package btClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class RUBTClient {
	static File file = null;

	/**
	 * This is the main client class for CS352, BitTorrent project 1 The program
	 * is designed to load a .torrent file, interface with a tracker and a
	 * single peer and will download a single file (a JPEG) from that peer. The
	 * file is then saved to a hard disk. All communication is done over TCP.
	 * 
	 * @param args
	 *            The name of the .torrent file to be loaded and the name of the
	 *            file to save the data to, using the proper path and file
	 *            extensions.
	 * @throws IOException
	 * @throws BencodingException
	 */
	public static void main(String[] args) throws IOException,
			BencodingException {

		// Step 1 - Take the command line arguments
		if (!validateArgs(args)) {
			return;
		}
		 ArrayList<Piece> pieces = new ArrayList<Piece>();

		// Step 2 - Open the .torrent file and parse the data inside
		byte[] torrentBytes = getFileBytes(args[0]);
		TorrentInfo activeTorrent = new TorrentInfo(torrentBytes);

		createPieces(pieces, activeTorrent);
		System.out.println(pieces.size());

		// Step 3 - Send an HTTP GET request to the tracker
		CommunicationTracker communicationTracker = new CommunicationTracker(
				activeTorrent);
		communicationTracker.CommunicateWithTracker();
		//Any errors in the communication tracker, we shouldn't proceed.
		if(communicationTracker.getError()){
			System.out.println("Exiting....");
			return;
		}

		// Step 4 - Connect with the Peer.
		Thread thread = new Thread(new MessageHandler(pieces,
				getTestPeer(communicationTracker.getPeersList()),
				activeTorrent.info_hash, communicationTracker.getClientID()));
		thread.start();

	}// END MAIN

	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad.
	 * 
	 * @param args
	 *            The command line arguments from the main method.
	 * @return
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

		file = new File(args[1]);
		if (!file.createNewFile()) {
			System.err
					.println("Error: file either already exists or could not be created");
			return false;
		}
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

	public static Peer getTestPeer(ArrayList<Peer> peers) {
		ByteBuffer prefix;
		for (Peer curr : peers) {
			System.out.println(curr.getPeer_id());
			if (curr.getPeer_id().startsWith(BtUtils.RU_PEER_PREFIX_STRING)) {
				return curr;
			}
		}
		return null;
	}

	private static void createPieces(ArrayList<Piece> pieces, TorrentInfo activeTorrent) throws FileNotFoundException {
		int numPieces = activeTorrent.file_length / activeTorrent.piece_length;
		int leftover = activeTorrent.file_length % activeTorrent.piece_length;

		for (int i = 0; i < numPieces; i++) {
			pieces.add(new Piece(i, activeTorrent.piece_length, i
					* activeTorrent.piece_length, file));
		}

		if (leftover != 0) {
			System.out.print(leftover);
			pieces.add(new Piece(numPieces, leftover, numPieces
					* activeTorrent.piece_length, file));
		}
		System.out.println("created pieces: " + pieces.size());
	}
}