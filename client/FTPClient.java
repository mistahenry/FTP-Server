package ftp.client;

/**
 * 
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FTPClient {
	private Set<Path> transferSet;
	private Set<Integer> terminateSet;
	private Map<Integer, Path> commandIDMap;
	
	public FTPClient() {
		transferSet = new HashSet<Path>();
		terminateSet = new HashSet<Integer>();
		commandIDMap = new HashMap<Integer, Path>();
	}
	
	public synchronized boolean transfer(Path path) {
		return !transferSet.contains(path);
	}
	
	public synchronized void transferIN(Path path, int commandID) {
		transferSet.add(path);
		commandIDMap.put(commandID, path);
	}
	
	public synchronized void transferOUT(Path path, int commandID) {
		try {
			transferSet.remove(path);
			commandIDMap.remove(commandID);
		} catch(Exception e) {}
	}
	
	public synchronized boolean quit() {
		return transferSet.isEmpty();
	}
	
	public synchronized boolean terminateADD(int commandID) {
		if (commandIDMap.containsKey(commandID)) {
			terminateSet.add(commandID);
			return true;
		} else
			return false;
	}
	
	public synchronized boolean terminateGET(Path path, Path serverPath, int commandID) {
		try {
			if (terminateSet.contains(commandID)) {
				commandIDMap.remove(commandID);
				transferSet.remove(serverPath);
				terminateSet.remove(commandID);
				Files.deleteIfExists(path);
				return true;
			}
		} catch (Exception e) {}
		
		return false;
	}
	
	public synchronized boolean terminatePUT(Path path, int commandID) {
		try {
			if (terminateSet.contains(commandID)) {
				commandIDMap.remove(commandID);
				transferSet.remove(path);
				terminateSet.remove(commandID);
				return true;
			}
		} catch (Exception e) {}
		
		return false;
	}
}
