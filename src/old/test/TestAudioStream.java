//package old.test;
//
//import java.io.File;
//
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.DataLine;
//import javax.sound.sampled.Mixer;
//import javax.sound.sampled.SourceDataLine;
//
//import old.dataprocessors.audio.AudioPlayback;
//import old.dataprocessors.network.UDPSender;
//import old.dataproviders.audio.MicAudioProvider;
//import old.eventsystem.EventDispatcher;
//
//public class TestAudioStream {
//
//  private static final int AUDIO_SEND_PORT = 50001;
//  private static final int AUDIO_RECEIVE_PORT = 50002;
//  private static final int UDP_RECEIVER_BUFFER_SIZE = 6000;
//  private static final int SAMPLE_RATE = 16000;
//  private static final int MICROPHONE_BUFFER_SIZE = 1024;
//  public static void main(String[] args) {
//    if (args.length < 1) {
//      System.out.println("Please provide the server IP as a command line argument.");
//      return;
//    }
//    String serverIp = args[0];
//    // simpleTest();
//    EventDispatcher dispatcher = new EventDispatcher();
//
//    MicAudioProvider micProvider = new MicAudioProvider(SAMPLE_RATE, MICROPHONE_BUFFER_SIZE);
//    UDPSender audioSender = new UDPSender(serverIp, AUDIO_SEND_PORT);
//    micProvider.addListener(audioSender);
//
//    micProvider.start();
//
//    UDPReceiver audioReceiver = new UDPReceiver(AUDIO_RECEIVE_PORT, UDP_RECEIVER_BUFFER_SIZE);
//    AudioPlayback audioPlayback = new AudioPlayback();
//    audioPlayback.playTestTone("../resources/utterances/test_hmm.wav");
//    audioReceiver.addListener(audioPlayback);
//
//    audioReceiver.start();
//    dispatcher.run();
//  }
//
//  // A simple old.test that plays back a wav file to the default audio output device
//  public static void simplePlaybackTest() {
//    try (AudioInputStream testStream = AudioSystem.getAudioInputStream(new File("../resources/utterances/test_hmm.wav"))) {
//      AudioFormat format = testStream.getFormat();
//      // we need to get the right audio device since it won't default to the correct one
//      // the correct device for Sota can be found on commandline with the following: pactl info | grep "Default Sink"
//      Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
//      for (Mixer.Info mixerInfo : mixerInfos) {
//        System.out.println("Mixer: " + mixerInfo.getName() + " - " + mixerInfo.getDescription());
//        // check if we've got the right mixer
//        if (mixerInfo.getName().contains("hw:2,0")) {
//          Mixer mixer = AudioSystem.getMixer(mixerInfo);
//          DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
//          if (mixer.isLineSupported(lineInfo)) {
//            try (SourceDataLine sourceLine = (SourceDataLine) mixer.getLine(lineInfo)) {
//              sourceLine.open(format);
//              sourceLine.start();
//
//              byte[] buffer = new byte[4096];
//              int bytesRead = 0;
//              while ((bytesRead = testStream.read(buffer)) != -1) {
//                sourceLine.write(buffer, 0, bytesRead);
//              }
//
//              sourceLine.drain();
//            } catch (Exception e) {
//              e.printStackTrace();
//            }
//          }
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//
//    }
//  }
//}
