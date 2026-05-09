package main;

import audioStreaming.MicService;
import audioStreaming.SpeakerService;
import httpserver.HTTPServer;
import httpserver.status.ActionResult;
import httpserver.status.MicStatus;
import httpserver.status.SpeakerStatus;

public class SotaSystemController {
    private HTTPServer httpServer = null;

    private final MicService micService = new MicService();
    private final SpeakerService speakerService = new SpeakerService();

    public SotaSystemController() {

    }

    public void start() {
        httpServer = new HTTPServer(this);
        httpServer.enableMicEndpoints();
    }

    // /////////// SPEAKER service
    public SpeakerStatus http_getSpeakerStatus() {
        SpeakerStatus status = new SpeakerStatus();
        status.enabled = speakerService.isEnabled();
        status.volume = -1;
        return status;
    }

    public ActionResult http_setSpeakerStatus(SpeakerStatus status) {
        // unset parameters are null.
        String error = "";

        if (status.enabled != null && status.enabled != speakerService.isEnabled()) {  // 'enabled' changed
            if (status.enabled) {

                if (status.streamListenPort == null) // we need a port
                    error += "Error enabling speaker, did not receive a port to listen on: " +
                            "port '" + status.streamListenPort + "'. ";
                else if (!speakerService.enable(status.streamListenPort))
                    error += speakerService.getLastError();

            } else // request to disable
                if (!speakerService.disable())
                    error += speakerService.getLastError();
        }
        if (error.equals("")) // no errror
            return ActionResult.ok();
        else return ActionResult.fail(error);
    }

    /// ////////// MIC service
    public MicStatus http_getMicStatus() {  // for the restful interface
        MicStatus status = new MicStatus();
        status.enabled = micService.isEnabled();
        status.volume = -1;

        if (status.enabled) {
            status.streamIP = micService.getIP();
            status.streamSendPort = micService.getPort();
        }
        return status;
    }

    public ActionResult http_setMicStatus(MicStatus status, String ip) {
        // unset parameters are null.
        String error = "";
        if (status.enabled != null && status.enabled != micService.isEnabled()) {  // 'enabled' changed
            if (status.enabled) {
                if (status.streamIP != null)
                    ip = status.streamIP; // overrule source IP by request IP

                if (status.streamSendPort == null) // we need a port
                    error += "Error enabling microphone, did not receive a port: " +
                            "IP '" + ip + "', port 'null'. ";
                else if (!micService.enable(ip, status.streamSendPort))
                        error += micService.getLastError();
            } else { // request to disable
                if (!micService.disable())
                    error += micService.getLastError();
            }
        }  // end "enabled" change

        if (error.equals("")) // no errror
            return ActionResult.ok();
        else return ActionResult.fail(error);
    }

    public static void main(String [] args) {
        SotaSystemController controller = new SotaSystemController();
        controller.start();
    }

}
