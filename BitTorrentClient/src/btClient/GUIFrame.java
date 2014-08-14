package btClient;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.Dimension;

import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class GUIFrame extends JFrame implements ActionListener, Runnable {
	private ArrayList<ActiveTorrent> torrents;
	private GUIFrame frame = this;
	private JTable torrentTable;
	private int server_port = BtUtils.STARTING_LISTENING_PORT;

	/**
	 * Creates a new Exceptional BitTorrent Client GUI
	 */
	public GUIFrame() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(400, 300));
		setPreferredSize(new Dimension(600, 800));
		setTitle("Exceptional BitTorrent Client");
		torrents = new ArrayList<ActiveTorrent>();

		JSplitPane splitPane = new JSplitPane();
		splitPane.setPreferredSize(new Dimension(600, 800));
		getContentPane().add(splitPane, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		splitPane.setLeftComponent(panel_1);
		panel_1.setLayout(new GridLayout(0, 1, 0, 0));

		JButton addTorrentButton = new JButton("Add Torrent");
		addTorrentButton.addActionListener(this);
		panel_1.add(addTorrentButton);

		JButton removeTorrentButton = new JButton("Remove Torrent");
		removeTorrentButton.addActionListener(this);
		panel_1.add(removeTorrentButton);

		JButton startTorrentButton = new JButton("Start Torrent");
		startTorrentButton.addActionListener(this);
		panel_1.add(startTorrentButton);

		JButton stopTorrentButton = new JButton("Stop Torrent");
		stopTorrentButton.addActionListener(this);
		panel_1.add(stopTorrentButton);

		JScrollPane scrollPane = new JScrollPane();
		splitPane.setRightComponent(scrollPane);

		torrentTable = new JTable();
		torrentTable.setRowHeight(30);
		torrentTable.setModel(new DefaultTableModel(new Object[][] {}, new String[] { "File Name", "Progress", "Status" }) {
			@SuppressWarnings("rawtypes")
			Class[] columnTypes = new Class[] { String.class, Object.class, String.class };

			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});

		scrollPane.setViewportView(torrentTable);

		// Set closing behavior, stop all torrent activity when window closes
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				frame.setVisible(false);
				// Stop each active torrent
				for (ActiveTorrent torrent : torrents) {
					try {
						torrent.stop();
					} catch (InterruptedException | BtException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void run() {
		this.setVisible(true);
		this.requestFocus();
		// Continuously update the status of active torrents while GUI is open
		while (true) {
			updateStatus();
			validate();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "Add Torrent":
			try {
				addTorrent();
			} catch (BencodingException | IOException e1) {
				e1.printStackTrace();
			}
			break;
		case "Remove Torrent":
			try {
				removeTorrent();
			} catch (IOException | InterruptedException | BtException e1) {
				e1.printStackTrace();
			}
			break;
		case "Start Torrent":
			try {
				startTorrent();
			} catch (IOException | BtException e1) {
				e1.printStackTrace();
			}
			break;
		case "Stop Torrent":
			try {
				stopTorrent();
			} catch (InterruptedException | BtException e1) {
				e1.printStackTrace();
			}
			break;
		default:

		}
	}

	/**
	 * Prompts user for .torrent file and save file locations and create a new
	 * ActiveTorrent object to be added
	 * 
	 * @throws FileNotFoundException
	 * @throws BencodingException
	 * @throws IOException
	 */
	private void addTorrent() throws FileNotFoundException, BencodingException, IOException {
		TorrentInfo torrentInfo = null;
		File file = null;
		// Create file chooser and torrent file filter
		FileFilter filter = new FileNameExtensionFilter("Torrent File", "torrent");
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(filter);

		// Prompt user for torrent file
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			torrentInfo = new TorrentInfo(BtUtils.getFileBytes(fc.getSelectedFile()));
		} else {
			return;
		}
		fc.removeChoosableFileFilter(filter);

		// prompt user for save location
		fc.setSelectedFile(new File(torrentInfo.file_name));
		returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
		} else {
			return;
		}

		// Create new ActiveTorrent object and add it to the table
		ActiveTorrent torrent = new ActiveTorrent(torrentInfo, file, getNextServerPort());
		addActiveTorrent(torrent);
	}

	/**
	 * Removes the selected {@link ActiveTorrent} from the torrent table
	 * 
	 * @throws IOException
	 * @throws BtException
	 * @throws InterruptedException
	 */
	private void removeTorrent() throws IOException, BtException, InterruptedException {
		int row = torrentTable.getSelectedRow();
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		String fileName = (String) model.getValueAt(row, 0);
		for (ActiveTorrent torrent : torrents) {
			if (fileName.equals(torrent.getFileName())) {
				torrent.stop();
				torrents.remove(torrent);
				break;
			}
		}
		model.removeRow(row);
	}

	/**
	 * Starts the {@link ActiveTorrent} object associated with the selected row
	 * of the Torrent Table
	 * 
	 * @throws IOException
	 * @throws BtException
	 */
	private void startTorrent() throws IOException, BtException {
		int row = torrentTable.getSelectedRow();
		for (ActiveTorrent torrent : torrents) {
			if (torrent.getGuiIndex() == row) {
				new Thread(torrent).start();
				break;
			}
		}
	}

	/**
	 * Stops the selected {@link ActiveTorrent}
	 * 
	 * @throws InterruptedException
	 * @throws BtException
	 */
	private void stopTorrent() throws InterruptedException, BtException {
		int row = torrentTable.getSelectedRow();
		for (ActiveTorrent torrent : torrents) {
			if (torrent.getGuiIndex() == row) {
				torrent.stop();
				break;
			}
		}
	}

	/**
	 * Updates the status of all the {@link ActiveTorrents} in the GUI Torrent
	 * Table
	 */
	private void updateStatus() {
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		for (ActiveTorrent torrent : torrents) {
			model.setValueAt((Object) torrent.getStatus().toString(), torrent.getGuiIndex(), BtUtils.TORRENT_TABLE_STATUS_COLUMN);
			model.setValueAt((Object) (Integer.toString(torrent.getPercentComplete()) + "%"), torrent.getGuiIndex(), BtUtils.TORRENT_TABLE_PROGRESS_COLUMN);
		}
	}

	/**
	 * Adds the given {@link ActiveTorrent} object to the GUI's Torrent Table
	 * 
	 * @param torrent
	 *            The {@link ActiveTorrent} to be added to the torrent table
	 */
	public void addActiveTorrent(ActiveTorrent torrent) {
		torrents.add(torrent);
		Object[] row = new Object[3];
		row[0] = (Object) torrent.getFileName();
		row[1] = (Object) (Integer.toString(torrent.getPercentComplete()) + "%");
		row[2] = (Object) torrent.getStatus().toString();
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		torrent.setGuiIndex(model.getRowCount());
		model.addRow(row);
	}

	public synchronized int getNextServerPort() {
		return server_port++;
	}
}
