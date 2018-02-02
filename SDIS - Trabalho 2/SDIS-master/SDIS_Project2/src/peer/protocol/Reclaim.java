package peer.protocol;

import java.util.ArrayList;
import java.util.PriorityQueue;

import peer.data.StoredData;
import peer.files.FileManager;
import peer.main.Peer;
import peer.message.MessageGenerator;
import peer.network.SenderSocket.Destination;

public class Reclaim implements Runnable
{
	private PriorityQueue<StoredData> storedChunks;
	private long desiredSpace;
	
	public Reclaim(long desiredSpace, ArrayList<StoredData> storedChunks)
	{
		this.desiredSpace = desiredSpace;
		this.storedChunks = new PriorityQueue<StoredData>();
		
		
		for(int i = 0; i < storedChunks.size(); i++)
			this.storedChunks.add(storedChunks.get(i));
		
	/*	PriorityQueue<StoredData> copy = this.storedChunks;
		while(!copy.isEmpty()){
			StoredData act = copy.remove();
			System.out.println(act.getPeers().size() - act.getDesiredReplicationDegree() + "   -   " + act.getSize());
		}
		*/
	}
	
	public ArrayList<StoredData> freeSpace()
	{
		int freedSpace = 0;
		ArrayList<StoredData> toRemove = new ArrayList<StoredData>();
		
		while(freedSpace <= desiredSpace)
		{
			StoredData actual = storedChunks.remove();
			freedSpace += actual.getSize();
			toRemove.add(actual);
			
			System.out.println("Removing " + actual.getFileId() + "-" + actual.getChunkNo());
			FileManager.deleteChunk(actual.getFileId(), actual.getChunkNo());
			Peer.getDataManager().removeStoredChunk(actual.getFileId(), actual.getChunkNo(), Peer.getPeerID());			
		}
		
		return toRemove;
	}

	public void sendRemoved(ArrayList<StoredData> removed)
	{
		for(int i = 0; i < removed.size(); i++)
		{
			StoredData actual = removed.get(i);
			byte[] message = MessageGenerator.generateREMOVED(actual.getFileId(), String.valueOf(actual.getChunkNo()));
			Peer.getSenderSocket().sendPacket(message, Destination.MC);
		}
	}
	
	@Override
	public void run()
	{
		ArrayList<StoredData> removed = freeSpace();
		sendRemoved(removed);
	}
}
