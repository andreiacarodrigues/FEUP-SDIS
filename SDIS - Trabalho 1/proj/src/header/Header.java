package proj.header;

public abstract class Header
{
    public final String version;
    public final int senderID;

    protected Header(String version, int senderID)
    {
        this.version = version;
        this.senderID = senderID;
    }

    @Override
    public String toString()
    {
        return getProtocol() + ' ' + this.version + ' ' + this.senderID;
    }

    public abstract String getProtocol();
}
