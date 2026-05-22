package main;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import httpserver.HTTPServer;
import sota.SotaConnector;
import sota.pose.PoseService;
import videoStreaming.VideoService;

public class SotaSystemController {
    private HTTPServer httpServer = null;

    private final SotaConnector sota = new SotaConnector();

    private final MicService micService = new MicService();
    private final SpeakerService speakerService = new SpeakerService();
    private final PoseService motorService = new PoseService(sota);
    private final VideoService videoService = new VideoService();

    public SotaSystemController() {
        ; // pass
    }

    public void start() {
        if (!sota.start())  // start the Sota subsystem
            System.out.println("CRITICAL: could not start the Sota subsystem, many features will likely not work.");

        httpServer = new HTTPServer(this);
        httpServer.enableMicEndpoints(micService);
        httpServer.enableSpeakerEndpoints(speakerService);
        httpServer.enablePoseEndpoints(motorService);
        httpServer.enableVideoEndpoints(videoService);
    }

    public void stop() {
        if (httpServer != null)
            httpServer.stop();
        if (sota != null)
            sota.stop();
    }

    public static void main(String [] args) {
        SotaSystemController controller = new SotaSystemController();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { // register shutdown hook to cleanly stop on SIGINT
            System.out.println("Shutting down...");
            controller.stop();
        }));
        controller.start();
    }
}