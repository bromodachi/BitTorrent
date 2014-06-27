import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class parser {
//will change to fit the requirement later on.
	public static void main (String []args) throws IOException, BencodingException{
		if(args.length!=1){
			System.out.println("Forgot to input args");
		}
		else{
			File torrentFile=new File(args[0]);
			String torrentFileName=torrentFile.getName();
			String torrentChecker=torrentFileName.substring(torrentFileName.lastIndexOf(".")+1, torrentFileName.length());
			System.out.println(torrentChecker);
			if(torrentChecker.equals("torrent")==true){
				System.out.println("Success");
				Bencoder2 test=new Bencoder2();
				byte [] array=readTorrent(torrentFile);
				ByteBuffer blah=test.getInfoBytes(array);
				Object another=test.decode(array);
				System.out.println(blah);
				System.out.println(another);
				byte[] test2=test.encode(another);
				System.out.println(test2);
			}
			else{
				System.out.println("False");
			}
			
		}
	}
	/*Converting file to byte*/ 
	public static byte [] readTorrent(File file) throws IOException{
		//avoid overflow
		if(file.length()>Integer.MAX_VALUE){
			System.out.println("Error, exiting");
			return null;
		}
		//create a byte array of the file size
		byte [] array=new byte[(int)file.length()];
		
		
		InputStream reader=null;
		try{
			//make sure we can read the whole file
			reader=new FileInputStream(file);
			if(reader.read(array)==-1){
				System.out.println("Couldn't read file");
			}
			
		} finally{
			
			try{
				if(reader!=null){
					reader.close();
				}
			} catch(IOException e){
				System.out.println("error, exiting program");
			}
		}
		return array;
		
	}

}