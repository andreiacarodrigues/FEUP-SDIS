package peer.protocol;

import java.io.File;
import java.net.*;
import java.util.HashMap;

import peer.data.DataManager;
import peer.files.FileManager;
import peer.main.Peer;
import peer.message.ChunkRec;
import peer.message.MessageGenerator;
import peer.network.SenderSocket.Destination;

public class RestoreEnhancement implements Runnable
{
	private String filename;
	private String ipAddress;
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
			e.printStackTrace();
		}
		
		String[] split = address.toString().split("/");
		ipAddress = split[1];
	}
	
	@Override
	public void run() {
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
		
		while(running)
		{
			if(attempts > 3)
			{
				System.out.println("Can't restore chunk");
				return;
			}
			
			byte[] message = MessageGenerator.generateGETCHUNK(fileId, chunkNo, ipAddress, Peer.getMDRPort());
			Peer.getSenderSocket().sendPacket(message, Destination.MC);
			
				
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
		
		if(DM.isEncrypted(filename))
		{
			FileManager.restoreEncryptedFile(f, fileParts);
		}
		else
		{
			FileManager.restoreFile(f, fileParts);
		}
	}
}
