package proj.header.types;

import proj.header.ClassBHeader;

import static proj.Constants.CRLF2;

public class PutchunkHeader extends ClassBHeader
{
    public final int replicationDegree;

    public PutchunkHeader(int id, String fileID, int chunkNumber, int replicationDegree)
    {
        super("1.0", id, fileID, chunkNumber);
        this.replicationDegree = replicationDegree;
    }

    public String getProtocol()
    {
        return "PUTCHUNK";
    }

    @Override
    public String toString()
    {
        return super.toString() + ' ' + this.replicationDegree + CRLF2;
    }
}
