package btClient;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class TorrentInfo {
	
	//Raw data - do we need this in this class? -CW
	byte[] torrentBytes;
	ByteBuffer info_hash;
	ByteBuffer[] rawPieces;
	//byte[] SHA1_hash;
	
	//parsed data
	boolean singleFile;
	URL url;
	String creator;
	int creationDate;
	int length;
	ArrayList<Map> fileList;
	String fileName;
	String fileNameUTF;
	int pieceLength;

	
	//Single file constructor
	public TorrentInfo(
	boolean singleFile,
	URL urlAddress,
	String creator,
	int creationDate,
	String fileName,
	String fileNameUTF,
	int pieceLength,
	int length){
		singleFile=true;
		this.url=urlAddress;
		this.creator=creator;
		this.creationDate=creationDate;
		this.fileName=fileName;
		this.fileNameUTF=fileNameUTF;
		this.pieceLength=pieceLength;
		this.length=length;
		this.fileList=null;
		System.out.println("In TorrentInfo single-file constructor: "+fileName);	
	}
	
	//Multiple file constructor
	public TorrentInfo(
	boolean singleFile,
	URL urlAddress,
	String creator,
	int creationDate,
	String fileName,
	String fileNameUTF,
	int pieceLength,
	ArrayList<Map> fileList){
		singleFile=false;
		this.url=urlAddress;
		this.creator=creator;
		this.creationDate=creationDate;
		this.fileName=fileName;
		this.fileNameUTF=fileNameUTF;
		this.pieceLength=pieceLength;
		this.length=0;
		this.fileList=fileList;
		System.out.println("In TorrentInfo multi-file constructor: "+fileName);	
	}
	
	//Getters
	public boolean getSingleFile(){
		return singleFile;
	}
	public URL getURL(){
		return url;
	}
	public String getCreator(){
		return creator;
	}
	public int getCreationDate(){
		return creationDate;
	}
	public int getLength(){
		return length;
	}
	public ArrayList<Map> getFileList(){
		return fileList;
	}
	public String gFileName(){
		return fileName;
	}
	public String getFileNameUTF(){
		return fileNameUTF;
	}
	public int getPieceLength(){
		return pieceLength;
	}

	
	public String toString(){
		String info = "info";	
		info=("File name: " + fileName + "\n" + "\n" +
				"Url: " + url + "\n" +
				"Creator: " + creator + "\n" +
				"Creation date: " + creationDate + "\n" +
				"File name: " + fileName + "\n" +
				"File name UTF: " + fileNameUTF + "\n" +
				"Piece length: " + pieceLength + "\n" +
				"Single file: " + singleFile + "\n");
		if(singleFile){
			info.concat("File Length: " + length + "\n");
		}
		else{
			
			info.concat("Files: \n");
			for(int i=0;i<fileList.size();i++){
				info.concat("  " + fileList.get(i));
			}
		}		
		return info;
		
	}
}
