package videoStreaming;

import Udp.UdpSender;
import audioStreaming.AudioStatus;
import audioStreaming.AudioStreamSender;
import httpserver.ActionResult;
import main.Properties;
import main.PropertyKey;
import sota.tools.Camera;

import java.net.UnknownHostException;

public class VideoService {

    private VideoStreamSender vidStreamer = null;
    private Camera camera = null;
    private UdpSender camUdpSender = null;

    private boolean enabled = false;
    private String lastError = null;

    public String getLastError() {
        String err = lastError;
        lastError = null;
        return err;
    }

    public Integer getPort() {
        if (camUdpSender == null) return null;
        return camUdpSender.getPort();
    }

    public String getIP() {
        if (camUdpSender == null) return null;
        return camUdpSender.getIP();
    }

    public boolean isEnabled() { return enabled; }

    public boolean enable(String ip, int port, Camera.ImageSize imageSize, Camera.CaptureFormat captureFormat) {
        if (isEnabled()) return true;

        try {
            camUdpSender = new UdpSender(ip, port);
            camera = new Camera(imageSize, captureFormat);
            camera.start();
            vidStreamer = new VideoStreamSender(camera::snap,
                    frame->camUdpSender.send_in_chunks(frame.data, frame.size));
            vidStreamer.start();

        } catch (UnknownHostException e) {
            lastError = "Error, unknown host: " + ip;
            return false;
        }

        enabled=true;
        System.out.println(this.getClass().getName()+" successfully enabled ("+ip+":"+port+").");
        return true;
    }

    public boolean disable() {
        if (!isEnabled()) return true;
        vidStreamer.stop();
        camera.stop();
;
        enabled=false;
        System.out.println(this.getClass().getName()+" successfully disabled");
        return true;
    }

    public VideoStatus getStatus() {
//        AudioStatus status = new AudioStatus();
//        status.enabled = this.isEnabled();
//        status.volume = -1;
//        status.sampleRate = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_RATE);
//        status.bufferSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE);
//        status.sampleSize_bits = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_SIZE);
//        status.channels = Properties.getPropAsInt(PropertyKey.KEY_MIC_CHANNELS);
//        status.streamPort = this.getPort();
//        status.streamIP = this.getIP();

        return null;
    }

    public ActionResult postStatus(VideoStatus status, String ip) {
//        // unset parameters are null.
//        String error = "";
//        if (status.enabled != null && status.enabled != this.isEnabled()) {  // 'enabled' changed
//            if (status.enabled) {  // turn on
//                if (status.streamIP != null) ip = status.streamIP; // overrule source IP by request IP
//
//                if (status.bufferSize != null) Properties.setProperty(PropertyKey.KEY_MIC_BUFFER_SIZE, status.bufferSize.toString());
//
//                if (status.streamPort == null) // we need a port
//                    error += "Error enabling microphone, did not receive a port: " +
//                            "IP '" + ip + "', port 'null'. ";
//                else if (!this.enable(ip, status.streamPort))
//                    error += this.getLastError();
//            } else { // request to disable
//                if (!this.disable())
//                    error += this.getLastError();
//            }
//        }  // end "enabled" change
//
//        if (error.equals("")) // no errror
//            return ActionResult.ok();
//        else return ActionResult.fail(error);
        return null;
    }
}