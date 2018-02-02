package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import message.MessageHandler;

public class ThreadedMulticastSocketListener implements Runnable
{
	public static final int MAX_SIZE = 65000;
	private MulticastSocket socket;
	
	private InetAddress address;
	private int port;
	
	private volatile boolean ready = false;
	
	public ThreadedMulticastSocketListener(InetAddress address, int port)
	{
		this.address = address;
		this.port = port;
	}

	@Override
	public void run()
	{	
		// Opening
		// System.out.println("Opening Socket " + address.getHostName() + ":" + port);

		try
		{
			socket = new MulticastSocket(port);
			socket.setTimeToLive(1);
			socket.joinGroup(address);
		}
		catch (IOException e)
		{
			e.printStackTrace();
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
				e.printStackTrace();
			}
			
			handlePacket(packet);
		}
		
		// Closing
		System.out.println("Closing Socket");
		socket.close();
	}
	
	public void handlePacket(DatagramPacket packet)
	{
		new Thread(new MessageHandler(packet)).start();
	}
	
	public boolean isReady()
	{
		return ready;
	}
	
	public void sendPacket(byte[] buf)
	{	
		if(!ready)
		{
			System.out.println("Socket not ready yet");
			return;
		}
		
		DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, address, port);
		
		try
		{
			socket.send(datagramPacket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public InetAddress getAddress()
	{
		return address;
	}
	
	public int getPort()
	{
		return port;
	}
}
