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

    public final static int SYSTEM_BUFFER_SIZE = 112; // This is the system read boundary. Java 8 bug, match a multiple of this.

    private Integer ioBufferSize = null;
    private int bytesPerFrame;
    private int sampleRate;

    private final BlockingQueue<byte[]> playbackQueue;
    private Thread workerThread = null;

    private SourceDataLine sourceLine;

    // manage audio playing thread
    private volatile boolean running = false;

    public AudioStreamPlayer(BlockingQueue<byte[]> queue) {
        playbackQueue = queue;
    }

    // open a SourceDataLine to the correct audio device
    private void openLine(AudioFormat format) {
        try {
            // we need to iterate through mixers to get the correct one
            // that represents Sota's speaker, which can be ascertained
            // for Sota via the terminal command:
            // pactl info | grep "Default Sink"
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().contains("hw:2,0")) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
                    System.out.println("Audio Stream player line supported: " + mixerInfo.getName()+" "+format.toString());
                    SourceDataLine line = (SourceDataLine) mixer.getLine(lineInfo);
                    line.open(format, ioBufferSize);
                    line.start();
                    sourceLine = line;
                    return;
                }
            }
            System.out.println("No expected audio stream found, trying default.");
            // otherwise try this as default
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            System.out.println(line.getLineInfo().toString());
            line.open(format, ioBufferSize);
            line.start();
            sourceLine = line;
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Unable to open audio playback line", e);
        }
    }

    /// ================== speaker draining thread functions

    public void start() {
        ioBufferSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_BUFFER_SIZE);
        openLine(
                new AudioFormat(
                    Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE),
                    Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE),
                    Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS), true,false
                )
        );
        this.sampleRate = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE);
        this.bytesPerFrame = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE) / 8;

//        ioBufferSize_ms = (int)(Math.ceil
//                ( (double)ioBufferSize
//                / Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS)
//                / (Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE)/8)
//                / Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE)
//                * 1000.0)
//        );

        workerThread = new Thread(this, "audio stream player thread");
        workerThread.start();
    }

//
//    @Override
//    public void run() {     // Dedicated worker keeps playback off the network IO threads.
//        running = true;
//        byte[] silenceBuffer = new byte[ioBufferSize];
//        long framesWritten = 0;
//        int jitterBufferSize = 5;
//        boolean fillJitterBuffer = true;
//
//        while (running) {
//            long framesPlayed = sourceLine.getLongFramePosition();
//            long framesBuffered = framesWritten - framesPlayed;
//            long frameBufferThreshold = (ioBufferSize / this.bytes_per_frame);
//
//            System.out.println("framesWritten=" + framesWritten
//                    + " framesPlayed=" + framesPlayed
//                    + " framesBuffered=" + framesBuffered
//                    + " threshold=" + frameBufferThreshold
//                    + " playbackkqueue= "+playbackQueue.size());
//
//            if (fillJitterBuffer) {
//                System.out.println("Waiting for buffer...");
//                if (playbackQueue.size() < jitterBufferSize) {
//                    LockSupport.parkNanos(1_000_000L);
//                } else {
//                    fillJitterBuffer = false;
//                    System.out.println("Starting playback, queue depth: " + playbackQueue.size());
//                }
//                }
//
//            if (framesBuffered < frameBufferThreshold) { // less than a full packet left in the buffer
//                byte[] data = playbackQueue.poll();
//                if (data != null) {
//                    writeData(data, data.length);
//                    framesWritten += data.length / this.bytes_per_frame;
//                    System.out.println("write data (playback queue "+playbackQueue.size()+")");
//                } else {
//                    // genuinely out of data relative to hardware position
//                    writeData(silenceBuffer, ioBufferSize);
//                    System.out.println("Starvation: write silence");
//                    framesWritten += frameBufferThreshold;
//                    fillJitterBuffer = true; // startover with jitter buffer
//                }
//            } else {
//                LockSupport.parkNanos(1_000_000L); // 1ms delay before trying again, avoid busy spin
//                System.out.print(".");
//            }
//        }
//    }



    @Override
    public void run() {     // Dedicated worker keeps playback off the network IO threads.
        running = true;
        byte[] silenceBuffer = new byte[ioBufferSize];

        long startNs = System.nanoTime();
        long framesScheduled = 0;
        long framesInBuffer = (ioBufferSize / this.bytesPerFrame);

        while (running) {

            // how many frames should have played by now
            long elapsedNs = System.nanoTime() - startNs;
            long framesPlayed = (long)(elapsedNs / 1_000_000_000.0 * this.sampleRate);

            System.out.println("framesScheduled=" + framesScheduled
                            + " framesPlayed=" + framesPlayed
                            + " framesInBuffer=" + framesInBuffer
                            + " playbackkqueue= "+playbackQueue.size());

            // only write when hardware should be ready for next packet
            if (framesScheduled <= framesPlayed + framesInBuffer) {
                byte[] data = playbackQueue.poll();
                if (data != null) {
                    System.out.println("write data (playback remaining queue "+playbackQueue.size()+")");
                    writeData(data, data.length);
                    framesScheduled += data.length / this.bytesPerFrame;
                } else {
                    System.out.println("Starvation. write silence");
                    writeData(silenceBuffer, ioBufferSize);
                    framesScheduled += framesInBuffer;
                }
            } else {
                LockSupport.parkNanos(1_000_000L);
                System.out.println("park");
            }
        }
    }

//    @Override
//    public void run() {     // Dedicated worker keeps playback off the network IO threads.
//        running = true;
//
//        byte[] silenceBuffer = new byte[ioBufferSize];
//
//        while (running) {
//            byte[] data = playbackQueue.poll();
//
//            if (data == null || data.length == 0) {  // no incoming data
////
////            if (sourceLine.available()==ioBufferSize) {// empty buffer, its starving
////                    System.out.println("Starvation: write silence");
////                    writeData(silenceBuffer, ioBufferSize);
////                } else {
//                LockSupport.parkNanos(1_000_000L); // 1ms, hardware is still playing
////                }
//            } else {
//                System.out.println("write data (queue size "+playbackQueue.size()+")");
//                writeData (data, data.length);
//            }
//        }
//    }

    private void writeData(byte[] data, int datalen) {
        try {
            int written = 0;
            // avoid overfilling the card buffer
            while (written < datalen) {  // TODO : probably needs fixing to fix SOTA buffer underflow bug
                int toWrite = Math.min(ioBufferSize, datalen - written);
                int actuallyWritten = sourceLine.write(data, written, toWrite);
                System.out.println("actually written "+actuallyWritten + " ("+(ioBufferSize-actuallyWritten)+")");
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