package jBackup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import gui.MainFrame;

public class Main {

	public static final String DESTINATION_PREFIX = "Backup destination : ";
	public static final String MAXSNAPSHOTS_PREFIX = "Max Snapshots : ";
	private static File listFile = new File("./backupList.txt");
	private static File backupDir = new File("./Mybackup");
	private static int maxSnapshots = 5;
	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH_mm_ss");
	
	private static MainFrame mf;
	
	public static void main(String[] args) throws IOException {
		if(!listFile.exists()) listFile.createNewFile();
		if(!backupDir.exists()) backupDir.mkdirs();
		
		/* If not using this, AbsolutePath might be like "/Users/eugenehong/git/JBackup/./Mybackup" */
		backupDir = backupDir.getCanonicalFile();
		
		SwingUtilities.invokeLater(() -> mf = new MainFrame());
	}
	

	public static File getListFile() {
		return listFile;
	}
	public static int getMaxSnapshots() {
		return maxSnapshots;
	}
	public static void setMaxSnapshots(int max) {
		maxSnapshots = max;
	}
	public static File getBackupDir() {
		return backupDir;
	}
	public static void setBackupDir(File dir) {
		backupDir = dir;
	}

	public static void save(BackupList list) {
		save(list, listFile);
	}
	public static void save(BackupList list, File file) {
		try(PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
			pw.println(DESTINATION_PREFIX + backupDir.getAbsolutePath());
			pw.println(MAXSNAPSHOTS_PREFIX + maxSnapshots);
			list.forEach(pw::println);
			listFile = file;
		} catch (IOException e) {
			MainFrame.error(e, "Failed to save in : " + file.getAbsolutePath(), "%e%");
		}		
	}
	
