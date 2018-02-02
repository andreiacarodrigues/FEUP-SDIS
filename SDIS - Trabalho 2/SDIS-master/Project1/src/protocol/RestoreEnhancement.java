package protocol;

import java.io.File;
import java.net.*;
import java.util.HashMap;

import data.DataManager;
import files.FileManager;
import message.MessageGenerator;
import peer.Peer;
import received.ChunkRec;
import socket.PrivateListener;

public class RestoreEnhancement implements Runnable
{
	private String filename;
	private static DatagramSocket socket;
	private String ipAddress;
	public final static int port = 7890;
	private HashMap<Integer, byte[]> fileParts;
	
	public RestoreEnhancement(String filename)
	{
		this.filename = filename;
		fileParts = new HashMap<Integer, byte[]>();
		System.out.println("Rest enh");
		startSocket();
	}
	
	private void startSocket()
	{
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] split = address.toString().split("/");
		ipAddress = split[1];
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		startSocket();
		
		DataManager DM = Peer.getDataManager();
		String fileId = DM.getFileId(filename);
		if(fileId == null)
		{
			System.out.println("No such file backed up");
			return;
		}
		
		File f = new File(filename);
		if(f.exists())
		{
			System.out.println("Can't restore a file that is already in the files folder");
			return;
		}
		
		boolean running = true;
		int chunkNo = 0;
		int attempts = 0;
		
		Thread t = new Thread(new PrivateListener(port));
		t.start();
		
		while(running)
		{
			if(attempts > 3)
			{
				System.out.println("Can't restore chunk");
				return;
			}
			
			byte[] message = MessageGenerator.generateGETCHUNK(fileId, chunkNo, ipAddress, port);
			Peer.getMC().sendPacket(message);
			
				
			long startTime = System.nanoTime();
			boolean pooling = true;
			
			boolean found = false;
			while(pooling)
			{
				byte[] received = ChunkRec.getMessage(fileId, chunkNo);
				if(received != null)
				{
					fileParts.put(chunkNo, received);
					chunkNo++;
					attempts = 0;
					
					System.out.println(received.length);
					
					if(received.length < 64000)
						running = false;
					
					found = true;
					pooling = false;
				}
				
				if(((double)System.nanoTime()- startTime)/ 1000000 > 500)
				{
					pooling = false;
				}
			}
			
			if(!found)
				attempts++;
		}
		
		System.out.println(fileParts.size());
		
		ChunkRec.resetFile(fileId);
		FileManager.restoreFile(f, fileParts);
	}

}
