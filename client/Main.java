package ftp.client;

/**
 * FTP Client Launcher
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Main {
	public static final boolean DEBUG = false;
	public static final String PROMPT = "mytftp>";
	public static final String EXIT_MESSAGE = "FTP session ended. Bye!";
	public static int nPort, tPort;
	public static String hostname;
	
	/**
	 * FTP client launcher which connects to remote host
	 * @param args machineName, nPort, tPort
	 */
	public static void main(String[] args) {
		//number of arguments
		if (args.length != 3) {
			System.out.println("error: Invalid number of arguments");
			return;
		}
		
		//hostname
		try {
			InetAddress.getByName(args[0]);
			hostname = args[0];
		} catch (Exception e) {
			System.out.println("error: hostname does not resolve to an IP address");
			return;
		}
		
		////////////////////////////
		// port range: 1 to 65535 //
		////////////////////////////
		
		//nPort port number
		try {
			nPort = Integer.parseInt(args[1]);
			if (nPort < 1 || nPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid nport number");
			return;
		} catch (Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//tPort port number
		try {
			tPort = Integer.parseInt(args[2]);
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
		
		/////////////////////////
		// FTP Client Launcher //
		/////////////////////////
		
		try {
			//shared memory object
			FTPClient ftpClient = new FTPClient();
			
			//initial starting thread
			(new Thread(new Worker(ftpClient, hostname, nPort))).start();
		} catch (SocketTimeoutException ste) {
			System.out.println("error: host could not be reached");
		} catch (ConnectException ce) {
			System.out.println("error: no running FTP at remote host");
		} catch (Exception e) {
			System.out.println("error: program quit unexpectedly");
		}
	}
}