package gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import jBackup.BackupList;

public class FileChooserPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private DirTree tree = new DirTree();
	private JButton chroot = new JButton("change tree root");
	private JButton chooseFolder = new JButton("choose file manually");
	private JButton addSelected = new JButton("add selected folder");
	
	private BackupList list;

	public FileChooserPanel(BackupList list) {
		this.list = list;
		setLayout(new BorderLayout());
		tree.show();
		add(tree, BorderLayout.CENTER);

		JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        chroot.addActionListener(e -> {
        	JFileChooser jfc = new JFileChooser(tree.getRoot());
        	jfc.setDialogTitle("Select a folder to be the root of tree view");
        	jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        	if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        	tree.show(jfc.getSelectedFile());
        });
        controls.add(chroot, gbc);
        chooseFolder.addActionListener(e -> {
        	JFileChooser jfc = new JFileChooser(list.size() != 0 ? list.get(list.size() - 1) : new File(System.getProperty("user.dir")));
        	jfc.setDialogTitle("Select files of folders to be included in backup list");
        	jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        	jfc.setMultiSelectionEnabled(true);
        	if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        	for(File f : jfc.getSelectedFiles()) {
        		list.add(f);
        	}
        });
        controls.add(chooseFolder, gbc);
        
        
		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.add(controls, BorderLayout.NORTH);
		addSelected.addActionListener(e -> {
			list.addAll(tree.getSelected());
		});
		btnPanel.add(addSelected, BorderLayout.SOUTH);
		add(btnPanel, BorderLayout.EAST);
	}
	
	
	public BackupList getList() {
		return list;
	}

	public void setList(BackupList list) {
		this.list = list;
	}

}
