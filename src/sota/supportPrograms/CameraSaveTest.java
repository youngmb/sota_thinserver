package sota.supportPrograms;

import jp.vstone.RobotLib.CRobotUtil;
import sota.tools.Camera;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraSaveTest {
    final static String TAG = "Camera Save Test";

    public static void main(String[] args){
        Camera cam = new Camera(Camera.ImageSize.HD_1080, Camera.CaptureFormat.MJPG);
        cam.start();
        CRobotUtil.Log(TAG, "Camera Startup Successful");
        int imagedataLength = 0;

        try {
            imagedataLength = cam.snap();
        } catch (IOException e) {
            System.out.println("Error snapping image.");
            throw new RuntimeException(e);
        }
        CRobotUtil.Log(TAG, "Camera snap Successful");
        try (FileOutputStream fos = new FileOutputStream("frame.jpg")) {
            fos.write(cam.getImageRawData(), 0, imagedataLength);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        cam.stop();
        CRobotUtil.Log(TAG, "Program End Reached");
    }

}
