package rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Rmi
{
	public static void main(String[] args)
	{
		try
		{
			LocateRegistry.createRegistry(1099);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		
		while(true)
		{
			
		}
	}
}