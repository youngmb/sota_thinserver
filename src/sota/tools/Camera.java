//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
/// /////// heavily modified from DECOMPILED FROM SOTA PACKAGED version
package sota.tools;

import com.sun.jna.Native;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.ImageObserver;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.camera.CameraCapture;

public class Camera {
    private static final String TAG = "CameraCapture";

    public static final int CAP_FORMAT_YUV2 = 0;
    public static final int CAP_FORMAT_MJPG = 1;
    public static final int CAP_FORMAT_3BYTE_BGR = 2;
    public static final int CAP_FORMAT_BYTE_GRAY = 3;

    LibCameraV4L2 snapcamera;
    private ImageSize imageSize;
    private CaptureFormat format;
    private int fd;
    private boolean b_capturing = false;
    private byte[] imagedata;
    String device = "/dev/video0";

    public enum ImageSize {
        QVGA(320, 240),
        VGA(640, 480),
        SVGA(800, 600),
        XGA(1024, 768),
        HD_720(1280, 720),
        SXGA(1280, 1024),
        UXGA(1600, 1200),
        HD_1080(1920, 1080),
        QXGA(2048, 1536),
        MP_5(2592, 1944);

        public final int width;
        public final int height;

        ImageSize(int width, int height) { this.width = width;  this.height = height;}
    }

    public enum CaptureFormat {  // to fit the underlying library
        YUV2(0),
        MJPG(1),
        BGR_3BYTE(2),
        BYTE_GRAY(3);

        public final int libraryKey;

        CaptureFormat(int libraryKey) {
            this.libraryKey = libraryKey;
        }
    }

    public Camera(ImageSize image_size, CaptureFormat format) {
        CRobotUtil.setDebugOut("CameraCapture", false);
        CRobotUtil.Debug("CameraCapture", "SotaCameraStill");

        this.format = format;
        this.snapcamera = (LibCameraV4L2)Native.loadLibrary("libsotacamv4l2.so", LibCameraV4L2.class);
        this.fd = -1;
        this.setsize(image_size);
    }

    public boolean start() {
        try {
            if (!(new File(this.device)).exists()) {
                File file = new File("/dev");
                File[] files = file.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.matches("video.*");
                    }
                });
                if (files != null && files.length > 0) {
                    this.device = files[0].getCanonicalPath();
                    CRobotUtil.Log("CRoboCamera", "Chenge Camere Device " + files[0].getCanonicalPath());
                }
            }

            if (this.device != null && (new File(this.device)).exists()) {
                this.openDevice(this.device);
                return true;
            }

            CRobotUtil.Err("CRoboCamera", "NotFound Camera Device");
        } catch (IOException e) {
            e.printStackTrace();
            CRobotUtil.PutErrorLogFile(e);
        }

        return false;
    }

    private void setsize(ImageSize imageSize) {
        CRobotUtil.Debug("CameraCapture", "setsize");

        this.imageSize = imageSize;
        int bufsize = this.imageSize.width * this.imageSize.height;
        switch (this.format) {
            case YUV2:
            case MJPG:
                bufsize *= 2;
                break;
            case BGR_3BYTE:
                bufsize *= 3;
        }

        this.imagedata = new byte[bufsize];
        return;

    }

    public int openDevice(String dev) throws IOException {
        CRobotUtil.Debug("CameraCapture", "openDevice " + dev);
        this.device = dev;
        CRobotUtil.Debug("CameraCapture", "openDevice");
        this.fd = this.snapcamera.open_device(dev);
        if (this.fd < 0) {
            this.onThrowCamError("can not open video device");
        }

        CRobotUtil.Debug("CameraCapture", "init_device");
        if (this.snapcamera.init_device(this.fd, this.format.libraryKey, this.imageSize.width, this.imageSize.height) < 0) {
            this.onThrowCamError("can not init video device");
        }

        CRobotUtil.Debug("CameraCapture", "start_capturing");
        if (this.snapcamera.start_capturing(this.fd, this.format.libraryKey) < 0) {
            this.onThrowCamError("can not start capturing video device");
        }

        this.b_capturing = true;
        CRobotUtil.Debug("CameraCapture", "fd " + this.fd);
        return this.fd;
    }

    public int snap() throws IOException {
        int retrycnt = 0;
        CRobotUtil.Debug("CameraCapture", "snap");
        int length = -1;

        while(true) {
            length = this.snapcamera.get_capturing(this.fd, this.format.libraryKey, this.imageSize.width, this.imageSize.height, this.imagedata);
            if (length >= 0) {
                retrycnt = 0;
                return length;
            }

            ++retrycnt;
            CRobotUtil.Debug("CameraCapture", "retrycnt " + retrycnt);
            if (retrycnt < 3) {
                this.snapcamera.stop_capturing(this.fd);
                this.snapcamera.close_device(this.fd);
                this.snapcamera.uninit_device(this.fd);
                CRobotUtil.Debug("CameraCapture", "Retry  openDevice" + retrycnt);
                CRobotUtil.wait(1500);
                this.openDevice(this.device);
            } else {
                this.b_capturing = false;
                this.snapcamera.stop_capturing(this.fd);
                this.snapcamera.close_device(this.fd);
                this.snapcamera.uninit_device(this.fd);
                this.onThrowCamError("can not snap video device:" + length);
            }
        }
    }

    public void stop() {
        CRobotUtil.Debug("CameraCapture", "close");
        this.snapcamera.stop_capturing(this.fd);
        this.snapcamera.close_device(this.fd);
        this.snapcamera.uninit_device(this.fd);
        this.b_capturing = false;
    }

    private void onThrowCamError(String comment) throws IOException {
        IOException ex = new IOException(comment);
        CRobotUtil.PutErrorLogFile(ex);
        throw ex;
    }

    public byte[] getImageRawData() {
        CRobotUtil.Debug("CameraCapture", "getImageRawData");
        return this.imagedata;
    }
}