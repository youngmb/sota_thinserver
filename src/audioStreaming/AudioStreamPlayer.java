package audioStreaming;

import main.Properties;
import main.PropertyKey;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * Streams raw PCM audio chunks emitted by a DataProvider (e.g. UDPReceiver) to the speakers.
 * - Starts a thread to manage draining the queue to the speaker while the main event system
 *   stays in the main thread.
 */
public class AudioStreamPlayer implements Runnable {

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

        workerThread = new Thread(this, "audio stream player thread");
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
    }

    @Override
    public void run() {     // Dedicated worker keeps playback off the network IO threads.
        running = true;
        byte[] silenceBuffer = new byte[ioBufferSize];

        // the writing does not seem to reliably block on full or time properly, so we end up thinking
        // that the stream is starved (and thus writing blank 0s) when its not, creating clicks.
        // if we let it starve naturally, we get garbage due to a java 8 bug so we need to write 0s.
        // -- the solution is we use wall time to keep timing alignment. it seems to work fine.

        // WHEN we were facing a slew of timing bugs, we found that the buffer behaved better if we had it a multople of 112
        // this was discovered by monitoring how many bytes were actually written.. Now that other bugs and timing is fixed this
        // constraint seems to have gone away.
//    public final static int SYSTEM_BUFFER_SIZE = 112; // This is the system read boundary. Java 8 bug, match a multiple of this.

        long startNs = System.nanoTime();  // won't overflow for years and years of runtime.
        long framesScheduled = 0;
        long framesInBuffer = (ioBufferSize / this.bytesPerFrame);

        int printcounter = 0;  // debug printing. usually off.
        boolean doPrint = false;

        while (running) {
            printcounter = (printcounter + 1) % 100;
//            doPrint =  (printcounter == 0);  // debug off.

            // how many frames should have played by now
            long elapsedNs = System.nanoTime() - startNs;
            long expectedFramesPlayed = (long)(elapsedNs / 1_000_000_000.0 * this.sampleRate);

            if (doPrint) System.out.println("framesScheduled=" + framesScheduled
                            + " expectedFramesPlayed=" + expectedFramesPlayed
                            + " framesInBuffer=" + framesInBuffer
                            + " diff ("+(expectedFramesPlayed-framesScheduled)+")"
                            + " playbackkqueue= "+playbackQueue.size());

            // only write when hardware is almost ready for the next packet.
            if (framesScheduled <= expectedFramesPlayed + framesInBuffer) {
                byte[] data = playbackQueue.poll();
                if (data != null) {
                    if (doPrint) System.out.println("write data (playback remaining queue "+playbackQueue.size()+")");
                    writeData(data, data.length);
                    framesScheduled += data.length / this.bytesPerFrame;
                } else {
                    if (doPrint) System.out.println("Starvation. write silence");
                    long t0 = System.nanoTime();
                    writeData(silenceBuffer, ioBufferSize);
                    long writeMs = (System.nanoTime() - t0) / 1_000_000;
//                    if (writeMs > 5) System.out.println("writeData took " + writeMs + "ms");
//                    writeData(silenceBuffer, ioBufferSize);
                    framesScheduled += framesInBuffer;
                }
            } else {
                LockSupport.parkNanos(1_000_000L);
                if (doPrint) System.out.println("park");
            }
        }
    }

    private void writeData(byte[] data, int datalen) {
        try {
            int written = 0;

            while (written < datalen) { // avoid overfilling the card buffer
                int toWrite = Math.min(ioBufferSize, datalen - written);
                int actuallyWritten = sourceLine.write(data, written, toWrite);
//                System.out.println("actually written "+actuallyWritten + " ("+(ioBufferSize-actuallyWritten)+")");
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