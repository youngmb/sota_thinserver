package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;

import java.util.HashMap;
import java.util.Map;

public class SotaConnector  {

    private final String TAG = "Sota Thinserver Connector";

//    CRobotPose _sotaPose = new CRobotPose();
    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);
    boolean motorsEnabled = false;

    public ServoRangeTool ranges = null;

    public SotaConnector() {
    ; // pass
    }

    public void enableMotors() { sotaMotion.ServoOn(); motorsEnabled = true;}
    public void disableMotors() { sotaMotion.ServoOff(); motorsEnabled = false;}
    public boolean isMotorsEnabled() { return motorsEnabled; }

    public boolean start() {
        if(!sotaMem.Connect()) { // connect to the robot's subsystem
            CRobotUtil.Log(TAG, "Sota connection failure " + TAG);
            return false;
        }

        CRobotUtil.Log(TAG, "connected " + TAG);
        sotaMotion.InitRobot_Sota();  // initialize the Sota VSMD
        CRobotUtil.Log(TAG, "Rev. " + sotaMem.FirmwareRev.get());

        ranges = ServoRangeTool.Load();
        if (ranges == null)
            return false;
        CRobotUtil.Log(TAG, "Servo Ranges Loaded");

        return true;
    }
//    void run() {
//
//        _sotaMotion.ServoOn();
//        CRobotUtil.Log(TAG, "Servos On"); // initialize in on state
//
//        Short[] startPose = new Short[]{-20, -261, -568, 261, 568, 31, -95, 26};  // a neutral stance
//        CRobotPose pose = new CRobotPose();
//        pose.SetPose(_sotaMotion.getDefaultIDs(), startPose);
//        _sotaMotion.play(pose, 1000);
//        _sotaMotion.waitEndinterpAll();
//        }
//    }

}