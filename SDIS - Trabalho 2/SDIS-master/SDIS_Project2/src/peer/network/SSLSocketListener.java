package peer.network;

import java.io.*;

import javax.net.ssl.SSLSocket;

import peer.main.Peer;

public class SSLSocketListener implements Runnable
{
	private SSLSocket socket;
	private PrintWriter out;
	private BufferedReader in;
	private boolean running;
	private int metadataExists; // 0 empty, 1 exists, 2 doesn't exist
	
	public SSLSocketListener(SSLSocket socket)
	{
		this.socket = socket;
		out = null;
		in = null;
		metadataExists = 0;
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
			System.out.println("Error creating the SSL Socket Listener");
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
				System.out.println("Input stream from SSL Socket Closed");
				running = false;
			}
			
			if(message != null)
			{
				handleMessage(message);
			}
		}
		
		Peer.restartMasterServer();
	}
	
	public void handleMessage(String message)
	{
		String[] messageTokens = message.split(" ");
		
		System.out.println(message);
		
		switch(messageTokens[0])
		{		
		case "UPDATE":
			
			if(messageTokens[1].equals("DONE"))
			{
				Peer.getSenderSocket().setContactListUpdated();
			}
			else
			{
				Peer.getSenderSocket().addContact(messageTokens[1], Integer.parseInt(messageTokens[2]), Integer.parseInt(messageTokens[3]), Integer.parseInt(messageTokens[4]), Integer.parseInt(messageTokens[5]), Integer.parseInt(messageTokens[6]));
			}
			
			break;
			
		case "Metadata":
			
			metadataExists = 1;
			
			try
			{
				byte [] array  = new byte [100000];
			    InputStream is = socket.getInputStream();
			    
			    int bytesRead = is.read(array);
			    
			    System.out.println("Read: " + bytesRead);

			    File f = new File("../Peer" + Peer.getPeerID() + "/metadata.ser");

			    if(f.exists())
			    	f.delete();

			    FileOutputStream fos = new FileOutputStream(f);
			    fos.write(array, 0, bytesRead);

			    fos.close();
			}
			catch(Exception e)
			{
				break;
			}
			
			break;
			
		case "NoMetadata":
		
			metadataExists = 2;
		
			break;
		
		default:
			
			// System.out.println("Received an unknown command");
		
			break;
		}
	}
	
	public void sendMessage(String message)
	{
		out.println(message);
	}
	
	public void sendBytes(byte[] message)
	{
		try
		{
			socket.getOutputStream().write(message, 0, message.length);
		}
		catch (IOException e)
		{
			System.out.println("Failed to send metadata bytes");
		}
	}
	
	public int metadataCheck()
	{
		return metadataExists;
	}
}
