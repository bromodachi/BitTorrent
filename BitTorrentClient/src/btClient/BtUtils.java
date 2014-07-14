package btClient;

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
}
