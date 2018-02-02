package proj.threads;

import proj.Message;
import proj.Peer;
import proj.file.ChunkIdentifier;
import proj.header.Header;
import proj.header.types.ChunkHeader;

public class RestoreEnhancementHandler extends Thread
{
    private byte[] data;
    private Peer peer;

    public RestoreEnhancementHandler(Peer peer, byte[] data)
    {
        this.data = data;
        this.peer = peer;
    }

    public void run()
    {
        Message message = new Message(data);
        this.data = null;

        Header header = message.getHeader();
        byte[] chunkData = message.getData();

        if((header.senderID == this.peer.getId()))
            return;

        if(!header.version.equals("1.1") || !this.peer.version.equals("1.1"))
            return;

        if(header instanceof ChunkHeader)
        {
            ChunkHeader chunkHeader = (ChunkHeader) header;
            ChunkIdentifier currentWantedChunk = this.peer.getListenForInMDR();
            if((chunkHeader.fileID.equals(currentWantedChunk.getFileID()))
                    && (chunkHeader.chunkNumber == currentWantedChunk.getChunkNumber()))
            {
                this.peer.setChunkData(chunkData);
                this.peer.getChunkDataAvailable().release();
            }
        }
    }
}
