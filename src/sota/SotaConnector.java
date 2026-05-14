package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;

import java.util.HashMap;
import java.util.Map;

public class SotaConnector  {

    private final String TAG = "Sota Thinserver Connector";

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
        if (ranges == null) {
            CRobotUtil.Log(TAG, "Failed to load servo ranges");
            return false;
        }
        CRobotUtil.Log(TAG, "Servo Ranges Loaded");

        return true;
    }
}