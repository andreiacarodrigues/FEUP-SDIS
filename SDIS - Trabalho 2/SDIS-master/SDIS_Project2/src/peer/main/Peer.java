package peer.main;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import peer.data.*;
import peer.files.FileManager;
import peer.files.FileSplitter;
import peer.message.Chunk;
import peer.message.ChunkRec;
import peer.message.MessageGenerator;
import peer.message.MessageHandler;
import peer.message.Stored;
import peer.network.*;
import peer.network.SenderSocket.Destination;
import peer.protocol.Backup;
import peer.protocol.BackupChunk;
import peer.protocol.Delete;
import peer.protocol.Reclaim;
import peer.protocol.ReclaimEnhancement;
import peer.protocol.RestoreEnhancement;

public class Peer implements RMI
{
	/* Network */
	private volatile static SSLSocket socket;
	private volatile static SSLSocketListener socketListener;
	
	private volatile static UDPListener MCListener;
	private volatile static UDPListener MDBListener;
	private volatile static UDPListener MDRListener;
	
	private volatile static SenderSocket senderSocket;
	
	/* Peer variables */
	private static Integer peerID;
	private static String serviceAccessPoint;
	
	/* Logical Structures */
	private volatile static DataManager dataManager;
	
	/* Disk Space */
	private static long diskSpaceBytes = 1 * 1000 * 1000 * 1000; // 1 GB
	
	/* Delete Enhancement */
	private static ArrayList<String> deletedFiles = new ArrayList<>();

	public static void main(String[] args)
	{
		if(args.length != 1){
			System.out.println("Must specify Peer ID");
			System.exit(1);
		}
		/* TEMPORARY Variable initialization, in future will be from args */
		peerID = Integer.valueOf(args[0]);
		serviceAccessPoint = "RMI" + peerID;
		
		System.out.println("Peer: " + peerID);
		
		/* Starts Every Communications Channel */
		startConnections();
		
		recoverBackupReplicationDegree();
	}
	
	/* Init Connections */
	
	public static void startConnections()
	{
		connectToMasterServer();
		startSocketListener();
		initUDPListeners();
		initSenderSocket();
		authenticate();
		FileManager.initFileManager();
		initRMI();
		Stored.initStored();
		ChunkRec.initChunkRec();
		initDataManager();
		Peer.checkDeleted();
		Thread t = new Thread(new ReclaimEnhancement());
		t.start();

		new Thread(new SendDataManager()).start();
	}

	public static void restartMasterServer()
	{
		connectToMasterServer();
		startSocketListener();
		
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		authenticate();
	}
	
	public static void connectToMasterServer()
	{
		System.setProperty("javax.net.ssl.trustStore", "truststore");
		// System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "client.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		
		SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault(); 
		
		int portToConnect = 5000 + (peerID % 3);
		boolean connected = false;
		while(!connected)
		{
			connected = true;
			
			try
			{
				socket = (SSLSocket) sf.createSocket(InetAddress.getByName("localhost"), portToConnect);
			}
			catch (IOException e)
			{
				connected = false;
				portToConnect++;
				
				if(portToConnect == 5003)
					portToConnect = 5000;
				
				if(portToConnect == 5000 + (peerID % 3))
				{
					System.out.println("Can't connect to master server");
					System.exit(-1);
				}
			}
		}
	}
	
	public static void startSocketListener()
	{
		socketListener = new SSLSocketListener(socket);
		Thread t = new Thread(socketListener);
		t.start();
				
		System.out.println("SSL Socket Listener started");
	}

	public static void initUDPListeners()
	{
		MCListener = new UDPListener();
		MDBListener = new UDPListener();
		MDRListener = new UDPListener();
		
		new Thread(MCListener).start();
		new Thread(MDBListener).start();
		new Thread(MDRListener).start();
		
		boolean socketsReady = false;
		while(!socketsReady)
		{
			socketsReady = (MCListener.isReady() && MDBListener.isReady() && MDRListener.isReady());
		}
		
		System.out.println("Sockets Ready");
	}
	
	public static void initSenderSocket()
	{
		senderSocket = new SenderSocket();
	}

	public static void authenticate()
	{	
		String message = "Authenticate ";

		message += peerID + " ";
		message += MCListener.getPort() + " ";
		message += MDBListener.getPort() + " ";
		message += MDRListener.getPort() + " ";
		message += senderSocket.getSocket().getLocalPort();
		
		socketListener.sendMessage(message);
	}
	
