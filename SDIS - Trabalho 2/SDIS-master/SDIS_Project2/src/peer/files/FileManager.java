package peer.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import peer.data.DataManager;
import peer.main.*;
import peer.message.Encryptor;
import peer.message.Stored;

public class FileManager
{
	public static void initFileManager()
	{	
		File peerFolder = new File("../Peer" + Peer.getPeerID());
		File disk = new File("../Peer" + Peer.getPeerID() + "/Files");
		File storedChunks = new File("../Peer" + Peer.getPeerID() + "/Chunks");
		
		createDir(peerFolder);
		createDir(disk);
		createDir(storedChunks);

		removeTempFiles();
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
		String filename = "../Peer" + Peer.getPeerID() + "/" + "Chunks/" + fileId + "-" + chunkNo;
		
		File f = new File(filename);
		if(f.exists())
		{
			return true;
		}
		

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
		String path = "../Peer" + Peer.getPeerID() + "/Chunks/" + fileID + "-"+ String.valueOf(chunkNo);
		File f = new File(path);
		f.delete();
	}
	
	public static void restoreFile(File f, HashMap<Integer, byte[]> fileParts)
	{
		String fileName = f.getName();
		String[] splitName = fileName.split("\\.");
		String tempName = splitName[0];
		tempName = "../Peer" + Peer.getPeerID() + "/Files/" + tempName + ".tmp";
		File tempFile = new File(tempName);

		try {
			FileOutputStream fos = new FileOutputStream(tempFile);
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

		tempFile.renameTo(f);
	}
	
	public static void restoreEncryptedFile(File f, HashMap<Integer, byte[]> fileParts)
	{
		String fileName = f.getName();
		String[] splitName = fileName.split("\\.");
		String tempName = splitName[0];
		tempName = "../Peer" + Peer.getPeerID() + "/Files/" + tempName + ".tmp";
		File tempFile = new File(tempName);

		try {
			FileOutputStream fos = new FileOutputStream(tempFile);
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

		int tempSize =  (int) tempFile.length();
		byte[] encBytes = new byte[tempSize];
		try{
			BufferedInputStream bufinst = new BufferedInputStream(new FileInputStream(tempFile));
			bufinst.read(encBytes);
			System.out.println("\n\n prev size " + tempSize );

			byte[] fileBytes = Encryptor.base64decodeAndDecryptBytes(encBytes);
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(fileBytes);


			System.out.println("\n\n new size " + fileBytes.length);
			bufinst.close();
			fos.close();

		} catch (Exception e){
			e.printStackTrace();
		}


		tempFile.renameTo(f);
	}
	
	public static byte[] getChunk(String fileId, int chunkNo)
	{
		String filename = "../Peer" + Peer.getPeerID() + "/" + "Chunks/" + fileId + "-" + chunkNo;
		
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
		String path = "../Peer" + Peer.getPeerID() + "/" + "Chunks/";
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

	public static void removeTempFiles(){
		String path = "../Peer" + Peer.getPeerID() + "/" + "Files/";
		File directory = new File(path);

		for (File file : directory.listFiles()) {
			String name = file.getName();
			if(name.contains(".tmp"))
				file.delete();
		}
	}
}
