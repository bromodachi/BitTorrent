package btClient;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
import javax.swing.table.TableModel;

import java.awt.Dimension;

import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class GUIFrame extends JFrame implements ActionListener, Runnable {
	private ArrayList<ActiveTorrent> torrents;
	private GUIFrame frame = this;
	private JTable torrentTable;

	public GUIFrame() {
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
		torrentTable.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"File Name", "Progress", "Status"
			}
		) {
			Class[] columnTypes = new Class[] {
				String.class, Object.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});
		scrollPane.setViewportView(torrentTable);

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				frame.setVisible(false);
				for (ActiveTorrent torrent : torrents) {
					try {
						torrent.stop();
					} catch (BtException | InterruptedException e) {
						// do nothing
					}
				}
			}
		});
	}

	@Override
	public void run() {
		this.setVisible(true);
		this.requestFocus();

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

	private void addTorrent() throws FileNotFoundException, BencodingException,
			IOException {
		File torrentInfo = null, file = null;
		FileFilter filter = new FileNameExtensionFilter("Torrent File",
				"torrent");
		JFileChooser fc = new JFileChooser();
		//fc.addChoosableFileFilter(filter);
		fc.setFileFilter(filter);
		// Prompt user for torrent file
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			torrentInfo = fc.getSelectedFile();
		} else {
			return;
		}
		fc.removeChoosableFileFilter(filter);
		// prompt user for save location
		returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fc.getSelectedFile();
		} else {
			return;
		}
		ActiveTorrent torrent = new ActiveTorrent(new TorrentInfo(BtUtils.getFileBytes(torrentInfo)), file);
		torrents.add(torrent);
		Object [] row = new Object [3];
		row[0] = (Object)file.getName();
		row[1] = (Object)torrent.getProgressBar();
		row[2] = (Object)torrent.getStatus().toString();
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		torrent.setGuiIndex(model.getRowCount());
		model.addRow(row);
		
	}

	private void removeTorrent() throws IOException, BtException, InterruptedException {
		int row = torrentTable.getSelectedRow();
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		String fileName = (String) model.getValueAt(row, 0);
		for(ActiveTorrent torrent : torrents){
			if(fileName.equals(torrent.getFileName())){
				torrent.stop();
				torrents.remove(torrent);
				break;
			}
		}
		model.removeRow(row);
	}

	private void startTorrent() throws IOException, BtException {
		int row = torrentTable.getSelectedRow();
		for(ActiveTorrent torrent : torrents){
			if(torrent.getGuiIndex() == row){
				torrent.start();
				break;
			}
		}
	}

	private void stopTorrent() throws InterruptedException, BtException {
		int row = torrentTable.getSelectedRow();
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		for(ActiveTorrent torrent : torrents){
			if(torrent.getGuiIndex() == row){
				torrent.stop();
				break;
			}
		}
	}
	
	private void updateStatus(){
		DefaultTableModel model = (DefaultTableModel) torrentTable.getModel();
		for(ActiveTorrent torrent : torrents){
			model.setValueAt((Object)torrent.getStatus().toString(), torrent.getGuiIndex(), BtUtils.TORRENT_TABLE_STATUS_COLUMN);
		}
	}
}
