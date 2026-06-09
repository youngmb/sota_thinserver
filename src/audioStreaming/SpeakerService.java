package audioStreaming;

import Udp.UdpReceiver;
import httpserver.ActionResult;
import main.Properties;
import main.PropertyKey;

import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SpeakerService {
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1000);  // randomly picked 100. should not get large.
    private final AudioStreamPlayer speaker = new AudioStreamPlayer(queue);
    private UdpReceiver udpReceiver = null;

    private boolean enabled = false;
    private String lastError = null;

    public SpeakerService() {
        ; // pass
    }

    public String getLastError() {
        String err = lastError;
        lastError = null;
        return err;
    }

    public Integer getPort() {
        if (udpReceiver == null) return null;
        return udpReceiver.getPort();
    }

    public boolean isEnabled() { return enabled; }

    public boolean enable(int port) {
        if (isEnabled()) return true;

        try {
            udpReceiver = new UdpReceiver(port, queue);
        } catch (SocketException e) {
            lastError = "Error: unable to bind to requested port for listening: port '"+port+"'";
            return false;
        }
        udpReceiver.start();
        speaker.start();
        enabled = true;
        System.out.println(this.getClass().getName()+" successfully enabled");
        return true;
    }

    public boolean disable() {
        if (!isEnabled()) return true;

        if (udpReceiver == null) { // weird
            System.err.println("disabling a running speaker service without an object");
            lastError = "Error, internal system. check logs";
            return false;
        }
        udpReceiver.stop();
        speaker.stop();
        enabled = false;
        System.out.println(this.getClass().getName()+" successfully disabled");
        return true;
    }

    public AudioStatus getStatus() {
        AudioStatus status = new AudioStatus();
        status.enabled = this.isEnabled();
        status.volume = -1;
        status.sampleRate = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_RATE);
        status.bufferSize = Properties.getPropAsInt(PropertyKey.KEY_SPK_BUFFER_SIZE);
        status.sampleSize_bits = Properties.getPropAsInt(PropertyKey.KEY_SPK_SAMPLE_SIZE);
        status.channels = Properties.getPropAsInt(PropertyKey.KEY_SPK_CHANNELS);
        status.streamPort = this.getPort();
        return status;
    }

    public ActionResult postStatus(AudioStatus status, String _unused) {
        // unset parameters are null.
        String error = "";

        if (status.enabled != null && status.enabled != this.isEnabled()) {  // 'enabled' changed
            if (status.enabled) {

                if (status.sampleRate != null) Properties.setProperty(PropertyKey.KEY_SPK_SAMPLE_RATE, status.sampleRate.toString());

                if (status.bufferSize != null) Properties.setProperty(PropertyKey.KEY_SPK_BUFFER_SIZE, status.bufferSize.toString());

                if (status.streamPort == null) // we need a port
                    error += "Error enabling speaker, did not receive a port to listen on: " +
                            "port '" + status.streamPort + "'. ";

                else if (!this.enable(status.streamPort))
                    error += this.getLastError();

            } else // request to disable
                if (!this.disable())
                    error += this.getLastError();
        }
        if (error.equals("")) // no errror
            return ActionResult.ok();
        else return ActionResult.fail(error);
    }

}
