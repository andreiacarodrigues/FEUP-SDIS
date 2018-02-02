package proj.client;

import proj.rmi.Request;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client
{
    private static void printUsage()
    {
        System.out.println(
                            "Usage:\n " +
                            "java proj.client.Client <RMI obj name> backup[enh] <filepath> <replication degree>\n" +
                            "java proj.client.Client <RMI obj name> restore[enh] <filepath>\n" +
                            "java proj.client.Client <RMI obj name> delete[enh] <filepath>\n" +
                            "java proj.client.Client <RMI obj name> reclaim[enh] <space to reclaim (in kB)>\n" +
                            "java proj.client.Client <RMI obj name> state"
                            );
    }

    public static void main(String[] args)
    {
        if(args.length > 4 || args.length < 2)
        {
            printUsage();
            return;
        }

        try
        {
            Registry registry = LocateRegistry.getRegistry();
            Request request = (Request) registry.lookup(args[0]);
            String tlcProto = args[1].toLowerCase();
            switch(tlcProto)
            {
                case "backup":
                case "backupenh":
                    int replicationDegree = Integer.parseInt(args[3]);
                    request.backup(args[2], replicationDegree, tlcProto.equals("backupenh"));
                    break;
                case "restore":
                case "restoreenh":
                    request.restore(args[2], tlcProto.equals("restoreenh"));
                    break;
                case "delete":
                case "deleteenh":
                    request.delete(args[2], tlcProto.equals("deleteenh"));
                    break;
                case "reclaim":
                case "reclaimenh":
                    long spaceToReclaim = Integer.parseInt(args[2]);
                    request.reclaim(spaceToReclaim, tlcProto.equals("reclaimenh"));
                    break;
                case "state":
                    System.out.println(request.state());
                    break;
                default:
                    System.out.println("Unrecognized command.");
                    return;
            }
        }
        catch (RemoteException e)
        {
            System.err.println("RMI Registry not found.");
        }
        catch (NotBoundException e)
        {
            System.err.println("RMI Object name not bound.");
        }
    }
}
