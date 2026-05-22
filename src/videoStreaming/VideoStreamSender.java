package videoStreaming;

import main.Properties;
import main.PropertyKey;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VideoStreamSender implements Runnable {

    private long bitrate_bps = 0;
    private boolean running = false;

    Supplier<VideoFrame> getFrame = null;
    Consumer<VideoFrame> putFrame = null;

    Thread workerThread = null;

    public VideoStreamSender(Supplier<VideoFrame> getFrame, Consumer<VideoFrame> putFrame)
    {
        this.getFrame = getFrame;  this.putFrame = putFrame;
    }

    public void start() {
        bitrate_bps = Properties.getPropAsInt(PropertyKey.KEY_VID_BITRATE_CAP) * 1_000L;
        workerThread = new Thread(this, "video stream sender thread");
        workerThread.start();
    }

    public void stop() {
        running = false;
        workerThread.interrupt();
        try {
            workerThread.join(1000); // wait 1 second to join
        } catch (InterruptedException e) {
            System.err.println("unable to join thread when closing.");
            e.printStackTrace();
        }
    }

    // streaming thread
    @Override
    public void run() {
        double bucketBits = 0;

        running = true;

        long lastTimeNs = System.nanoTime();

        while (running) {

            long now = System.nanoTime();
            long elapsedNs = now - lastTimeNs;
            lastTimeNs = now;

            // refill bitrate bucket
            bucketBits += (bitrate_bps * ( (double)elapsedNs / 1e9));
            bucketBits = Math.min(bucketBits, bitrate_bps * .25); // don't let the bucket overfill. Max of 250ms to avoid bursts

            VideoFrame frame = getFrame.get();
            if (frame == null || frame.data == null) continue;

            int frameBits = frame.size * 8;
            if (bucketBits >= frameBits) {
                bucketBits -= frameBits;

                putFrame.accept(frame);

            } else { // not enough budget -> wait a bit and get a new frame.
                long sleepNs = (long)(  (frameBits - bucketBits) / bitrate_bps * 1e9 );

                if (sleepNs > 0)
                    LockSupport.parkNanos(sleepNs);
            }
        }
    }
}
