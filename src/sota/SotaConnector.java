package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;

public class SotaConnector  {

    private final String TAG = "Sota Thinserver Connector";

    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);
    boolean motorsEnabled = false;

    public ServoMappingTools servoMapper = null;

    public SotaConnector() {
    ; // pass
    }

    public void enableMotors() { sotaMotion.play(sotaMotion.getReadPose(),0); sotaMotion.ServoOn(); motorsEnabled = true;}
    public void disableMotors() { sotaMotion.ServoOff(); motorsEnabled = false;}
    public boolean isMotorsEnabled() { return motorsEnabled; }

//    public Short[] getMotorValues() {
//        CRobotPose pose = sotaMotion.getReadPose();
//        return pose.getServoAngles(sotaMotion.getDefaultIDs());
//    }

    public void setSparseMotorValuesInRadians(Double[] target_angles, int msec) { // only update for non-null entries
        double[] angles = servoMapper.calcAngles_array(sotaMotion.getReadPose());

        for (int i = 0; i < target_angles.length; i++) {  // only change motors for which new values arrived
            if (target_angles[i] == null) continue;
            angles[i] = target_angles[i];
        }
        sotaMotion.play(servoMapper.makePose_fromRadians(angles), msec);
    }

    public double[] getMotorValuesAsRadians() {return servoMapper.calcAngles_array(sotaMotion.getReadPose());}


    public boolean start() {
        if(!sotaMem.Connect()) { // connect to the robot's subsystem
            CRobotUtil.Log(TAG, "Sota connection failure " + TAG);
            return false;
        }

        CRobotUtil.Log(TAG, "connected " + TAG);
        sotaMotion.InitRobot_Sota();  // initialize the Sota VSMD
        CRobotUtil.Log(TAG, "Rev. " + sotaMem.FirmwareRev.get());

        servoMapper = ServoMappingTools.Load();
        if (servoMapper == null) {
            CRobotUtil.Log(TAG, "Failed to load servo ranges");
            return false;
        }
        CRobotUtil.Log(TAG, "Servo Ranges Loaded");

        return true;
    }
}