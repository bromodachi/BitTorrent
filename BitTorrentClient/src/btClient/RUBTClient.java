/* Internet Technologies 198:352:F6
 * Rutgers University Summer 2014
 * Programming Project: BitTorrent Client Part 1
 * Team: Exception-all-ists
 * Cody Goodman
 * Conrado Uraga 
 */
package btClient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * This is the main client class for CS352, BitTorrent project 1 The program is
 * designed to load a .torrent file, interface with a tracker and a single peer
 * and will download a single file (a JPEG) from that peer. The file is then
 * saved to a hard disk. All communication is done over TCP.
 * 
 * @author Cody Goodman & Conrado Uraga
 *
 */
public class RUBTClient implements ActionListener {

	/**
	 * This is the main method that is called upon program startup, this method
	 * is tasked with validating the program's arguments and then instantiating
	 * the other classes need to run the program. When the download is finished
	 * this method verifies that it has completed successfully
	 * 
	 * @param args
	 *            The name of the .torrent file to be loaded and the name of the
	 *            file to save the data to, using the proper path and file
	 *            extensions.
	 * @throws IOException
	 * @throws BencodingException
	 * @throws InterruptedException
	 */
	public static void main (String[] args) throws IOException,
			BencodingException, InterruptedException {

		// Step 1 - Take the command line arguments
		if (!validateArgs(args)) {
			return;
		}
		GUIFrame gui = new GUIFrame();
		gui.run();
		
/*		try {
			new ActiveTorrent(new TorrentInfo(BtUtils.getFileBytes(new File(args[0]))), new File(args[1])).start();
		} catch (BtException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		// temporary testing code

	}// END MAIN

	/**
	 * Validates the command line arguments to see if the parameters are good.
	 * The program exits if the arguments are bad.
	 * 
	 * @param args
	 *            The command line arguments from the main method.
	 * @return True if both arguments are valid, otherwise false
	 * @throws IOException
	 */
	public static boolean validateArgs(String[] args) throws IOException {
		// The main method requires two command line arguments:
		// The name of the torrent file to load, and the name to the resulting
		// file to save-as.
		if (args.length != 2) {
			System.err.println("Incorrect arguments. "
					+ "Need 'somefile.torrent' 'picture.jpg'");
			return false;
		}

		// Validating if arg[0] is a correct .torrent file extension
		String torrentChecker = args[0].substring(args[0].lastIndexOf(".") + 1,
				args[0].length());
		if (!(torrentChecker.equals("torrent"))) {
			System.err.println("Not a valid .torrent file, exiting program.");
			return false;
		}
		// check that a new file can be created with the second argument
		return true;
	}// END validateArgs

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
}