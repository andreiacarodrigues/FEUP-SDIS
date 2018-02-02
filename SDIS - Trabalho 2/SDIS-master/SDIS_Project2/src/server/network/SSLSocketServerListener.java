package server.network;

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import server.logic.*;

public class SSLSocketServerListener implements Runnable
{
	SSLServerSocket serverSocket;
	
	public SSLSocketServerListener(SSLServerSocket serverSocket)
	{
		this.serverSocket = serverSocket;
	}
	
	@Override
	public void run()
	{
		boolean running = true;
		
		while(running)
		{
			SSLSocket socket = null;
			
			try
			{
				socket = (SSLSocket) serverSocket.accept();
			}
			catch (IOException e)
			{
				System.out.println("Could not accept socket");
			}
			
			if(!socket.equals(null))
			{
				PeerChannelsStore.addSocket(socket);
			}
		}
	}
	
}
