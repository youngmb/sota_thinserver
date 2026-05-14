package main;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import httpserver.HTTPServer;
import sota.SotaConnector;
import sota.motors.MotorService;

public class SotaSystemController {
    private HTTPServer httpServer = null;

    private final SotaConnector sota = new SotaConnector();

    private final MicService micService = new MicService();
    private final SpeakerService speakerService = new SpeakerService();
    private final MotorService motorService = new MotorService(sota);

    public SotaSystemController() {
        ; // pass
    }

    public void start() {
        sota.start();  // start the Sota subsystem

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
