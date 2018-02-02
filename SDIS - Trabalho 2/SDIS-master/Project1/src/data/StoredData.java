package data;

import java.io.Serializable;
import java.util.ArrayList;


public class StoredData implements Serializable, Comparable<StoredData>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String fileId;
	private int chunkNo;
	private int desiredReplicationDegree;
	private ArrayList<Integer> peers;
	private int size;
	
	public StoredData(String fileId, int chunkNo, int desiredReplicationDegree, ArrayList<Integer> peers, int size)
	{
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.desiredReplicationDegree = desiredReplicationDegree;
		this.peers = peers;
		this.size = size;
	}
	
	// GETS
	
	public String getFileId()
	{
		return fileId;
	}
	
	public int getChunkNo()
	{
		return chunkNo;
	}
	
	public int getDesiredReplicationDegree()
	{
		return desiredReplicationDegree;
	}
	
	public ArrayList<Integer> getPeers()
	{
		return peers;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public String toString()
	{
		String ret = "File ID: " + this.fileId + "  -  " + "Chunk No " + String.valueOf(this.chunkNo) + "\n";
		ret += "Chunk Size(kbytes): " + String.valueOf(this.size) + "  -  ";
		ret += "Number of owners: " + String.valueOf(this.peers.size());
		
		ret += " ( ";
		for(int i = 0; i < this.peers.size(); i++)
		{
			ret += String.valueOf(this.peers.get(i)) + " ";
		}

		ret += ") " + "\n";
		
		ret += "Desired Replication Degree: " + desiredReplicationDegree;
		
		ret += "\n" + "\n";
		
		return ret;
	}

	@Override
	public int compareTo(StoredData o) {
		int aboveRep = this.peers.size() - this.desiredReplicationDegree;
		int aboveRep2 = o.peers.size() - o.desiredReplicationDegree;
		
		if(aboveRep == aboveRep2)
			return o.size - this.size;
		
		return aboveRep2 - aboveRep;
	}
}
