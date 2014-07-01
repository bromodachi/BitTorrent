package btClient;

import java.io.File;
import java.io.IOException;
import java.net.URL; //not sure if we need this one yet but let's see --Conrado, see my note below, -CW
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
//Use ctrl-shift-o or cmd-shift-o in eclipse to automatically import the necessary classes -CW

public class RUBTClient {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws BencodingException 
	 */
	public static void main(String[] args) throws IOException, BencodingException {
		
		TorrentInfoRU activeTorrent;
		
		//Validate args
		validateArgs(args);
			
		//Get all the bytes from the torrent file
		byte[] torrentBytes=getFileBytes(args[0]);
		
		//Decode the torrent to produce it's TorrentInfo object.
		//activeTorrent=decodeTorrent(torrentBytes); 	//This is for our TorrentInfo class
		activeTorrent= new TorrentInfoRU(torrentBytes); 		//This is for Rutgers' TorrentInfoRU class
				
		//Output for testing
		System.out.println("Torrent object contents:\n" + activeTorrent);
		
		//Step 3 - Send an HTTP GET request to the tracker 
		CommunicationTracker establishConnection=new CommunicationTracker(activeTorrent);
		establishConnection.establishConnection();
		
	}//END MAIN
	


	
	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad.
	 * We need to change the image file validation later in the project.
	 * @param args the command line arguments from the main method.
	 */
	public static void validateArgs(String[] args){
		//The main method requires two command line arguments:
		//The name of the torrent file to load, and the name to the resulting file to save-as.
		if(args.length!=2){
			System.err.println("Incorrect arguments. Need 'somefile.torrent' 'picture.jpg'");
			System.exit(1);
		}
		
		//Validating if arg[0] is a correct .torrent file extension
		String torrentChecker=args[0].substring(args[0].lastIndexOf(".")
				+1, args[0].length());
		if(!(torrentChecker.equals("torrent"))){
			System.err.println("Not a valid .torrent file, exiting program.");
		}
		
		
		//TODO - Modify this later for general file types
		//Validating if arg[1] has correct file extension .JPG, .PNG, .GIF, .jpg, .gif, .png;
		String extChecker=args[1].substring(args[1].lastIndexOf(".")
				+1, args[1].length());
		if (!(extChecker.toLowerCase().equals("gif")||
				extChecker.toLowerCase().equals("png")||
				extChecker.toLowerCase().equals("jpg"))){
			System.err.println("Not a valid image file type, exiting program.");
			System.exit(1);
		}
		//System.out.println("Valid args.\n");
	}//END validateArgs
		
	
	/**
	 * Return the bytes of a file to be used with Bencoder2.java
	 * Requires Java7
	 * @param file
	 * @return byte
	 * @throws IOException 
	 */
	public static byte[] getFileBytes(String fileName) throws IOException, BencodingException{
		
		//Create a file with the first argument
		File torrentFile=new File(fileName);
		
		//Make sure the file exists and it's not a directory
		if(!torrentFile.exists() || torrentFile.isDirectory()){
			System.err.println("Couldn't load the torrent file.  Exiting program.");
			System.exit(1);
		}
		
		Path filePath= torrentFile.toPath();
		byte[] torrentBytes=Files.readAllBytes(filePath);
		if(torrentBytes==null){
			System.err.println("Torrent file is empty.  Exiting program.");
			System.exit(1);
		}
		return torrentBytes;
	}//END getFileBytes

	
	/**
	 * Decodes the torrent and creates a TorrentInfo object
	 * 
	 * Note, there are two nested k-v maps: torrentMap (outer) and torrentInfoMap (inner)
	 * torrentMap has the tracker URL (announce), creator, creation date, and torrent info dictionary (torrentInfoMap).
	 * torrentInfoMap is the 'info' dictionary with the rest of the torrent info: 
	 * name (file or directory), length (int) xor files (list of file-maps), and pieces (SHA1 hashes).
	 * The keys of type ByteBuffer Because that is what Bencoder2 returns.
	 * ByteBuffer is used for fast low-level I/O, good for TCP/IP
	 * 
	 * @param torrentMap
	 * @return
	 */
	public static TorrentInfo decodeTorrent(byte[] torrentBytes) throws IOException, BencodingException{
		
		//Returned object
		TorrentInfo thisTorrent=null; //TorentInfo object to be returned
		
		//Temp variables
		ByteBuffer pathBytes=null;
		Map<ByteBuffer, Object> torrentMap = null;
		Map<ByteBuffer, Object> torrentInfoMap =null;
		ByteBuffer urlBytes;
		ByteBuffer createdByBytes;
		ByteBuffer infoBytes;
		ByteBuffer nameBytes;
		ByteBuffer nameBytesUTF;
		ByteBuffer piecesBytes;
		String urlString;
		
		//Raw data in object - do we need to send this to the new object? -CW
		ByteBuffer infoHash=null; //for the tracker?
		ByteBuffer[] piecesHashValues;
		byte[] SHA1Hash; //Not implemented yet.
		
		//declare vars for parsed data in object
		boolean singleFile=true; //Single file to be downloaded
		URL url;
		String creator;
		int creationDate;
		int totalLength=0; //Length of a single file download
		String fileName;
		String fileNameUTF;
		int pieceLength=0;
		ArrayList<Map> fileList=null;
		
		
		try{
		//decode the outer torrent map
		torrentMap=(Map<ByteBuffer, Object>)Bencoder2.decode(torrentBytes); 
		}catch(BencodingException e){
			System.err.println("Error, couldn't decode the torrentmap");
			System.exit(1);
		}
		
		try{
		//Decode the inner 'info' map
		infoBytes=Bencoder2.getInfoBytes(torrentBytes); 
		}catch(BencodingException e){
			System.err.println("Error, couldn't decode the torrentmap");
			System.exit(1);
		}		
		
		//Get the values related to each map key
		torrentInfoMap=(Map<ByteBuffer, Object>) torrentMap.get(ByteBuffer.wrap(new byte[]
				{'i', 'n','f','o'}));
		urlBytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]
				{'a','n','n','o','u','n','c','e'}));
		createdByBytes=(ByteBuffer) torrentMap.get(ByteBuffer.wrap(new byte[]
				{'c','r','e','a','t','e','d',' ','b','y'}));
		creationDate= (int)torrentMap.get(ByteBuffer.wrap(new byte[]
				{'c','r','e','a','t','i','o','n',' ','d','a','t','e'}));
		nameBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
				{'n', 'a','m','e'}));
		nameBytesUTF=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
				{'n', 'a','m','e','.','u','t','f','-','8'}));
		pieceLength=(int) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
				{'p', 'i','e','c','e',' ','l', 'e','n','g','t','h'}));
		piecesBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
				{'p', 'i','e','c','e','s'}));
		if(torrentInfoMap.containsKey(ByteBuffer.wrap(new byte[]{'l', 'e','n','g','t','h'}))){
			singleFile=true;
			totalLength=(int) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
					{'l', 'e','n','g','t','h'}));
		}
		else if(torrentInfoMap.containsKey(ByteBuffer.wrap(new byte[]{'f', 'i','l','e','s'}))){
			singleFile=false;
			pathBytes=(ByteBuffer) torrentInfoMap.get(ByteBuffer.wrap(new byte[]
					{'p', 'a','t','h'}));
		}
		
								
		//Convert ByteBuffer objects to strings
		urlString=new String(urlBytes.array(), "ASCII");
		creator=new String(createdByBytes.array(), "ASCII");
		fileName=new String(nameBytes.array(), "ASCII");
		fileNameUTF=new String(nameBytesUTF.array(), "ASCII");
		String pieces=new String(piecesBytes.array(), "ASCII");
		
		
		/*
		try {
			//don't know what to do with this
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
		
		}*/
		
		/*singleFile, 
		url, 
		creator, 
		creationDate, 
		fileName, 
		fileNameUTF, 
		pieceLength, 
		length, 
		fileList*/
		
		url=new URL(urlString);
		
		
		//Make TorrentInfo object from the data
		thisTorrent=new TorrentInfo(singleFile, url, creator, creationDate, fileName, fileNameUTF, pieceLength, totalLength, fileList);
		
		return thisTorrent;

	}//END decodeTorrent
}
