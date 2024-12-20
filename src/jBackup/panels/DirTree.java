package jBackup.panels;

import java.awt.BorderLayout;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class DirTree extends JPanel {

	private static final long serialVersionUID = -209686442103557344L;
	
	private JTree tree;
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode rootNode;
	
	public DirTree() {
		setLayout(new BorderLayout());
		tree = new JTree();
        tree.setShowsRootHandles(true);
		show();
		add(new JScrollPane(tree), BorderLayout.CENTER);
	}

	public void show() {
		show(new File(System.getProperty("user.dir")));
	}
	public void show(File root) {
		rootNode = new DefaultMutableTreeNode(new FileNode(root));
	    treeModel = new DefaultTreeModel(rootNode);
	    tree.setModel(treeModel);

        addChild(root, rootNode);
	}
	
    private void addChild(File file, DefaultMutableTreeNode parent) {
        File[] files = file.listFiles();
        if (files == null) return;

        for (File f : files) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(f));
            parent.add(childNode);
            if (f.isDirectory()) {
            	SwingUtilities.invokeLater(() -> addChild(f, childNode));
            }
        }
    }
    
    public File getRoot() {
    	return ((FileNode)rootNode.getUserObject()).file;
    }


	public LinkedList<File> getSelected() {
		LinkedList<File> list = new LinkedList<>();
		
		for(TreePath f : tree.getSelectionPaths()) {
			list.add(((FileNode)((DefaultMutableTreeNode)f.getLastPathComponent()).getUserObject()).file);
		}
		return list;
	}

	
}
