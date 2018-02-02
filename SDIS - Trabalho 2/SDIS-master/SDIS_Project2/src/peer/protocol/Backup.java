package peer.protocol;

import java.util.ArrayList;

import peer.data.DataManager;
import peer.files.FileSplitter;
import peer.main.Peer;
import peer.message.Chunk;
import peer.message.Stored;

public class Backup implements Runnable
{
	private FileSplitter FS;
	private int desiredReplicationDegree;
	private boolean encrypt;
	
	public Backup(String filename, int replicationDegree, boolean encrypt)
	{
		FS = new FileSplitter(filename, replicationDegree, encrypt);
		this.desiredReplicationDegree = replicationDegree;
		this.encrypt = encrypt;
	}

	@Override
	public void run()
	{
		DataManager DM = Peer.getDataManager();
		boolean success = DM.addBackedUpData(FS.getFilename(), FS.getFileID(), desiredReplicationDegree, encrypt);
		
		if(!success)
		{
			System.out.println("File already backed up");
			return;
		}
		Stored.resetFile(FS.getFileID());
		
		ArrayList<Chunk> chunks = FS.getChunkList();
		
		for(int i = 0; i < chunks.size(); i++)
		{
			Thread thread = new Thread(new BackupChunk(chunks.get(i)));
			thread.start();
		}
	}
}
