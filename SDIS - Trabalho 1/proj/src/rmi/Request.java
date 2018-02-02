package proj.rmi;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Request extends Remote
{
    void backup(String filepath, int replicationDegree, boolean enhancement) throws RemoteException;

    void restore(String filepath, boolean enhancement) throws RemoteException;

    void delete(String filepath, boolean enhancement) throws RemoteException;

    void reclaim(long space, boolean enhancement) throws RemoteException;

    String state() throws RemoteException;
}
