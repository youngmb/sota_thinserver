//package main;
//
//import old.dataprocessors.audio.AudioPlayback;
//import old.dataprocessors.network.UDPSender;
//import old.dataprocessors.sota.SotaDialogController;
//import old.dataproviders.audio.MicAudioProvider;
//import old.dataproviders.network.HttpCommandProvider;
//import old.eventsystem.EventDispatcher;
//
////note from Jonathan: MicAudioProvider currently outputs double data, which makes data sent over the network larger than necessary
////if better peformance is needed then short data should be sent instead
//
///**
// * Device-side layer of the dialog system for robot control.
// * Communicates with the DialogServer running on a separate device.
// */
//public class SotaDialog {
//
//  public static void main(String [] args) {
//    run();
//  }
//
//  // #### WARNING - literal strings throughout here must match the properties file.
//  public static void run() {
//
//    // setup the core event system
//    EventDispatcher dispatcher = new EventDispatcher();
//
////    String localIp = getLocalIpForRemote(serverIp, 6100);
////    System.out.println("Detected local IP: " + localIp);
//
//    // setup the streaming audio servers
//
//    // send microphone data to the server
//    MicAudioProvider mic = new MicAudioProvider();
//    UDPSender audioSender = new UDPSender(serverIp, AUDIO_SEND_PORT);
//    mic.addListener(audioSender);
//
//    // handle incoming audio from the server
//    UDPReceiver audioReceiver = new UDPReceiver(AUDIO_RECEIVE_PORT, UDP_RECEIVER_BUFFER_SIZE);
//    AudioPlayback audioPlayback = new AudioPlayback();
//    audioReceiver.addListener(audioPlayback);
//
//    // HTTP client that polls for commands and outputs state updates to its listeners
//    final HttpCommandProvider commandProvider = new HttpCommandProvider("http://" + serverIp + "/api", 1000, robotId, localIp, AUDIO_SEND_PORT);
//
//    SotaDialogController controller = new SotaDialogController();
//    commandProvider.addListener(controller);
//
//    // Orchestrate command polling from the main app: start the provider paused
//    // and request a command whenever the controller transitions to READY.
//    // start paused to ensure we only poll when requested
//    commandProvider.pausePolling();
//    commandProvider.initializeRobot(robotId);
//    commandProvider.start();
//
//    // when the controller is ready, tell HttpCommandProvider to request the next command
//    controller.addListener(new old.eventsystem.EventListener() {
//      @Override
//      public void handle(old.datatypes.Data d, old.eventsystem.EventGenerator sender) {
//        if (d instanceof SotaDialogController.SotaStateData) {
//          SotaDialogController.SotaStateData sd = (SotaDialogController.SotaStateData) d;
//          if (sd.data == SotaDialogController.SotaState.READY) {
//            commandProvider.requestOnce();
//          }
//        }
//      }
//    });
//
//    // trigger an initial request
//    commandProvider.requestOnce();
//
//    mic.startAsNewThread();
//    audioReceiver.start();
//
//    dispatcher.run();
//
//    /// ////////
//
////    = new LinkedBlockingQueue<>()
//            //
//  }
//
//  // quickly fetch the robot' local IP address for reaching a given remote host
//  private static String getLocalIpForRemote(String remoteHost, int remotePort) {
//    try {
//      java.net.DatagramSocket socket = new java.net.DatagramSocket();
//      socket.connect(java.net.InetAddress.getByName(remoteHost), remotePort);
//      String localIp = socket.getLocalAddress().getHostAddress();
//      socket.close();
//      return localIp;
//    } catch (Exception e) {
//      e.printStackTrace();
//      return null;
//    }
//  }
//}
