package jBackup;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

public class BackupList {

	private TreeSet<File> list = new TreeSet<>();
	private Runnable callback;

	public BackupList(Runnable callback) {
		this.callback = callback;
	}

	public void add(File f) {
		try {
			list.add(new File(f.getCanonicalPath()));
		} catch (IOException e) {
			list.add(new File(f.getAbsolutePath()));
			e.printStackTrace();
		}
		scanList();
		callback.run();
	}
	
	public void addAll(List<File> selected) {
		selected.forEach(this::add);
	}

	private void scanList() {
		Iterator<File> it = list.iterator();
		while(it.hasNext()) {
			File f = it.next();
			for(File other : list) {
				if(f.getAbsolutePath().startsWith(other.getAbsolutePath() + File.separator)) {
					it.remove();
					break;
				}
			}
		}
	}

	public int size() {
		return list.size();
	}
	public boolean isEmpty() {
		return list.isEmpty();
	}

	public File get(int i) {
		return (File) list.toArray()[i];
	}

	public void forEach(Consumer<File> c) {
		list.forEach(c);
	}
	
	public Stream<File> stream() {
		return list.stream();
	}
	
	public void clear() {
		list.clear();
		callback.run();
	}

	public void removeIndex(int i) {
		list.remove(get(i));
		callback.run();
	}

	public void runCallback() {
		SwingUtilities.invokeLater(callback);
	}
}
