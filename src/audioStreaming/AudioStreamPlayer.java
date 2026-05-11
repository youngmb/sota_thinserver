package audioStreaming;

import main.Properties;
import main.PropertyKey;

import javax.sound.sampled.*;
import java.sql.Time;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Streams raw PCM audio chunks emitted by a DataProvider (e.g. UDPReceiver) to the speakers.
 * - Starts a thread to manage draining the queue to the speaker while the main event system
 *   stays in the main thread.
 */
public class AudioStreamPlayer implements Runnable {

    private static final int ioBufferSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_BUFFER_SIZE);
    private static final AudioFormat audioFormat = new AudioFormat(
            Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE),
            Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE),
            Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS), true,false
    );

    private final BlockingQueue<byte[]> playbackQueue;
    private Thread workerThread = null;

    private SourceDataLine sourceLine;

    // manage audio playing thread
    private volatile boolean running = false;

    public AudioStreamPlayer(BlockingQueue<byte[]> queue) {
        playbackQueue = queue;
    }

    // open a SourceDataLine to the correct audio device
    private void openLine() {
        try {
            // we need to iterate through mixers to get the correct one
            // that represents Sota's speaker, which can be ascertained
            // for Sota via the terminal command:
            // pactl info | grep "Default Sink"
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().contains("hw:2,0")) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
                    System.out.println("Audio Stream player line supported: " + mixer.isLineSupported(lineInfo));
                    SourceDataLine line = (SourceDataLine) mixer.getLine(lineInfo);
                    line.open(audioFormat);
                    line.start();
                    sourceLine = line;
                    return;
                }
            }
            // otherwise try this as default
            SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
            System.out.println(line.getLineInfo().toString());
            line.open(audioFormat);
            line.start();
            sourceLine = line;
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Unable to open audio playback line", e);
        }
    }

    /// ================== speaker draining thread functions

    public void start() {
        openLine();
        workerThread = new Thread(this, "audio stream player thread");
        workerThread.start();
    }

    @Override
//    public void run() {     // Dedicated worker keeps playback off the network IO threads.
//        running = true;
//        workerThread = Thread.currentThread();
//        byte[] data = null;
//        while (running) {
////            try {
////                data = playbackQueue.take();  // block only on empty
//                data = playbackQueue.poll();
//                if (data == null)
//
////            } catch (InterruptedException e) { // continue gracefully if interrupted
////                continue;
////            }
//
//            if (data.length == 0) {
//                System.err.println("AudioPlayback: empty payload ");
//                return;
//            }
//
//            try {
//            int written = 0;
//                while (written < data.length) {  // TODO : probably needs fixing to fix SOTA buffer underflow bug
//                    int toWrite = Math.min(ioBufferSize, data.length - written);
//                    sourceLine.write(data, written, toWrite);
//                    written += toWrite;
//                }
//            } catch (Exception e) {  // shouldn't happen
//                e.printStackTrace();
//            }
//        }
//    }

    public void run() {     // Dedicated worker keeps playback off the network IO threads.
        int frameSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS) * Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE)/8;

        double s_in_buffer =
                (double) ioBufferSize / frameSize / Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE);
        long bufferNs = (long)(s_in_buffer * 1_000_000_000L);

        long next = System.nanoTime();

        running = true;
        workerThread = Thread.currentThread();
        byte[] data = null;

        while (running) {
            data = playbackQueue.poll();

            if (data != null) // have data
                writeData(data);
            else
                writeData(new byte[ioBufferSize]); // write silence

            if (playbackQueue.size() > Properties.getPropAsInt(PropertyKey.KEY_NET_UDP_WAITBUFFER_SIZE)) { // we're behind
                data = playbackQueue.poll();
                while (data !=  null) {
                    writeData(data);
                    data = playbackQueue.poll();
                }
                next = System.nanoTime();
            }

            next += bufferNs;  // wait this much for every frame we receive
            long sleepNs = next - System.nanoTime();
            if (sleepNs > 0)
                LockSupport.parkNanos(sleepNs);
        }
    }

    private void writeData(byte[] data) {
        try {
            int written = 0;
            // avoid overfilling the card buffer
            while (written < data.length) {  // TODO : probably needs fixing to fix SOTA buffer underflow bug
                int toWrite = Math.min(ioBufferSize, data.length - written);
                int actuallyWritten = sourceLine.write(data, written, toWrite);
                written += actuallyWritten;
            }
        } catch (Exception e) {  // shouldn't happen
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        sourceLine.drain();
        sourceLine.stop();
        sourceLine.close();
        workerThread.interrupt();
        try {
            workerThread.join(1000); // wait 1 second to join
        } catch (InterruptedException e) {
            System.err.println("unable to join thread when closing.");
            e.printStackTrace();
        }
    }
}