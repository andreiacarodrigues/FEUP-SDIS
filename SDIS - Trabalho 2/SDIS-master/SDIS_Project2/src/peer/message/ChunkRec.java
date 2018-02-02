package peer.message;

import java.util.HashMap;

public class ChunkRec
{
	private static volatile HashMap<String, HashMap<Integer, byte[]>> chunkMessages;
	// FileId, ChunkNo, Body
	
	public static void initChunkRec()
	{
		chunkMessages = new HashMap<String, HashMap<Integer, byte[]>>();
	}
	
	public static void resetFile(String fileId)
	{
		chunkMessages.remove(fileId);
	}
	
	public static void addMessage(String fileId, Integer chunkNo, byte[] buf)
	{
		// TODO
		// Apenas de o fileId de um ficheiro dele
		
		HashMap<Integer, byte[]> innerHashMap = chunkMessages.get(fileId);
		if(innerHashMap == null)
		{
			innerHashMap = new HashMap<Integer, byte[]>();
			chunkMessages.put(fileId, innerHashMap);
		}
		innerHashMap.put(chunkNo, buf);
	}
	
	public static byte[] getMessage(String filename, Integer chunkNo)
	{
		HashMap<Integer, byte[]> innerHashMap = chunkMessages.get(filename);
		if(innerHashMap == null)
			return null;
		
		return innerHashMap.get(chunkNo);
	}
}
