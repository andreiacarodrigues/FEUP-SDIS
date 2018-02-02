package peer.message;

import java.net.*;
import java.util.*;

import peer.files.FileManager;
import peer.main.Peer;
import peer.network.*;
import peer.network.SenderSocket.Destination;

public class MessageHandler implements Runnable
{
	private DatagramPacket packet;
	private String[] headerTokens;
	private byte[] body;
	
	private static volatile ArrayList<String> receivedChunks = new ArrayList<String>();
	
	public static ArrayList<String> getReceivedChunks()
	{
		return receivedChunks;
	}
	
	public MessageHandler(DatagramPacket packet)
	{	
		this.packet = packet;
		
		splitMessage();
	}
	
	private void splitMessage()
	{
		byte[] packetData = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), packetData, 0, packet.getLength());
		
		int delimiterIndex = indexOf(packetData, MessageGenerator.CRLF.getBytes());
		
		byte[] headerBytes = Arrays.copyOfRange(packetData, 0, delimiterIndex);
		body = Arrays.copyOfRange(packetData, delimiterIndex + 4, packetData.length);
		
		String headerString = new String(headerBytes, 0, headerBytes.length).trim();
		headerTokens = headerString.split("(?<=[\\S])[ ]+(?=[\\S])");
	}
	
	@Override
	public void run()
	{
		parseMessage();
	}
	
	private void parseMessage()
	{	
		String messageType = headerTokens[0];
		
		switch(messageType)
		{
		case "PUTCHUNK":
			
			System.out.println("PUTCHUNK");
			handlePutchunk();
			
			break;
			
		case "STORED":
			
			System.out.println("STORED");
			handleStored();
			
			break;
			
		case "GETCHUNK":
			
			System.out.println("GETCHUNK");
			handleGetchunk();
			
			break;
			
		case "CHUNK":

			System.out.println("CHUNK");
			handleChunk();
			
			break;
			
		case "DELETE":
			
			System.out.println("DELETE");
			handleDelete();

			break;
			
		case "REMOVED":
			
			System.out.println("REMOVED");
			handleRemoved();

			break;
			
		case "CHECKDELETED":
			
			System.out.println("CHECKDELETED");
			handleCheckDeleted();
			
			break;
			
		default:
			
			System.out.println("Unkown Message Type");
			
			break;
		}
	}
	
	/* Handlers */
	
	public void handlePutchunk()
	{
		System.out.println("AQUI");
		
		/* Rejects putchunk requests if I'm the owner of the file */
		if(Peer.getDataManager().isInitiatorPeer(headerTokens[3]))
			return;
		
		randomWait(400);
		
		if(FileManager.storeChunk(headerTokens[3], Integer.valueOf(headerTokens[4]), body, Integer.valueOf(headerTokens[5])))
		{
			byte[] response = MessageGenerator.generateSTORED(headerTokens[3], headerTokens[4]);
			Peer.getSenderSocket().sendPacket(response, SenderSocket.Destination.MC);
		}
	}
	
	public void handleStored()
	{
		Stored.addMessage(headerTokens[3], Integer.valueOf(headerTokens[4]), Integer.valueOf(headerTokens[2]));
	}
	
	public void handleGetchunk()
	{
		String chunkF = headerTokens[3] + "-" + headerTokens[4];

		randomWait(400);
		
		if(receivedChunks.contains(chunkF))
		{
			return;
		}
		
		byte[] read = FileManager.getChunk(headerTokens[3], Integer.valueOf(headerTokens[4]));
		
		if(read != null)
		{
			String address = headerTokens[5];
			String port = headerTokens[6];
			
			Chunk chunk = new Chunk(headerTokens[3], Integer.valueOf(headerTokens[4]), 0, read);
			byte[] buf = MessageGenerator.generateCHUNK(chunk);
			Peer.getSenderSocket().sendMessage(address, Integer.valueOf(port), buf);
			
			byte[] emptyMsg = new byte[1];
			Chunk emptyChunk = new Chunk(headerTokens[3], Integer.valueOf(headerTokens[4]), 0, emptyMsg);
			byte[] emptyBuf = MessageGenerator.generateCHUNK(emptyChunk);
			Peer.getSenderSocket().sendPacket(emptyBuf, Destination.MDB);
			
			return;
		}
	}
	
	public void handleChunk()
	{
		String chunk = headerTokens[3] + "-" + headerTokens[4];
		
		if(!receivedChunks.contains(chunk))
		{
			receivedChunks.add(chunk);
		}
		
		if(body.length == 1)
			return;
		
		ChunkRec.addMessage(headerTokens[3], Integer.valueOf(headerTokens[4]), body);
	}
	
	public void handleDelete()
	{
		String fileID = headerTokens[3];
		
		//if(headerTokens[1].equals("2.0"))
			Peer.getDeletedFiles().add(fileID);
		
		ArrayList<Integer> chunks = Peer.getDataManager().getOwnedFileChunks(fileID);
		
		for(int i = 0; i < chunks.size(); i++)
			FileManager.deleteChunk(fileID, chunks.get(i));
		
		//System.out.println(chunks.size());
		Peer.getDataManager().deleteChunks(fileID);
	}
	
	public void handleRemoved()
	{
		int peerId = Integer.valueOf(headerTokens[2]);
		String fileId = headerTokens[3];
		int chunkNo = Integer.valueOf(headerTokens[4]);
		
		Peer.getDataManager().removeStoredChunk(fileId, chunkNo, peerId);
		Peer.getDataManager().removeBackedUpDataPeer(fileId, chunkNo, peerId);
		
		if(Peer.getDataManager().validReplicationDegree(fileId, chunkNo))
			return;
		
		int desiredReplicationDegree = Peer.getDataManager().getDesiredReplicationDegree(fileId);
		Peer.recoverChunkReplicationDegree(fileId, chunkNo, desiredReplicationDegree);
	}
	
	public void handleCheckDeleted()
	{
		String fileIdd = headerTokens[3];
		ArrayList<String> previousDeletes = Peer.getDeletedFiles();
		
		if(previousDeletes.contains(fileIdd))
		{
			byte[] message = MessageGenerator.generateDELETE(fileIdd);
			Peer.getSenderSocket().sendPacket(message, Destination.MC);
		}
	}
	
	/* Aux functions */
	
	public int indexOf(byte[] list, byte[] element)
	{
	    for(int i = 0; i < list.length - element.length + 1; ++i)
	    {
	        boolean found = true;
	        
	        for(int j = 0; j < element.length; ++j)
	        {
	           if (list[i + j] != element[j])
	           {
	               found = false;
	               break;
	           }
	        }
	        
	        if(found)
	        	return i;
	     }
	   return -1;  
	} 
	
	public void randomWait(int maxTime)
	{
		Random rand = new Random();
		int waitTime = rand.nextInt(400);
		
		try
		{
			Thread.sleep(waitTime);
		}
		catch (InterruptedException e)
		{
			System.out.println("Error at wait time");
		}
	}
}
