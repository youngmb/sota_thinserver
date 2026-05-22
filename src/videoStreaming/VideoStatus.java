package videoStreaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class VideoStatus {

    public VideoFrame.ImageSize[] supportedSizes = VideoFrame.ImageSize.values();

    public VideoFrame.ImageFormat[] supportedFormats = VideoFrame.ImageFormat.values();

    public Boolean enabled = false;
    public Integer streamPort  = null;;
    public String streamIP  = null;;
    public VideoFrame.ImageSize streamImageSize = null;
    public VideoFrame.ImageFormat streamImageFormat = null;
    public Integer bitrate_cap_kbps = null;
}
