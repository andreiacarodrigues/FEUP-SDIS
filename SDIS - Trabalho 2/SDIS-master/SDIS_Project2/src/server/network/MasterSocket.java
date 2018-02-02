package server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

import server.logic.PeerChannelsStore;
import server.main.Server;

public class MasterSocket implements Runnable
{
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private ArrayList<String> buffer;
	
	public MasterSocket(Socket socket)
	{
		this.socket = socket;
		out = null;
		in = null;
		buffer = new ArrayList<String>();
	}

	@Override
	public void run()
	{
		try
		{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (IOException e)
		{
			System.out.println("Error creating the Master Socket Listener");
		}
		
		boolean running = true;
		
		while(running)
		{
			String message = null;
			
			try
			{
				message = in.readLine();
			}
			catch (IOException e)
			{
				System.out.println("Input stream from SSL Socket Closed");
				running = false;
				Server.removeMe(this);
			}
			
			if(message != null)
			{
				handleMessage(message);
			}
		}
	}
	
	public void handleMessage(String message)
	{
		String[] messageTokens = message.split(" ");
		
		switch(messageTokens[0])
		{
		case "GetPeers":
			
			System.out.println("Received a request for the peers");
			out.println(PeerChannelsStore.getPeers());
			
			break;
			
		case "UPDATE":
			
			if(messageTokens[1].equals("DONE"))
			{
				// DO NOTHING
			}
			else
			{
				buffer.add(message);
			}
			
			break;
			
		default:
			
			System.out.println("Received an unknown command");
		
			break;
		}
	}
	
	public ArrayList<String> getBufffer()
	{
		return buffer;
	}
	
	public void resetBuffer()
	{
		buffer = new ArrayList<String>();
	}
	
	public void sendMessage(String message)
	{
		if(out != null)
			out.println(message);
	}
}
