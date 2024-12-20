package jBackup;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BackupList {

	private TreeSet<File> list = new TreeSet<>();
	private Runnable callback;

	public BackupList(Runnable callback) {
		this.callback = callback;
	}

	public void add(File f) {
		list.add(f);
		scanList();
		callback.run();
	}
	
	public void addAll(List<File> selected) {
		list.addAll(selected);
		scanList();
		callback.run();
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
	}
}
