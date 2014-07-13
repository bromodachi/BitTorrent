package btClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This class is tasked with managing a single piece of the downloaded file.
 * This includes writing blocks to the file and checking the piece's
 * completeness and computing the piece's hash when complete
 * 
 * @author Cody
 * 
 */
public class Piece {
	/**
	 * true if all blocks are downloaded (all values in boolean [] blocks are
	 * true)
	 */
	private boolean complete;
	/**
	 * The zero based index corresponding to the position of this piece in the
	 * overall file
	 */
	private final int index;
	/**
	 * The total size of the piece in bytes
	 */
	private final int size;
	/**
	 * The number of blocks in this piece
	 */
	private final int numBlocks;
	/**
	 * The file where the downloaded data is to be saved
	 */
	private final RandomAccessFile file;
	/**
	 * An array of boolean values indicating whether or not each block index has
	 * already been downloaded
	 */
	private boolean[] blocks;
	/**
	 * Starting position of this piece in the output file
	 */
	private final int offset;
	/**
	 * The SHA-1 hash of this piece (only available once piece is completed)
	 */
	private byte[] hash;

	/**
	 * Creates a new piece object with the given parameters
	 * 
	 * @param index
	 *            Zero based index of the piece relative to other pieces of the
	 *            file
	 * @param size
	 *            Size the piece in bytes
	 * @param offset
	 *            The offset to the begining of this piece in the file, in bytes
	 * @param file
	 *            the file to which this piece is to be saved
	 * @throws FileNotFoundException
	 */
	public Piece(int index, int size, int offset, File file)
			throws FileNotFoundException {
		this.index = index;
		this.size = size;
		this.offset = offset;
		this.file = new RandomAccessFile(file, "rw");
		complete = false;
		// find total number of blocks in piece
		if (size % BtUtils.BLOCK_SIZE == 0) {
			numBlocks = (size / BtUtils.BLOCK_SIZE);
		} else {
			numBlocks = (size / BtUtils.BLOCK_SIZE) + 1;
		}
		// Initialize boolean blocks array to false
		blocks = new boolean[numBlocks];
		for (int i = 0; i < numBlocks; i++) {
			blocks[i] = false;
		}
	}

	public boolean isComplete() {
		return complete;
	}

	public int getIndex() {
		return index;
	}

	public int getSize() {
		return size;
	}

	public byte[] getHash() {
		return hash;
	}

	public int getOffset() {
		return offset;
	}

	/**
	 * Returns the index of the next incomplete block, returns -1 if all blocks
	 * are complete
	 * 
	 * @return
	 */
	public int getNextBlockIndex() {
		for (int i = 0; i < numBlocks; i++) {
			if (!blocks[i]) {
				return i;
			}
		}
		return -1; // this should never happen
	}

	public int getNextBlockSize() {
		if (size % BtUtils.BLOCK_SIZE != 0
				&& getNextBlockIndex() == numBlocks - 1) {
			return size % BtUtils.BLOCK_SIZE;
		}
		return BtUtils.BLOCK_SIZE;
	}

	public int getNextBlockOffest() {
		return getNextBlockIndex() * BtUtils.BLOCK_SIZE;
	}

	/**
	 * Sets the complete value by checking if all blocks are downloaded
	 */
	public void setComplete() {
		for (boolean curr : blocks) {
			if (curr == false) {
				complete = false;
				return;
			}
		}
		complete = true;
	}

	private void setHash(byte[] hash) {
		this.hash = hash;
	}

	/**
	 * Computes SHA-1 hash for this piece
	 * 
	 * @return SHA-1 hash if piece is completed, otherwise returns null
	 * @throws IOException
	 */
	private byte[] computeHash() throws IOException {
		if (!complete) {
			System.err
					.println("Attempted to create hash for piece that is not complete");
			return null;
		}
		FileChannel input = file.getChannel();
		ByteBuffer bytes = ByteBuffer.wrap(new byte[size]);
		input.read(bytes, this.offset);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			// digest.update(bytes.array());
			byte[] hash = digest.digest(bytes.array());
			return hash;
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("Algorithm not found");
			return null;
		}
	}

	/**
	 * Writes the payload of the given message to the download file
	 * 
	 * @param message
	 *            message with payload to be written to file
	 * @throws BtException
	 *             thrown if invalid message is passed
	 * @throws IOException
	 */
	public void writeBlock(byte[] message) throws BtException, IOException {
		ByteBuffer parser = ByteBuffer.wrap(message);
		byte message_id = parser.get();
		if (message_id != BtUtils.PIECE_ID) {
			throw new BtException(
					"Message was not a piece message, message id: "
							+ (int) message_id);
		}
		int piece_index = parser.getInt();
		if (piece_index != this.index) {
			throw new BtException("Piece index does not match");
		}

		/*
		 * Make sure the offset resolves to a valid block index (make sure
		 * offset doesn't land data in the middle of a block)
		 */
		int block_offset = parser.getInt();
		if (block_offset % BtUtils.BLOCK_SIZE != 0) {
			throw new BtException(
					"block offset does not reslove to a valid block index");
		}
		int block_index = block_offset / BtUtils.BLOCK_SIZE;
		System.out.println("block index " + block_index);
		byte[] payload = new byte[parser.remaining()];
		parser.get(payload, 0, payload.length);
		// Check if block is last block in piece
		if (block_index == numBlocks - 1) {
			if (payload.length > BtUtils.BLOCK_SIZE) {
				throw new BtException(
						"Last block in piece is longer than block size");
			}
		} else if (payload.length != BtUtils.BLOCK_SIZE) {
			throw new BtException("Incorrect block size " + payload.length);
		}
		// write payload to file
		FileChannel output = file.getChannel();
		output.write(ByteBuffer.wrap(payload), (this.offset + block_offset));
		// update boolean values
		blocks[block_index] = true;
		setComplete();
		// create hash if complete
		if (complete) {
			setHash(computeHash());

		}
	}

	public boolean checkHash(byte[] p) {
		return Arrays.equals(this.hash, p);
	}
}
