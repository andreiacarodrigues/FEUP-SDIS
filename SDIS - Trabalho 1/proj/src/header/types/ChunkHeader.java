package proj.header.types;

import proj.header.ClassBHeader;

import static proj.Constants.CRLF2;

public class ChunkHeader extends ClassBHeader
{
    public ChunkHeader(String version, int id, String fileID, int chunkNumber) { super(version, id, fileID, chunkNumber); }

    public String getProtocol()
    {
        return "CHUNK";
    }

    @Override
    public String toString()
    {
        return super.toString() + CRLF2;
    }
}

