package audioStreaming;

import UdpServers.UdpReceiver;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SpeakerService {
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(100);  // randomly picked 100. should not get large. avoids memory leak on hang
    private final AudioStreamPlayer speaker = new AudioStreamPlayer(queue);
    private UdpReceiver udpReceiver = null;

    private boolean enabled = false;
    private String lastError = null;

    public SpeakerService() {
        speaker.init();
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
        System.out.println(this.getClass().getName()+" successfully disabled");
        return true;
    }

}
