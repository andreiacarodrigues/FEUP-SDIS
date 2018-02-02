package files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import chunk.Chunk;

public class FileSplitter
{
	private ArrayList<Chunk> chunkList;
	
	public static final int chunkSize = 64000;
	
	private String filename;
	private int replicationDegree;
	private boolean read;
	
	private String fileID;
	
	public FileSplitter(String filename, int replicationDegree)
	{
		this.chunkList = new ArrayList<Chunk> ();
		this.filename = filename;
		this.replicationDegree = replicationDegree;
		this.read = false;
		splitFile();
	}
	
	private void splitFile()
	{	
		byte[] buffer = new byte[chunkSize];
		File file = new File(filename);	
		
		if(file.length() > 1000000)// cant have more than one million chunks
		{
			System.out.println("File too big");
			return;
		}
		
		FileIDGenerator fid = new FileIDGenerator(filename);	
		fileID = fid.getHash();
		int chunkNo = 0;
		
		
		try
		{
			BufferedInputStream bufinst = new BufferedInputStream (new FileInputStream(file));
			int tmp = 0;
			boolean mult64 = true;
			while ((tmp = bufinst.read(buffer)) > 0)
			{
				byte[] body = new byte[tmp];
				System.arraycopy(buffer, 0, body, 0, tmp);
				
				Chunk chunk = new Chunk(fileID, chunkNo, replicationDegree, body);
				chunkList.add(chunk);
				chunkNo++;
				if(tmp < 64000)
					mult64 = false;
			}
			if(mult64)
			{
				byte[] empty = new byte[0];
				chunkList.add(new Chunk(fileID, chunkNo, replicationDegree, empty));
			}
			bufinst.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		this.read = true;
	}
	
	public ArrayList<Chunk> getChunkList()
	{
		if(this.read)
		{
			return this.chunkList;
		}
		else
		{
			System.out.println("File hasn't been read yet");
			return null;	
		}
	}
	
	public String getFilename()
	{
		return filename;
	}

	public String getFileID()
	{
		return this.fileID;
	}
}
