package proj.threads;

import proj.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class RestoreEnhancementListener extends Thread
{
    private Peer peer;
    private DatagramSocket socket;

    public RestoreEnhancementListener(Peer peer, DatagramSocket socket)
    {
        this.peer = peer;

        this.socket = socket;
    }

    public void run()
    {
        byte[] buf = new byte[65536];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        while(true)
        {
            try
            {
                socket.receive(dp);
                byte[] data = Arrays.copyOfRange(dp.getData(), 0, dp.getLength());
                RestoreEnhancementHandler handler = new RestoreEnhancementHandler(peer, data);
                handler.start();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
