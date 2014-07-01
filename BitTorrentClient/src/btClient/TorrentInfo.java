package btClient;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class TorrentInfo {
	
	//Raw data - do we need this in this class? -CW
	byte[] torrentBytes; //
	ByteBuffer infoHash;
	ByteBuffer[] rawPieces;
	//byte[] SHA1Hash;
	
	//parsed data
	boolean singleFile=true;
	URL url=null;
	String creator="a";
	int creationDate=0;
	int totalLength=0;
	ArrayList<Map> fileList=null;
	String fileName="a";
	String fileNameUTF="a";
	int pieceLength=0;
	
	//Vars From new file on Sakai
	//Merge these or delete these...
	byte[] torrent_file_bytes; //A byte array containing the raw bytes of the torrent metainfo file.
	Map<ByteBuffer,Object> torrent_file_map; //The base dictionary of the torrent metainfo file.
	Map<ByteBuffer,Object> info_map; //The unbencoded info dictionary of the torrent metainfo file.
	ByteBuffer info_hash; //The SHA-1 hash of the bencoded form of the info dictionary from the torrent metainfo file.
	URL announce_url; //The base URL of the tracker for client scrapes.
	int piece_length; //The default length of each piece in bytes.
	String file_name; //The name of the file referenced in the torrent metainfo file.
	int file_length; //The length of the file in bytes.
	ByteBuffer[] piece_hashes; //The SHA-1 hashes of each piece of the file.

	//No-arg constructor
	public TorrentInfo(){
	}
	
	//Constructor
	public TorrentInfo(
	boolean singleFile,
	URL urlAddress,
	String creator,
	int creationDate,
	String fileName,
	String fileNameUTF,
	int pieceLength,
	int length,
	ArrayList<Map> fileList){
		singleFile=true;
		this.url=urlAddress;
		this.creator=creator;
		this.creationDate=creationDate;
		this.fileName=fileName;
		this.fileNameUTF=fileNameUTF;
		this.pieceLength=pieceLength;
		this.totalLength=length;
		this.fileList=fileList;	
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
		return totalLength;
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
		info=("File name: " + fileName + "\n" +
				"Url: " + url + "\n" +
				"Creator: " + creator + "\n" +
				"Creation date: " + creationDate + "\n" +
				"Total length: " + totalLength + "\n" +
				"File name: " + fileName + "\n" +
				"File name UTF: " + fileNameUTF + "\n" +
				"Piece length: " + pieceLength + "\n" +
				"Single file: " + singleFile + "\n");
		if(singleFile){
			info.concat("File Length: " + totalLength + "\n");
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
