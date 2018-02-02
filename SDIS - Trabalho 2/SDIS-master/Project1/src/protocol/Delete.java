package protocol;

import java.io.File;

import data.DataManager;
import message.MessageGenerator;
import peer.Peer;

public class Delete implements Runnable
{
	private String filename;
	
	
	public Delete(String filename)
	{
		this.filename = filename;
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
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
			if(file.delete())
				System.out.println("DELETED FILE");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Peer.getDataManager().deleteBackedUpData(filename, fileID);
		
		for(int i = 0; i < 3; i++)
		{
			byte[] message = MessageGenerator.generateDELETE(fileID);
			Peer.getMC().sendPacket(message);
		}
	}
	
}
