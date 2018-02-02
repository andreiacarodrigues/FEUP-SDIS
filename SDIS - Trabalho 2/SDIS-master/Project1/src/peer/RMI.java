package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote
{
	void backup(String filename, int replicationDegree) throws RemoteException;

	void backupEnhanced(String filename, int replicationDegree) throws RemoteException;
	
	void restore(String filename) throws RemoteException;
	
	void restoreEnhanced(String filename) throws RemoteException;

	void delete(String filename) throws RemoteException;

	void deleteEnhanced(String filename) throws RemoteException;
	
	void reclaim(long kbytes) throws RemoteException;
	
	void reclaimEnhanced(long kbytes) throws RemoteException;
	
	String state() throws RemoteException;
}
