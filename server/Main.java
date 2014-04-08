package ftp.server;

/**
 * FTP Server Launcher
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.net.BindException;
import java.net.ServerSocket;

public class Main {
	public static final boolean DEBUG = false;
	private static ServerSocket nSocket, tSocket;
	
	/**
	 * Repeatedly listens for incoming messages on TCP port normal commands (nport) and terminate port (tport)
	 * @param args nport, tport
	 */
	public static void main(String[] args) {
		//number of arguments
		if (args.length != 2) {
			System.out.println("error: Invalid number of arguments");
			return;
		}
		
		////////////////////////////
		// port range: 1 to 65535 //
		////////////////////////////
		
		//nPort port number
		int nPort = 0;
		try {
			nPort = Integer.parseInt(args[0]);
			if (nPort < 1 || nPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid nport number");
			return;
		} catch (Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//tPort port number
		int tPort = 0;
		try {
			tPort = Integer.parseInt(args[1]);
			if (tPort < 1 || tPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid tport number");
			return;
		} catch(Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//port numbers must be different
		if (nPort == tPort) {
			System.out.println("error: nPort and tPort must be port numbers");
			return;
		}
		
		//listening sockets
		try {
			nSocket = new ServerSocket(nPort);
			tSocket = new ServerSocket(tPort);
		} catch(BindException be) {
			System.out.println("error: one or more ports are already in use");
			return;
		} catch(Exception e) {
			System.out.println("error: server could not be started");
			return;
		}
		
		/////////////////////////
		// FTP Server Launcher //
		/////////////////////////
		
		try {
			//shared memory object
			FTPServer ftpServer = new FTPServer();
			
			//two threads, one for each socket
			(new Thread(new NormalDaemon(ftpServer, nSocket))).start();
			(new Thread(new TerminateDaemon(ftpServer, tSocket))).start();
		} catch (Exception e) {
			System.out.println("ftp.server.Main");
			e.printStackTrace(); //TODO
		}
	}
}