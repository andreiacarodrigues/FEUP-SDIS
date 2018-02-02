package proj.file;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Chunk implements Serializable
{
    static final long serialVersionUID = 1L;
    //region Private Chunk

    private final Set<Integer> hasChunk = new HashSet<>();

    private boolean fileExists = false;

    private int actualReplication = 0;

    private boolean last;

    //endregion

    //region Public Chunk

    //region Public Attributes Chunk

    public final File file;

    public final int chunkNumber;

    //endregion

    //region Constructor Chunk

    public Chunk(File file, int chunkNumber)
    {
        this(file, chunkNumber, false);
    }

    public Chunk(File file, int chunkNumber, boolean last)
    {
        this.file = file;
        this.chunkNumber = chunkNumber;
        this.last = last;
    }

    //endregion

    //region General Chunk

    public void incrementActualReplication()
    {
        actualReplication++;
    }

    public void decrementActualReplication()
    {
        actualReplication--;
    }

    public byte[] getData()
    {
        java.io.File file = new java.io.File(this.file.peer.chunksFolder, this.file.getFileID() + '_' + this.chunkNumber);
        try (FileInputStream fis = new FileInputStream(file))
        {
            byte[] buf = new byte[64000];
            int read = fis.read(buf);
            return Arrays.copyOf(buf, read);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void addHasChunk(int peerID)
    {
        boolean added = hasChunk.add(peerID);
        if (added)
        {
            System.out.println("Hello! " + this.chunkNumber + ' ' + this.file.fileID + ' ' + this.file.peer.id);
            this.incrementActualReplication();
        }
    }

    public void removeHasChunk(int peerID)
    {
        boolean delete = hasChunk.remove(peerID);
        if(delete)
        {
            this.decrementActualReplication();
        }
    }

    public long deleteFile()
    {
        java.io.File file = new java.io.File(this.file.peer.chunksFolder, this.file.getFileID() + '_' + chunkNumber);
        if (fileExists)
        {
            long size = file.length();
            if (file.delete())
            {
                this.fileExists(false);
                this.decrementActualReplication();
                return size;
            }
        }
        return -1;
    }

    public boolean createFile(byte[] data)
    {
        if (!fileExists)
        {
            java.io.File file = new java.io.File(this.file.peer.chunksFolder, this.file.getFileID() + '_' + chunkNumber);

            boolean created;

            try
            {
                created = file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return false;
            }

            if (created)
            {
                try
                {
                    FileOutputStream os = new FileOutputStream(file);
                    os.write(data);
                    os.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return false;
                }
            }

            this.fileExists(true);
            this.incrementActualReplication();

            return true;
        }

        return false;
    }

    public boolean replicationSatisfied()
    {
        return (actualReplication >= file.getReplicationDegree());
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\t\t\t\tChunk Number: ").append(chunkNumber).append('\n')
                .append("\t\t\t\t\tAchieved Replication Degree: ").append(actualReplication).append('\n')
                .append("\t\t\t\t\tData File Exists: ").append(fileExists).append('\n')
                .append("\t\t\t\t\tOther Peers That Have This Chunk: \n");

        if(hasChunk.isEmpty())
        {
            builder.append("\t\t\t\t\t\t<None>\n");
        }
        for (Integer peerID : hasChunk)
        {
            builder.append("\t\t\t\t\t\t").append(peerID).append('\n');
        }

        return builder.toString();
    }

    //endregion

    //region Getters Chunk

    public boolean fileExists()
    {
        return fileExists;
    }

    public int getActualReplication()
    {
        return actualReplication;
    }

    public boolean isLast()
    {
        return last;
    }

    //endregion

    //region Setters Chunk

    public void fileExists(boolean fileExists)
    {
        this.fileExists = fileExists;
    }

    public void setLast()
    {
        this.last = true;
    }

    //endregion





    //endregion
}
