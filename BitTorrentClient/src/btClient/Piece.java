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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

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
	 * Lock for this piece
	 */
	private final ReentrantLock lock;
	/**
	 * An array list of {@link Block} objects associated with this piece
	 */
	private ArrayList<Block> blocks;
	/**
	 * Starting position of this piece in the output file
	 */
	private final int offset;
	/**
	 * The SHA-1 hash of this piece (only available once piece is completed)
	 */
	private byte[] hash;
	/**
	 * Number of times this piece has been downloaded (piece gets redownloaded
	 * if hash doesn't match when complete)
	 */
	private int downloadAttempts;

	private int peerCount;

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
	public Piece(int index, int size, int offset, File file, byte[] hash)
			throws FileNotFoundException {
		this.index = index;
		this.size = size;
		this.offset = offset;
		this.hash = hash;
		this.file = new RandomAccessFile(file, "rw");
		complete = false;
		// find total number of blocks in piece
		int last_block_size;
		if (size % BtUtils.BLOCK_SIZE == 0) {
			numBlocks = (size / BtUtils.BLOCK_SIZE);
			last_block_size = BtUtils.BLOCK_SIZE;
		} else {
			numBlocks = (size / BtUtils.BLOCK_SIZE) + 1;
			last_block_size = size % BtUtils.BLOCK_SIZE;
		}
		// Initialize boolean blocks array to false
		blocks = new ArrayList<Block>(numBlocks);
		for (int i = 0; i < numBlocks; i++) {
			if (i == (numBlocks - 1)) {
				blocks.add(new Block(index, i, i * BtUtils.BLOCK_SIZE,
						last_block_size, true));
			} else {
				blocks.add(new Block(index, i, i * BtUtils.BLOCK_SIZE,
						BtUtils.BLOCK_SIZE, false));
			}
		}

		peerCount = 0;

		lock = new ReentrantLock();
		// Check if piece is already completed in file
		if (file.length() >= (offset + size)) {
			if (checkHash()) {
				setComplete();
			}
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object.getClass() == Piece.class) {
			return (((Piece) object).getIndex() == this.index
					&& ((Piece) object).getSize() == this.size && ((Piece) object)
						.getOffset() == this.offset);
		}
		return false;
	}

	/* ============== Getters ================== */
	/**
	 * Checks whether the piece is completely downloaded
	 * 
	 * @return True if piece is complete, otherwise false
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Gets the integer index of the piece
	 * 
	 * @return The zero based index of this piece relative to other pieces in
	 *         the file
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * 
	 * @return The size of the piece in bytes
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gets the SHA-1 hash of the completed piece as a byte array
	 * 
	 * @return SHA-1 hash of the piece if completed, otherwise null
	 */
	public byte[] getHash() {
		return hash;
	}

	/**
	 * Gets the file offset in bytes of the begining of where this piece begins
	 * 
	 * @return File offset in bytes
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Gets the number of attempts that have been made to download this piece, a
	 * piece may have multiple attempts if one or more attempts have failed to
	 * verify its SHA-1 hash
	 * 
	 * @return Number of attempts at downloading this piece
	 */
	public int getDownloadAttempts() {
		return downloadAttempts;
	}

	/**
	 * Gets the next block that has not yet been downloaded and is not locked by
	 * another thread. If such a block exists the lock on that block is acquired
	 * before returning
	 * 
	 * @return the next {@link#Block} to be downloaded, null if no block is
	 *         available
	 */
	public Block getNextBlock() {
		for (Block block : blocks) {
			if (!block.isDownloaded()) {
				return block;
			}
		}
		return null;
	}

	/**
	 * Gets the number of peers that currently have this piece
	 * 
	 * @return {@link#peerCount}
	 */
	public int getPeerCount() {
		return peerCount;
	}

	/**
	 * @see ReentrantLock#isLocked()
	 * @return True if Piece is locked by any thread, otherwise false
	 */
	public boolean isLocked() {
		return lock.isLocked();
	}

	/* ======================= SETTERS ======================= */
	/**
	 * Sets all blocks to downloaded and sets completed to true
	 */
	public void setComplete() {
		for (Block block : blocks) {
			block.setDownloaded();
		}
		complete = true;
	}

	/**
	 * Sets the complete value by checking if all blocks are downloaded
	 */
	public void checkComplete() {
		for (Block block : blocks) {
			if (!block.isDownloaded()) {
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
	 * Increases the number of download attempts made by one; This method is
	 * synchronized for thread saftey
	 */
	public synchronized void incrementAttempts() {
		downloadAttempts++;
	}

	/**
	 * Increments the {@link#peerCount} for this piece by one; This method is
	 * synchronized for thread safety
	 */
	public synchronized void incrementPeerCount() {
		peerCount++;
	}

	/**
	 * Decrements {@link#peerCount} for this piece by one; This method is
	 * synchronized for thread safety
	 */
	public synchronized void decrementPeerCount() {
		peerCount--;
	}

	/* =========== METHODS ========= */
	/**
	 * @see ReentrantLock#tryLock()
	 * @return True if lock is acquired otherwise false
	 */
	public boolean tryLock() {
		return lock.tryLock();
	}

	/**
	 * @see ReentrantLock#unlock()
	 */
	

	/**
	 * Computes SHA-1 hash for this piece
	 * 
	 * @return SHA-1 hash if piece is completed, otherwise returns null
	 * @throws IOException
	 */
	public byte[] computeHash() throws IOException {
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
		Block block = blocks.get(block_index);
		byte[] payload = new byte[parser.remaining()];
		parser.get(payload, 0, payload.length);
		// Check if block is last block in piece
		if (payload.length != block.getSize()) {
			throw new BtException("payload does not match block size");
		}
		// write payload to file
		FileChannel output = file.getChannel();
		output.write(ByteBuffer.wrap(payload), (this.offset + block_offset));
		// update boolean values
		block.setDownloaded();
		checkComplete();
		// create hash if complete
		if (complete) {
			setHash(computeHash());
		}
	}

	public boolean checkHash() {
		try {
			return Arrays.equals(hash, computeHash());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks if a given hash is equal to this piece's computed hash;
	 * 
	 * @param hash
	 *            the SHA-1 hash that this piece is supposed to have
	 * @return True if hashes match, otherwise false
	 * @throws IOException
	 */
	public boolean checkHash(byte[] hash) {
		try {
			return Arrays.equals(computeHash(), hash);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sets all true values for blocks[] to false (used to set this piece for
	 * redownload if hash verification failed)
	 */
	public void clearBlocks() {
		for (Block block : blocks) {
			block.setDownloaded(false);
		}
		complete = false;
	}

	/**
	 * Reads an array of bytes form the file that this piece is written too
	 * 
	 * @param offset
	 *            the offset in bytes of where to begin reading
	 * @param size
	 *            the number of bytes to read
	 * @return an array of bytes read from the file
	 * @throws IOException
	 */
	public byte[] getBytes(int offset, int size) throws IOException {
		byte[] bytes = new byte[size];
		file.read(bytes, offset + this.offset, size);
		return bytes;
	}
}