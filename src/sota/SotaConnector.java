package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SotaConnector  {

    private final String TAG = "Sota Thinserver Connector";

    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);
    boolean motorsEnabled = false;
    String sotaThreadKey=null;

    public ServoMappingTools servoMapper = null;

    private final BlockingQueue<ActionQueueEntry> sotaActionQueue = new ArrayBlockingQueue<>(1000); // TODO: NAME AND THINK ABOUT QUEUE SIZE

    static private class ActionQueueEntry {
        CRobotPose pose;
        int msec;
        ActionQueueEntry(CRobotPose pose, int msec){
            this.pose=pose; this.msec=msec;
        }
    }


    public SotaConnector() {
    ; // pass
    }

    public void enableMotors() { sotaMotion.ServoOn(); motorsEnabled = true;}
    public void disableMotors() { sotaMotion.ServoOff(); motorsEnabled = false;}
    public boolean isMotorsEnabled() { return motorsEnabled; }

    public void addPoseToActionQueue(CRobotPose pose, int msec) { // only update for non-null entries
        sotaActionQueue.add(new ActionQueueEntry(pose, msec));
        sotaMotion.play(pose, msec, this.sotaThreadKey);
//        sotaMotion.waitEndinterpAll();
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

        this.sotaThreadKey = sotaMotion.getThreadkey();

        // spin up sota action thread queue

        return true;
    }

    //RUN
}