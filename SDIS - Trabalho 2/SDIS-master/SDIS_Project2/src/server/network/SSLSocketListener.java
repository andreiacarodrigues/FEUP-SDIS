package server.network;

import java.io.*;
import java.util.ArrayList;

import javax.net.ssl.*;

import server.logic.*;
import server.main.Server;

public class SSLSocketListener implements Runnable
{
	private PeerChannel peerChannel;
	private PrintWriter out;
	private BufferedReader in;
	boolean running;
	
	public SSLSocketListener(PeerChannel peerChannel)
	{
		this.peerChannel = peerChannel;
		out = null;
		in = null;
	}

	@Override
	public void run()
	{		
		SSLSocket socket = peerChannel.getSSLSocket();
		
		try
		{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (IOException e)
		{
			PeerChannelsStore.removeSocket(peerChannel);
		}
		
		running = true;
		
		while(running)
		{
			String message = null;
			
			try
			{
				message = in.readLine();
			}
			catch (IOException e)
			{
				PeerChannelsStore.removeSocket(peerChannel);
				running = false;
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
			
			ArrayList<String> buffers = Server.getAllBuffers();
			
			for(String messageBuff : buffers)
			{
				out.println(messageBuff);
			}
			
			out.println(PeerChannelsStore.getPeers());
			
			break;
			
		case "Authenticate":
			
			System.out.println("Received an authentication");
			peerChannel.setInfo(Integer.parseInt(messageTokens[1]), Integer.parseInt(messageTokens[2]), Integer.parseInt(messageTokens[3]), Integer.parseInt(messageTokens[4]), Integer.parseInt(messageTokens[5]));
			
			break;
			
		case "StoreMetadata":
				
			System.out.println("Received store metadata");
			
			try
			{
				byte [] array  = new byte [100000];
			    InputStream is = peerChannel.getSSLSocket().getInputStream();
			    
			    int bytesRead = is.read(array);
			    
			    System.out.println("Read: " + bytesRead);
			    
			    FileOutputStream fos = new FileOutputStream("../Master/Peer" + peerChannel.getPeerID());
			    fos.write(array, 0, bytesRead);

			    fos.close();
			}
			catch(Exception e)
			{
				break;
			}
			
			break;
			
		case "GetMetadata":
			
			
			File myFile = new File("../Master/Peer" + peerChannel.getPeerID());
			
			if(myFile.exists())
			{
				out.println("Metadata");
			}
			else
			{
				out.println("NoMetadata");
			}
	        
	        try
	        {
	        	byte [] mybytearray  = new byte [(int)myFile.length()];
	        	FileInputStream fis = new FileInputStream(myFile);
	        	BufferedInputStream bis = new BufferedInputStream(fis);
	        	bis.read(mybytearray,0,mybytearray.length);
	        	peerChannel.getSSLSocket().getOutputStream().write(mybytearray,0,mybytearray.length);
	        	
	        	bis.close();
	        	fis.close();
	        }
	        catch(Exception e)
	        {
	        	break;
	        }
			
			break;
			
		default:
			
			System.out.println("Received an unknown command");
		
			break;
		}
		
		PeerChannelsStore.printState();
	}
}
