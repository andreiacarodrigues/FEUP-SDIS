package proj.header;

public abstract class ClassBHeader extends ClassAHeader
{
    public final int chunkNumber;

    protected ClassBHeader(String version, int id, String fileID, int chunkNumber)
    {
        super(version, id, fileID);
        this.chunkNumber = chunkNumber;
    }

    @Override
    public String toString()
    {
        return super.toString() + ' ' + chunkNumber;
    }
}
