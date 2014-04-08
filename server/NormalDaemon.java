package ftp.server;

/**
 * 
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.net.ServerSocket;

public class NormalDaemon implements Runnable {
	private FTPServer ftpServer;
	private ServerSocket nSocket;
	
	public NormalDaemon(FTPServer ftpServer, ServerSocket nSocket) {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
	}
	
	public void run() {
		System.out.println(Thread.currentThread().getName() + " NormalDaemon Started");
		while (true) {
			try {
				(new Thread(new NormalWorker(ftpServer, nSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " NormalDaemon failed to start NormalWorker");
			}
		}
	}
}