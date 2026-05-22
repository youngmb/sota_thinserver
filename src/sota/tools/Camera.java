//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
/// /////// heavily modified from DECOMPILED FROM SOTA PACKAGED version
package sota.tools;

import com.sun.jna.Native;

import java.io.*;

import jp.vstone.RobotLib.CRobotUtil;
import videoStreaming.VideoFrame;
import videoStreaming.VideoFrame.ImageSize;
import videoStreaming.VideoFrame.ImageFormat;


public class Camera {
    private static final String TAG = "CameraCapture";

    public static final int CAP_FORMAT_YUV2 = 0;
    public static final int CAP_FORMAT_MJPG = 1;
    public static final int CAP_FORMAT_3BYTE_BGR = 2;
    public static final int CAP_FORMAT_BYTE_GRAY = 3;

    LibCameraV4L2 snapcamera;
    private ImageSize imageSize;
    private ImageFormat format;
    private int fd;
    private boolean b_capturing = false;
    private byte[] imagedata;
    String device = "/dev/video0";

    public Camera(ImageSize image_size, ImageFormat format) {
        CRobotUtil.setDebugOut("CameraCapture", false);
        CRobotUtil.Debug("CameraCapture", "SotaCameraStill");

        this.format = format;
        this.snapcamera = (LibCameraV4L2)Native.loadLibrary("libsotacamv4l2.so", LibCameraV4L2.class);
        this.fd = -1;
        this.setSize(image_size);
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

    private void setSize(ImageSize imageSize) {
        CRobotUtil.Debug("CameraCapture", "setsize");

        this.imageSize = imageSize;
        int bufsize = this.imageSize.width * this.imageSize.height;
        switch (this.format) {
            case YUV2:
            case MJPG:  // mjpeg much smaller but caps out at 16bpp
                bufsize *= 2;
                break;
            case BGR_3BYTE:
                bufsize *= 3;
        }

        this.imagedata = new byte[bufsize];
        return;

    }

    public void openDevice(String dev) throws IOException {
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
    }

    public VideoFrame snap() {
        int retrycnt = 0;
        CRobotUtil.Debug("CameraCapture", "snap");
        int length = -1;

        while(true) {
            length = this.snapcamera.get_capturing(this.fd, this.format.libraryKey, this.imageSize.width, this.imageSize.height, this.imagedata);
            if (length >= 0) {
                retrycnt = 0;
                return new VideoFrame(length, this.imagedata);
            }

            try {
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
            } catch (IOException e) {
                System.out.println(e);  // but don't propagate
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
}