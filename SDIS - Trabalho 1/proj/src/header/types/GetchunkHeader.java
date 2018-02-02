package proj.header.types;

import proj.header.ClassBHeader;

import static proj.Constants.CRLF;
import static proj.Constants.CRLF2;

public class GetchunkHeader extends ClassBHeader
{
    public GetchunkHeader(int id, String fileID, int chunkNumber)
    {
        super("1.0", id, fileID, chunkNumber);
    }

    public String getProtocol()
    {
        return "GETCHUNK";
    }

    @Override
    public String toString()
    {
        return super.toString() + CRLF2;
    }

    public String toStringOpen()
    {
        return super.toString() + CRLF;
    }
}
