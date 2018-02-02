package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import message.MessageHandler;

public class PrivateListener implements Runnable
{
	private DatagramSocket socket;
	private DatagramPacket packet;		
	
	public PrivateListener(int port)
	{	
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		boolean running = true;
		while(running)
		{
			byte[] buf = new byte[ThreadedMulticastSocketListener.MAX_SIZE];
			
			packet = new DatagramPacket(buf, buf.length);

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
	}

	public void handlePacket(DatagramPacket packet)
	{
		new Thread(new MessageHandler(packet)).start();
	}
}
