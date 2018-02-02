package proj.file;

import proj.Constants;
import proj.Peer;
import proj.header.types.RemovedHeader;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class File implements Serializable
{
    static final long serialVersionUID = 1L;
    public Peer peer;
    public final String fileID;

    public void setReplicationDegree(int replicationDegree)
    {
        this.replicationDegree = replicationDegree;
    }

    private int replicationDegree;
    private boolean metaExists = false;
    private final HashMap<Integer, Chunk> chunks = new HashMap<>();

    public static File fromFilename(Peer peer, String filename, int replicationDegree) throws IOException, NoSuchAlgorithmException
    {
        java.io.File fileMeta = new java.io.File(filename);

        String fileHash = computeFileHash(fileMeta);

        File file = new File(peer, fileHash, replicationDegree);
        file.createMetaFile(filename, fileHash);

        FileInputStream fis = new FileInputStream(fileMeta);

        peer.addFile(file);

        int index = 0;
        int read;
        int lastRead = 0;
        byte[] buf = new byte[Constants.MAXCHUNKSIZE];
        for(;(read = fis.read(buf)) != -1;index++)
        {
            lastRead = read;
            byte[] chunkData = Arrays.copyOfRange(buf, 0, read);
            Chunk chunk = new Chunk(file, index);
            file.addChunk(chunk, index);
            peer.createMDBChunkSender(chunkData, chunk);
        }
        if(lastRead == Constants.MAXCHUNKSIZE)
        {
            byte[] chunkData = {};
            Chunk chunk = new Chunk(file, index);
            file.addChunk(chunk, index);
            peer.createMDBChunkSender(chunkData, chunk);
        }

        fis.close();
        return file;
    }

    public double reclaimSpace(double space, boolean aggressive)
    {
        synchronized(this.chunks)
        {
            double delta = space;

            for(Chunk chunk : this.chunks.values())
            {
                if(chunk.getActualReplication() > 1 || aggressive)
                {
                    double ret = chunk.deleteFile();
                    if(ret != -1)
                    {
                        delta -= ret/1000.0;
                        RemovedHeader header = new RemovedHeader(this.peer.id, this.fileID, chunk.chunkNumber);
                        this.peer.sendMessageToMC(header.toString().getBytes(StandardCharsets.US_ASCII));
                    }
                    if(delta <= 0)
                    {
                        break;
                    }
                }
            }

            return delta;
        }
    }

    public long delete()
    {
        long spaceDeleted = 0;
        for(Chunk chunk : this.chunks.values())
        {
            long chunkSpaceDeleted = chunk.deleteFile();
            if(chunkSpaceDeleted != -1)
            {
                spaceDeleted += chunkSpaceDeleted;
            }
        }

        return spaceDeleted;
    }

    private void createMetaFile(String filename, String hash)
    {
        OriginalFileMeta ofm = new OriginalFileMeta(hash, filename);
        java.io.File file = new java.io.File(this.peer.metaFolder, hash);
        try
        {
            if(file.createNewFile())
            {
                try(ObjectOutputStream oij = new ObjectOutputStream(new FileOutputStream(file)))
                {
                    oij.writeObject(ofm);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.setMetaExists(true);
    }

    private static String computeFileHash(java.io.File fileMeta) throws NoSuchAlgorithmException
    {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        String message = fileMeta.getAbsolutePath() + fileMeta.getName() + fileMeta.lastModified();

        byte hash[] = sha256.digest(message.getBytes());

        BigInteger bi = new BigInteger(1, hash);
        return String.format("%0" + (hash.length << 1) + "X", bi);
    }

    public int getReplicationDegree()
    {
        return replicationDegree;
    }

    public boolean createFile(int chunkNumber, byte[] data)
    {
        Chunk chunk = chunks.get(chunkNumber);
        return chunk.createFile(data);
    }

    public Chunk getChunk(int chunkNumber)
    {
        return chunks.get(chunkNumber);
    }

    public File(Peer peer, String fileID)
    {
        this(peer, fileID, -1);
    }

    public File(Peer peer, String fileID, int replicationDegree)
    {
        this.peer = peer;
        this.fileID = fileID;
        this.replicationDegree = replicationDegree;
    }

    public String getFileID()
    {
        return fileID;
    }

    public void addChunk(Chunk data, int chunkNumber)
    {
        chunks.put(chunkNumber, data);
    }

    public void addHasChunk(int chunkNumber, int peerID)
    {
        Chunk chunk = chunks.get(chunkNumber);

        if(chunk == null)
        {
            chunk = new Chunk(this, chunkNumber);
            this.addChunk(chunk, chunkNumber);
        }

        chunk.addHasChunk(peerID);
    }

    public boolean addChunk(int chunkNumber, byte[] data)
    {
        chunks.computeIfAbsent(chunkNumber, f -> new Chunk(this, chunkNumber));
        return createFile(chunkNumber, data);
    }

    public boolean isComplete()
    {
        for(int i = 0; i < 65536; i++)
        {
            Chunk chunk = chunks.get(i);

            if(chunk == null)
                return false;

            if(!chunk.fileExists())
                return false;

            if(chunk.isLast())
                return true;
        }
        return false;
    }

    public boolean hasChunk(int number)
    {
        return chunks.get(number) != null;
    }

    public Collection<Chunk> getChunks()
    {
        return chunks.values();
    }

    public boolean metaExists()
    {
        return metaExists;
    }

    public void setMetaExists(boolean metaExists)
    {
        this.metaExists = metaExists;
    }

    public Peer getPeer()
    {
        return peer;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\t\tFile Identifier: ").append(fileID).append('\n')
                .append("\t\t\tDesired Replication: ").append(replicationDegree).append('\n')
                .append("\t\t\tMetaFile Exists: ").append(metaExists).append('\n')
                .append("\t\t\tChunks Information: \n");

        if(chunks.isEmpty())
        {
            builder.append("\t\t\t\t<None>\n");
        }
        for(Chunk chunk : chunks.values())
        {
            builder.append(chunk);
        }

        return builder.toString();
    }
}
