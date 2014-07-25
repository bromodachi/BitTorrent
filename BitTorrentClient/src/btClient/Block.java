package btClient;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An object representing a single block within a {@link Piece}; This class
 * contains the {@link#lock} for this block and keeps track of relevant data
 * pertaining to the block such as {@link#piece_index} {@link#index}
 * {@link#offset} {@link#size} {@link#downloaded} {@link#lastBlock}
 * 
 * @author Cody
 *
 */
public class Block {
	/**
	 * The zero based index of the piece that this block belongs too
	 */
	private final int piece_index;
	/**
	 * The zero based index of this block
	 */
	private final int index;
	/**
	 * The offset of this block in bytes
	 */
	private final int offset;
	/**
	 * The size of this block in bytes
	 */
	private final int size;
	/**
	 * The lock for the use of this block
	 */
	private final ReentrantLock lock;
	/**
	 * Boolean value indicating whether this block as already been downloaded
	 * and saved to the disk
	 */
	private boolean downloaded;
	/**
	 * Indicates whether this block is the last block in the piece
	 */
	private final boolean lastBlock;

	/**
	 * Creates a new block using the given parameters
	 * 
	 * @param {@link #piece_index}
	 * @param {@link #index}
	 * @param {@link #offset}
	 * @param {@link #size}
	 */
	public Block(int piece_index, int index, int offset, int size,
			boolean lastBlock) {
		this.piece_index = piece_index;
		this.index = index;
		this.offset = offset;
		this.size = size;
		this.lock = new ReentrantLock(false);
		this.lastBlock = lastBlock;
		downloaded = false;
	}

	/**
	 * @return the zero based index of the piece that this block belongs too
	 */
	public int getPieceIndex() {
		return piece_index;
	}

	/**
	 * @return the zero based index of this block
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the offset of this block in bytes
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return the size of this block in bytes
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Checks if this block has already been downloaded
	 * 
	 * @return {@link#downloaded}
	 */
	public boolean isDownloaded() {
		return downloaded;
	}

	/**
	 * Sets the value of downloaded to true
	 */
	public void setDownloaded() {
		downloaded = true;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	/**
	 * 
	 * @return {@link#lastBlock}
	 */
	public boolean isLastBlock() {
		return lastBlock;
	}

	/**
	 * 
	 * @see java.util.concurrent.locks.ReentrantLock#tryLock()
	 */
	public boolean tryLock() {
		return lock.tryLock();
	}

	/**
	 * 
	 * @see java.util.concurrent.locks.ReentrantLock#isLocked()
	 */
	public boolean isLocked() {
		return lock.isLocked();
	}

	/**
	 * 
	 * @see java.util.concurrent.locks.ReentrantLock#isHeldByCurrentThread()
	 */
	public boolean isHoldingLock() {
		return lock.isHeldByCurrentThread();
	}

	/**
	 * @see java.util.concurrent.locks.ReentrantLock#unlock()
	 */
	public void unlock() {
		lock.unlock();
	}

}
