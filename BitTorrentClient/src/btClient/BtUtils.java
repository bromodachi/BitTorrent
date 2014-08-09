/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class was written to provide utilities such as constants and commonly
 * called methods as a convience for developing the a BitTorrent client for the
 * Rutgers Internet Technologies (352) couse : Summer 2014
 */
public class BtUtils {
	/**
	 * Byte array representation of the Bit Torrent protocol handshake header
	 */
	public static final byte[] p2pHandshakeHeader = { (byte) 0x13, 'B', 'i',
			't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't',
			'o', 'c', 'o', 'l', (byte) 0, (byte) 0, (byte) 0, (byte) 0,
			(byte) 0, (byte) 0, (byte) 0, (byte) 0 };
	/**
	 * number of bytes in the complete (header + info_hash + peer_id) Bit
	 * torrent protocol handshake
	 */
	public static final int p2pHandshakeLength = 68;
	/**
	 * BitTorrent message id for choke
	 */
	public static final byte CHOKE_ID = 0;
	/**
	 * BitTorrent message id for unchoke
	 */
	public static final byte UNCHOKE_ID = 1;
	/**
	 * BitTorrent message id for interested
	 */
	public static final byte INTERESTED_ID = 2;
	/**
	 * BitTorrent message id for uninterested
	 */
	public static final byte UNINTERESTED_ID = 3;
	/**
	 * BitTorrent message id for have
	 */
	public static final byte HAVE_ID = 4;
	/**
	 * BitTorrent message id for bitfield
	 */
	public static final byte BITFIELD_ID = 5;
	/**
	 * BitTorrent message id for request
	 */
	public static final byte REQUEST_ID = 6;
	/**
	 * BitTorrent message id for piece
	 */
	public static final byte PIECE_ID = 7;
	/**
	 * BitTorrent length prefix for choke message
	 */
	public static final int CHOKE_LENGTH_PREFIX = 1;
	/**
	 * BitTorrent length prefix for unchoke message
	 */
	public static final int UNCHOKE_LENGTH_PREFIX = 1;
	/**
	 * BitTorrent length prefix for interested message
	 */
	public static final int INTERESTED_LENGTH_PREFIX = 1;
	/**
	 * BitTorrent length prefix for uninterested message
	 */
	public static final int UNINTERESTED_LENGTH_PREFIX = 1;
	/**
	 * BitTorrent length prefix for have message
	 */
	public static final int HAVE_LENGTH_PREFIX = 5;
	/**
	 * BitTorrent length prefix for request message
	 */
	public static final int REQUEST_LENGTH_PREFIX = 13;
	/**
	 * BitTorrent length prefix for piece message
	 */
	public static final int PIECE_LENGTH_PREFIX = 9;
	/**
	 * The number of bytes that comprise the BitTorrent protocol length prefix
	 */
	public static final int PREFIX_LENGTH = 4;
	/**
	 * BitTorrent keep alive message as a byte array
	 */
	public static final byte[] KEEP_ALIVE = { 0x00, 0x00, 0x00, 0x00 };
	/**
	 * Length of a SHA-1 hash for the BitTorrent protocol
	 */
	public static final int INFO_HASH_LENGTH = 20;
	/**
	 * Offset for the start of the BitTorrent info_hash sent in a handshake
	 * message
	 */
	public static final int INFO_HASH_OFFSET = 28;
	/**
	 * 
	 */
	public static final byte[] MESSAGE_READ_ERROR = "failed to read message"
			.getBytes();
	/**
	 * The prefix for the peer specified on the rutgers sakai page (byte array)
	 */
	public static final byte[] RU_PEER_PREFIX = "-RU1103".getBytes();
	/**
	 * The prefix for the peer specified on the ruters sakai page (string)
	 */
	public static final String RU_PEER_PREFIX_STRING = "-RU1103";
	/**
	 * The number of bytes in a block
	 */
	public static final int BLOCK_SIZE = 16384;

	public static final int MAX_DOWNLOAD_ATTEMPTS = 3;

	/**
	 * 2 minutes into millseconds
	 */
	public static final int MAX_TIME = 120000;
	/**
	 * The number of bytes that comprise the header for a Piece message in the
	 * BitTorrent Protocol
	 */
	public static final int PIECE_HEADER_SIZE = 9;
	/**
	 * BitTorrent message offset for the index of a requested piece
	 */
	public static final int REQUEST_INDEX = 1;
	/**
	 * BitTorrent message offset for the offset of a requested block
	 */
	public static final int REQUEST_OFFSET = 5;
	/**
	 * BitTorrent message offset for the size of a requested block
	 */
	public static final int REQUEST_SIZE = 9;
	/**
	 * The index of the status column in the torrent table for the GUI
	 */
	public static final int TORRENT_TABLE_STATUS_COLUMN = 2;
	/**
	 * Number of bytes in an integer
	 */
	public static final int SIZE_OF_INT = 4;
	/**
	 * The maximum number of unchoked peers for any given {@link ActiveTorrent}
	 */
	public static final int MAX_UNCHOKED_PEERS = 6;
	/**
	 * The interval in milliseconds for which a peer should be choked/unchoked
	 */
	public static final int CHOKE_INTERVAL = 30000;
	
	public static final int MAX_UPDATE_INTERVAL = 180;
	/**
	 * Keep alive interval in milliseconds
	 */
	public static final long KEEP_ALIVE_INTERVAL = 120000;
	
	
	
	public static enum Status {
		Active, Seeding, Stopped, Starting, Complete
	}
	
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
	public static byte[] getFileBytes(File torrentFile) throws IOException,
			BencodingException {

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
}
