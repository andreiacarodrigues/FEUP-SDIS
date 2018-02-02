package server.main;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.net.ssl.*;

import peer.main.Peer;
import server.logic.*;
import server.network.*;

public class Server
{
	private static SSLServerSocket serverSocket;
	private static ArrayList<MasterSocket> masterServers;
	private static int port;
	
	public static void main(String[] args)
	{	
		File masterFolder = new File("../Master");
		createDir(masterFolder);

		if(args.length != 1){
			System.out.println("Must specify server port");
			System.exit(1);
		}
		port = Integer.valueOf(args[0]);
		if(port > 5002 || port < 5000){
			System.out.println("Port must be between 5000 and 5002");
			System.exit(0);
		}
		PeerChannelsStore.PeerChannelsStoreInit();
		initSocket(port);
		startSocketServerListener();
		initConnectionToMasters();
	}
	
	public static void initSocket(int port)
	{
		System.setProperty("javax.net.ssl.trustStore", "truststore");
		// System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "server.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault(); 
		
		try
		{
			serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
		}
		catch (IOException e)
		{
			System.out.println("Could not listen on port: " + port);
			System.exit(-1);
		}
		
		System.out.println("SSL Server Socket listening on port: " + port);
		
		// Require client authentication 
		serverSocket.setNeedClientAuth(true);
		
		System.out.println("Clients now need authentication");
	}
	
	public static void startSocketServerListener()
	{
		SSLSocketServerListener socketServerListener = new SSLSocketServerListener(serverSocket);
		Thread t = new Thread(socketServerListener);
		t.start();
				
		System.out.println("SSL Socket Server Listener started");
	}
	
	public static void initConnectionToMasters()
	{
		masterServers = new ArrayList<MasterSocket>();
		
		boolean connected = true;
		Socket socket = null;
		
		try
		{
			socket = new Socket(InetAddress.getByName("localhost"), getNextPort(1) + 100);
		}
		catch(Exception e)
		{
			connected = false;
		}
		
		if(connected && socket != null)
		{
			MasterSocket masterSocketListener = new MasterSocket(socket);
			Thread t = new Thread(masterSocketListener);
			t.start();
			
			masterServers.add(masterSocketListener);
			System.out.println("Connected to a new master");
		}
		
		connected = true;
		socket = null;
		
		try
		{
			socket = new Socket(InetAddress.getByName("localhost"), getNextPort(2) + 100);
		}
		catch(Exception e)
		{
			connected = false;
		}
		
		if(connected && socket != null)
		{
			MasterSocket masterSocketListener = new MasterSocket(socket);
			Thread t = new Thread(masterSocketListener);
			t.start();
			
			masterServers.add(masterSocketListener);
			System.out.println("Connected to a new master");
		}
		
		startSocketServerMasterListener();
	}
	
	public static void startSocketServerMasterListener()
	{	
		try
		{
			ServerSocket serverSocket = new ServerSocket(port + 100);
			
			MasterSocketListener socketServerListener = new MasterSocketListener(serverSocket);
			Thread t = new Thread(socketServerListener);
			t.start();
					
			System.out.println("Master Socket Server Listener started");
		}
		catch (IOException e)
		{
			System.out.println("Error opening Server Socket for Masters");
			System.exit(-1);
		}
	}
	
	public static void addMasterSocket(Socket socket)
	{
		MasterSocket masterSocketListener = new MasterSocket(socket);
		Thread t = new Thread(masterSocketListener);
		t.start();
		
		masterServers.add(masterSocketListener);
	}
	
	public static void removeMe(MasterSocket masterSocket)
	{
		masterServers.remove(masterSocket);
		System.out.println("Removed a Master Socket");
	}
	
	public static int getPort()
	{
		return port;
	}
	
	private static int getNextPort(int index)
	{
		if(port == 5000)
		{
			if(index == 1)
			{
				return 5001;
			}
			else
			{
				return 5002;
			}
		}
		
		if(port == 5001)
		{
			if(index == 1)
			{
				return 5000;
			}
			else
			{
				return 5002;
			}
		}
		
		if(port == 5002)
		{
			if(index == 1)
			{
				return 5000;
			}
			else
			{
				return 5001;
			}
		}
		
		return -1;
	}
	
	public static ArrayList<String> getAllBuffers()
	{
		ArrayList<String> buffers = new ArrayList<String>();
		
		for(MasterSocket masterSocket : masterServers)
		{
			masterSocket.sendMessage("GetPeers");
			
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			ArrayList<String> buffer = masterSocket.getBufffer();
			for(String message : buffer)
			{
				buffers.add(message);
			}
			masterSocket.resetBuffer();
		}
		
		return buffers;
	}
	
	public static void createDir(File f)
	{
		// if the directory does not exist, create it
		if (!f.exists())
		{
		    System.out.println("Creating directory: " + f.getName());
		    boolean result = false;

		    try
		    {
		        f.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se)
		    {
		        //handle it
		    }        
		    if(result)
		    {    
		        System.out.println("DIR created");  
		    }
		}
	}
}