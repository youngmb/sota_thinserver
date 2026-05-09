//package old.dataproviders.audio;
//
//import java.io.File;
//import java.nio.ByteBuffer;
//
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//
//import old.dataproviders.DataProvider;
//
//public class FileAudioProvider extends DataProvider {
//
//    private File audioFile;
//    private int bufferSize;
//
//    public FileAudioProvider(File audioFile, int bufferSize) {
//        this.audioFile = audioFile;
//        this.bufferSize = bufferSize;
//    }
//
//    @Override
//    public void run() {
//        try {
//            AudioInputStream audioStream = AudioSystem.getAudioInputStream(this.audioFile);
//
//            byte[] buffer = new byte[this.bufferSize];
//            double[] samples = new double[this.bufferSize / 2];
//
//            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
//
//            while(audioStream.read(buffer) >= 0) {
//                byteBuffer.position(0);
//                for(int i = 0; i < this.bufferSize/2; i++) {
//                   samples[i] = (double)byteBuffer.getShort();
//                }
//                this.notifyListeners(new DoubleArrayData(samples));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
