package audioStreaming;

import Udp.UdpSender;
import httpserver.status.ActionResult;
import httpserver.status.AudioStatus;
import main.Properties;
import main.PropertyKey;

import java.net.UnknownHostException;

public class MicService {

    private AudioStreamSender micStreamer = null;
    private UdpSender micUdpSender = null;

    private boolean enabled = false;
    private String lastError = null;

    public String getLastError() {
        String err = lastError;
        lastError = null;
        return err;
    }

    public Integer getPort() {
        if (micUdpSender == null) return null;
        return micUdpSender.getPort();
    }

    public String getIP() {
        if (micUdpSender == null) return null;
        return micUdpSender.getIP();
    }

    public boolean isEnabled() { return enabled; }

    public boolean enable(String ip, int port) {
        if (isEnabled()) return true;

        try {
            micUdpSender = new UdpSender(ip, port, Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE));
            micStreamer = new AudioStreamSender(micUdpSender::send);
            micStreamer.init();
            micStreamer.start();

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

        if (micStreamer == null) { // weird
            System.err.println("disabling a running Mic Service without an object");
            lastError = "Error, internal system. check logs";
            return false;
        }
        micStreamer.stop();
        micStreamer = null;
        micUdpSender = null;
        enabled=false;
        System.out.println(this.getClass().getName()+" successfully disabled");
        return true;
    }

    public AudioStatus getStatus() {
        AudioStatus status = new AudioStatus();
        status.enabled = this.isEnabled();
        status.volume = -1;
        status.sampleRate = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_RATE);
        status.bufferSize = Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE);
        status.sampleSize_bits = Properties.getPropAsInt(PropertyKey.KEY_MIC_SAMPLE_SIZE);
        status.channels = Properties.getPropAsInt(PropertyKey.KEY_MIC_CHANNELS);
        status.streamPort = this.getPort();
        status.streamIP = this.getIP();

        return status;
    }

    public ActionResult setStatus(AudioStatus status, String ip) {
        // unset parameters are null.
        String error = "";
        if (status.enabled != null && status.enabled != this.isEnabled()) {  // 'enabled' changed
            if (status.enabled) {  // turn on
                if (status.streamIP != null) ip = status.streamIP; // overrule source IP by request IP

                if (status.bufferSize != null) Properties.setProperty(PropertyKey.KEY_MIC_BUFFER_SIZE, status.bufferSize.toString());

                if (status.streamPort == null) // we need a port
                    error += "Error enabling microphone, did not receive a port: " +
                            "IP '" + ip + "', port 'null'. ";
                else if (!this.enable(ip, status.streamPort))
                    error += this.getLastError();
            } else { // request to disable
                if (!this.disable())
                    error += this.getLastError();
            }
        }  // end "enabled" change

        if (error.equals("")) // no errror
            return ActionResult.ok();
        else return ActionResult.fail(error);
    }
}