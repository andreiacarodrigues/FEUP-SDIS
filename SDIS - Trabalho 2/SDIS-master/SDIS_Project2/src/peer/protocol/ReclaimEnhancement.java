package peer.protocol;

import peer.main.Peer;

public class ReclaimEnhancement implements Runnable
{
	public static final long waitTime = 1000 * 60 * 5; // 5  minutes
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
			Peer.recoverReplicationDegree();
		}
	}

}
