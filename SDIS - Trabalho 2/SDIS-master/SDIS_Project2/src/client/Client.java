package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import peer.main.RMI;



public class Client
{
	private static String serviceAccessPoint;
	private static String operation;
	private static ArrayList<String> operands;
	
	public static void main(String[] args)
	{
		if(!initArgs(args))
			return;
		
		try
		{
			Registry registry = LocateRegistry.getRegistry("localhost");
			RMI rmi = (RMI) registry.lookup(serviceAccessPoint);
			executeCommand(rmi);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (NotBoundException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void executeCommand(RMI rmi)
	{
		
		switch(operation)
		{
		case "BACKUP":
			
			if(Integer.valueOf(operands.get(1)) < 1 || Integer.valueOf(operands.get(1)) > 9)
			{
				System.out.println("Replication degree must be between 1 and 9");
				return;
			}
			
			try
			{
				if(Integer.parseInt(operands.get(2)) != 0)
				{
					rmi.backup(operands.get(0), Integer.parseInt(operands.get(1)), true);
				}
				else
				{
					rmi.backup(operands.get(0), Integer.parseInt(operands.get(1)), false);
				}
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			break;
			
		case "DELETE":
			try
			{
				rmi.delete(operands.get(0));
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			break;
			
		case "RECLAIM":
			try
			{
				rmi.reclaim(Long.parseLong(operands.get(0)));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			break;
			
		case "STATE":
			try
			{
				String response = rmi.state();
				System.out.println(response);
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			break;
			
		case "RESTORE":
			try
			{
				rmi.restore(operands.get(0));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			break;	
		
		default:
			System.out.println("Unknown command");
			break;
		}
	}
	
	public static boolean initArgs(String[] args)
	{
		if(args.length < 2 || args.length > 5 || args.length == 4)
		{
			System.out.println("Invalid usage, wrong number of args");
			return false;
		}
		
		serviceAccessPoint = args[0];
		operation = args[1];
		operands = new ArrayList<String>();
		
		if(args.length == 2 && operation.equals("STATE"))
		{
			return true;
		}
		else if(args.length == 3 && !operation.equals("BACKUP") && !operation.equals("STATE"))
		{
			operands.add(args[2]);
			return true;
		}
		else if(args.length == 5 && operation.equals("BACKUP"))
		{
			operands.add(args[2]);
			operands.add(args[3]);
			operands.add(args[4]);
			return true;
		}
		else
		{
			System.out.println("Invalid usage, operands number doesn't match operation");
			return false;
		}
	}
}
