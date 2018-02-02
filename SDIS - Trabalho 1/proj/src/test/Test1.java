package proj.test;

import proj.header.HeaderParser;

/**
 * Created by Griffrez on 03/04/2017.
 */
public class Test1
{
    public static void main(String[] args)
    {
        try
        {
            HeaderParser.parseHeader("1 2 3 4 5 6 \r\n 7 8 9 \r\n 10 11 12 13 \r\n\r\n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
