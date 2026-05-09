//package old.dataproviders.audio;
//
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.DataLine;
//import javax.sound.sampled.TargetDataLine;
//
//import main.Properties;
//
//import main.PropertyKey;
//import old.datatypes.Data;
//import old.datatypes.DataFromPool;
//import old.datatypes.Pool;
//
//public class MicAudioProvider implements Runnable {
//    private final int sampleRate = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_RATE);
//    private final int bufferSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE);
//
//    @Override
//    public void run() {
//
//        Pool<byte[]> pool = new Pool<>(
//                this.sampleRate / this.bufferSize,  // enough for 1 second delay
//                () -> new byte[this.bufferSize],
//                null // no resetter
//        );
//
//        TargetDataLine dataLine = null;
//
//        try {
//            AudioFormat audioFormat = new AudioFormat(this.sampleRate,
//                    Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_SIZE),
//                    1, true, false);
//            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
//            dataLine = (TargetDataLine)AudioSystem.getLine(lineInfo);
//
//            dataLine.open(audioFormat, bufferSize);
//            dataLine.start();
//
//        } catch (Exception e) {
//            System.out.println("Error starting Mic Audio Provider:");
//            e.printStackTrace();
//            return;
//        }
//
//        int bytesRead;
//        boolean keepRunning = true;
//        while(keepRunning) {
//            byte[] readBuffer = pool.acquire();
//
//            try {
//                bytesRead = dataLine.read(readBuffer, 0, bufferSize);
//            } catch (Exception e) {  // shouldn't happen, but Java 8 DataLine is poorly documented
//                keepRunning = false;
//                continue;
//            }
//
//            if (bytesRead == 0) { // should never happen, but may spuriously wake up
//                Thread.yield(); // minimize impact of potential busy spin
//                continue;
//            }
//
//            if (bytesRead == -1) { // not guaranteed to be implemented in the dataline, EOF
//                keepRunning = false;
//                continue;
//            }
//
//            Data micData = new DataFromPool<>(pool, readBuffer, bytesRead);
//            this.notifyListeners(micData);
//        }
//    }
//
//    public void startAsNewThread() {
//        Thread t = new Thread(this);
//        t.start();
//    }
//}