package chunk;

public class Chunk
{
	private String fileId;
	private int chunkNo;
	private int replicationDegree;
	
	private byte[] body;
	
	public Chunk(String fileId, int chunkNo, int replicationDegree, byte[] body)
	{
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.replicationDegree = replicationDegree;
		this.body = body;
	}
	
	public String getFileId()
	{
		return fileId;
	}
	
	public int getChunkNo()
	{
		return chunkNo;
	}
	
	public int getReplicationDegree()
	{
		return replicationDegree;
	}
	
	public byte[] getBody()
	{
		return body;
	}
}
