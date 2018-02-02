package server.logic;

import java.util.*;

import javax.net.ssl.SSLSocket;

import server.network.SSLSocketListener;

public class PeerChannelsStore
{
	private static ArrayList<PeerChannel> peers;
	
	public static void PeerChannelsStoreInit()
	{
		peers = new ArrayList<PeerChannel>();
	}
	
	public static void addSocket(SSLSocket socket)
	{
		PeerChannel peerChannel = new PeerChannel(socket);
		peers.add(peerChannel);
		
		SSLSocketListener socketListener = new SSLSocketListener(peerChannel);
		Thread t = new Thread(socketListener);
		t.start();
		
		printState();
	}
	
	public static void removeSocket(PeerChannel peerChannel)
	{
		peers.remove(peerChannel);

		printState();
	}

	public static void updateSockets()
	{	
		ArrayList<Integer> deadSocketIndexes = new ArrayList<Integer>();
		
		for(PeerChannel peerChannel : peers)
		{		
			SSLSocket socket = peerChannel.getSSLSocket();
			
			if(socket.isClosed() || !socket.isConnected() || socket.isOutputShutdown() || socket.isInputShutdown())
			{
				System.out.println("Found a Dead Socket");
				deadSocketIndexes.add(peers.indexOf(peerChannel));
			}
		}
		
		Collections.reverse(deadSocketIndexes);
		
		for(Integer index : deadSocketIndexes)
		{
			peers.remove(peers.get(index));
		}
	}
	
	public static void printState()
	{	
		for(int i = 0; i < 3; i++)
		{
			if(i == 1)
				System.out.println("----------------------------------------");
			else
				System.out.println();
		}
		
		updateSockets();
		
		System.out.println("Master server state");
		
		for(PeerChannel peerChannel : peers)
		{
			System.out.println();
			
			SSLSocket socket = peerChannel.getSSLSocket();
			
			if(peerChannel.getPeerID() == null)
			{
				continue;
			}
			
			System.out.println(socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
			
			System.out.println("PeerId: " + peerChannel.getPeerID());
			System.out.println("MC Channel: " + peerChannel.getMCPort());
			System.out.println("MDB Channel: " + peerChannel.getMDBPort());
			System.out.println("MDR Channel: " + peerChannel.getMDRPort());
			System.out.println("Sender Port: " + peerChannel.getSenderPort());
		}
		
		for(int i = 0; i < 3; i++)
		{
			if(i == 1)
				System.out.println("----------------------------------------");
			else
				System.out.println();
		}
	}
	
	public static String getPeers()
	{
		updateSockets();
		
		String s = "";
		
		boolean first = true;
		
		for(PeerChannel peerChannel : peers)
		{
			if(peerChannel.getPeerID() != null)
			{
				if(first)
					first = false;
				else
					s += "\n";
				
				s += "UPDATE ";
				
				SSLSocket socket = peerChannel.getSSLSocket();
				
				s += socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " ";
				s += peerChannel.getPeerID() + " ";
				s += peerChannel.getMCPort() + " ";
				s += peerChannel.getMDBPort() + " ";
				s += peerChannel.getMDBPort() + " ";
				s += peerChannel.getSenderPort();
				;
			}
		}
		
		s += "\n";
		s += "UPDATE DONE";

		return s;
	}


}
