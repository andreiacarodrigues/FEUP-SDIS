package peer.protocol;

import java.io.File;

import peer.data.DataManager;
import peer.main.Peer;
import peer.message.MessageGenerator;
import peer.network.SenderSocket.Destination;

public class Delete implements Runnable
{
	private String filename;
	private boolean deleteOwnFile = true;
	
	
	public Delete(String filename)
	{
		this.filename = filename;
	}

	public Delete(String filename, boolean delete) {
		this.filename = filename;
		deleteOwnFile = delete;
	}

	@Override
	public void run()
	{
		DataManager DM = Peer.getDataManager();
		String fileID = DM.getFileId(filename);
		
		if(fileID == null)
		{
			System.out.println("No such file backed up");
			return;
		}
		
		System.out.println(filename);
		File file = new File(filename);
		try
		{
			if(deleteOwnFile) {
				if (file.delete())
					System.out.println("DELETED FILE");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Peer.getDataManager().deleteBackedUpData(filename, fileID);
		
		for(int i = 0; i < 3; i++)
		{
			byte[] message = MessageGenerator.generateDELETE(fileID);
			Peer.getSenderSocket().sendPacket(message, Destination.MC);
		}
	}
	
}
