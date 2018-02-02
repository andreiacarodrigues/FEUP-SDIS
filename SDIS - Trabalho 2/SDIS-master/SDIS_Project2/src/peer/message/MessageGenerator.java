package peer.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import peer.main.Peer;

public class MessageGenerator
{
	public static final String CRLF = "\r\n";
	
	public static byte[] generatePUTCHUNK(Chunk chunk)
	{
		String header = "PUTCHUNK";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + chunk.getFileId();
		header += " " + chunk.getChunkNo();
		header += " " + chunk.getReplicationDegree();
		header += " " + CRLF + CRLF;
		
		try
		{
			return appendBytes(header.getBytes("ASCII"), chunk.getBody());
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MDB
	}

	public static byte[] generateSTORED(String fileID, String chunkNo)
	{
		String header = "STORED";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + fileID;
		header += " " + chunkNo;
		header += " " + CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MC
	}

	public static byte[] generateGETCHUNK(String fileId, Integer chunkNo)
	{
		String header = "GETCHUNK";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + fileId;
		header += " " + chunkNo;
		header += " " + CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MC
	}

	public static byte[] generateCHUNK(Chunk chunk)
	{
		String header = "CHUNK";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + chunk.getFileId();
		header += " " + chunk.getChunkNo();
		header += " " + CRLF + CRLF;
		
		try
		{
			return appendBytes(header.getBytes("ASCII"), chunk.getBody());
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MDR
	}

	public static byte[] generateDELETE(String fileID)
	{
		String header = "DELETE";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + fileID;
		header += " " + CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MC
	}

	public static byte[] generateREMOVED(String fileId, String chunkNo)
	{
		String header = "REMOVED";
		header += " " + "1.0";
		header += " " + Peer.getPeerID();
		header += " " + fileId;
		header += " " + chunkNo;
		header += " " + CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MC
	}
	
	public static byte[] generateCHECKDELETED(String fileId)
	{
		String header = "CHECKDELETED";
		header += " " + "2.0";
		header += " " + Peer.getPeerID();
		header += " " + fileId;
		header += CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static byte[] generateGETCHUNK(String fileId, Integer chunkNo, String address, Integer port)
	{
		String header = "GETCHUNK";
		header += " " + "2.0";
		header += " " + Peer.getPeerID();
		header += " " + fileId;
		header += " " + chunkNo;
		header += " " + address;
		header += " " + String.valueOf(port);
		header += " " + CRLF + CRLF;
		
		try
		{
			return header.getBytes("ASCII");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return null;
		
		// MC
	}

	public static byte[] appendBytes(byte[] header, byte[] body)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		try
		{
			outputStream.write(header);
			outputStream.write(body);
		}
		catch (IOException e)
		{

			e.printStackTrace();
		}

		return outputStream.toByteArray();
	}
	
}
