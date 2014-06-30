package btClient;

import java.io.File;
import java.io.IOException;
import java.net.URL; //not sure if we need this one yet but let's see --Conrado, see my note below, -CW
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
//Use ctrl-shift-o or cmd-shift-o in eclipse to automatically import the necessary classes -CW

public class main {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws BencodingException 
	 */
	public static void main(String[] args) throws IOException, BencodingException {
		boolean singleFile=true; //Flag for setting the download to expect a single file
		File torrentFile=null;
		int infoLength=0;
		byte[] torrentBytes; 	//The raw bytes from the torrent file.
		byte[] SHA1Hash;		//Not implemented yet.
		ByteBuffer infoHash=null; //for the tracker?
		ByteBuffer[] piecesHashValues;
		ByteBuffer pathBytes=null;
		Map<ByteBuffer, Object> torrentMap = null; 	//bencoder gives us a Map for the url
															//use this to get the url and the info map
		Map<ByteBuffer, Object> torrentInfoMap =null; 		//the torrent Info's map. Use this to get the torrent
															//information
		//The keys of type ByteBuffer Because that is what Bencoder2 returns.
		//ByteBuffer is used for fast low-level I/O, good for TCP/IP
		
		//The main method requires two command line arguments:
		//The name of the torrent file to load, and the name to the resulting file to save-as.
		if(args.length!=2){
			System.out.println("Incorrect arguments. Need 'somefile.torrent' 'picture.jpg'");
			return;
		}
		/*Are we only allowing pictures for now?*/
		//I don't think we need to limit it, but we can leave the method in for now. -CW
		
		
		
		//Check if the first argument is a torrent file and proceed
		if(hasTorrentFileExtension(args[0])){
			/*Thinking of using this info to create an object to use
			 * later on. 
			 */
			
			//Check if the second argument is a valid image file and proceed
			if(hasImageFileExtension(args[1])){
				System.out.println("Valid command line arguments:");
				System.out.println("   " + args[0]);
				System.out.println("   " + args[1] + "\n");
				
				//Create a file with the first argument
				torrentFile=new File(args[0]);
				
				//Make sure the file exists and it's not a directory
				if(torrentFile.exists() && !torrentFile.isDirectory()){
				
				//Get all the bytes from the torrent file
				torrentBytes=getFileBytes(torrentFile);
				if(torrentBytes==null){
					System.out.println("Couldn't load the torrent file.");
					return;
				}
				
				//Note, there are two nested k-v maps: torrentMap (outer) and torrentInfoMap (inner)
				//torrentMap has the tracker URL (announce), creator, creation date, and torrent info dictionary (torrentInfoMap).
				//torrentInfoMap is the 'info' dictionary with the rest of the torrent info: 
				//name (file or directory), length (int) xor files (list of file-maps), and pieces (SHA1 hashes).
				
				//Decode the torrent file (byte[]) to get the main torrentMap.
				try{
				torrentMap=(Map<ByteBuffer, Object>)Bencoder2.decode(torrentBytes); //decode() returns object, needs casting
				}catch(BencodingException e){
					System.out.println("Error, couldn't decode the torrentmap");
					return;
				}
				
				//Get-by-key each value from the outer torrentMap
				
				//urlBytes (announce) - the URL of the tracker
				ByteBuffer urlBytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]
						{'a','n','n','o','u','n','c','e'}));
				//createdByBytes (created by) - the torrent author
				ByteBuffer createdByBytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]
						{'c','r','e','a','t','e','d',' ','b','y'}));
				//creationDateBytes (creation date) - the date the torrent was created
				int creationDate= (int)torrentMap.get(ByteBuffer.wrap(new byte[]
						{'c','r','e','a','t','i','o','n',' ','d','a','t','e'}));
				//info -This maps to a dictionary, with keys described below.
				torrentInfoMap=(Map<ByteBuffer, Object>) torrentMap.get(ByteBuffer.wrap(new byte[]{'i', 'n','f','o'}));
				//A byteBuffer version of the same infoMap? What is this for? Do we need both parses?-CW
				ByteBuffer infoBytes=Bencoder2.getInfoBytes(torrentBytes);
				
				//Get-by-key each value from the inner torrentInfoMap
				
				//name- key maps to a string which is the suggested name to save the file/directory as (optional). 
				ByteBuffer nameBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
						{'n', 'a','m','e'}));
				//name.utf-8 - not sure what this is for, but it is in our torrent file
				ByteBuffer nameBytesUTF=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
						{'n', 'a','m','e','.','u','t','f','-','8'}));
				//piece length
				int piecesLength=(int) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
						{'p', 'i','e','c','e',' ','l', 'e','n','g','t','h'}));
				//pieces- an n*20 length string of concatenated SHA1 hashes of each piece at the corresponding index.
				ByteBuffer piecesBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
						{'p', 'i','e','c','e','s'}));
				//length or files - There are two cases here, single file (k=length) and multiple files (k=files)
				if(torrentInfoMap.containsKey(ByteBuffer.wrap(new byte[]{'l', 'e','n','g','t','h'}))){
					singleFile=true;
					//length - The length of the file, in bytes.
					infoLength=(int) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
							{'l', 'e','n','g','t','h'}));
				}
				else if(torrentInfoMap.containsKey(ByteBuffer.wrap(new byte[]{'f', 'i','l','e','s'}))){
					singleFile=false;
					//Path - A list of UTF-8 encoded strings corresponding to subdirectory names, last being filename.
					pathBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
							{'p', 'a','t','h'}));
				}
				
				
				
				//Do we expect to find this key in the torrent file? 
				//I think this is for sending GET request to tracker -CW
				//I'm going to comment this out for now. -CW
				/*
				ByteBuffer trackerBytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]{'i', 'p'}));
				if(trackerBytes==null){
					//An optional parameter giving the IP (or dns name) which this peer is at. 
					//Generally used for the origin if it's on the same machine as the tracker.
					System.out.println("IP key not found in torrent file.");
					//Is this something we expect to find in the torrent file?
					//I thought this was implemented in the tracker? -CW
				}
				*/
								
				
				/*Converting some of the bytes into string. However,
				 * some seems to be encrypted. */
				String urlAddress=new String(urlBytes.array(), "ASCII");
				String author=new String(createdByBytes.array(), "ASCII");
				//creationDate already an int.
				//from info map:
				String fileName=new String(nameBytes.array(), "ASCII");
				String fileNameUTF=new String(nameBytesUTF.array(), "ASCII");
				String pieces=new String(piecesBytes.array(), "ASCII");
				
				
				
				String info_test=new String(infoBytes.array(), "ASCII");
				if(!singleFile){
					System.out.println("yes");
					String path=new String(pathBytes.array(), "ACII");
				}
				try {
					/*don't know what to do with this*/
						MessageDigest crypt=MessageDigest.getInstance("SHA-1");
						crypt.update(infoBytes);
						byte [] infoHashTemp=crypt.digest();
						infoHash=ByteBuffer.wrap(infoHashTemp);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
				
				byte piece_array []=piecesBytes.array();
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
						torrentBytes, infoHash,
						piecesHashValues, addressFile,
						urlAddress, infoLength, piecesLength);
				/*System.out.println("testing: "+torrentInfoObject.getStringURLAddress());
				System.out.println(fileName);
				System.out.println(infoLength);
				System.out.println(piecesLength);
				System.out.println(pieces.length()+ "pieces array length: "+piece_array.length);
				

				*/
				
				
				
				//Output for testing
				
				
				System.out.println(torrentInfoObject);
				
				
				CommunicationTracker establishConnection=new CommunicationTracker(torrentInfoObject);
				establishConnection.establishConnection();
				
				}
				
				else{
				System.out.println("Couldn't load torrent file.");
					return;
				}
				
		
			}
			else{
				System.out.println("Not a valid photo file.");
			}
			
		}
		else{
			System.out.println("Not a valid Torrent file.\n Exiting");
			return;
		}

	}
	
	/**
	 * File extension checker for the 2nd arg, returns a boolean.
	 * Will possibly remove this later if we can different file types
	 * @param fileName
	 * @return true or false
	 */
	public static boolean hasImageFileExtension(String fileName){
		//"JPG, PNG, & GIF Images", "jpg", "gif", "png"));
		String extChecker=fileName.substring(fileName.lastIndexOf(".")
				+1, fileName.length());
		return extChecker.toLowerCase().equals("gif")||
				extChecker.toLowerCase().equals("png")||
				extChecker.toLowerCase().equals("jpg");
	}
	
	/**
	 * Will check if the file argument has the correct .torrent extension. Returns a boolean.
	 * At the moment, it is case sensitive and will only approve an all-lowercase extension.
	 * @param torrentFile
	 * @return true or false
	 */
	public static boolean hasTorrentFileExtension(String torrentFile){
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