	private static void initRMI()
	{
		Peer peer = new Peer();
		
		try
		{
			RMI rmi = (RMI) UnicastRemoteObject.exportObject(peer, 0);
			Registry registry = LocateRegistry.getRegistry();
            registry.rebind(serviceAccessPoint, rmi);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}

	
	/* Logical Structures */

	public static void initDataManager()
	{
		File f = new File("../Peer" + peerID + "/metadata.ser");
		if(f.exists())
		{
			try
			{
				FileInputStream fin = new FileInputStream("../Peer" + peerID + "/metadata.ser");
				ObjectInputStream ois = new ObjectInputStream(fin);
				dataManager = (DataManager) ois.readObject();
				ois.close();
				fin.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		else
		{			
			socketListener.sendMessage("GetMetadata");
			
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			if(socketListener.metadataCheck() == 1)
			{
				boolean done = false;
				while(!done)
				{
					File fserver = new File("../Peer" + peerID + "/metadata.ser");
					if(fserver.exists())
					{
						try
						{
							FileInputStream fin = new FileInputStream("../Peer" + peerID + "/metadata.ser");
							ObjectInputStream ois = new ObjectInputStream(fin);
							dataManager = (DataManager) ois.readObject();
							ois.close();
							fin.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						catch (ClassNotFoundException e)
						{
							e.printStackTrace();
						}
						
						done = true;
					}
				}
			}
			else
			{
				dataManager = new DataManager();
			}
		}
	}
	
	/* GETS */
	
	public static Integer getPeerID()
	{
		return peerID;
	}

	public static SSLSocketListener getSocketListener()
	{
		return socketListener;
	}

	public static SenderSocket getSenderSocket()
	{
		return senderSocket;
	}

	public static DataManager getDataManager()
	{
		return dataManager;
	}
	
	public static long getDiskSpaceBytes()
	{
		return diskSpaceBytes;
	}
	
	public static ArrayList<String> getDeletedFiles()
	{
		return deletedFiles;
	}
	
	/* RMI */
	
	@Override
	public void backup(String filename, int replicationDegree, boolean encrypt) throws RemoteException
	{
		if(replicationDegree < 1 || replicationDegree > 9)
			System.out.println("Replication degree must be between 1 and 9");
		
		String filenameWithPath = "../Peer" + peerID + "/Files/" + filename;
		new Thread(new Backup(filenameWithPath, replicationDegree, encrypt)).start();	
	}

	@Override
	public void restore(String filename) throws RemoteException
	{
		String filenameWithPath = "../Peer" + peerID + "/Files/" + filename;
		new Thread(new RestoreEnhancement(filenameWithPath)).start();
	}

	@Override
	public void delete(String filename) throws RemoteException
	{
		String filenameWithPath = "../Peer" + peerID + "/Files/" + filename;
		new Thread(new Delete(filenameWithPath)).start();
	}

	@Override
	public void reclaim(long kbytes) throws RemoteException
	{
		diskSpaceBytes = kbytes * 1000;
		if(FileManager.getChunksSize() > diskSpaceBytes)
		{
			long toReclaim = FileManager.getChunksSize() - diskSpaceBytes;
			new Thread(new Reclaim(toReclaim, getDataManager().getStoredData())).start();
		}
	}
	
	@Override
	public String state() throws RemoteException
	{
		return dataManager.toString();
	}
	
	/* Enhancement methods */
	
	public  static int getMDRPort()
	{
		return MDRListener.getPort();
	}
	
	private static void checkDeleted()
	{
		ArrayList<StoredData> storedData = Peer.getDataManager().getStoredData();
		
		ArrayList<String> ownedFiles = new ArrayList<String>();
		
		for(int i = 0; i < storedData.size(); i++)
		{
			StoredData actual = storedData.get(i);
			if(!ownedFiles.contains(actual.getFileId()))
				ownedFiles.add(actual.getFileId());
		}
		
		for(int i = 0; i < ownedFiles.size(); i++)
		{
			byte[] message = MessageGenerator.generateCHECKDELETED(ownedFiles.get(i));
			senderSocket.sendPacket(message, Destination.MC);
		}
	}
	
	public static void recoverChunkReplicationDegree(String fileId, int chunkNo, int replicationDegree)
	{
		Random r = new Random();
		int waitTime1 = r.nextInt(400);
		
		try
		{
			Thread.sleep(waitTime1);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		String chunkName = fileId + "-" + String.valueOf(chunkNo);
		if(MessageHandler.getReceivedChunks().contains(chunkName))
		{
			System.out.println("Received chunk, not sending");
			return;
		}
		
		String path = "../Peer" + Peer.getPeerID() + "/Chunks/" + chunkName;
		File file = new File(path);
		
		if(!file.exists())
			return;
		
		BufferedInputStream bufinst = null;
		try {
			bufinst = new BufferedInputStream (new FileInputStream(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] body = new byte[FileSplitter.chunkSize];
		try {
			bufinst.read(body);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Chunk chunk = new Chunk(fileId, chunkNo, replicationDegree, body);
		
		Thread thread = new Thread(new BackupChunk(chunk));
		thread.start();
				
	}
	
	public static void recoverReplicationDegree()
	{
		ArrayList<StoredData> ownedChunks = Peer.getDataManager().getStoredData();
		for(int i = 0; i < ownedChunks.size(); i++)
		{
			StoredData actual = ownedChunks.get(i);
			if(actual.getDesiredReplicationDegree() < actual.getPeers().size())
				recoverChunkReplicationDegree(actual.getFileId(), actual.getChunkNo(), actual.getDesiredReplicationDegree());
		}
	}

	public static void recoverBackupReplicationDegree(){
		ArrayList<BackedUpData> backedUpData = Peer.getDataManager().getBackedUpData();
		for(int i = 0; i < backedUpData.size(); i++){
			BackedUpData actual = backedUpData.get(i);

			int desiredReplicationDegree = actual.getDesiredReplicationDegree();
			HashMap<Integer, ArrayList<Integer>> chunkPeers = actual.getChunkPeers();

			int maxNumBytes = chunkPeers.size() * 64000;
			File f = new File(actual.getFilename());
			if(f.length() > maxNumBytes){
				Thread t = new Thread(new Thread(new Delete(actual.getFilename(), false)));
				t.start();
				try {
					t.join();
				} catch (InterruptedException e){
					e.printStackTrace();
				}
				new Thread(new Backup(actual.getFilename(), desiredReplicationDegree, actual.isEncrypted())).start();
				continue;
			}

			chunkPeers.forEach((k, v)-> {
				if(v.size() < desiredReplicationDegree){
					int chunkNo = k;
					FileSplitter fs = new FileSplitter(actual.getFilename(), desiredReplicationDegree, actual.isEncrypted());
					ArrayList<Chunk> chunks = fs.getChunkList();

					Thread thread = new Thread(new BackupChunk(chunks.get(chunkNo)));
					thread.start();

					System.out.println("Recovering rep degree chunk " + chunkNo);
				}
			});
		}
	}

}