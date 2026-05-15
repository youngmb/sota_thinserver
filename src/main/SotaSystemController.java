package main;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import httpserver.HTTPServer;
import sota.SotaConnector;
import sota.servos.ServoService;

public class SotaSystemController {
    private HTTPServer httpServer = null;

    private final SotaConnector sota = new SotaConnector();

    private final MicService micService = new MicService();
    private final SpeakerService speakerService = new SpeakerService();
    private final ServoService motorService = new ServoService(sota);

    public SotaSystemController() {
        ; // pass
    }

    public void start() {
        if (!sota.start())  // start the Sota subsystem
            System.out.println("CRITICAL: could not start the Sota subsystem, many features will likely not work.");

        httpServer = new HTTPServer(this);
        httpServer.enableMicEndpoints(micService);
        httpServer.enableSpeakerEndpoints(speakerService);
        httpServer.enableMotorEndpoints(motorService);
    }

    public static void main(String [] args) {
        SotaSystemController controller = new SotaSystemController();
        controller.start();
    }
}
