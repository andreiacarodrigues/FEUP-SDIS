package server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import server.main.Server;

public class MasterSocketListener implements Runnable
{
	ServerSocket serverSocket;
	
	public MasterSocketListener(ServerSocket serverSocket)
	{
		this.serverSocket = serverSocket;
	}
	
	@Override
	public void run()
	{
		boolean running = true;
		
		while(running)
		{
			Socket socket = null;
			
			try
			{
				socket = (Socket) serverSocket.accept();
			}
			catch (IOException e)
			{
				System.out.println("Could not accept socket");
			}
			
			if(!socket.equals(null))
			{
				Server.addMasterSocket(socket);
			}
			
			System.out.println("Connected to a new master");
		}
	}
	
}
