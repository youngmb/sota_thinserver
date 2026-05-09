//package old.dataprocessors.audio;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.atomic.AtomicLong;
//
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.DataLine;
//import javax.sound.sampled.LineUnavailableException;
//import javax.sound.sampled.Mixer;
//import javax.sound.sampled.SourceDataLine;
//import javax.sound.sampled.UnsupportedAudioFileException;
//
//import old.dataprocessors.DataProcessor;
//import old.datatypes.Data;
//import old.eventsystem.EventGenerator;
//import old.eventsystem.EventListener;
//import main.Properties;
//
///**
// * Streams raw PCM audio chunks emitted by a DataProvider (e.g. UDPReceiver) to the speakers.
// */
//public class AudioPlayback extends EventListener {
//
//    private static final int DEFAULT_IO_BUFFER_SIZE = 4096;
//    private static final AudioFormat DEFAULT_AUDIO_FORMAT = new AudioFormat(
//            Properties.getPropAsInt(Properties.KEY_SPK_SAMPLE_RATE, 16000),
//            Properties.getPropAsInt(Properties.KEY_SPK_SAMPLE_SIZE, 16),
//        1, true,false
//    );
//
//    private final BlockingQueue<byte[]> playbackQueue = new ArrayBlockingQueue<>();
//
//    private final Thread playbackThread;
//
//    private final int ioBufferSize;
//    private final AudioFormat audioFormat;
//    private final SourceDataLine sourceLine;
//    private final AtomicLong chunksReceived = new AtomicLong(0);
//    private final AtomicLong bytesWritten = new AtomicLong(0);
//    private volatile boolean running = true;
//
//    public AudioPlayback() {
//        this(DEFAULT_IO_BUFFER_SIZE, DEFAULT_AUDIO_FORMAT);
//    }
//
//    public AudioPlayback(int ioBufferSize) {
//        this(ioBufferSize, DEFAULT_AUDIO_FORMAT);
//    }
//
//    public AudioPlayback(int ioBufferSize, AudioFormat audioFormat) {
//        if (ioBufferSize <= 0) {
//            throw new IllegalArgumentException("ioBufferSize must be positive");
//        }
//        if (audioFormat == null) {
//            throw new IllegalArgumentException("audioFormat must not be null");
//        }
//        this.ioBufferSize = ioBufferSize;
//        this.audioFormat = audioFormat;
//        this.sourceLine = openLine(audioFormat);
//        this.playbackThread = new Thread(this::drainQueue, "AudioPlaybackThread");
//        this.playbackThread.setDaemon(true);
//        this.playbackThread.start();
//    }
//
//    @Override
//    public void handle(Data input, EventGenerator sender) {
//        if (!running) {
//            return null;
//        }
//
//        byte[] wavBytes = extractWavBytes(input);
//        if (wavBytes == null || wavBytes.length == 0) {
//            System.err.println("AudioPlayback: Unsupported or empty payload " + (input == null ? "null" : input.getClass().getName()));
//            return null;
//        }
//
//        long count = chunksReceived.incrementAndGet();
//        if ((count & 0x3F) == 0) { // log every 64 chunks
//            System.out.println("AudioPlayback: queued chunk #" + count + " size=" + wavBytes.length);
//        }
//
//        playbackQueue.offer(wavBytes);
//        return null;
//    }
//
//    private byte[] extractWavBytes(Data payload) {
//        if (payload instanceof ByteArrayData) {
//            return ((ByteArrayData) payload).data;
//        }
//        return null;
//    }
//
//    // Dedicated worker keeps playback off the network IO threads.
//    private void drainQueue() {
//        while (running) {
//            try {
//                byte[] wavBytes = playbackQueue.take();
//                writeChunk(wavBytes);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                break;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void writeChunk(byte[] pcmBytes) {
//        if (pcmBytes == null || pcmBytes.length == 0) {
//            return;
//        }
//        int written = 0;
//        while (written < pcmBytes.length) {
//            int toWrite = Math.min(ioBufferSize, pcmBytes.length - written);
//            sourceLine.write(pcmBytes, written, toWrite);
//            written += toWrite;
//        }
//
//        long total = bytesWritten.addAndGet(pcmBytes.length);
//        if ((total & 0xFFFF) == 0) { // log approximately every 64KB
//            System.out.println("AudioPlayback: total bytes written=" + total);
//        }
//    }
//
//    /**
//     * Play a WAV file through the opened SourceDataLine by streaming its bytes directly.
//     */
//    public void playTestTone(String wavFilePath) {
//        if (wavFilePath == null || wavFilePath.isEmpty()) {
//            System.err.println("AudioPlayback: playTestTone requires a wav file path");
//            return;
//        }
//
//        File wavFile = new File(wavFilePath);
//        if (!wavFile.exists()) {
//            System.err.println("AudioPlayback: wav file not found " + wavFile.getAbsolutePath());
//            return;
//        }
//
//        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(wavFile);
//             AudioInputStream playbackStream = ensureAudioFormat(sourceStream)) {
//
//            byte[] buffer = new byte[ioBufferSize];
//            int bytesRead;
//            while ((bytesRead = playbackStream.read(buffer, 0, buffer.length)) != -1) {
//                if (bytesRead > 0) {
//                    sourceLine.write(buffer, 0, bytesRead);
//                }
//            }
//            sourceLine.drain();
//            System.out.println("AudioPlayback: played old.test WAV " + wavFile.getName());
//        } catch (UnsupportedAudioFileException | IOException e) {
//            System.err.println("AudioPlayback: failed to play old.test tone - " + e.getMessage());
//        }
//    }
//
//    private AudioInputStream ensureAudioFormat(AudioInputStream sourceStream) throws UnsupportedAudioFileException {
//        AudioFormat sourceFormat = sourceStream.getFormat();
//        boolean compatible =
//            sourceFormat.getEncoding().equals(audioFormat.getEncoding()) &&
//            sourceFormat.getChannels() == audioFormat.getChannels() &&
//            sourceFormat.getSampleSizeInBits() == audioFormat.getSampleSizeInBits() &&
//            Math.abs(sourceFormat.getSampleRate() - audioFormat.getSampleRate()) < 0.01 &&
//            sourceFormat.isBigEndian() == audioFormat.isBigEndian();
//
//        if (compatible) {
//            return sourceStream;
//        }
//
//        try {
//            return AudioSystem.getAudioInputStream(audioFormat, sourceStream);
//        } catch (IllegalArgumentException e) {
//            throw new UnsupportedAudioFileException("Unsupported format conversion: " + e.getMessage());
//        }
//    }
//
//    // open a SourceDataLine to the correct audio device
//    private SourceDataLine openLine(AudioFormat format) {
//        try {
//            // we need to iterate through mixers to get the correct one
//            // that represents Sota's speaker, which can be ascertained
//            // for Sota via the terminal command:
//            // pactl info | grep "Default Sink"
//            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
//            for (Mixer.Info mixerInfo : mixerInfos) {
//                if (mixerInfo.getName().contains("hw:2,0")) {
//                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
//                    DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
//                    System.out.println("is line supported: " + mixer.isLineSupported(lineInfo));
//                    SourceDataLine line = (SourceDataLine) mixer.getLine(lineInfo);
//                    line.open(format);
//                    line.start();
//                    return line;
//                }
//            }
//            SourceDataLine line = AudioSystem.getSourceDataLine(format);
//            System.out.println(line.getLineInfo().toString());
//            line.open(format);
//            line.start();
//            return line;
//        } catch (LineUnavailableException e) {
//            throw new IllegalStateException("Unable to open audio playback line", e);
//        } catch (Exception e) {
//            throw e;
//        }
//    }
//
//    public void shutdown() {
//        running = false;
//        playbackThread.interrupt();
//        sourceLine.drain();
//        sourceLine.stop();
//        sourceLine.close();
//    }
//}
