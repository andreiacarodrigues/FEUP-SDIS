package proj.header.types;

import proj.header.ClassAHeader;
import proj.header.Header;

import static proj.Constants.CRLF2;

public class DeleteHeader extends ClassAHeader
{
    public DeleteHeader(int id, String fileID) { super("1.0", id, fileID); }

    public String getProtocol()
    {
        return "DELETE";
    }

    @Override
    public String toString()
    {
        return super.toString() + CRLF2;
    }
}
