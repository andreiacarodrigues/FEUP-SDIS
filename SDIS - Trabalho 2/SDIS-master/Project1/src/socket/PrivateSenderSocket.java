package socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class PrivateSenderSocket 
{
	String address;
	int port;
	static volatile DatagramSocket socket;
	
	public PrivateSenderSocket()
	{
		try {
			socket = new DatagramSocket();
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void sendMessage(String address, int port, byte[] message)
	{		
		InetAddress inad = null;
		try {
			inad = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DatagramPacket packet = new DatagramPacket(message, message.length, inad, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
