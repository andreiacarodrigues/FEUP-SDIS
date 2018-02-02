package peer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;

import chunk.Chunk;
import data.DataManager;
import data.StoredData;
import files.FileManager;
import files.FileSplitter;
import message.MessageGenerator;
import message.MessageHandler;
import protocol.Backup;
import protocol.BackupChunk;
import protocol.Delete;
import protocol.Reclaim;
import protocol.ReclaimEnhancement;
import protocol.Restore;
import protocol.RestoreEnhancement;
import received.ChunkRec;
import received.Stored;
import socket.PrivateSenderSocket;
import socket.SenderSocket;
import socket.ThreadedMulticastSocketListener;

// TESTE "../Files/pena.bmp"

public class Peer implements RMI
{
	// Peer Information
	private static String protocolVersion;
	private static int serverId;
	private static String serviceAccessPoint;
	
	// Socket Listeners
	private static ThreadedMulticastSocketListener MC;
	private static ThreadedMulticastSocketListener MDB;
	private static ThreadedMulticastSocketListener MDR;
	private static SenderSocket SS;
	private static PrivateSenderSocket PSS;
	
	// Data Manager
	private volatile static DataManager DM;
	
	// Disk Space
	private static long diskSpaceBytes = 1 * 1000 * 1000 * 1000; // 1 GB
	
	// Delete Enhancement
	private static ArrayList<String> deletedFiles;
	
	public static void main(String[] args)
	{
		// Temporary Arguments Initialization
		/*String[] addresses = {"224.1.1.1", "224.2.2.2", "224.3.3.3"};
		int[] ports = {5000, 5001, 5002};
		protocolVersion = "2.0";
		serverId = 3;
		serviceAccessPoint = "RMI" + serverId;*/
		if(args.length != 9)
		{
			System.out.println("Wrong number of arguments.Usage:\n\t  java peer.Peer <protocol version> <server id> <service access point> <MC IP> <MC Port> "
					+ "<MDB IP> <MDB Port>  <MDR IP> <MDR Port>");
			return;
		}
		//java peer.Peer <protocol version> <server id> <service access point> <MC IP> <MC Port> <MDB IP> <MDB Port>  <MDR IP> <MDR Port>
		protocolVersion = args[0];
		if(!protocolVersion.matches("[0-9]\\.[0-9]"))
		{
			System.out.println("Protocol version is not valid");
			return;
		}
		serverId = Integer.valueOf(args[1]);
		serviceAccessPoint = args[2];
		String[] addresses = {args[3], args[5], args[7]};
		int[] ports = {Integer.valueOf(args[4]), Integer.valueOf(args[6]), Integer.valueOf(args[8])};
		
		deletedFiles = new ArrayList<String>();
		
		initListeners(addresses, ports);
		SS = new SenderSocket();
		FileManager.initFileManager();
		
		File f = new File("../Peer" + Peer.getServerId() + "/metadata.ser");
		if(f.exists())
		{
			try
			{
				FileInputStream fin = new FileInputStream("../Peer" + Peer.getServerId() + "/metadata.ser");
				ObjectInputStream ois = new ObjectInputStream(fin);
				DM = (DataManager) ois.readObject();
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
			DM = new DataManager();
		}
		
		initRMI();
		Stored.initStored();
		ChunkRec.initChunkRec();
		
		if(Peer.getProtocolVersion().equals("2.0"))
		{
			Peer.checkDeleted();
			Thread t = new Thread(new ReclaimEnhancement());
			t.start();
		}
	}
	
	// INITS
		
	private static void initListeners(String[] addresses, int[] ports)
	{
		try
		{
			MC = new ThreadedMulticastSocketListener(InetAddress.getByName(addresses[0]), ports[0]);
			MDB = new ThreadedMulticastSocketListener(InetAddress.getByName(addresses[1]), ports[1]);
			MDR = new ThreadedMulticastSocketListener(InetAddress.getByName(addresses[2]), ports[2]);
			PSS = new PrivateSenderSocket();
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		
		new Thread(MC).start();
		new Thread(MDB).start();
		new Thread(MDR).start();
		
		boolean socketsReady = false;
		while(!socketsReady)
		{
			socketsReady = (MC.isReady() && MDB.isReady() && MDR.isReady());
		}
		System.out.println("Sockets Ready");
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
	
	// GETS
	
	public static String getProtocolVersion()
	{
		return protocolVersion;
	}
	
	public static int getServerId()
	{
		return serverId;
	}
	
	public static ThreadedMulticastSocketListener getMC()
	{
		return MC;
	}
	
	public static ThreadedMulticastSocketListener getMDB()
	{
		return MDB;
	}
	
	public static ThreadedMulticastSocketListener getMDR()
	{
		return MDR;
	}
	
	public static SenderSocket getSenderSocket()
	{
		return SS;
	}
	
	public static DataManager getDataManager()
	{
		return DM;
	}
	
	public static long getDiskSpaceBytes()
	{
		return diskSpaceBytes;
	}

	public static ArrayList<String> getDeletedFiles()
	{
		return deletedFiles;
	}
	
	// OTHER METHODS
	
	@Override
	public void backup(String filename, int replicationDegree) throws RemoteException
	{
		if(replicationDegree < 1 || replicationDegree > 9)
			System.out.println("Replication degree must be between 1 and 9");
		
		String filenameWithPath = "../Peer" + Peer.getServerId() + "/Files/" + filename;
		new Thread(new Backup(filenameWithPath, replicationDegree)).start();	
	}

	@Override
	public void restore(String filename) throws RemoteException
	{
		String filenameWithPath = "../Peer" + Peer.getServerId() + "/Files/" + filename;
		new Thread(new Restore(filenameWithPath)).start();
	}

	@Override
	public void delete(String filename) throws RemoteException
	{
		String filenameWithPath = "../Peer" + Peer.getServerId() + "/Files/" + filename;
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
		return DM.toString();
	}

	@Override
	public void backupEnhanced(String filename, int replicationDegree) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restoreEnhanced(String filename) throws RemoteException {
		// TODO Auto-generated method stub
		String filenameWithPath = "../Peer" + Peer.getServerId() + "/Files/" + filename;
		new Thread(new RestoreEnhancement(filenameWithPath)).start();
	}

	@Override
	public void deleteEnhanced(String filename) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reclaimEnhanced(long kbytes) throws RemoteException {
		// TODO Auto-generated method stub
		
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
			Peer.getMC().sendPacket(message);
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
		
		String path = "../Peer" + Peer.getServerId() + "/Chunks/" + chunkName;
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
}
