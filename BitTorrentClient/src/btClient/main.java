package btClient;

//import Bencoder2;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.net.*; //not sure if we need this one yet but let's see
import java.util.*;

public class main {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws BencodingException 
	 */
	public static void main(String[] args) throws IOException, BencodingException {
		File torrentFile;
		byte [] torrentBytes;
		Map<ByteBuffer, Object> metaInfo; //for extracting the url and info
											//but can't seem to get the info
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
				System.out.println("Success");
				torrentFile=new File(args[0]);
				torrentBytes=getFileBytes(torrentFile);
				System.out.println(torrentBytes);
				//will change the name
				Bencoder2 test=new Bencoder2();
				//decode returns a map, in the description
				metaInfo=(Map<ByteBuffer, Object>)test.decode(torrentBytes);
				ByteBuffer url_bytes=(ByteBuffer) metaInfo.get(ByteBuffer.wrap(new byte[]{'a', 'n','n','o', 'u','n','c','e'}));
		//this seems to do nothing:		
			//	ByteBuffer info_bytes=(ByteBuffer) metaInfo.get(ByteBuffer.wrap(new byte[]{'i', 'n','f','o'}));
			//	String info=new String(info_bytes.array(), "ASCII");
			//	System.out.println(info);
				
				String urlAddress=new String(url_bytes.array(), "ASCII");
				
				System.out.println(urlAddress);
		
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
	 * Will check if a torrent file is valid. if not, returns a boolean.
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

}
