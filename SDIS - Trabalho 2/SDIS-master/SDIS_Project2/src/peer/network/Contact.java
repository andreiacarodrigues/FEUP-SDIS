package peer.network;

public class Contact
{
	private String hostname;
	private Integer peerID;
	private Integer MCPort;
	private Integer MDBPort;
	private Integer MDRPort;
	private Integer senderPort;
	
	public Contact(String hostname, Integer peerID, Integer MCPort, Integer MDBPort, Integer MDRPort, Integer senderPort)
	{
		this.hostname = hostname;
		this.peerID = peerID;
		this.MCPort = MCPort;
		this.MDBPort = MDBPort;
		this.MDRPort = MDRPort;
		this.senderPort = senderPort;
	}
	
	public String getHostname()
	{
		return hostname;
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
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "Hostname: " + hostname + "\n";
		s += "PeerID: " + peerID + "\n";
		s += "MC: " + MCPort + "\n";
		s += "MDB: " + MDBPort + "\n";
		s += "MDR: " + MDRPort + "\n";
		s += "Sender: " + senderPort + "\n";
		
		return s;
	}
}
