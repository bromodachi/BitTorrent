package btClient;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;

public class TorrentInfo {
	File torrentFile=null;
	byte [] torrentBytes;
	//byte [] SHA1_hash;
	ByteBuffer info_hash; //for the tracker?
	ByteBuffer [] piecesHashValues;
	URL urlAddress;
	String stringURLAddress;
	String fileName;
	int infoLength;
	int piecesLength;
	
	
	
	
	public TorrentInfo(File file, String name, byte[] torrentBytes,
			 ByteBuffer info, ByteBuffer [] piecehash,
			URL add, String addURL, int infoLength,
			int pieceLength){
		this.torrentFile=file;
		this.torrentBytes=torrentBytes;
		this.info_hash=info;
		this.fileName=name;
		this.piecesHashValues=piecehash;
		this.urlAddress=add;
		this.stringURLAddress=addURL;
		this.infoLength=infoLength;
		this.piecesLength=pieceLength;
		System.out.println("In constructor: "+fileName);
		
	}
	public File getFile(){
		return this.torrentFile;
	}
	public byte[] getTorrentBytes(){
		return this.getTorrentBytes();
	}
	public ByteBuffer getInfoHash(){
		return this.info_hash;
	}
	
	public ByteBuffer[] getPiecesHash(){
		return this.piecesHashValues;
	}
	public int getInfoLength(){
		return this.infoLength;
	}
	
	public int getPiecesLength(){
		return this.piecesLength;
	}
	
	public String getFileName(){
		return this.fileName;
	}
	public String getStringURLAddress(){
		return this.stringURLAddress;
	}
	
	public URL getURL(){
		return this.urlAddress;
	}

}
