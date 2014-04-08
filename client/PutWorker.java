package ftp.client;

/**
 * 
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PutWorker implements Runnable {
	private FTPClient ftpClient;
	private Socket socket;
	private Path path, serverPath;
	private List<String> tokens;
	private int terminateID;
	
	//Stream
	private InputStreamReader iStream;
	private BufferedReader reader;
	private OutputStream oStream;
	private DataOutputStream dStream;
	
	
	public PutWorker(FTPClient ftpClient, String hostname, int nPort, List<String> tokens, Path serverPath) throws Exception {
		this.ftpClient = ftpClient;
		this.tokens = tokens;
		this.serverPath = serverPath;
		
		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), nPort), 1000);
		
		iStream = new InputStreamReader(socket.getInputStream());
		reader = new BufferedReader(iStream);
		oStream = socket.getOutputStream();
		dStream = new DataOutputStream(oStream);
		
		path = Paths.get(System.getProperty("user.dir"));
	}
	
	public void put() throws Exception {
		//same transfer
		if (!ftpClient.transfer(serverPath.resolve(tokens.get(1)))) {
			System.out.println("error: file already transfering");
			return;
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
			dStream.writeBytes("put " + serverPath.resolve(tokens.get(1)) + "\n");
			
			//wait for terminate ID
			try {
				terminateID = Integer.parseInt(reader.readLine());
			} catch(Exception e) {
				if (Main.DEBUG) System.out.println("Invalid TerminateID");
			}
			System.out.println("TerminateID: " + terminateID);
			
			//CLIENT side locking
			ftpClient.transferIN(serverPath.resolve(tokens.get(1)), terminateID);
			
			if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;
			
			//signal to start writing
			reader.readLine();
			
			//need to figure
			Thread.sleep(100);
			
			if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;
			
			byte[] buffer = new byte[1000];
			try {
				File file = new File(path.resolve(tokens.get(1)).toString());
				
				//write long filesize as first 8 bytes
				long fileSize = file.length();
				byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
				dStream.write(fileSizeBytes, 0, 8);
				
				if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;
				
				//write file
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count = 0;
				while((count = in.read(buffer)) > 0) {
					if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) {
						in.close();
						return;
					}
					dStream.write(buffer, 0, count);
				}
				
				in.close();
			} catch(Exception e){
				if (Main.DEBUG) System.out.println("transfer error: " + tokens.get(1));
			}
			
			//CLIENT side un-locking
			ftpClient.transferOUT(serverPath.resolve(tokens.get(1)), terminateID);
		}
	}
	
	public void run() {
		try {
			put();
			Thread.sleep(100);
			dStream.writeBytes("quit" + "\n");
		} catch (Exception e) {
			if (Main.DEBUG) System.out.println("PutWorker error");
		}
	}
}
