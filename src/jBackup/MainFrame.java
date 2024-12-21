package jBackup;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import jBackup.panels.FileChooserPanel;
import jBackup.panels.FileNode;

public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private BackupList list = new BackupList(this::updateBackupList);
	private DefaultMutableTreeNode backupStatusTreeRoot;
	private DefaultTreeModel backupStatusTreeModel;
	private JTree backupStatusTree = new JTree();
	
	private JMenuBar menuBar;

	private JLabel backupDestLabel = new JLabel("Backup destination : " + Main.getBackupDir().getAbsolutePath());
	private JTextField snapshot = new JTextField(5);
	private JComboBox<String> cb_snapshotSelect = new JComboBox<String>();
	private DefaultComboBoxModel<String> cb_snapshotModel = (DefaultComboBoxModel<String>)cb_snapshotSelect.getModel();
	private DefaultListModel<String> listModel = new DefaultListModel<>();
	
	public MainFrame() {
        setLocationByPlatform(true);
        setSize(600, 600);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        Main.load(list, Main.getListFile());
        setTitle("Backup : " + Main.getListFile().getName());
        
		backupStatusTree.setShowsRootHandles(false);
		updateBackupList();
        
        JTabbedPane tab = new JTabbedPane();
        tab.add(getSummaryPanel(), "Backuped configuration");
        tab.add(new FileChooserPanel(list), "Choose what to backup");
        tab.add(new JScrollPane(backupStatusTree), "Backuped files(Tree view)");
        add(tab, BorderLayout.CENTER);
        
        JPanel bottom = new JPanel();
        JButton startBackup = new JButton("Start backup");
        JButton restoreBackup = new JButton("Restore backup");
        startBackup.addActionListener(e -> {
        	Main.backup(list);
        });
        restoreBackup.addActionListener(e -> {
        	Main.restore(list);
        });
        bottom.add(Box.createHorizontalStrut(30));
        bottom.add(startBackup);
        bottom.add(restoreBackup);
        add(bottom, BorderLayout.SOUTH);
        
        addMenuBar();
        
        //pack();
        setVisible(true);
	}
	
	private JPanel getSummaryPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel backupDest = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton browse = new JButton("Browse");
        browse.addActionListener(e -> setBackupDir());
        backupDest.add(backupDestLabel);
        backupDest.add(browse);
        p.add(backupDest);
        
        JPanel snapshots = new JPanel(new FlowLayout(FlowLayout.LEFT));
        snapshots.add(new JLabel("Maximum number of snapshots : "));
        snapshot.setText("" + Main.getMaxSnapshots());
        snapshot.setToolTipText("Press enter to apply!");
        snapshot.addActionListener(e ->{
        	try {
				Main.setMaxSnapshots(Integer.parseInt(snapshot.getText()));
			} catch (NumberFormatException e1) {}
        	updateBackupList();
        });
        snapshots.add(snapshot);
        p.add(snapshots);
		
        JPanel snapshotSelect = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton remove_snapshot = new JButton("remove");
        remove_snapshot.addActionListener(e -> {
        	String selected = (String)cb_snapshotModel.getSelectedItem();
        	File dir = new File(Main.getBackupDir(), selected);
        	if(JOptionPane.showConfirmDialog(null, "Remove backup snapshot %s?\nLocation : %s".formatted(selected, dir.getAbsolutePath()),
        			"Remove snapshot?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;
        	
        	try {
				Main.removeDir(dir);
				cb_snapshotModel.removeElement(selected);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        snapshotSelect.add(new JLabel("Choose snapshot : "));
        snapshotSelect.add(cb_snapshotSelect);
        snapshotSelect.add(remove_snapshot);
        p.add(snapshotSelect);
        p.add(Box.createVerticalStrut(10));
        p.add(new JSeparator());
        
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.add(new JLabel("Backuped files list"));
        JButton remove_list = new JButton("remove selected");
        labelPanel.add(remove_list);
        p.add(labelPanel);
        
        JPanel listPanel = new JPanel(new BorderLayout());
        JList<String> jlist = new JList<>(listModel);
        jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        remove_list.addActionListener(e ->{
        	int[] arr = jlist.getSelectedIndices();
        	for(int i = arr.length - 1; i > -1; i--) {
        		listModel.removeElementAt(arr[i]);
        		list.removeIndex(arr[i]);
        	}
        });
		listPanel.add(new JScrollPane(jlist), BorderLayout.CENTER);
		p.add(listPanel);
        
        
        return p;
	}

	public void updateBackupList() {
		backupDestLabel.setText(Main.DESTINATION_PREFIX + Main.getBackupDir().getAbsolutePath());
		listModel.clear();
		list.stream().map(File::getAbsolutePath).forEach(listModel::addElement);
		
		cb_snapshotModel.removeAllElements();
	    Main.getBackupDateString().sorted((s1, s2) -> s2.compareTo(s1)).forEach(cb_snapshotModel::addElement);
	    
	    snapshot.setText("" + Main.getMaxSnapshots());
	    
		backupStatusTreeRoot = new DefaultMutableTreeNode("(root)");
		backupStatusTreeModel = new DefaultTreeModel(backupStatusTreeRoot);
		backupStatusTree.setModel(backupStatusTreeModel);

        list.forEach(f -> addChild(f, backupStatusTreeRoot));
        for (int i = 0; i < backupStatusTree.getRowCount(); i++) {
        	backupStatusTree.expandRow(i);
        }
	}
	
	private void addChild(File file, DefaultMutableTreeNode parent) {
        File[] files = file.listFiles();
        if (files == null) files = new File[] { file };

        for (File f : files) {
        	Stack<File> s = new Stack<File>();
        	File p = f;
        	while((p = p.getParentFile()) != null) s.add(p);
        	
        	DefaultMutableTreeNode n = backupStatusTreeRoot;
        	while(!s.isEmpty()) n = addNode(s.pop(), n);
        	
        	DefaultMutableTreeNode childNode = addNode(f, n);
        	//int row = backupStatusTree.getRowForPath(new TreePath(childNode.getPath()));
        	//for(TreeNode t : childNode.getPath()) System.out.print(t.toString());
        	//System.out.println();
        	//System.out.println(row);
        	//for(int i = 0; i < row; i++) backupStatusTree.expandRow(i);
        	//backupStatusTree.expandPath(new TreePath((childNode.isLeaf() ? (DefaultMutableTreeNode)childNode.getParent() : childNode).getPath()));
        	
            if (f.isDirectory()) {
            	addChild(f, childNode);
            }
        }
    }

	private DefaultMutableTreeNode addNode(File f, DefaultMutableTreeNode parent) {
    	boolean exist = false;
		Enumeration<TreeNode> e = parent.children();
		DefaultMutableTreeNode ret = null;
    	while(e.hasMoreElements()) {
    		ret = (DefaultMutableTreeNode)e.nextElement();
			FileNode fn = (FileNode) (ret).getUserObject();
    		if(fn.file.equals(f)) {
    			exist = true;
    			break;
    		}
    	}
    	DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(f));
    	if(!exist) {
    		ret = childNode;
			parent.add(childNode);
		}
    	return ret;
	}
    
	private void addMenuBar() {
		menuBar = new JMenuBar();
		 
		JMenu m_backupList = new JMenu("Backup List");
		menuBar.add(m_backupList);
		 
		JMenuItem mi_save = new JMenuItem("Save", KeyEvent.VK_S);
		mi_save.addActionListener(e -> {
			Main.save(list);
		});
		JMenuItem mi_saveAs = new JMenuItem("Save as...", KeyEvent.VK_A);
		mi_saveAs.addActionListener(e -> {
			File file = Main.getListFile();
			JFileChooser jfc = new JFileChooser();
			jfc.setDialogTitle("Save backup list");
			int response = JOptionPane.CANCEL_OPTION;
			while (response != JOptionPane.OK_OPTION) {
				jfc.setCurrentDirectory(file.getParentFile());
				if (jfc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
					return;
				file = jfc.getSelectedFile();
				if(!file.exists()) break;
				
				response = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Confirm Overwrite",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			}
			Main.save(list, file);
		});
		JMenuItem mi_load = new JMenuItem("Load", KeyEvent.VK_L);
		mi_load.addActionListener(e -> {
			JFileChooser jfc = new JFileChooser(Main.getListFile().getParent());
			jfc.setDialogTitle("Load backup list");
			if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;
			File file = jfc.getSelectedFile();
			Main.load(list, file);
			setTitle("Backup : " + file.getName());
		});
		JMenuItem mi_open = new JMenuItem("Open list file", KeyEvent.VK_O);
		mi_open.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(Main.getListFile());
			} catch (IOException err) {
				error(err, "Cannot open config file!", "%e%");
			}
		});
		m_backupList.add(mi_save);
		m_backupList.add(mi_saveAs);
		m_backupList.add(mi_load);
		m_backupList.add(mi_open);
		
		
		JMenu m_backupDir = new JMenu("Backup Destination");
		menuBar.add(m_backupDir);
		 
		JMenuItem mi_setDir = new JMenuItem("Change", KeyEvent.VK_S);
		mi_setDir.addActionListener(e -> {
			setBackupDir();
		});
		JMenuItem mi_openDir = new JMenuItem("Open backup dir", KeyEvent.VK_O);
		mi_openDir.addActionListener(e -> {
			File f = Main.getBackupDir();
    		try {
    			Desktop.getDesktop().open(f);
    		} catch (IOException err) {
    			error(err, "Cannot open directory explorer!", "Please open manually " + f.getAbsolutePath() + "\n%e%");
    		}
		});
		m_backupDir.add(mi_setDir);
		m_backupDir.add(mi_openDir);
		
		
		setJMenuBar(menuBar);
	}
	
	
	private void setBackupDir() {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Change backup directory");
		jfc.setCurrentDirectory(Main.getBackupDir());
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		Main.setBackupDir(jfc.getSelectedFile());
		updateBackupList();
	}

	public static void error(Exception e, String title, String content) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(null, title, content.replace("%e%", sw.toString()), JOptionPane.ERROR_MESSAGE);
		});
	}
}
