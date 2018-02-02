package server.logic;

import javax.net.ssl.SSLSocket;

public class PeerChannel
{
	private SSLSocket socket;
	
	private Integer peerID;
	private Integer MCPort;
	private Integer MDBPort;
	private Integer MDRPort;
	private Integer senderPort;
	
	public PeerChannel(SSLSocket socket)
	{
		this.socket = socket;
	}
	
	public void setInfo(Integer peerID, Integer MCPort, Integer MDBPort, Integer MDRPort, Integer senderPort)
	{
		this.peerID = peerID;
		this.MCPort = MCPort;
		this.MDBPort = MDBPort;
		this.MDRPort = MDRPort;
		this.senderPort = senderPort;
	}
	
	public SSLSocket getSSLSocket()
	{
		return socket;
	}
	
	public Integer getPeerID()
	{
		return peerID;
	}
	
	public Integer getMCPort()
	{
		return MCPort;
	}
	
	public Integer getMDBPort()
	{
		return MDBPort;
	}
	
	public Integer getMDRPort()
	{
		return MDRPort;
	}
	
	public Integer getSenderPort()
	{
		return senderPort;
	}
}
