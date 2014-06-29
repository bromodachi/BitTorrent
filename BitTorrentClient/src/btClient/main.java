package btClient;

//import Bencoder2;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.net.*; //not sure if we need this one yet but let's see
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class main {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws BencodingException 
	 */
	public static void main(String[] args) throws IOException, BencodingException {
		File torrentFile=null;
		byte [] torrentBytes;
		byte [] SHA1_hash;
		ByteBuffer info_hash=null; //for the tracker?
		ByteBuffer [] piecesHashValues;
		Map<ByteBuffer, Object> torrentMap = null; //bencoder gives us a Map for the url
											//use this to get the url and the info map
		Map<ByteBuffer, Object> torrentInfo =null; //the torrent Info's map. Use this to get the torrent
											//information
		// TODO Auto-generated method stub
		//blah blah blha this is a comment
		//When compiled, we only allow two args so:
		if(args.length!=2){
			System.out.println("Correct input: somefile.torrent picture.jpg");
			return;
		}
		/*Are we only allowing pictures for now?*/
		if(fileTorrentValid(args[0])){
			/*Thinking of using this info to create an object to use
			 * later on. 
			 */
			//check 2nd arg...
			if(fileExtensionChecker2ndArg(args[1])){
				String path="";
				System.out.println("Success");
				//Create a file with the first arg, then get its bytes
				torrentFile=new File(args[0]);
				/*Make sure the file exists and it's not a directory*/
				if(torrentFile.exists() && !torrentFile.isDirectory()){
					
				torrentBytes=getFileBytes(torrentFile);
				if(torrentBytes==null){
					System.out.println("Couldn't load torrent file");
					return;
				}
				//Extract the maps from the torrent's bytes
				try{
				torrentMap=(Map<ByteBuffer, Object>)Bencoder2.decode(torrentBytes);
				}catch(BencodingException e){
					System.out.println("Error, couldn't decode the torrentmap");
					return;
				}
				ByteBuffer url_bytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]{'a', 'n','n','o', 'u','n','c','e'}));
				torrentInfo=(Map<ByteBuffer, Object>) torrentMap.get(ByteBuffer.wrap(new byte[]{'i', 'n','f','o'}));
				//	Extracting the info keys
				// Should check if we get a length or a file(latter is for 
				// multiple files. Will we be downloading multiple files?)
				ByteBuffer info_bytes=Bencoder2.getInfoBytes(torrentBytes);
				ByteBuffer name_bytes=(ByteBuffer) torrentInfo.get(ByteBuffer.wrap(new byte[]{'n', 'a','m','e'}));
				ByteBuffer pieces_bytes=(ByteBuffer) torrentInfo.get(ByteBuffer.wrap(new byte[]{'p', 'i','e','c','e','s'}));
				ByteBuffer path_bytes=(ByteBuffer) torrentInfo.get(ByteBuffer.wrap(new byte[]{'p', 'a','t','h'}));
				
				ByteBuffer tracker_bytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]{'i', 'p'}));
				if(tracker_bytes==null){
					System.out.println("yes, the path is  null");
				}
				
				/*The lengths:*/
				System.out.println("testing: "+torrentInfo.get(ByteBuffer.wrap(new byte[]{'l', 'e','n','g','t','h'})));
				int infoLength=(int) torrentInfo.get(ByteBuffer.wrap(new byte[]{'l', 'e','n','g','t','h'}));
				int piecesLength=(int) torrentInfo.get(ByteBuffer.wrap(new byte[]{'p', 'i','e','c','e',' ','l', 'e','n','g','t','h'}));
				
				/*Converting some of the bytes into string. However,
				 * some seems to be crypted. */
				String fileName=new String(name_bytes.array(), "ASCII");
				String urlAddress=new String(url_bytes.array(), "ASCII");
				String pieces=new String(pieces_bytes.array(), "ASCII");
				String info_test=new String(info_bytes.array(), "ASCII");
				if(path_bytes!=null){
					System.out.println("yes");
					path=new String(path_bytes.array(), "ACII");
				}
				try {
					/*don't know what to do with this*/
						MessageDigest crypt=MessageDigest.getInstance("SHA-1");
						crypt.update(info_bytes);
						byte [] infoHash=crypt.digest();
						info_hash=ByteBuffer.wrap(infoHash);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				if(pieces.length()%20!=0){
					System.out.println("Error, not a multiple of 20");
					return;
				}
				byte piece_array []=pieces_bytes.array();
				piecesHashValues=new ByteBuffer[piece_array.length/20];
				for (int i=0; i<piece_array.length/20;i++){
					byte []temp= new byte[20];
					System.arraycopy(piece_array, i*20, temp, 0, 20);
					piecesHashValues[i]=ByteBuffer.wrap(temp);
				
				}
				URL addressFile=new URL(urlAddress);
				
				/*File file, byte[] torrentBytes,
			 ByteBuffer info, ByteBuffer [] piecehash,
			URL add, String addURL, int infoLength,
			int pieceLength)*/
				TorrentInfo torrentInfoObject= new TorrentInfo(torrentFile, fileName,
						torrentBytes, info_hash,
						piecesHashValues, addressFile,
						urlAddress, infoLength, piecesLength);
				System.out.println("testing: "+torrentInfoObject.getStringURLAddress());
			/*	System.out.println(fileName);
				System.out.println(infoLength);
				System.out.println(piecesLength);
				System.out.println(pieces.length()+ "pieces array length: "+piece_array.length);
				System.out.println("Rest is gibberish:");
				System.out.println(pieces);
				System.out.println(path);
				System.out.println("testing info: "+info_test);*/
				CommunicationTracker establishConnection=new CommunicationTracker(torrentInfoObject);
				establishConnection.establishConnection();
				
				}
				
				else{
				System.out.println("Couldn't load torrent file");
					return;
				}
				
		
			}
			else{
				System.out.println("Not a valid photo file");
			}
			
		}
		else{
			System.out.println("Not a valid Torrent file\n Exiting");
			return;
		}

	}
	
	/**
	 * File extension checker for the 2nd arg, returns a boolean.
	 * Will possibly remove this later if we can different file types
	 * @param fileName
	 * @return true or false
	 */
	public static boolean fileExtensionChecker2ndArg(String fileName){
		//"JPG, PNG, & GIF Images", "jpg", "gif", "png"));
		String extChecker=fileName.substring(fileName.lastIndexOf(".")
				+1, fileName.length());
		return extChecker.toLowerCase().equals("gif")||
				extChecker.toLowerCase().equals("png")||
				extChecker.toLowerCase().equals("jpg");
	}
	
	/**
	 * Will check if a torrent file is valid. returns a boolean.
	 * @param torrentFile
	 * @return true or false
	 */
	public static boolean fileTorrentValid(String torrentFile){
		String torrentChecker=torrentFile.substring(torrentFile.lastIndexOf(".")
				+1, torrentFile.length());
		return torrentChecker.equals("torrent")==true;
	}
	
	
	/**
	 * Return the bytes of a file to be used with Bencoder2.java
	 * @param file
	 * @return byte
	 * @throws IOException 
	 */
	public static byte [] getFileBytes(File file) throws IOException{
		/*a java 7 approach to get Bytes. Should double check
		 * if we can use java 7*/
		Path filePath= file.toPath();
		return Files.readAllBytes(filePath);
		
		
	}
	
	//Eclipse git test, CW

}
