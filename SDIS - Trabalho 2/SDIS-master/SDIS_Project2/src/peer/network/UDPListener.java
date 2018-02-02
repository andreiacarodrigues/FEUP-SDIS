package peer.network;

import java.io.IOException;
import java.net.*;

import peer.message.MessageHandler;

public class UDPListener implements Runnable
{
	public static final int MAX_SIZE = 65000;
	private volatile DatagramSocket socket;
	private boolean ready = false;

	@Override
	public void run()
	{
		try
		{
			socket = new DatagramSocket();
		}
		catch (SocketException e)
		{
			System.out.println("Couldn't open the UDP socket listener");
		}
		
		ready = true;
		
		// Receiving
		boolean running = true;
		while(running)
		{
			byte[] buf = new byte[MAX_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try
			{
				socket.receive(packet);
			}
			catch (IOException e)
			{
				System.out.println("Error receiving at the UDP Listener");
			}
			
			new Thread(new MessageHandler(packet)).start();
		}
	}
	
	public Integer getPort()
	{
		if(!ready)
		{
			System.out.println("Could not get the port of UDP Listener");
			System.exit(-1);
		}
			
		return socket.getLocalPort();
	}

	public boolean isReady()
	{
		return ready;
	}
}
