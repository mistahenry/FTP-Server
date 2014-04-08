package ftp;

/**
 * FTP Server
 * @author henryw14
 * @author vincentlee
 * @version 2.0
 * @since February 3, 2014
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class myftpserver implements Runnable {
	private final boolean DEBUG = false;
	private Path path;
	private Socket controlSocket;
	
	public myftpserver(Socket controlSocket) {
		this.controlSocket = controlSocket;
		path = Paths.get(System.getProperty("user.dir"));
	}
	
	public void run() {
		try {
			//threading messages
			if (DEBUG) System.out.print("<id:" + Thread.currentThread().getId() + " of " + Thread.activeCount());
			if (DEBUG) System.out.println("  " + controlSocket.getRemoteSocketAddress().toString().substring(1) + ">");
			
			//Input
			InputStreamReader iStream = new InputStreamReader(controlSocket.getInputStream());
			BufferedReader reader = new BufferedReader(iStream);
			
			//Data
			DataInputStream byteStream = new DataInputStream(controlSocket.getInputStream());
			
			//Output
			OutputStream oStream = controlSocket.getOutputStream();
			DataOutputStream dStream = new DataOutputStream(oStream);
			
			//main loop
			exitThread:
			while (true) {
				try {
					//check every 10 ms for input
					while (!reader.ready())
						Thread.sleep(10);
					
					//capture and parse input
					List<String> tokens = new ArrayList<String>();
					String command = reader.readLine();
					Scanner tokenize = new Scanner(command);
					//gets command
					if (tokenize.hasNext())
					    tokens.add(tokenize.next());
					//gets rest of string after the command; this allows filenames with spaces: 'file1 test.txt'
					if (tokenize.hasNext())
						tokens.add(command.substring(tokens.get(0).length()).trim());
					tokenize.close();
					if (DEBUG) System.out.println(tokens.toString());
					
					//command selector
					switch(tokens.get(0)) {
						case "get": //sorta works, fix put, reverse of this. gives error stream doesn't close
							if (DEBUG) System.out.println("-get");
							
							//not a directory or file
							if (Files.notExists(path.resolve(tokens.get(1)))) {
								dStream.writeBytes("get: " + tokens.get(1) + ": No such file or directory" + "\n");
							} 
							//is a directory
							else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
								dStream.writeBytes("get: " + tokens.get(1) + ": Is a directory" + "\n");
							} 
							//transfer file
							else {
								//blank message
								dStream.writeBytes("\n");
								
								File file = new File(path.resolve(tokens.get(1)).toString());
								long fileSize = file.length();
								
								//send file size
								dStream.writeBytes(fileSize + "\n");
								
								//need to figure
								Thread.sleep(100);
								
								byte[] buffer = new byte[8192];
								try {
									BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
									int count = 0;
									while((count = in.read(buffer)) > 0)
										dStream.write(buffer, 0, count);
									
									in.close();
								} catch(Exception e) {
									System.out.println("transfer error: " + tokens.get(1));
								}
							}
							
							break;
						case "put":
							if (DEBUG) System.out.println("-put");
							
							//get file size
							long fileSize = Long.parseLong(reader.readLine());
							FileOutputStream f = new FileOutputStream(new File(path.resolve(tokens.get(1)).toString()));
							int count = 0;
							byte[] buffer = new byte[8192];
							long bytesReceived = 0;
							while(bytesReceived < fileSize) {
								count = byteStream.read(buffer);
								f.write(buffer, 0, count);
								bytesReceived += count;
							}
							f.close();
							
							break;
						case "delete": //complete
							if (DEBUG) System.out.println("-delete");
							try {
								boolean confirm = Files.deleteIfExists(path.resolve(tokens.get(1)));
								if (!confirm) {
									dStream.writeBytes("delete: cannot remove '" + tokens.get(1) + "': No such file" + "\n");
									dStream.writeBytes("\n");
								} else
									dStream.writeBytes("\n");
							} catch(DirectoryNotEmptyException enee) {
								dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "': Directory not empty" + "\n");
								dStream.writeBytes("\n");
							} catch(Exception e) {
								dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "'" + "\n");
								dStream.writeBytes("\n");
							}
							
							break;
						case "ls": //list in alphabetical order?
							if (DEBUG) System.out.println("-ls");
							try {
								DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
								for (Path entry: dirStream)
									dStream.writeBytes(entry.getFileName() + "\n");
								dStream.writeBytes("\n");
							} catch(Exception e) {
								dStream.writeBytes("ls: failed to retrive contents" + "\n");
								dStream.writeBytes("\n");
							}						
	
							break;
						case "cd": //complete
							if (DEBUG) System.out.println("-cd");
							try {
								//cd
								if (tokens.size() == 1) {
									path = Paths.get(System.getProperty("user.dir"));
									dStream.writeBytes("\n");
								}
								//cd ..
								else if (tokens.get(1).equals("..")) {
									if (path.getParent() != null)
										path = path.getParent();
									
									dStream.writeBytes("\n");
								}
								//cd somedirectory
								else {
									//not a directory or file
									if (Files.notExists(path.resolve(tokens.get(1)))) {
										dStream.writeBytes("cd: " + tokens.get(1) + ": No such file or directory" + "\n");
									} 
									//is a directory
									else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
										path = path.resolve(tokens.get(1));
										dStream.writeBytes("\n");
									}
									//is a file
									else {
										dStream.writeBytes("cd: " + tokens.get(1) + ": Not a directory" + "\n");
									}
								}
							} catch (Exception e) {
								dStream.writeBytes("cd: " + tokens.get(1) + ": Error" + "\n");
							}
							
							break;
						case "mkdir": //complete
							if (DEBUG) System.out.println("-mkdir");
							try {
								Files.createDirectory(path.resolve(tokens.get(1)));
								dStream.writeBytes("\n");
							} catch(FileAlreadyExistsException falee) {
								dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': File or folder exists" + "\n");
							} catch(Exception e) {
								dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': Permission denied" + "\n");
							}
							
							break;
						case "pwd": //complete
							if (DEBUG) System.out.println("-pwd");
							
							//send path
							dStream.writeBytes(path + "\n");
					        
							break;
						case "quit": //complete
							if (DEBUG) System.out.println("-quit");
							
							//close socket
							controlSocket.close();
							
							//exit while loop
							break exitThread;
						default:
							if (DEBUG) System.out.println("-error");
							break;
					}
				} catch (Exception e) {
					if (DEBUG) System.out.println("loop error");
					if (DEBUG) System.out.println(Thread.activeCount());
					break;
				}
			}
			
			//thread messages exit
			if (DEBUG) System.out.print("<id:" + Thread.currentThread().getId() + " exit");
			if (DEBUG) System.out.println("  " + controlSocket.getRemoteSocketAddress().toString().substring(1) + ">");
			
		} catch(Exception e) {
			if (DEBUG) System.out.println("run error");
		}
	}
	
	/**
	 * Repeatedly listens for incoming messages on TCP port CONTROL_PORT and responds. 
	 * @param args port number
	 */
	public static void main(String[] args) {
		//num of args
		if (args.length != 1) {
			System.out.println("Error: Invalid number of arguments");
			System.exit(1);
		}
		
		//port number
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch(Exception e) {
			System.out.println("Error: Invalid port number");
			System.exit(1);
		}
		
		//open a socket on a port number
		try {
			ServerSocket socketControl = new ServerSocket(port);
			
			while (true) {
				//blocks until client connects; starts connection on new thread
				(new Thread(new myftpserver(socketControl.accept()))).start();
			}
			
		} catch(BindException be) {
			System.out.println("Error: Port already in use");
		} catch(Exception e) {
			System.out.println("Error: server could not be started");
		}
	}
}