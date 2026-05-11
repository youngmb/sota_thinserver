package main;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import httpserver.HTTPServer;
import httpserver.status.ActionResult;
import httpserver.status.AudioStatus;

public class SotaSystemController {
    private HTTPServer httpServer = null;

    public final MicService micService = new MicService();
    public final SpeakerService speakerService = new SpeakerService();

    public SotaSystemController() {

    }

    public void start() {
        httpServer = new HTTPServer(this);
        httpServer.enableMicEndpoints(micService);
        httpServer.enableSpeakerEndpoints(speakerService);
    }

    public static void main(String [] args) {
        SotaSystemController controller = new SotaSystemController();
        controller.start();
    }

}
