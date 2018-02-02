package peer.network;

import java.io.*;
import java.net.*;
import java.util.*;

import peer.main.Peer;

public class SenderSocket
{
	private volatile DatagramSocket socket;
	private volatile ArrayList<Contact> contactList;
	private volatile boolean contactListUpdated = true;
	
	public enum Destination {MC, MDB, MDR};
	
	public SenderSocket()
	{
		contactList = new ArrayList<Contact>();
		
		try
		{
			socket = new DatagramSocket();
		}
		catch (IOException e)
		{
			System.out.println("Error opening sender socket");
		}
	}
	
	public synchronized void sendPacket(byte[] buf, Destination dest)
	{	
		contactListUpdated = false;
		
		contactList = new ArrayList<Contact>();
		Peer.getSocketListener().sendMessage("GetPeers");
		
		boolean waiting = true;
		while(waiting)
		{
			waiting = !contactListUpdated;
		}
			
		System.out.println("ContactList Updated");
		
		for(Contact contact : contactList)
		{
			/* Don't send messages to self */
			if(contact.getPeerID() == Peer.getPeerID())
				continue;
			
			String hostnameNoPort = contact.getHostname().split(":")[0];
			
			InetAddress address = null;
			try
			{	
				address = InetAddress.getByName(hostnameNoPort);
			}
			catch (UnknownHostException e1)
			{
				System.out.println("Unknown Host Exception at Sender Socket");
			}
			
			int port = 0;
			switch(dest)
			{
			case MC:
				port = contact.getMCPort();
				break;
				
			case MDB:
				port = contact.getMDBPort();
				break;
				
			case MDR:
				port = contact.getMDRPort();
				break;
						
			default:
				
				System.out.println("Unknown destination in sender socket");
				return;	
			}
			
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
			
			try
			{
				socket.send(packet);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}	
		}
	}
	
	public void sendMessage(String address, int port, byte[] message)
	{		
		InetAddress inad = null;
		try
		{
			inad = InetAddress.getByName(address);
		}
		catch (UnknownHostException e)
		{
			System.out.println("Error getting address at send direct message");
		}

		DatagramPacket packet = new DatagramPacket(message, message.length, inad, port);
		try
		{
			socket.send(packet);
		}
		catch (IOException e)
		{
			System.out.println("Couldn't send direct packet");
		}
	}
	
	public void setContactListUpdated()
	{
		contactListUpdated = true;
	}
	
	public void addContact(String hostname, Integer peerID, Integer MCPort, Integer MDBPort, Integer MDRPort, Integer senderPort)
	{
		if(peerID.equals(Peer.getPeerID()))
			return;
		
		Contact contact = new Contact(hostname, peerID, MCPort, MDBPort, MDRPort, senderPort);
		contactList.add(contact);
	}
	
	public DatagramSocket getSocket()
	{
		return socket;
	}
}
