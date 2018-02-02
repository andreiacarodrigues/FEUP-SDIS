package message;

import java.net.*;
import java.util.*;

import chunk.Chunk;
import data.DataManager;
import files.*;
import peer.Peer;
import socket.PrivateSenderSocket;
import received.ChunkRec;
import received.Stored;
import socket.SenderSocket;

public class MessageHandler implements Runnable
{
	private DatagramPacket packet;
	private String[] headerTokens;
	private byte[] body;
		
	private static volatile ArrayList<String> receivedChunks = new ArrayList<String>();
	
	public MessageHandler(DatagramPacket packet)
	{	
		this.packet = packet;
	}
	
	public static ArrayList<String> getReceivedChunks()
	{
		return receivedChunks;
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
		
		parseMessage();
	}
	
	private synchronized void parseMessage()
	{
		if(Integer.valueOf(headerTokens[2]) == Peer.getServerId())
		{
			return;
		}
		else if(peerHasLowerVersion(Peer.getProtocolVersion(), headerTokens[1]))
		{
			//System.out.println("Lower protocol version");
			return;
		}
		
		String messageType = headerTokens[0];
		DataManager DM = Peer.getDataManager();
		
		switch(messageType)
		{
		case "PUTCHUNK":
			
			if(Peer.getDataManager().isInitiatorPeer(headerTokens[3]))
				return;
			
			//System.out.println("BODY LEN: " + body.length);
			
			Random randPut = new Random();
			int waitTimePut = randPut.nextInt(400);
			
			try
			{
				Thread.sleep(waitTimePut);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			if(FileManager.storeChunk(headerTokens[3], Integer.valueOf(headerTokens[4]), body, Integer.valueOf(headerTokens[5])))
			{
				byte[] response = MessageGenerator.generateSTORED(headerTokens[3], headerTokens[4]);
				Peer.getSenderSocket().sendPacket(response, SenderSocket.Destination.MC);
				//System.out.println("SENT STORED");
			}
			
			break;
			
		case "STORED":
			
			Stored.addMessage(headerTokens[3], Integer.valueOf(headerTokens[4]), Integer.valueOf(headerTokens[2]));
			//System.out.println("RECEIVED STORED");
			
			break;
			
		case "GETCHUNK":
			String chunkF = headerTokens[3] + "-" + headerTokens[4];

			Random rand = new Random();
			int waitTime = rand.nextInt(400);
			
			try
			{
				Thread.sleep(waitTime);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			if(receivedChunks.contains(chunkF))
			{
				//System.out.println("Received chunk, not sending");
				return;
			}
			
			byte[] read = FileManager.getChunk(headerTokens[3], Integer.valueOf(headerTokens[4]));
			
			if(headerTokens[1].equals("2.0") && read != null)
			{
				String address = headerTokens[5];
				String port = headerTokens[6];
				PrivateSenderSocket ss = new PrivateSenderSocket();
				
				Chunk chunk = new Chunk(headerTokens[3], Integer.valueOf(headerTokens[4]), 0, read);
				byte[] buf = MessageGenerator.generateCHUNK(chunk);
				ss.sendMessage(address, Integer.valueOf(port), buf);
				
				byte[] emptyMsg = new byte[1];
				Chunk emptyChunk = new Chunk(headerTokens[3], Integer.valueOf(headerTokens[4]), 0, emptyMsg);
				byte[] emptyBuf = MessageGenerator.generateCHUNK(emptyChunk);
				Peer.getMDB().sendPacket(emptyBuf);
				
				return;
			}
			
			
			if(read != null)
			{
				Chunk chunk = new Chunk(headerTokens[3], Integer.valueOf(headerTokens[4]), 0, read);
				byte[] buf = MessageGenerator.generateCHUNK(chunk);
				Peer.getMDB().sendPacket(buf);
			}
			break;
			
		case "CHUNK":
			String chunk = headerTokens[3] + "-" + headerTokens[4];
				
			if(!receivedChunks.contains(chunk)){
				receivedChunks.add(chunk);
				//System.out.println("Received chunk " + chunk);
			}
			
			if(body.length == 1 && Peer.getProtocolVersion().equals("2.0"))
				return;
			
			ChunkRec.addMessage(headerTokens[3], Integer.valueOf(headerTokens[4]), body);
			break;
			
		case "DELETE":
			//System.out.println("DELETE RECEIVED");
			
			String fileID = headerTokens[3];
			
			if(headerTokens[1].equals("2.0"))
				Peer.getDeletedFiles().add(fileID);
			
			ArrayList<Integer> chunks = DM.getOwnedFileChunks(fileID);
			
			for(int i = 0; i < chunks.size(); i++)
				FileManager.deleteChunk(fileID, chunks.get(i));
			
			//System.out.println(chunks.size());
			Peer.getDataManager().deleteChunks(fileID);

			break;
			
		case "REMOVED":
			//System.out.println("REMOVED RECEIVED");
			
			int peerId = Integer.valueOf(headerTokens[2]);
			String fileId = headerTokens[3];
			int chunkNo = Integer.valueOf(headerTokens[4]);
			
			Peer.getDataManager().removeStoredChunk(fileId, chunkNo, peerId);
			Peer.getDataManager().removeBackedUpDataPeer(fileId, chunkNo, peerId);
			
			if(Peer.getDataManager().validReplicationDegree(fileId, chunkNo))
				return;
			
			int desiredReplicationDegree = Peer.getDataManager().getDesiredReplicationDegree(fileId);
			Peer.recoverChunkReplicationDegree(fileId, chunkNo, desiredReplicationDegree);

			break;
			
		case "CHECKDELETED":
			if(Peer.getProtocolVersion().equals("1.0"))
				return;
			
			String fileIdd = headerTokens[3];
			ArrayList<String> previousDeletes = Peer.getDeletedFiles();
			
			if(previousDeletes.contains(fileIdd))
			{
				byte[] message = MessageGenerator.generateDELETE(fileIdd);
				Peer.getMC().sendPacket(message);
			}
			
			break;
		}
	}
	
	public boolean peerHasLowerVersion(String peerVersion, String messageVersion)
	{
		if(peerVersion.charAt(0) < messageVersion.charAt(0))
		{
			return true;
		}
		
		if(peerVersion.charAt(0) == messageVersion.charAt(0) && peerVersion.charAt(2) < messageVersion.charAt(2))
		{
			return true;
		}
		
		return false;
	}

	@Override
	public void run()
	{
		splitMessage();
	}
	
	public synchronized static int indexOf(byte[] list, byte[] element)
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
	

}
