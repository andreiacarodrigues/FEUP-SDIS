package data;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import files.FileManager;
import message.MessageGenerator;
import peer.Peer;
import received.Stored;

public class DataManager implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private volatile ArrayList<StoredData> storedFilesData;
	private volatile ArrayList<BackedUpData> backedUpData;
	
	public DataManager()
	{
		storedFilesData = new ArrayList<StoredData>();
		backedUpData = new ArrayList<BackedUpData>();
	}
	
	public ArrayList<StoredData> getStoredData()
	{
		return storedFilesData;
	}
	
	// STORED
	
	public void addStoredFilesData(String fileId, int chunkNo, int desiredReplicationDegree, int size)
	{	
		ArrayList<Integer> peers = Stored.getPeers(fileId, chunkNo);
		if(peers == null)
		{
			peers = new ArrayList<Integer>();
		}
		
		if(!peers.contains(Peer.getServerId()))
			peers.add(Peer.getServerId());
		
		storedFilesData.add(new StoredData(fileId, chunkNo, desiredReplicationDegree, peers, size));
		serialize();
	}
	
	public void updateStoredFilesData(String fileId, int chunkNo, int peerId)
	{
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			StoredData SD = storedFilesData.get(i);
			if(SD == null || SD.getFileId() == null)
			{
				continue;
			}
			
			if(SD.getFileId().equals(fileId) && SD.getChunkNo() == chunkNo)
			{
				ArrayList<Integer> peers = SD.getPeers();
				if(!peers.contains(peerId))
					peers.add(peerId);
			}
		}
		serialize();
	}
	
	public ArrayList<Integer> getOwnedFileChunks(String fileId)
	{
		ArrayList<Integer> chunks = new ArrayList<Integer>();

		for(int i = 0; i < storedFilesData.size(); i++)
		{
			StoredData SD = storedFilesData.get(i);
			if(SD.getFileId().equals(fileId))
			{
				if(SD.getPeers().contains(Peer.getServerId()))
					chunks.add(SD.getChunkNo());
			}
		}
		return chunks;
	}
	
	public void removeStoredChunk(String fileId, int chunkNo, int peerId)
	{
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			StoredData SD = storedFilesData.get(i);
			if(SD.getFileId().equals(fileId) && SD.getChunkNo() == chunkNo)
			{
				ArrayList<Integer> peers = SD.getPeers();
				for(int j = 0; j < peers.size(); j++)
				{
					if(peers.get(j) == peerId)
						peers.remove(j);
				}
			}
		}
	}
	
	public boolean validReplicationDegree(String fileId, int chunkNo)
	{
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			StoredData SD = storedFilesData.get(i);
			if(SD.getFileId().equals(fileId) && SD.getChunkNo() == chunkNo)
			{
				if(SD.getPeers().size() >= SD.getDesiredReplicationDegree())
					return true;
				else
					return false;
			}
		}
		return false;
	}
	
	public int getDesiredReplicationDegree(String fileId)
	{
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			StoredData SD = storedFilesData.get(i);
			if(SD.getFileId().equals(fileId))
			{
				return SD.getDesiredReplicationDegree();
			}
		}
		return 0;
	}
	
	// BACKUP
	
	/*
	Retorna 0 - filename não existe
	Retorna 1 - filename existe com o fileId indicado
	Retorna 2 - filename existe com outro fileId
	*/
	public boolean addBackedUpData(String filename, String fileId, int desiredReplicationDegree)
	{
		boolean modified = false;
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData data = backedUpData.get(i);
			
			if(data.getFilename().equals(filename))
			{
				if(data.getFileId().equals(fileId))
				{
					return false;
				}
				else
				{
					for(int j = 0; j < 3; j++)
					{
						byte[] message = MessageGenerator.generateDELETE(data.getFileId());
						Peer.getMC().sendPacket(message);
					}
					Stored.resetFile(data.getFileId());
					data.setFileId(fileId);
					modified = true;
				}
			}
		}
		
		if(!modified)
		{
			BackedUpData data = new BackedUpData(filename, fileId, desiredReplicationDegree);
			backedUpData.add(data);
		}
		
		serialize();
		
		return true;
	}
	
	public void deleteBackedUpData(String filename, String fileID)
	{
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData data = backedUpData.get(i);
			if(data.getFileId().equals(fileID))
			{
				backedUpData.remove(i);
				break;
			}
		}
		
		serialize();
	}
	
	public void removeBackedUpDataPeer(String fileId, int chunkNo, int peerId)
	{
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData actual = backedUpData.get(i);
			if(actual.getFileId().equals(fileId))
			{
				HashMap<Integer, ArrayList<Integer>> chunkPeers = actual.getChunkPeers();
				ArrayList<Integer> peers = chunkPeers.get(chunkNo);
				if(peers == null)
					return;
				
				for(int j = 0; j < peers.size(); j++)
				{
					if(peers.get(j) == peerId)
					{
						peers.remove(j);
						return;
					}
				}
			}
		}
	}
	
	public boolean isInitiatorPeer(String fileId)
	{
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData actual = backedUpData.get(i);
			if(actual.getFileId().equals(fileId))
				return true;
		}
		return false;
	}
	
	public void deleteChunks(String fileID)
	{
		ArrayList<StoredData> newList = new ArrayList<StoredData>();
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			if(!storedFilesData.get(i).getFileId().equals(fileID))
			{
				newList.add(storedFilesData.get(i));
				/*storedFilesData.remove(i);
				i--;
				if(i < 0)
					break;*/
			}
		}
		storedFilesData = newList;
		serialize();
	}
	
	public String getFileId(String filename)
	{
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData BUD = backedUpData.get(i);
			
			if(BUD.getFilename().equals(filename))
				return BUD.getFileId();
		}
		return null;
	}
	
	public void addChunkPeers(String fileId, int chunkNo, ArrayList<Integer> peers)
	{
		for(int i = 0; i < backedUpData.size(); i++)
		{
			BackedUpData bud = backedUpData.get(i);
			if(bud.getFileId().equals(fileId))
			{
				bud.addChunkPeers(chunkNo, peers);
			}
		}
		serialize();
	}
	
	// SERIALIZE

	public void serialize()
	{
		int tries = 0;
		while(tries < 5)
		{	
			FileOutputStream fout;
			try
			{
				fout = new FileOutputStream("../Peer" + Peer.getServerId() + "/metadata.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fout);
				oos.writeObject(this);
				
				oos.close();
				fout.close();
				tries = 5;
			}
			catch (Exception e)
			{
				tries++;
			}
		}
	}
	
	// TO STRING
	
	public String toString()
	{
		String ret = "Received Chunks Information:  \n";
		
		for(int i = 0; i < storedFilesData.size(); i++)
		{
			if(storedFilesData.get(i) != null)
				ret += storedFilesData.get(i).toString();
		}
		
		ret += "\n\nBacked Up Data Information:  \n";
		for(int i = 0; i < backedUpData.size(); i++)
		{
			if(backedUpData.get(i) != null)
				ret += backedUpData.get(i).toString();
		}
		
		
		ret += "\n\n" + "Total Chunks Size Saved(bytes): " + FileManager.getChunksSize() + "\n";
		
		return ret;
	}

}
