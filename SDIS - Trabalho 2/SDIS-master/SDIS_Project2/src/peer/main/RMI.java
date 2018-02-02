package peer.main;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote
{
	public void backup(String filename, int replicationDegree, boolean encrypt) throws RemoteException;

	public void restore(String filename) throws RemoteException;

	public void delete(String filename) throws RemoteException;
	
	public void reclaim(long kbytes) throws RemoteException;
	
	public String state() throws RemoteException;
}
