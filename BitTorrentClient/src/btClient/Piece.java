package btClient;

import java.nio.ByteBuffer;

/**
 * This class is tasked with managing a single piece of the downloaded file.
 * This includes writing blocks to the piece and checking the piece's
 * completeness
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
	private boolean[] blocks;
	private byte[] hash;
	private byte[] data;

	public Piece(int index, int size) {
		this.index = index;
		this.size = size;
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

	public byte[] getData() {
		return data;
	}

	/**
	 * Sets the complete value by checking if all blocks are downloaded
	 */
	public void setComplete() {
		if (complete == true) {
			return;
		}
		for (boolean curr : blocks) {
			if (curr == false) {
				break;
			}
		}
		complete = true;
	}

	/**
	 * Adds the data in a block to the piece and records that block as being
	 * downloaded
	 * 
	 * @param block
	 *            block to be added to piece
	 * @param offset
	 *            offset within the piece (should resolve to a block index)
	 */
	public void addBlock(byte[] block, int offset) {
		// Make sure the offset resolves to a valid block index (make sure
		// offset doesn't land data in the middle of a block)
		if (offset % BtUtils.BLOCK_SIZE != 0) {
			System.err
					.println("Invalid block offset: offset does not resolve to a valid block index");
			return;
		}
		int index = offset / BtUtils.BLOCK_SIZE;
		// Check if block is last block in piece
		if (index == numBlocks - 1) {
			if (block.length > BtUtils.BLOCK_SIZE) {
				System.err.println("last block is longer than set block size");
				return;
			}
		} else if (block.length != BtUtils.BLOCK_SIZE) {
			System.err.println("new block is not the correct size");
			return;
		}
		// add the block to data
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.put(block, offset, block.length);
		blocks[index] = true;
		setComplete();
	}
}
