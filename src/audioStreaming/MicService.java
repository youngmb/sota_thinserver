package audioStreaming;

import UdpServers.UdpSender;

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
            micUdpSender = new UdpSender(ip, port);
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
}