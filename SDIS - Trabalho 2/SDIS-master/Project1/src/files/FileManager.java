package files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import data.DataManager;
import peer.Peer;
import received.Stored;

public class FileManager
{
	public static void initFileManager()
	{	
		File peerFolder = new File("../Peer" + Peer.getServerId());
		File disk = new File("../Peer" + Peer.getServerId() + "/Files");
		File storedChunks = new File("../Peer" + Peer.getServerId() + "/Chunks");
		
		createDir(peerFolder);
		createDir(disk);
		createDir(storedChunks);
	}
	
	public static void createDir(File f)
	{
		// if the directory does not exist, create it
		if (!f.exists())
		{
		    System.out.println("Creating directory: " + f.getName());
		    boolean result = false;

		    try
		    {
		        f.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se)
		    {
		        //handle it
		    }        
		    if(result)
		    {    
		        System.out.println("DIR created");  
		    }
		}
	}
	
	public static boolean storeChunk(String fileId, int chunkNo, byte[] body, Integer replicationDegree)
	{		
		DataManager DM = Peer.getDataManager();
		String filename = "../Peer" + Peer.getServerId() + "/" + "Chunks/" + fileId + "-" + chunkNo;
		
		File f = new File(filename);
		if(f.exists())
		{
			return true;
		}
		
		if(Peer.getProtocolVersion().equals("2.0"))
		{
			ArrayList<Integer> peers = Stored.getPeers(fileId, chunkNo);
			int peerCount = 0;
			if(peers != null)
			{
				System.out.println("Peers was null");
				peerCount = peers.size();
			}
			
			System.out.println("PEER COUNT AT: " + peerCount);
			System.out.println("REPLICATION DEGREE IS: " + replicationDegree);
			
			if(peerCount >= replicationDegree)
			{
				System.out.println("Already reached desired replication degree");
				return false;
			}
		}
		
		if(Peer.getDiskSpaceBytes() <= FileManager.getChunksSize() + body.length)
		{
			System.out.println("Not enough space to store chunk");
			return false;
		}
		
		try
		{
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(body);
			fos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		DM.addStoredFilesData(fileId, chunkNo, replicationDegree, body.length);
		
		return true;
	}
	
	public static void deleteChunk(String fileID, Integer chunkNo)
	{
		String path = "../Peer" + Peer.getServerId() + "/Chunks/" + fileID + "-"+ String.valueOf(chunkNo);
		File f = new File(path);
		f.delete();
	}
	
	public static void restoreFile(File f, HashMap<Integer, byte[]> fileParts)
	{
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fileParts.forEach( (k, v) -> {
				try {
					fos.write(v);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static byte[] getChunk(String fileId, int chunkNo)
	{
		String filename = "../Peer" + Peer.getServerId() + "/" + "Chunks/" + fileId + "-" + chunkNo;
		
		File f = new File(filename);
		if(f.exists())
		{
			try
			{
				BufferedInputStream bufinst = new BufferedInputStream (new FileInputStream(f));
				
				byte[] buffer = new byte[FileSplitter.chunkSize];
				int tmp = bufinst.read(buffer);
				
				byte[] body = new byte[tmp];
				System.arraycopy(buffer, 0, body, 0, tmp);
				
				bufinst.close();
				return body;
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static long getChunksSize()
	{
		String path = "../Peer" + Peer.getServerId() + "/" + "Chunks/";
		File directory = new File(path);
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += getChunksSize();
		}
		return length;
	}
}
