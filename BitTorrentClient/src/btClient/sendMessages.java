package btClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class sendMessages {
	private int length;
	private int id;
	protected static DataOutputStream outputStream;
	
	public sendMessages(int length, int ID){
		this.length=length;
		this.id=ID;
	}
	public void sendMessage() throws IOException{
		//I want this to be overrided
	}
	

	
	public static class sendChoke extends sendMessages{
		public sendChoke(DataOutputStream outputStream){
			super(BtUtils.CHOKE_LENGTH_PREFIX, BtUtils.CHOKE_ID);
			this.outputStream=outputStream;
		}
		
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.CHOKE_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			ByteBuffer message = ByteBuffer.wrap(bytes);
			message.putInt(BtUtils.CHOKE_LENGTH_PREFIX);
			message.put((BtUtils.CHOKE_ID));
			outputStream.write(message.array());
			outputStream.flush();
		}
		
	}
	
	public static class sendUnchoke extends sendMessages{

		public sendUnchoke(DataOutputStream outputStream) {
			super(BtUtils.UNCHOKE_LENGTH_PREFIX, BtUtils.UNCHOKE_ID);
			// TODO Auto-generated constructor stub
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.UNCHOKE_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		message.putInt(BtUtils.UNCHOKE_LENGTH_PREFIX);
			        		message.put((BtUtils.UNCHOKE_ID));
			        		outputStream.write(message.array());
			        		outputStream.flush();
		}
		
	}
	public static class sendInterested extends sendMessages{
		public sendInterested(DataOutputStream outputStream){
			super(BtUtils.INTERESTED_LENGTH_PREFIX, BtUtils.INTERESTED_ID);
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.INTERESTED_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		message.putInt(BtUtils.INTERESTED_LENGTH_PREFIX);
			        		message.put((BtUtils.INTERESTED_ID));
			        		outputStream.write(message.array());
			        		outputStream.flush();
		}
	
}
	public class sendUninterested extends sendMessages{
		public sendUninterested(DataOutputStream outputStream){
			super(BtUtils.UNINTERESTED_LENGTH_PREFIX, BtUtils.UNINTERESTED_ID);
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.UNINTERESTED_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		message.putInt(BtUtils.UNINTERESTED_LENGTH_PREFIX);
			        		message.put((BtUtils.UNINTERESTED_ID));
			        		outputStream.write(message.array());
			
		}
	}
	
	public static class sendHave extends sendMessages{
		int index;
		public sendHave(int index, DataOutputStream outputStream){
			super(BtUtils.HAVE_LENGTH_PREFIX, BtUtils.HAVE_ID);
			this.index=index;
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.HAVE_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		message.putInt(BtUtils.HAVE_LENGTH_PREFIX);
			        		message.put((BtUtils.HAVE_ID));
			        		message.putInt(index);
			        		outputStream.write(message.array());
			
		}
	}
	public class sendRequest extends sendMessages{
		int index;
		int block_offset;
		int block_length;
		public sendRequest(int index, int block_offset, int block_length, DataOutputStream outputStream){
			super(BtUtils.REQUEST_LENGTH_PREFIX, BtUtils.REQUEST_ID);
			this.index=index;
			this.block_offset=block_offset;
			this.block_length=block_length;
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			byte[] bytes = new byte[BtUtils.REQUEST_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		message.putInt(BtUtils.REQUEST_LENGTH_PREFIX);
			        		message.put((BtUtils.REQUEST_ID));
			        		message.putInt(index);
			        		message.putInt(block_offset);
			        		message.putInt(block_length);
			        		outputStream.write(message.array());
			
		}
	}
	public class sendPiece extends sendMessages{
		Piece piece;
		int offset;
		int size;
		public sendPiece(Piece piece, int offset, int size,DataOutputStream outputStream){
			super(BtUtils.HAVE_LENGTH_PREFIX, BtUtils.HAVE_ID);
			this.piece=piece;
			this.offset=offset;
			this.size=size;
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			int payloadLength = size; // need to figure out code for adding payload
			byte[] bytes = new byte[BtUtils.PIECE_LENGTH_PREFIX
					+ BtUtils.PREFIX_LENGTH + payloadLength];
			ByteBuffer message = ByteBuffer.wrap(bytes);
			message.putInt(BtUtils.PIECE_LENGTH_PREFIX);
			message.put((BtUtils.PIECE_ID));
			message.putInt(piece.getIndex());
			message.putInt(offset);
			message.put(piece.getBytes(offset, size));
			outputStream.write(message.array());
			outputStream.flush();
	//		downloaded += size;
			
		}
	}
	public static class sendBlock extends sendMessages{
		Block block;
		public sendBlock(Block block,DataOutputStream outputStream){
			super(BtUtils.REQUEST_LENGTH_PREFIX, BtUtils.REQUEST_ID);
			this.block=block;
			this.outputStream=outputStream;
		}
		public void sendMessage() throws IOException {
			
			byte[] bytes = new byte[BtUtils.REQUEST_LENGTH_PREFIX
			        				+ BtUtils.PREFIX_LENGTH];
			        		ByteBuffer message = ByteBuffer.wrap(bytes);
			        		outputStream.writeInt(BtUtils.REQUEST_LENGTH_PREFIX);
			        		outputStream.write((BtUtils.REQUEST_ID));
			        		outputStream.writeInt(block.getPieceIndex());
			        		outputStream.writeInt(block.getOffset());
			        		outputStream.writeInt(block.getSize());
			        		
			
		}
	}


}