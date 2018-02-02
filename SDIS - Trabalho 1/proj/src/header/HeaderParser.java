package proj.header;

import proj.Constants;

import java.net.InetAddress;

public abstract class HeaderParser
{
    public static Header parseHeader(String headerText) throws Exception
    {
        Header result;

        String str = headerText.trim();
        String[] lines = str.split(Constants.CRLF);
        String[][] arr = new String[lines.length][];
        int index = 0;
        for(String line : lines)
        {
            line = line.trim();
            arr[index] = line.split("(?<=[\\S])[ ]+(?=[\\S])");
            index++;
        }

        switch(arr[0][0])
        {
            case "PUTCHUNK":
                result = processPutchunk(arr);
                break;
            case "STORED":
                result = processStored(arr);
                break;
            case "GETCHUNK":
                result = processGetchunk(arr);
                break;
            case "CHUNK":
                result = processChunk(arr);
                break;
            case "DELETE":
                result = processDeleteChunk(arr);
                break;
            case "REMOVED":
                result = processRemovedChunk(arr);
                break;
            case "FILES":
                result = processFiles(arr);
                break;
            default:
                throw new Exception("Bad Header.");
        }
        return result;
    }

    private static Header processPutchunk(String[][] arr) throws Exception {
        if(arr.length < 1 || arr[0].length != 6){
            throw new Exception("Bad Length PUTCHUNK.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];
        int chunkNo = Integer.parseInt(arr[0][4]);
        int replicationDeg = Integer.parseInt(arr[0][5]);

        return HeaderFactory.buildPutchunkHeader(senderId, fileId, chunkNo, replicationDeg);
    }

    private static Header processStored(String[][] arr) throws Exception {

        if(arr.length < 1 || arr[0].length != 5){
            throw new Exception("Bad Length STORED.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];
        int chunkNo = Integer.parseInt(arr[0][4]);

        return HeaderFactory.buildStoredHeader(senderId, fileId, chunkNo);
    }

    private static Header processGetchunk(String[][] arr) throws Exception {

        if(arr.length < 1 || arr[0].length != 5){
            throw new Exception("Bad Length GETCHUNK.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];
        int chunkNo = Integer.parseInt(arr[0][4]);

        if(arr.length > 1 && arr[1].length == 2)
        {
            InetAddress address = InetAddress.getByName(arr[1][0]);
            int port = Integer.parseInt(arr[1][1]);

            return HeaderFactory.buildEnhancedGetchunkHeader(senderId, fileId, chunkNo, address, port);
        }

        return HeaderFactory.buildGetchunkHeader(senderId, fileId, chunkNo);
    }

    private static Header processFiles(String[][] arr) throws Exception
    {
        if(arr.length < 1 || arr[0].length != 3){
            throw new Exception("Bad Length GETCHUNK.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);

        return HeaderFactory.buildFilesHeader(senderId);
    }

    private static Header processChunk(String[][] arr) throws Exception {

        if(arr.length < 1 || arr[0].length != 5){
            throw new Exception("Bad Length CHUNK.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];
        int chunkNo = Integer.parseInt(arr[0][4]);

        return HeaderFactory.buildChunkHeader(version, senderId, fileId, chunkNo);
    }

    private static Header processDeleteChunk(String[][] arr) throws Exception {

        if(arr.length < 1 || arr[0].length != 4){
            throw new Exception("Bad Length DELETE.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];

        return HeaderFactory.buildDeleteHeader(senderId, fileId);
    }

    private static Header processRemovedChunk(String[][] arr) throws Exception {

        if(arr.length < 1 || arr[0].length != 5){
            throw new Exception("Bad Length REMOVED.");
        }

        String version = arr[0][1];
        int senderId = Integer.parseInt(arr[0][2]);
        String fileId = arr[0][3];
        int chunkNo = Integer.parseInt(arr[0][4]);

        return HeaderFactory.buildRemovedHeader(senderId, fileId, chunkNo);
    }
}
