package proj.threads;

import proj.Peer;
import proj.file.Chunk;
import proj.header.HeaderFactory;
import proj.header.types.PutchunkHeader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MDBChunkSender extends Thread
{
    private Peer peer;
    private byte[] data;
    private Chunk chunk;

    public MDBChunkSender(Peer peer, byte[] data, Chunk chunk)
    {
        this.peer = peer;
        this.data = data;
        this.chunk = chunk;
    }

    public void run()
    {
        int tries = 5;
        int secondsToWait = 1;
        try
        {
            PutchunkHeader header = HeaderFactory.buildPutchunkHeader(peer.getId(), this.chunk.file.fileID, this.chunk.chunkNumber, this.chunk.file.getReplicationDegree());
            byte[] headerData = header.toString().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer buffer = ByteBuffer.allocate(headerData.length + this.data.length);
            buffer.put(headerData);
            buffer.put(this.data);
            byte[] message = buffer.array();
            while(!(this.chunk.replicationSatisfied()) && (tries > 0))
            {
                this.peer.sendMessageToMDB(message);
                Thread.sleep(secondsToWait*1000);
                secondsToWait *= 2;
                tries--;
            }
            this.peer.releaseSemaphore();
        }
        catch (Exception e)
        {
            return;
        }
    }
}
