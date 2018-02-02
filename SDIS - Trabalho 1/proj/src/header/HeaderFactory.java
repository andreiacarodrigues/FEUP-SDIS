package proj.header;

import proj.header.types.*;

import java.net.InetAddress;

public abstract class HeaderFactory
{
    public static PutchunkHeader buildPutchunkHeader(int peerID, String fileID, int chunkNumber, int replicationDegree) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        if (replicationDegree > 9)
        {
            throw new Exception("Replication Degree too high.");
        }

        return new PutchunkHeader(peerID, fileID, chunkNumber, replicationDegree);
    }

    public static StoredHeader buildStoredHeader(int peerID, String fileID, int chunkNumber) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        return new StoredHeader(peerID, fileID, chunkNumber);
    }

    public static GetchunkHeader buildGetchunkHeader(int peerID, String fileID, int chunkNumber) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        return new GetchunkHeader(peerID, fileID, chunkNumber);
    }

    public static EnhancedGetchunkHeader buildEnhancedGetchunkHeader(int peerID, String fileID, int chunkNumber, InetAddress address, int port) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        GetchunkHeader getchunkHeader = new GetchunkHeader(peerID, fileID, chunkNumber);

        return new EnhancedGetchunkHeader(getchunkHeader, address, port);
    }

    public static FilesHeader buildFilesHeader(int peerID)
    {
        return new FilesHeader(peerID);
    }

    public static ChunkHeader buildChunkHeader(String version,int peerID, String fileID, int chunkNumber) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        return new ChunkHeader(version, peerID, fileID, chunkNumber);
    }

    public static DeleteHeader buildDeleteHeader(int peerID, String fileID)
    {
        return new DeleteHeader(peerID, fileID);
    }

    public static RemovedHeader buildRemovedHeader(int peerID, String fileID, int chunkNumber) throws Exception
    {
        if (chunkNumber >= 1e6)
        {
            throw new Exception("Chunk Number too high.");
        }

        return new RemovedHeader(peerID, fileID, chunkNumber);
    }
}
