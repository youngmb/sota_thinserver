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
    private Integer ioBufferSize_ms = null;

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
                if (mixerInfo.getName().contains("plughw:2,0")) {
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


    @Override
    public void run() {     // Dedicated worker keeps playback off the network IO threads.
        running = true;

        byte[] silenceBuffer = new byte[ioBufferSize];

        while (running) {
            byte[] data = playbackQueue.poll();

            if (data == null || data.length == 0) {
                System.out.println("Starvation: write silence");
                    writeData(silenceBuffer, ioBufferSize);
            } else {
                System.out.println("write data");
                writeData (data, data.length);
            }
        }
    }


//    @Override
//    public void run() {     // Dedicated worker keeps playback off the network IO threads.
//        running = true;
//        workerThread = Thread.currentThread();
//        byte[] silenceBuffer = new byte[ioBufferSize];
//        byte[] data = null;
//
//        while (running) {
//            try {
////                data = playbackQueue.take();  // block only on empty
//                data = playbackQueue.poll(1, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException e) { // continue gracefully if interrupted
//                continue;
//            }
//            int qs = playbackQueue.size();
//            if (qs > 1) System.err.println("Queue depth: " + qs);
////            if (data == null || data.length == 0) {
////                int silenceLen = Math.min(0, ioBufferSize-sourceLine.available());
////                writeData(silenceBuffer, silenceLen);
////            } else {
////                writeData (data, data.length);
////            }
//            if (data == null || data.length == 0) {
//                int avail = sourceLine.available();
//                System.err.println("Queue empty: sdl.available()=" + avail + "/" + ioBufferSize);
//                if (sourceLine.available() == ioBufferSize) {
//                    // Hardware buffer fully drained - now we need silence
//                    writeData(silenceBuffer, ioBufferSize);
//                }
//            } else {
//                writeData (data, data.length);
//            }
//            // JITTER CONTROL / CATCH-UP:
//            // If the queue is building up, skip (drop) older packets to eliminate latency.
//            // Do NOT try to write them all sequentially, or you will block and freeze!
//            if (playbackQueue.size() > 10) {
//                System.err.println("Audio Lag Detected! Skipping backed-up frames.");
//                while (playbackQueue.size() > 1) {
//                    playbackQueue.poll();
//                }
//            }
//        }
//    }

//    public void run() {     // Dedicated worker keeps playback off the network IO threads.
//        int frameSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS) * Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE)/8;
//
//        double s_in_buffer =
//                (double) ioBufferSize / frameSize / Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE);
//        long bufferNs = (long)(s_in_buffer * 1_000_000_000L);
//
//        long next = System.nanoTime();
//
//        running = true;
//        workerThread = Thread.currentThread();
//        byte[] data = null;
//
//        while (running) {
//            data = playbackQueue.poll();
//
//            if (data != null) // have data
//                writeData(data);
//            else
//                writeData(new byte[ioBufferSize]); // write silence
//
//            if (playbackQueue.size() > Properties.getPropAsInt(PropertyKey.KEY_NET_UDP_WAITBUFFER_SIZE)) { // we're behind
//                data = playbackQueue.poll();
//                while (data !=  null) {
//                    writeData(data);
//                    data = playbackQueue.poll();
//                }
//                next = System.nanoTime();
//            }
//
//            next += bufferNs;  // wait this much for every frame we receive
//            long sleepNs = next - System.nanoTime();
//            if (sleepNs > 0)
//                LockSupport.parkNanos(sleepNs);
//        }
//    }

    private void writeData(byte[] data, int datalen) {
        try {
            int written = 0;
            // avoid overfilling the card buffer
            while (written < datalen) {  // TODO : probably needs fixing to fix SOTA buffer underflow bug
                int toWrite = Math.min(ioBufferSize, datalen - written);
                int actuallyWritten = sourceLine.write(data, written, toWrite);
//                System.out.println("actually written "+actuallyWritten);
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