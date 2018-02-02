package proj.header.types;

import java.net.InetAddress;

import static proj.Constants.CRLF2;

public class EnhancedGetchunkHeader extends GetchunkHeader
{
    public final String address;
    public final int port;

    public EnhancedGetchunkHeader(GetchunkHeader getchunkHeader, InetAddress address, int port)
    {
        super(getchunkHeader.senderID, getchunkHeader.fileID, getchunkHeader.chunkNumber);
        this.address = address.getHostAddress();
        this.port = port;
    }

    @Override
    public String toString()
    {
        return super.toStringOpen() + address + ' ' + port + CRLF2;
    }
}
