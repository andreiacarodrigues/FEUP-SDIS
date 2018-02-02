package proj;

import proj.header.Header;
import proj.header.HeaderParser;

import java.util.Arrays;

public class Message
{
    private Header header;
    private byte[] data;

    private static int indexOf(byte[] array, byte[] target)
    {
       outer: for(int i = 0; i < array.length - target.length + 1; i++)
       {
           for(int j = 0; j < target.length; j++)
           {
               if(array[i + j] != target[j])
                    continue outer;
           }
           return i;
       }
       return -1;
    }

    public Message(byte[] message)
    {
        byte[] separator = Constants.CRLF2.getBytes();
        int indexOfSeparator = indexOf(message, separator);
        if(indexOfSeparator == -1)
        {
            //error;
        }

        this.data = Arrays.copyOfRange(message, indexOfSeparator + separator.length, message.length);
        String headerText = new String(message, 0, indexOfSeparator);
        try
        {
            this.header = HeaderParser.parseHeader(headerText);
        } catch (Exception e)
        {

        }
    }

    public Header getHeader()
    {
        return header;
    }

    public void setHeader(Header header)
    {
        this.header = header;
    }

    public byte[] getData()
    {
        return data;
    }

    public void setData(byte[] data)
    {
        this.data = data;
    }
}
