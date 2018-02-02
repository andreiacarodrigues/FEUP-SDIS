package proj;

import proj.file.Chunk;
import proj.file.ChunkIdentifier;
import proj.file.File;
import proj.file.OriginalFileMeta;
import proj.header.types.DeleteHeader;
import proj.header.types.FilesHeader;
import proj.header.types.GetchunkHeader;
import proj.header.HeaderFactory;
import proj.header.types.ChunkHeader;
import proj.rmi.Request;
import proj.threads.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Peer implements Request, Serializable
{
    static final long serialVersionUID = 1L;
    //region Private proj.Peer

    private class PeerMetaData implements Serializable
    {
        static final long serialVersionUID = 1L;
        //region Private PeerMetaData

        private long availableSpace = (int)1e6;
        private double occupiedSpace = 0;
        private final HashMap<String, File> files = new HashMap<>();
        private final Set<String> deleted = new HashSet<>();

        //endregion

        //region Public PeerMetaData

        public long getAvailableSpace()
        {
            return availableSpace;
        }

        public boolean canAddToOccupiedSpace(long bytes)
        {
            double kiloBytes = bytes/1000.0;
            double newOccupiedSpace = kiloBytes + this.occupiedSpace;

            if(newOccupiedSpace <= this.availableSpace)
            {
                return true;
            }

            return false;
        }

        public boolean addToOccupiedSpace(long bytes)
        {
            double kiloBytes = bytes/1000.0;
            double newOccupiedSpace = kiloBytes + this.occupiedSpace;

            if(newOccupiedSpace <= this.availableSpace)
            {
                this.occupiedSpace = newOccupiedSpace;
                return true;
            }

            return false;
        }

        public void removeFromOccupiedSpace(long bytes)
        {
            double kiloBytes = bytes/1000.0;
            double newOccupiedSpace = this.occupiedSpace - kiloBytes;

            if(newOccupiedSpace >= 0)
            {
                occupiedSpace = newOccupiedSpace;
                return;
            }

            //TODO ERROR
        }

        public HashMap<String, File> getFiles()
        {
            return files;
        }

        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder
                    .append("Peer Data:\n")
                    .append("\tAvailable Space: ").append(availableSpace).append("kB\n")
                    .append("\tOccupied Space: ").append(occupiedSpace).append("kB\n")
                    .append("\tChunks Information: \n");

            if(files.isEmpty())
            {
                builder.append("\t\t<None>\n");
            }
            for(File file : files.values())
            {
                builder.append(file);
            }

            return builder.toString();
        }

        //endregion
    }

    private void createDirectories()
    {
        peerFolder.mkdir();
        chunksFolder.mkdir();
        metaFolder.mkdir();
    }

    public transient final int id;
    public transient final boolean enhancements;
    private transient final PeerMetaData data;
    public transient final String version;

    public transient final java.io.File peerFolder;
    public transient final java.io.File metaFolder;
    public transient final java.io.File chunksFolder;

    private transient final Semaphore sendChunkThreads = new Semaphore(30000);

    private transient final Lock restoreProtocol = new ReentrantLock();
    private transient ChunkIdentifier listenForInMDR = null;
    private transient final Semaphore chunkDataAvailable = new Semaphore(0);
    private transient byte[] chunkData = null;
    private transient final Set<MCChannelHandler> warnAboutChunk = new HashSet<>();

    private transient final Map<ChunkIdentifier, Set<MCChannelHandler>> warnAboutPutchunk = new HashMap<>();

    public transient final InetAddress MCAddress;
    public transient final int MCPort;
    public transient final InetAddress MDBAddress;
    public transient final int MDBPort;
    public transient final InetAddress MDRAddress;
    public transient final int MDRPort;

    private transient final MulticastSocket MCSocket;
    private transient final MulticastSocket MDBSocket;
    private transient final MulticastSocket MDRSocket;
    private transient final DatagramSocket outputSocket;
    private transient final DatagramSocket restoreEnhancementSocket;

    private transient final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    //endregion

    //region Public proj.Peer

    //region Constructor proj.Peer

    public Peer(String version, int id, String accessPoint, InetAddress MCAddress, int MCPort, InetAddress MDBAddress, int MDBPort, InetAddress MDRAddress, int MDRPort) throws IOException
    {
        this.id = id;
        this.version = version;

        if(version.equals("1.1"))
        {
            this.enhancements = true;
        }
        else
        {
            this.enhancements = false;
        }

        this.peerFolder = new java.io.File("Peer" + this.id + '/');
        this.metaFolder = new java.io.File(this.peerFolder, "Meta/");
        this.chunksFolder = new java.io.File(this.peerFolder, "Chunks/");

        createDirectories();

        PeerMetaData data = getPeerMeta();
        if(data == null)
        {
            this.data = new PeerMetaData();
        }
        else
        {
            for(File file : data.files.values())
            {
                file.peer = this;
            }
            this.data = data;
        }

        final Runnable saver = this::savePeerMeta;
        scheduler.scheduleAtFixedRate(saver, 0, 1, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(saver));

        if(this.enhancements)
        {
            final Runnable chunkChecker = this::sendUnsatisfiedChunks;
            scheduler.scheduleWithFixedDelay(chunkChecker, 1, 1, TimeUnit.MINUTES);
            this.restoreEnhancementSocket = new DatagramSocket();
        }
        else
        {
            this.restoreEnhancementSocket = null;
        }


        Request stub = (Request) UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(accessPoint, stub);

        this.MCAddress = MCAddress;
        this.MCPort = MCPort;

        this.MDBAddress = MDBAddress;
        this.MDBPort = MDBPort;

        this.MDRAddress = MDRAddress;
        this.MDRPort = MDRPort;

        this.MCSocket = new MulticastSocket(MCPort);
        this.MCSocket.joinGroup(MCAddress);

        this.MDBSocket = new MulticastSocket(MDBPort);
        this.MDBSocket.joinGroup(MDBAddress);

        this.MDRSocket = new MulticastSocket(MDRPort);
        this.MDRSocket.joinGroup(MDRAddress);

        this.outputSocket = new DatagramSocket();

        new MCChannelListener(this).start();
        new MDBChannelListener(this).start();
        new MDRChannelListener(this).start();

        if(this.enhancements)
        {
            this.sendFilesMessage();
            new RestoreEnhancementListener(this, restoreEnhancementSocket).start();
        }
    }

    //endregion

    //region Request Interface Implementation proj.Peer

    public void backup(String filename, int replicationDegree, boolean enhancement)
    {
        if(enhancement & !this.enhancements)
        {
            //TODO ERROR
            return;
        }
        backupFileFromFilename(filename, replicationDegree);
    }

    public void restore(String filename, boolean enhancement)
    {
        if(enhancement & !this.enhancements)
        {
            //TODO ERROR
        }
        //TODO Restore RMI Call
        this.restoreProtocol.lock();
        checkIfFileAlreadyExists(filename);
        String hash = getOriginalFileHash(filename);
        if(hash != null)
            restoreFile(filename, hash);
        this.restoreProtocol.unlock();
    }

    public void delete(String filename, boolean enhancement)
    {
        if(enhancement & !this.enhancements)
        {
            //TODO ERROR
        }
        String hash = getOriginalFileHash(filename);
        if(hash != null)
        {
            deleteFile(hash);
            this.data.deleted.add(hash);
        }
    }

    public void reclaim(long space, boolean enhancement)
    {
        if(enhancement & !this.enhancements)
        {
            //TODO ERROR
        }
        if(!this.removeFromAvailableSpace(space))
        {

        }
    }

    public String state()
    {
        return this.data.toString();
    }

    //endregion

    //region General proj.Peer

    public static void main(String[] args) throws IOException, InterruptedException
    {
        if (args.length != 9 || (!args[0].equals("1.0") && !args[0].equals("1.1"))) {
            System.out.println("Usage: java proj.Peer <protocolVersion> <peerID> <accessPoint> <MC hostname/address> <MC port> <MDB hostname/address> <MDB port> <MDR hostname/address> <MDR port>\nAvailable versions: 1.0 (w/o enhancements) and 1.1 (w/ enhancements)");
            return;
        }

        int id = Integer.parseInt(args[1]);

        InetAddress MCHostname = InetAddress.getByName(args[3]);
        int MCPort = Integer.parseInt(args[4]);

        InetAddress MDBHostname = InetAddress.getByName(args[5]);
        int MDBPort = Integer.parseInt(args[6]);

        InetAddress MDRHostname = InetAddress.getByName(args[7]);
        int MDRPort = Integer.parseInt(args[8]);

        Peer peer = new Peer(args[0], id, args[2], MCHostname, MCPort, MDBHostname, MDBPort, MDRHostname, MDRPort);
    }

    public void sendFilesMessage()
    {
        FilesHeader header = HeaderFactory.buildFilesHeader(this.id);
        byte[] headerData = header.toString().getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try
        {
            stream.write(headerData);
            for(File file : this.data.files.values())
            {
                stream.write(file.fileID.getBytes(StandardCharsets.US_ASCII));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        byte[] message = stream.toByteArray();
        this.sendMessageToMC(message);
    }

    public void registerToWarnForPutchunks(ChunkIdentifier chunkID, MCChannelHandler handler)
    {
       Set<MCChannelHandler> set = warnAboutPutchunk.computeIfAbsent(chunkID, f -> new HashSet<MCChannelHandler>());

       set.add(handler);
    }

    public void unregisterToWarnForPutchunks(ChunkIdentifier chunkID, MCChannelHandler handler)
    {
        Set<MCChannelHandler> set = warnAboutPutchunk.get(chunkID);

        if(set != null)
        {
            set.remove(handler);
        }
    }

    public void warnAboutPutchunk(ChunkIdentifier chunkID)
    {
        Set<MCChannelHandler> set = warnAboutPutchunk.get(chunkID);

        if(set != null)
        {
            for(MCChannelHandler handler : set)
            {
                handler.chunkFound();
            }
        }

        warnAboutPutchunk.remove(chunkID);
    }

    public boolean removeFromAvailableSpace(long space)
    {
        synchronized(this.data)
        {
            long newAvailableSpace = this.data.availableSpace - space;

            if (newAvailableSpace < 0)
            {
                return false;
            }

            this.data.availableSpace = newAvailableSpace;

            if (newAvailableSpace < this.data.occupiedSpace)
            {
                double delta = this.data.occupiedSpace - newAvailableSpace;

                for (File file : this.data.files.values())
                {
                    double ret = file.reclaimSpace(delta, false);
                    delta -= ret;
                    this.removeOccupiedSpace((long)(ret*1000));
                    if (delta <= 0)
                        break;
                }

                if (delta > 0)
                {
                    for (File file : this.data.files.values())
                    {
                        double ret = file.reclaimSpace(delta, true);
                        delta -= ret;
                        this.removeOccupiedSpace((long)(ret*1000));
                        if (delta <= 0)
                            break;
                    }
                }

                if(delta > 0)
                {
                    //TODO Error, delta can't be greater than zero at this stage
                }


            }
        }

        return true;
    }

    public boolean owns(String fileID)
    {
        File file = this.data.files.get(fileID);

        if(file == null)
            return false;

        return file.metaExists();
    }

    public boolean addOccupiedSpace(long space)
    {
        return this.data.addToOccupiedSpace(space);
    }

    public void removeOccupiedSpace(long space)
    {
        this.data.removeFromOccupiedSpace(space);
    }

    public void savePeerMeta()
    {
        java.io.File metaFile = new java.io.File(this.peerFolder, "metaFile");
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(metaFile));
            oos.writeObject(this.data);
            oos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendUnsatisfiedChunks()
    {
        for(File file : data.files.values())
        {
            for(Chunk chunk : file.getChunks())
            {
                if(!chunk.replicationSatisfied() && chunk.fileExists())
                {
                    createMDBChunkSender(chunk);
                }
            }
        }
    }

    public PeerMetaData getPeerMeta()
    {
        PeerMetaData ret = null;

        java.io.File metaFile = new java.io.File(this.peerFolder, "metaFile");

        if(metaFile.exists())
        {
            try
            {
                ret = (PeerMetaData) new ObjectInputStream(new FileInputStream(metaFile)).readObject();
            }
            catch (IOException | ClassNotFoundException e)
            {
                System.err.println("Peer Metadata failed to load");
            }
        }

        return ret;
    }

    public void registerToWarnForChunks(MCChannelHandler handler)
    {
        synchronized(this.warnAboutChunk)
        {
            this.warnAboutChunk.add(handler);
        }
    }

    public void warnToWarnForChunks()
    {
        synchronized(this.warnAboutChunk)
        {
            for (MCChannelHandler handler : this.warnAboutChunk)
            {
                handler.chunkFound();
            }
            this.warnAboutChunk.clear();
        }
    }

    public String getOriginalFileHash(String filename)
    {
        String hash;

        java.io.File[] files = metaFolder.listFiles();

        if(files == null)
            return null;

        for(java.io.File file : files)
        {
            OriginalFileMeta ofm;
            try
            {
                ofm = (OriginalFileMeta)(new ObjectInputStream(new FileInputStream(file)).readObject());
                if(ofm.getFilePath().equals(filename))
                {
                    return ofm.getFileID();
                }
            }
            catch (IOException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void checkIfFileAlreadyExists(String filename)
    {
        java.io.File file = new java.io.File(filename);
        if(file.exists())
            ;//TODO Error
    }

    public void restoreFile(String filename, String fileID)
    {
        java.io.File file = new java.io.File(filename);
        try
        {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);

            int index = 0;
            do
            {
                this.setListenForInMDR(new ChunkIdentifier(fileID, index));
                int tries = 5;
                int timeToWait = 1;
                boolean gotPermission = false;
                while(!gotPermission && tries > 0)
                {
                    this.sendGetchunkRequest(fileID, index);
                    gotPermission = this.chunkDataAvailable.tryAcquire();
                    Thread.sleep(timeToWait*1000);
                    tries--;
                }
                if(gotPermission)
                {
                    fos.write(this.chunkData);
                    index++;
                }
                else
                {
                    //TODO Error
                }
            } while(this.chunkData.length >= Constants.MAXCHUNKSIZE);

            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void sendGetchunkRequest(String fileID, int chunkNumber)
    {
        try
        {
            GetchunkHeader header;

            if(this.enhancements)
            {
                header = HeaderFactory.buildEnhancedGetchunkHeader(this.id, fileID, chunkNumber, InetAddress.getLocalHost(), this.restoreEnhancementSocket.getLocalPort());
            }
            else
            {
                header = HeaderFactory.buildGetchunkHeader(this.id, fileID, chunkNumber);
            }
            this.sendMessageToMC(header.toString().getBytes(StandardCharsets.US_ASCII));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void deleteFile(String fileID)
    {
        java.io.File metaFile = new java.io.File(metaFolder, fileID);
        metaFile.delete();
        this.data.files.remove(fileID);
        DeleteHeader deleteHeader = HeaderFactory.buildDeleteHeader(this.id, fileID);
        this.sendMessageToMC(deleteHeader.toString().getBytes(StandardCharsets.US_ASCII));
    }

    public void sendMessage(byte[] data, InetAddress address, int port)
    {
        DatagramPacket dp = new DatagramPacket(data, data.length, address, port);
        synchronized(this.outputSocket)
        {
            try
            {
                this.outputSocket.send(dp);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToMDR(byte[] data)
    {
        DatagramPacket dp = new DatagramPacket(data, data.length, this.getMDRAddress(), this.getMDRPort());
        synchronized(this.outputSocket)
        {
            try
            {
                this.outputSocket.send(dp);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToMDB(byte[] data)
    {
        DatagramPacket dp = new DatagramPacket(data, data.length, this.getMDBAddress(), this.getMDBPort());
        synchronized(this.outputSocket)
        {
            try
            {
                this.outputSocket.send(dp);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToMC(byte[] data)
    {
        DatagramPacket dp = new DatagramPacket(data, data.length, this.getMCAddress(), this.getMCPort());
        synchronized(this.outputSocket)
        {
            try
            {
                this.outputSocket.send(dp);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void deleteChunks(String fileID)
    {
        File file = this.data.files.get(fileID);

        if(file == null)
            return;

        long spaceDeleted = file.delete();

        this.removeOccupiedSpace(spaceDeleted);

        this.data.files.remove(fileID, file);
        this.data.deleted.add(fileID);
    }

    public void createMDBChunkSender(byte[] data, Chunk chunk)
    {
        try
        {
            this.sendChunkThreads.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        new MDBChunkSender(this, data, chunk).start();
    }

    public void createMDBChunkSender(Chunk chunk)
    {
        if(!chunk.fileExists())
        {
            return;
        }

        byte[] data = chunk.getData();
        try
        {
            this.sendChunkThreads.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        new MDBChunkSender(this, data, chunk).start();
    }

    public void sendChunk(String fileID, int chunkNumber, InetAddress address, int port)
    {
        File file = this.data.files.get(fileID);

        if(file == null)
            return;

        Chunk chunk = file.getChunk(chunkNumber);

        if(chunk == null)
            return;

        if(!chunk.fileExists())
            return;

        byte[] chunkData = chunk.getData();

        try
        {
            ChunkHeader chunkHeader = HeaderFactory.buildChunkHeader("1.1", this.getId(), fileID, chunkNumber);
            byte[] headerData = chunkHeader.toString().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer buffer = ByteBuffer.allocate(headerData.length + chunkData.length);
            buffer.put(headerData);
            buffer.put(chunkData);
            byte[] message = buffer.array();
            this.sendMessageToMDR(headerData);
            this.sendMessage(message, address, port);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendChunk(String fileID, int chunkNumber)
    {
        File file = this.data.files.get(fileID);

        if(file == null)
            return;

        Chunk chunk = file.getChunk(chunkNumber);

        if(chunk == null)
            return;

        if(!chunk.fileExists())
            return;

        byte[] chunkData = chunk.getData();

        try
        {
            ChunkHeader chunkHeader = HeaderFactory.buildChunkHeader("1.0", this.getId(), fileID, chunkNumber);
            byte[] headerData = chunkHeader.toString().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer buffer = ByteBuffer.allocate(headerData.length + chunkData.length);
            buffer.put(headerData);
            buffer.put(chunkData);
            byte[] message = buffer.array();
            this.sendMessageToMDR(message);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendChunkToSpecificPeer(String fileID, int chunkNumber, InetAddress address, int port)
    {
        File file = this.data.files.get(fileID);

        if(file == null)
            return;

        Chunk chunk = file.getChunk(chunkNumber);

        if(chunk == null)
            return;

        if(!chunk.fileExists())
            return;

        byte[] chunkData = chunk.getData();

        try
        {
            ChunkHeader chunkHeader = HeaderFactory.buildChunkHeader("1.1", this.getId(), fileID, chunkNumber);
            byte[] headerData = chunkHeader.toString().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer buffer = ByteBuffer.allocate(headerData.length + chunkData.length);
            buffer.put(headerData);
            buffer.put(chunkData);
            byte[] message = buffer.array();
            this.sendMessage(message, address, port);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void backupFileToNetwork(File file) throws InterruptedException
    {
        if(!file.isComplete())
        {
            //TODO Error
        }

        Collection<Chunk> chunks = file.getChunks();
        for(Chunk chunk : chunks)
        {
            sendChunkThreads.acquire();
            byte[] chunkData = chunk.getData();
            new MDBChunkSender(this, chunkData, chunk).start();
        }
    }

    public void addFile(File file)
    {
        this.data.files.put(file.fileID, file);
    }

    public void backupFileFromFilename(String filename, int replicationDegree)
    {
        try
        {
            File file = File.fromFilename(this, filename, replicationDegree);
            this.data.files.put(file.fileID, file);
        } catch (IOException | NoSuchAlgorithmException e)
        {
            //
        }
    }

    public void addStored(int peerID, String fileID, int chunkNumber)
    {
        synchronized(this.data)
        {
            this.data.deleted.remove(fileID);
            File file = this.data.files.computeIfAbsent(fileID, func -> new File(this, fileID, -1));
            file.addHasChunk(chunkNumber, peerID);
        }
    }

    public void addChunk(String fileID, int chunkNumber, byte[] data, int replicationDegree)
    {
        long length = data.length;

        synchronized(this.data)
        {
            if(this.data.canAddToOccupiedSpace(length))
            {
                File file = this.data.files.computeIfAbsent(fileID, func -> new File(this, fileID, replicationDegree));
                if(file.getReplicationDegree() == -1)
                {
                    file.setReplicationDegree(replicationDegree);
                }
                if(file.addChunk(chunkNumber, data))
                {
                    this.data.addToOccupiedSpace(length);
                }
            }
        }
    }

    //endregion

    //region Getters Peer

    public Set<String> getDeleted()
    {
        return this.data.deleted;
    }

    public Semaphore getChunkDataAvailable()
    {
        return chunkDataAvailable;
    }

    public byte[] getChunkData()
    {
        return chunkData;
    }

    public int getId()
    {
        return id;
    }

    public HashMap<String, File> getFiles()
    {
        return this.data.getFiles();
    }

    public InetAddress getMCAddress()
    {
        return MCAddress;
    }

    public int getMCPort()
    {
        return MCPort;
    }

    public InetAddress getMDBAddress()
    {
        return MDBAddress;
    }

    public int getMDBPort()
    {
        return MDBPort;
    }

    public MulticastSocket getMCSocket()
    {
        return MCSocket;
    }

    public MulticastSocket getMDBSocket()
    {
        return MDBSocket;
    }

    public void releaseSemaphore()
    {
        this.sendChunkThreads.release();
    }

    public InetAddress getMDRAddress()
    {
        return MDRAddress;
    }

    public int getMDRPort()
    {
        return MDRPort;
    }

    public MulticastSocket getMDRSocket()
    {
        return MDRSocket;
    }

    public String getVersion()
    {
        return version;
    }

    public ChunkIdentifier getListenForInMDR()
    {
        return listenForInMDR;
    }

    //endregion

    //region Setters Peer

    public void setListenForInMDR(ChunkIdentifier listenForInMDR)
    {
        this.listenForInMDR = listenForInMDR;
    }

    public void setChunkData(byte[] chunkData)
    {
        this.chunkData = chunkData;
    }

    //endregion

    //endregion
}
