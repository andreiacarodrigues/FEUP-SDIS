package proj.threads;

import proj.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Arrays;

public class MDRChannelListener extends Thread
{
    private Peer peer;
    private MulticastSocket socket;

    public MDRChannelListener(Peer peer) throws IOException
    {
        this.peer = peer;

        this.socket = this.peer.getMDRSocket();
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
                MDRChannelHandler handler = new MDRChannelHandler(peer, data);
                handler.start();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
