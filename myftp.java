package ftp;

/**
 * FTP Client
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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class myftp {
	private final boolean DEBUG = false;
	private static final String PROMPT = "mytftp>";
	private static final String EXIT_MESSAGE = "FTP session ended. Bye!";
	private Socket controlSocket;
	private Path path;
	
	public myftp(String hostname, int port) throws Exception {
		InetAddress ip = InetAddress.getByName(hostname);
		controlSocket = new Socket();
		controlSocket.connect(new InetSocketAddress(ip.getHostAddress(), port), 1000);
		
		path = Paths.get(System.getProperty("user.dir"));
		System.out.println("Connected to: " + ip);
	}
	
	public myftp() {
		
	}
	
	public void input() {
		try {
			//Input
			InputStreamReader iStream = new InputStreamReader(controlSocket.getInputStream());
			BufferedReader reader = new BufferedReader(iStream);
			
			//Data
			DataInputStream byteStream = new DataInputStream(controlSocket.getInputStream());
			
			//Output
			OutputStream oStream = controlSocket.getOutputStream();
			DataOutputStream dStream = new DataOutputStream(oStream);
			
			//keyboard input
			Scanner input = new Scanner(System.in);
			String command;
			
			do {
				//get input
				System.out.print(PROMPT);
				command = input.nextLine();
				command = command.trim();
				
				//parse input into tokens
				List<String> tokens = new ArrayList<String>();
				Scanner tokenize = new Scanner(command);
				//gets command
				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				//gets rest of string after the command; this allows filenames with spaces: 'file1 test.txt'
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				if (DEBUG) System.out.println(tokens);
				
				//allows for blank enter
				if (tokens.isEmpty())
					continue;
				
				//command selector
				switch(tokens.get(0)) {
					case "get":
						if (DEBUG) System.out.println("-get");
						if (tokens.size() != 2) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("get " + tokens.get(1) + "\n");
						
						//error messages
						String get_line;
						if (!(get_line = reader.readLine()).equals("")) {
							System.out.println(get_line);
							continue;
						}
						
						//get file size
						long fileSize = Long.parseLong(reader.readLine());
						FileOutputStream f = new FileOutputStream(new File(tokens.get(1)));
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
					case "put":
						if (DEBUG) System.out.println("-put");
						if (tokens.size() != 2) {
							invalid();
							continue;
						}
						
						//not a directory or file
						if (Files.notExists(path.resolve(tokens.get(1)))) {
							System.out.println("put: " + tokens.get(1) + ": No such file or directory");
						} 
						//is a directory
						else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
							System.out.println("put: " + tokens.get(1) + ": Is a directory");
						}
						//transfer file
						else {
							//send command
							dStream.writeBytes("put " + tokens.get(1) + "\n");
							
							File file = new File(path.resolve(tokens.get(1)).toString());
							long fileSize1 = file.length();
							
							//send file size
							dStream.writeBytes(fileSize1 + "\n");
							
							//need to figure
							Thread.sleep(100);
							
							byte[] buffer1 = new byte[8192];
							try {
								BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
								int count2 = 0;
								while((count2 = in.read(buffer1)) > 0)
									dStream.write(buffer1, 0, count2);
								
								in.close();
							} catch(Exception e){
								System.out.println("transfer error: " + tokens.get(1));
							}
						}
						
						break;
					case "delete": //complete
						if (DEBUG) System.out.println("-delete");
						if (tokens.size() != 2) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("delete " + tokens.get(1) + "\n");
						
						//messages
						String delete_line;
						while (!(delete_line = reader.readLine()).equals(""))
						    System.out.println(delete_line);
						
						break;
					case "ls": //complete
						if (DEBUG) System.out.println("-ls");
						if (tokens.size() != 1) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("ls" + "\n");
						
						//messages
						String ls_line;
						while (!(ls_line = reader.readLine()).equals(""))
						    System.out.println(ls_line);
						
						break;
					case "cd": //complete
						if (DEBUG) System.out.println("-cd");
						if (tokens.size() > 2) {
							invalid();
							continue;
						}
						
						//send command
						if (tokens.size() == 1) //allow "cd" goes back to home directory
							dStream.writeBytes("cd" + "\n");
						else
							dStream.writeBytes("cd " + tokens.get(1) + "\n");
						
						//messages
						String cd_line;
						if (!(cd_line = reader.readLine()).equals(""))
							System.out.println(cd_line);
						
						break;
					case "mkdir": //complete
						if (DEBUG) System.out.println("-mkdir");
						if (tokens.size() != 2) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("mkdir " + tokens.get(1) + "\n");
						
						//messages
						String mkdir_line;
						if (!(mkdir_line = reader.readLine()).equals(""))
							System.out.println(mkdir_line);
						
						break;
					case "pwd": //complete
						if (DEBUG) System.out.println("-pwd");
						if (tokens.size() != 1) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("pwd" + "\n");
						
						//message
						System.out.println(reader.readLine());
				        
						break;
					case "quit": //complete
						if (DEBUG) System.out.println("-quit");
						if (tokens.size() != 1) {
							invalid();
							continue;
						}
						
						//send command
						dStream.writeBytes("quit" + "\n");
						
						break;
					case "help": //complete
						if (DEBUG) System.out.println("-help");
						
						//display help
						help();
						
						break;
					default:
						if (DEBUG) System.out.println("-error");
						
						System.out.println("unrecognized command '" + tokens.get(0) + "'");
						System.out.println("Try `help' for more information.");
						
						break;
				}
			} while (!command.equalsIgnoreCase("quit"));
			input.close();
			
			
			System.out.println(EXIT_MESSAGE);
		} catch(Exception e) {
			System.out.println("Error: disconnected from host");
		}
	}
	
	public void invalid() {
		System.out.println("Invalid Arguments");
		System.out.println("Try `help' for more information.");
	}
	
	public void help() {
		System.out.println("Available commands:");
		System.out.println(" get (get <remote_filename>) \t\t  – Copy file with the name <remote_filename> from remote directory to local directory.");
		System.out.println(" put (put <local_filename>) \t\t  – Copy file with the name <local_filename> from local directory to remote directory.");
		System.out.println(" delete (delete <remote_filename>) \t  – Delete the file with the name <remote_filename> from the remote directory.");
		System.out.println(" ls (ls) \t\t\t\t  – List the files and subdirectories in the remote directory.");
		System.out.println(" cd (cd <remote_direcotry_name> or cd ..) – Change to the <remote_direcotry_name> on the remote machine or change to the parent directory of the current directory.");
		System.out.println(" mkdir (mkdir <remote_directory_name>) \t  – Create directory named <remote_direcotry_name> as the sub-directory of the current working directory on the remote machine.");
		System.out.println(" pwd (pwd) \t\t\t\t  – Print the current working directory on the remote machine.");
		System.out.println(" quit (quit) \t\t\t\t  – End the FTP session.");
	}
	
	/**
	 * Starts up FTP Client and connects
	 * @param args machine name & port number
	 */
	public static void main(String[] args) {
		//num of args
		if (args.length != 2) {
			System.out.println("Error: Invalid number of arguments");
			System.exit(1);
		}
		
		//port number
		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch(Exception e) {
			System.out.println("Error: Invalid port number");
			System.exit(1);
		}
		
		//hostname
		try {
			InetAddress.getByName(args[0]);
		} catch (Exception f) {
			System.out.println("Error: hostname does not resolve to an IP address");
			System.exit(1);
		}
		
		//server
		try {
			myftp instance = new myftp(args[0], port);
			instance.input();
		} catch(SocketTimeoutException ste) {
			System.out.println("Error: host could not be reached");
		} catch(ConnectException ce) {
			System.out.println("Error: no running FTP at remote host");
		} catch(Exception h) {
			System.out.println("Error: program quit unexpectedly");
		}
	}
}
