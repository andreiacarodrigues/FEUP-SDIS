package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import peer.Peer;

public class SenderSocket
{
	private volatile MulticastSocket socket;
	public enum Destination {MC, MDB, MDR};
	
	public SenderSocket()
	{
		try
		{
			socket = new MulticastSocket();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public synchronized void sendPacket(byte[] buf, Destination dest)
	{
		ThreadedMulticastSocketListener TMS;
		
		switch(dest)
		{
		case MC:
			TMS = Peer.getMC();
			break;
			
		case MDR:
			TMS = Peer.getMDB();
			break;
			
		case MDB:
			TMS = Peer.getMDB();
			break;
			
		default:
			TMS = null;
			System.out.println("Failed to get Socket in SenderSocket");
			return;
		}
		
		InetAddress address = TMS.getAddress();
		int port = TMS.getPort();
		
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		try
		{
			socket.send(packet);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
	}
}
