package proj.threads;

import proj.Peer;
import proj.file.Chunk;
import proj.header.HeaderFactory;
import proj.header.types.ChunkHeader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MDRChunkSender extends Thread
{
    private Peer peer;
    private byte[] data;
    private Chunk chunk;

    public MDRChunkSender(Peer peer, byte[] data, Chunk chunk)
    {
        this.peer = peer;
        this.data = data;
        this.chunk = chunk;
    }

    public void run()
    {
        try
        {
            ChunkHeader header = HeaderFactory.buildChunkHeader("1.0", peer.getId(), this.chunk.file.fileID, this.chunk.chunkNumber);
            byte[] headerData = header.toString().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer buffer = ByteBuffer.allocate(headerData.length + this.data.length);
            buffer.put(headerData);
            buffer.put(this.data);
            byte[] message = buffer.array();
            this.peer.sendMessageToMDR(message);
        }
        catch (Exception e)
        {
            return;
        }
    }
}