	public static void load(BackupList list, File file) {
		list.clear();
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line = br.readLine();
			if(line != null) backupDir = new File(line.substring(DESTINATION_PREFIX.length()));
			line = br.readLine();
			if(line != null) maxSnapshots = Integer.parseInt(line.substring(MAXSNAPSHOTS_PREFIX.length()));
			br.lines().filter(s -> !s.isEmpty()).map(File::new).forEach(list::add); //TODO : what if file name is invalid
			listFile = file;
		} catch (IOException e) {
			MainFrame.error(e, "Failed to load from : " + file.getAbsolutePath(), "%e%");
		}
	}


	public static void backup(BackupList list) {
		long fileNum = getFileNum(list);
		long backupSize = getBackupSize(list);
		if(JOptionPane.showConfirmDialog(null, "Backup %d files(total %s) into %s\nProceed?".formatted(fileNum, formatFileSize(backupSize), backupDir.getAbsolutePath()),
				"Start backup?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;
		
		String timeStamp = dateFormat.format(new Date());
		File destFolderFile = new File(backupDir, timeStamp );
		destFolderFile.mkdirs();
		
		String backupDirStr = destFolderFile.getAbsolutePath();
		List<PathPair> pairList = new LinkedList<>();
		for(int i = 0; i < list.size(); i++) {
			File in = list.get(i);
			try(PrintWriter pw = new PrintWriter(new FileWriter(new File(backupDirStr, (i+1) + ".backupDestination.txt")))){
				pw.println(in.getCanonicalPath());
			} catch (IOException e) {
				MainFrame.error(e, "Failed to write : " + (i+1) + ".backupDestination.txt", "%e%");
				return;
			}
			
			pairList.add(new PathPair(in.toPath(), Paths.get(backupDirStr, Integer.toString(i+1), in.getName())));
		}
		pairList.parallelStream().forEach(pair -> {
			Path source = pair.source;
			Path target = pair.target;
			try {
				if(!Files.exists(target)) Files.createDirectories(target);
				if(Files.isDirectory(source)) {
					copyFolder(source, target);
				} else {
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				MainFrame.error(e, "Failed to copy %s to %s".formatted(source.toAbsolutePath(), target.toAbsolutePath()), "%e%");
				return;
			}
		});
		Worker.work(() -> {
			while(Arrays.stream(backupDir.listFiles()).filter(File::isDirectory).count() > maxSnapshots) {
				try {
					removeDir(new File(backupDir, getOldestBackupDate()));
				} catch (IOException e) {
					MainFrame.error(e, "Failed to remove oldest backup!", "%e%");
				}
			}
			list.runCallback();
		});
		JOptionPane.showMessageDialog(null, "Backup done!\nTimestamp : " + timeStamp, "Backup finished!", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static void removeDir(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
	            Files.delete(dir);
	            return FileVisitResult.CONTINUE;
	        }
	        
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }
	    });
	}
	private static class PathPair {
		Path source;
		Path target;
		public PathPair(Path source, Path target) {
			this.source = source;
			this.target = target;
		}
	}

	private static void copyFolder(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
					throws IOException {
				Files.createDirectories(target.resolve(source.relativize(dir).toString()));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	private static long getBackupSize(BackupList list) {
		return list.stream().mapToLong(f -> {
			try {
				return Files.walk(f.toPath())
						.parallel()
						.filter(p -> !Files.isDirectory(p))
						.mapToLong(p -> {
							try {
								return Files.size(p);
							} catch (IOException e) {
								MainFrame.error(e, "Failed finding size of : " + p.toAbsolutePath().toString(), "%e%");
								return 0;
							}
						})
						.sum();
			} catch (IOException e) {
				MainFrame.error(e, "Failed to calculate backup size", "%e%");
				return 0;
			}
		})
		.sum();
	}

	/**
	 * Numbers of files in list(recursively)
	 * @param list
	 * @return
	 */
	private static long getFileNum(BackupList list) {
		return list.stream().mapToLong(f -> {
			try {
				return Files.walk(f.toPath())
						.parallel()
						.filter(p -> !Files.isDirectory(p))
						.count();
			} catch (IOException e) {
				MainFrame.error(e, "Failed to get numbers of backuped file", "%e%");
				return 0;
			}
		})
		.sum();
	}
	
	private static String formatFileSize(long fileSize) {
		
		if(fileSize < 0) return fileSize + "";
		if(fileSize == 0L) return "0.00byte";
		
		switch ((int)(Math.log(fileSize) / Math.log(1024))) {
		
		case 0:
			return String.format("%d", fileSize) + "byte";
		case 1:
			return String.format("%.2f", fileSize / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", fileSize / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024 * 1024)) + "TB";
		
	}


	public static void restore(BackupList list) {
		String latest = getLatestBackupDate();
		if(latest == null) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(null, backupDir.getAbsolutePath() + " is empty!", "No backups to restore!", JOptionPane.ERROR_MESSAGE);
			});
			return;
		}
		String[] latestDate = latest.split("T");

		if(JOptionPane.showConfirmDialog(null, "Backup date : %s, time : %s\nBackup location : %s.\nProceed?\n\nEffected files :\n".formatted(latestDate[0], latestDate[1].replace("_", ":"), backupDir.getAbsolutePath())
				+ list.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")),
				"Start restore?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) return;
		
		File latestBackupdir = new File(backupDir, latest);
		
		for(int i = 1; ; i++) {
			File info = new File(latestBackupdir, i + ".backupDestination.txt");
			if(!info.exists()) break;
			File dst;
			try(BufferedReader bw = new BufferedReader(new FileReader(info))){
				dst = new File(bw.readLine());
			} catch (IOException e) {
				MainFrame.error(e, "Failed to write to " + i + ".backupDestination.txt", "%e%");
				return;
			}
			
			Path source = Paths.get(latestBackupdir.getAbsolutePath(), Integer.toString(i), dst.getName());
			Path target = dst.toPath();
			try {
				if(!Files.exists(target)) Files.createDirectories(target);
				if(Files.isDirectory(source)) {
					copyFolder(source, target);
				} else {
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				MainFrame.error(e, "Failed to copy %s to %s".formatted(source.toAbsolutePath(), target.toAbsolutePath()), "%e%");
			}
		}
		JOptionPane.showMessageDialog(null, "Restore done!", "Restore finished!", JOptionPane.INFORMATION_MESSAGE);
	}


	private static String getLatestBackupDate() {
		String ret = mf.getRestoreSnapshot();
		if(ret != null) return ret;
		
		Date date = getBackupDates().max(Comparator.comparing(Date::toString)).orElse(null);
		if(date == null) return null;
		else return dateFormat.format(date);
	}
	private static String getOldestBackupDate() {
		Date date = getBackupDates().min(Comparator.comparing(Date::toString)).orElse(null);
		if(date == null) return null;
		return dateFormat.format(date);
	}
	private static Stream<Date> getBackupDates() {
		return getBackupDateString()
				.map(s -> {
					try {
						return dateFormat.parse(s);
					} catch (ParseException e) {
						return null; //not going to happen, because of getBackupDateString()
					}
				})
				.filter(Objects::nonNull);
	}
	public static Stream<String> getBackupDateString() {
		return Arrays.stream(backupDir.listFiles())
				.filter(File::isDirectory)
				.map(File::getName)
				.filter(s -> {
					try {
						dateFormat.parse(s);
						return true;
					} catch (ParseException e) {
						return false;
					}
				});
	}

}
