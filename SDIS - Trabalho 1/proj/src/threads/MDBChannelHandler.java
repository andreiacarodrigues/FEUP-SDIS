package proj.threads;

import proj.Message;
import proj.Peer;
import proj.file.Chunk;
import proj.file.ChunkIdentifier;
import proj.file.File;
import proj.header.Header;
import proj.header.HeaderFactory;
import proj.header.types.PutchunkHeader;
import proj.header.types.StoredHeader;

import java.nio.charset.StandardCharsets;

public class MDBChannelHandler extends Thread
{
    private byte[] data;
    private Peer peer;

    public MDBChannelHandler(Peer peer, byte[] data)
    {
        this.data = data;
        this.peer = peer;
    }

    @Override
    public void run()
    {
        Message message = new Message(data);
        this.data = null;

        Header header = message.getHeader();
        byte[] chunkData = message.getData();

        if((header.senderID == this.peer.getId()))
            return;

        if(!header.version.equals("1.0") && !header.version.equals("1.1"))
            return;

        if(header.version.equals("1.1"))
        {
            if(!this.peer.version.equals("1.1"))
                return;
        }

        if(header instanceof PutchunkHeader)
        {
            PutchunkHeader putchunkHeader = (PutchunkHeader) header;

            if(this.peer.owns(putchunkHeader.fileID))
                return;

            this.peer.getDeleted().remove(putchunkHeader.fileID);
            this.peer.warnAboutPutchunk(new ChunkIdentifier(putchunkHeader.fileID, putchunkHeader.chunkNumber));

            int timeToWait = (int)(Math.random()*400);
            try
            {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            if(this.peer.enhancements)
            {
                File file = this.peer.getFiles().get(putchunkHeader.fileID);

                if(file != null)
                {
                    Chunk chunk = file.getChunk(putchunkHeader.chunkNumber);

                    if(chunk != null)
                    {
                        if(chunk.replicationSatisfied())
                        {
                            return;
                        }
                    }
                }
            }

            this.peer.addChunk(putchunkHeader.fileID, putchunkHeader.chunkNumber, chunkData, putchunkHeader.replicationDegree);
            try
            {
                StoredHeader storedHeader = HeaderFactory.buildStoredHeader(this.peer.getId(), putchunkHeader.fileID, putchunkHeader.chunkNumber);
                this.peer.sendMessageToMC(storedHeader.toString().getBytes(StandardCharsets.US_ASCII));

            } catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }
}
