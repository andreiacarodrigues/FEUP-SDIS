package proj.file;

import java.io.Serializable;

public class OriginalFileMeta implements Serializable
{
    private final String fileID;
    private final String filePath;

    public OriginalFileMeta(String fileID, String filePath)
    {
        this.fileID = fileID;
        this.filePath = filePath;
    }

    public String getFileID()
    {
        return fileID;
    }

    public String getFilePath()
    {
        return filePath;
    }
}
