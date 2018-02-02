package proj.header.types;

import proj.header.Header;

import static proj.Constants.CRLF2;

public class FilesHeader extends Header
{
    public FilesHeader(int id) { super("1.1", id); }

    public String getProtocol()
    {
        return "FILES";
    }

    @Override
    public String toString()
    {
        return super.toString() + CRLF2;
    }
}
