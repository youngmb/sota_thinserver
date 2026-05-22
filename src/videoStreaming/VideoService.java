package videoStreaming;

import Udp.UdpSender;
import httpserver.ActionResult;
import main.Properties;
import main.PropertyKey;
import sota.tools.Camera;


import java.awt.*;
import java.net.UnknownHostException;

public class VideoService {

    private VideoStreamSender vidStreamer = null;
    private Camera camera = null;
    private UdpSender camUdpSender = null;

    private VideoFrame.ImageFormat imageFormat = null;
    private VideoFrame.ImageSize imageSize = null;
    private final VideoFrame.ImageFormat DEFAULT_IMAGE_FORMAT = VideoFrame.ImageFormat.valueOf(Properties.getProperty(PropertyKey.KEY_VID_IMAGE_FORMAT));
    private final VideoFrame.ImageSize DEFAULT_IMAGE_SIZE = VideoFrame.ImageSize.valueOf(Properties.getProperty(PropertyKey.KEY_VID_IMAGE_SIZE));

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

    public boolean enable(String ip, int port) {

        if (isEnabled()) return true;

        if (this.imageFormat == null || this.imageSize == null){
            lastError = "Critical error: no image format or size, not even defaults. shouldn't happen";
            return false;
        }

        try {
            camUdpSender = new UdpSender(ip, port);
            camera = new Camera(imageSize, imageFormat);
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

        enabled=false;
        System.out.println(this.getClass().getName()+" successfully disabled");
        return true;
    }

    public VideoStatus getStatus() {
        VideoStatus status = new VideoStatus();
        status.bitrate_cap_kbps = Properties.getPropAsInt(PropertyKey.KEY_VID_BITRATE_CAP);
        status.enabled = this.isEnabled();
        status.streamPort = this.getPort();
        status.streamIP = this.getIP();
        status.streamImageSize = this.imageSize;
        status.streamImageFormat = this.imageFormat;
        return status;
    }

    public ActionResult postStatus(VideoStatus status, String ip) {
        // unset parameters are null.
        String error = "";
        if (status.enabled != null && status.enabled != this.isEnabled()) {  // 'enabled' changed
            if (status.enabled) {  // turn on
                if (status.streamIP != null) ip = status.streamIP; // overrule source IP by request IP if given

                if (status.streamImageFormat != null) // overwrite if new setting came in
                    this.imageFormat = status.streamImageFormat;
                else if (this.imageFormat == null)  // ensure we have an existing setting or use default
                    this.imageFormat = DEFAULT_IMAGE_FORMAT;

                if (status.streamImageSize != null)
                    this.imageSize = status.streamImageSize;
                else if (this.imageSize == null)
                    this.imageSize = DEFAULT_IMAGE_SIZE;

                if (status.streamPort == null) // we need a port
                    error += "Error enabling video, did not receive a port: " +
                            "IP '" + ip + "', port 'null'. ";

                if (error.isEmpty())
                    if (!this.enable(ip, status.streamPort))
                        error += this.getLastError();

            } else {  // request to disable
                if (!this.disable()) error += this.getLastError();
            }
        }
        if (error.isEmpty())
            return ActionResult.ok();
        else
            return ActionResult.fail(error);
    }
}