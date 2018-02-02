package proj.header;

public abstract class ClassAHeader extends Header
{
    public final String fileID;

    protected ClassAHeader(String version, int senderID, String fileID)
    {
        super(version, senderID);
        this.fileID = fileID;
    }

    @Override
    public String toString()
    {
        return super.toString() + ' ' + this.fileID;
    }
}
