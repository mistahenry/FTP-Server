package ftp.server;

/**
 * 
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NormalWorker implements Runnable {
	private FTPServer ftpServer;
	private Socket nSocket;
	private Path path;
	private List<String> tokens;
	
	
	//Input
	InputStreamReader iStream;
	BufferedReader reader;
	//Data
	DataInputStream byteStream;
	//Output
	OutputStream oStream;
	DataOutputStream dStream;
	
	
	public NormalWorker(FTPServer ftpServer, Socket nSocket) throws Exception {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
		path = Paths.get(System.getProperty("user.dir"));
		
		//streams
		iStream = new InputStreamReader(nSocket.getInputStream());
		reader = new BufferedReader(iStream);
		byteStream = new DataInputStream(nSocket.getInputStream());
		oStream = nSocket.getOutputStream();
		dStream = new DataOutputStream(oStream);
	}
	
	public void get() throws Exception {
		//not a directory or file
		if (Files.notExists(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": No such file or directory" + "\n");
			return;
		} 
		//is a directory
		if (Files.isDirectory(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": Is a directory" + "\n");
			return;
		}
		
		//////////
		// LOCK //
		//////////
		int lockID = ftpServer.getIN(path.resolve(tokens.get(1)));
		if (Main.DEBUG) System.out.println(lockID);
		if (lockID == -1) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": No such file or directory" + "\n");
			return;
		}
		
		//blank message
		dStream.writeBytes("\n");
		
		//send terminateID
		dStream.writeBytes(lockID + "\n");
		
		//need to figure
		Thread.sleep(100);
		
		if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}
		
		//transfer file
		byte[] buffer = new byte[1000];
		try {
			File file = new File(path.resolve(tokens.get(1)).toString());
			
			//write long filesize as first 8 bytes
			long fileSize = file.length();
			byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
			dStream.write(fileSizeBytes, 0, 8);
			
			if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
				quit();
				return;
			}
			
			//write file
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			int count = 0;
			while((count = in.read(buffer)) > 0) {
				if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
					in.close();
					quit();
					return;
				}
				dStream.write(buffer, 0, count);
			}
			
			in.close();
		} catch(Exception e) {
			if (Main.DEBUG) System.out.println("transfer error: " + tokens.get(1));
		}
		
		////////////
		// UNLOCK //
		////////////
		ftpServer.getOUT(path.resolve(tokens.get(1)), lockID);
	}
	
	public void put() throws Exception {
		//LOCK ID
		int lockID = ftpServer.putIN_ID(path.resolve(tokens.get(1)));
		if (Main.DEBUG) System.out.println(lockID);
		
		//send message ID
		dStream.writeBytes(lockID + "\n");
		
		
		//////////
		// LOCK //
		//////////
		while (!ftpServer.putIN(path.resolve(tokens.get(1)), lockID))
			Thread.sleep(10);
		
		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}
		
		//can write
		dStream.writeBytes("\n");
		
		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}
		
		//get file size
		byte[] fileSizeBuffer = new byte[8];
		byteStream.read(fileSizeBuffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
		DataInputStream dis = new DataInputStream(bais);
		long fileSize = dis.readLong();
		
		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}
		
		//receive the file
		FileOutputStream f = new FileOutputStream(new File(tokens.get(1)).toString());
		int count = 0;
		byte[] buffer = new byte[1000];
		long bytesReceived = 0;
		while(bytesReceived < fileSize) {
			if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
				f.close();
				quit();
				return;
			}
			count = byteStream.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		}
		f.close();
		
		////////////
		// UNLOCK //
		////////////
		ftpServer.putOUT(path.resolve(tokens.get(1)), lockID);
	}
	
	public void delete() throws Exception {
		if (!ftpServer.delete(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("delete: cannot remove '" + tokens.get(1) + "': The file is locked" + "\n");
			dStream.writeBytes("\n");
			return;
		}
		
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
	}
	
	public void ls() throws Exception {
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
			for (Path entry: dirStream)
				dStream.writeBytes(entry.getFileName() + "\n");
			dStream.writeBytes("\n");
		} catch(Exception e) {
			dStream.writeBytes("ls: failed to retrive contents" + "\n");
			dStream.writeBytes("\n");
		}
	}
	
	public void cd() throws Exception {
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
	}
	
	public void mkdir() throws Exception {
		try {
			Files.createDirectory(path.resolve(tokens.get(1)));
			dStream.writeBytes("\n");
		} catch(FileAlreadyExistsException falee) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': File or folder exists" + "\n");
		} catch(Exception e) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': Permission denied" + "\n");
		}
	}
	
	public void pwd() throws Exception {
		//send path
		dStream.writeBytes(path + "\n");
	}
	
	public void quit() throws Exception {
		//close socket
		nSocket.close();
		throw new Exception();
	}
	
	public void run() {
		System.out.println(Thread.currentThread().getName() + " NormalWorker Started");
		exitThread:
		while (true) {
			try {
				//check every 10 ms for input
				while (!reader.ready())
					Thread.sleep(10);
				
				//capture and parse input
				tokens = new ArrayList<String>();
				String command = reader.readLine();
				Scanner tokenize = new Scanner(command);
				//gets command
				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				//gets rest of string after the command; this allows filenames with spaces: 'file1 test.txt'
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				if (Main.DEBUG) System.out.println(tokens.toString());
				
				//command selector
				switch(tokens.get(0)) {
					case "get": 	get();		break;
					case "put": 	put();		break;
					case "delete": 	delete();	break;
					case "ls": 		ls();		break;
					case "cd": 		cd();		break;
					case "mkdir": 	mkdir();	break;
					case "pwd": 	pwd();		break;
					case "quit": 	quit();		break exitThread;
					default:
						System.out.println("invalid command");
				}
			} catch (Exception e) {
				break exitThread;
			}
		}
		System.out.println(Thread.currentThread().getName() + " NormalWorker Exited");
	}
}