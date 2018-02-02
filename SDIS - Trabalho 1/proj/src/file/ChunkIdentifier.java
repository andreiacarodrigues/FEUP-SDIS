package proj.file;

public class ChunkIdentifier
{
    private final String fileID;
    private final int chunkNumber;

    public ChunkIdentifier(String fileID, int chunkNumber)
    {
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
    }

    public String getFileID()
    {
        return fileID;
    }

    public int getChunkNumber()
    {
        return chunkNumber;
    }

    @Override
    public int hashCode()
    {
        return 100*fileID.hashCode() + chunkNumber;
    }
}
