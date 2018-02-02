package proj.threads;


import proj.Message;
import proj.Peer;
import proj.file.Chunk;
import proj.file.ChunkIdentifier;
import proj.header.Header;
import proj.header.HeaderFactory;
import proj.header.HeaderParser;
import proj.header.types.*;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class MCChannelHandler extends Thread
{
    private byte[] data;
    private Peer peer;
    private Boolean chunkReceived = false;

    public MCChannelHandler(Peer peer, byte[] data)
    {
        this.data = data;
        this.peer = peer;
    }

    public void run()
    {
        try
        {
            Message message = new Message(data);

            Header header = message.getHeader();

            if((header.senderID == this.peer.getId()))
                return;

            if(!header.version.equals("1.0") && !header.version.equals("1.1"))
                return;

            if(header.version.equals("1.1"))
            {
                if(!this.peer.version.equals("1.1"))
                    return;
            }

            if(header instanceof StoredHeader)
            {
                StoredHeader storedHeader = (StoredHeader) header;
                peer.addStored(storedHeader.senderID, storedHeader.fileID, storedHeader.chunkNumber);
            }
            else if(header instanceof GetchunkHeader)
            {
                int timeToWait = (int) (Math.random() * 400);
                this.peer.registerToWarnForChunks(this);
                try
                {
                    Thread.sleep(timeToWait);
                }
                catch (InterruptedException e)
                {
                    // TODO
                }

                if (!this.chunkReceived)
                {
                    if (this.peer.enhancements && header instanceof EnhancedGetchunkHeader)
                    {
                        EnhancedGetchunkHeader getchunkHeader = (EnhancedGetchunkHeader) header;

                        InetAddress address = InetAddress.getByName(getchunkHeader.address);

                        this.peer.sendChunk(getchunkHeader.fileID, getchunkHeader.chunkNumber, address, getchunkHeader.port);
                    }
                    else
                    {
                        GetchunkHeader getchunkHeader = (GetchunkHeader) header;

                        peer.sendChunk(getchunkHeader.fileID, getchunkHeader.chunkNumber);
                    }
                }
            }
            else if(header instanceof DeleteHeader)
            {
                DeleteHeader deleteHeader = (DeleteHeader) header;
                this.peer.deleteChunks(deleteHeader.fileID);
            }
            else if(header instanceof RemovedHeader)
            {
                RemovedHeader removedHeader = (RemovedHeader) header;
                Chunk chunk = this.peer.getFiles().get(removedHeader.fileID).getChunk(removedHeader.chunkNumber);
                chunk.decrementActualReplication();
                if(!chunk.replicationSatisfied() && chunk.fileExists())
                {
                    int timeToWait = (int) (Math.random() * 400);
                    ChunkIdentifier chunkID = new ChunkIdentifier(removedHeader.fileID, removedHeader.chunkNumber);
                    this.peer.registerToWarnForPutchunks(chunkID, this);
                    try
                    {
                        Thread.sleep(timeToWait);
                    }
                    catch (InterruptedException e){}
                    this.peer.unregisterToWarnForPutchunks(chunkID, this);
                    if (!this.chunkReceived)
                        peer.createMDBChunkSender(chunk);
                }
            }
            else if(this.peer.enhancements)
            {
                if(header instanceof FilesHeader)
                {
                    ByteArrayInputStream bais = new ByteArrayInputStream(message.getData());
                    byte[] buf = new byte[64];

                    while(bais.read(buf, 0, 64) == 64)
                    {
                        String fileID = new String(buf, StandardCharsets.US_ASCII);
                        if(this.peer.getDeleted().contains(fileID))
                        {
                            DeleteHeader deleteHeader = HeaderFactory.buildDeleteHeader(this.peer.id, fileID);
                            this.peer.sendMessageToMC(deleteHeader.toString().getBytes(StandardCharsets.US_ASCII));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void chunkFound()
    {
        chunkReceived = true;
    }
}
