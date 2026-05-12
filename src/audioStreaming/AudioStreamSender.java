package audioStreaming;

import main.Properties;
import main.PropertyKey;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.function.Consumer;

public class AudioStreamSender implements Runnable {
    private final int sampleRate = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_RATE);
    private int bufferSize = 0;
    private final int micSampleSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_SIZE);

    TargetDataLine dataLine = null;
    boolean running = false;
    private Thread workerThread = null;
    Consumer<byte[]> audioSink;

    public AudioStreamSender(Consumer<byte[]> audioSink) {
        this.audioSink = audioSink;
    }

    public void init() {
        try {
            AudioFormat audioFormat = new AudioFormat(this.sampleRate, this.micSampleSize, 1, true, false);
            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            dataLine = (TargetDataLine)AudioSystem.getLine(lineInfo);

            dataLine.open(audioFormat, bufferSize);
            dataLine.start();

        } catch (Exception e) {
            System.out.println("Error starting Mic Audio Provider:");
            e.printStackTrace();
            return;
        }
    }

    public void start() {
        this.bufferSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE);
        workerThread = new Thread(this, "audio stream sender thread");
        workerThread.start();
    }

    public void stop() {
        running = false;
        if (dataLine != null) {
            dataLine.stop();
            dataLine.close();
        }
        workerThread.interrupt();
        try {
            workerThread.join(1000); // wait 1 second to join
        } catch (InterruptedException e) {
            System.err.println("unable to join thread when closing.");
            e.printStackTrace();
        }
    }

    public boolean isRunning() {return running;}

    @Override
    public void run() {
        workerThread = Thread.currentThread();
        running = true;
        byte[] readBuffer = new byte[bufferSize];

        int bytesRead;
        while(running) {

            try {
                bytesRead = dataLine.read(readBuffer, 0, bufferSize);
            } catch (Exception e) {  // shouldn't happen, but Java 8 DataLine is poorly documented
                running = false;
                continue;
            }

            if (bytesRead == 0) { // should never happen, but may spuriously wake up
                Thread.yield(); // minimize impact of potential busy spin
                continue;
            }

            if (bytesRead == -1) { // not guaranteed to be implemented in the dataline, EOF
                running = false;
                continue;
            }

            byte[] micData = new byte[bytesRead];
            System.arraycopy(readBuffer, 0, micData, 0, bytesRead);
            audioSink.accept(micData);
        }
    }
}