package peer.data;

import peer.main.Peer;
import peer.network.SSLSocketListener;
import java.io.*;

/**
 * Created by fabio on 28-05-2017.
 */
public class SendDataManager implements Runnable {
    public static final long waitTime = 1000 * 60 ; // 1  minute

    @Override
    public void run() {
        boolean running = true;

        while(running)
        {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Backuping metadata");
            File f = new File("../Peer" + Peer.getPeerID() + "/metadata.ser");
            if(f.exists())  {
                try   {
    	        	byte [] array  = new byte [(int)f.length()];
    	        	FileInputStream fis = new FileInputStream(f);
    	        	BufferedInputStream bis = new BufferedInputStream(fis);
    	        	bis.read(array, 0, array.length);
    	        	
    	        	bis.close();
    	        	fis.close();

    	        	Peer.getSocketListener().sendMessage("StoreMetadata");
                    Peer.getSocketListener().sendBytes(array);
                    
                }   catch (IOException e)   {
                    return;
                }
            }

        }
    }
}
