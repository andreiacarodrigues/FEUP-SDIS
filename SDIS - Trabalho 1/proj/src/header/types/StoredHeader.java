package proj.header.types;

import proj.header.ClassBHeader;

import static proj.Constants.CRLF2;

public class StoredHeader extends ClassBHeader
{
    public StoredHeader(int id, String fileID, int chunkNumber)
    {
        super("1.0", id, fileID, chunkNumber);
    }

    public String getProtocol()
    {
        return "STORED";
    }

    @Override
    public String toString()
    {
        return super.toString() + CRLF2;
    }
}
