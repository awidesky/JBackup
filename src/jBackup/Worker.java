package jBackup;

public class Worker {

	//TODO : use ExecutorService later
	public static void work(Runnable r) {
		new Thread(r).start();
	}
}
