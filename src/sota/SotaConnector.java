package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SotaConnector implements Runnable {

    private final String TAG = "Sota Thinserver Connector";

    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);
    boolean motorsEnabled = false;
    String sotaThreadKey = null;   // steal Sota's internal lock key so we can work across a different thread

    private Thread workerThread = null;
    private volatile boolean running = false;

    public ServoMappingTools servoMapper = null;

    private final BlockingQueue<ActionQueueEntry> sotaActionQueue = new ArrayBlockingQueue<>(1000); // TODO: NAME AND THINK ABOUT QUEUE SIZE

    static private class ActionQueueEntry {
        CRobotPose pose;
        int msec;

        ActionQueueEntry(CRobotPose pose, int msec) {
            this.pose = pose;
            this.msec = msec;
        }
    }

    public SotaConnector() {
        ; // pass
    }

    public void enableMotors() {
        // ServoOn does not work across threads so we need to manually tell the Sota to start at the current pos.
        sotaMotion.play(sotaMotion.getReadPose(), 100, this.sotaThreadKey);
        sotaMotion.ServoOn();
        motorsEnabled = true;
    }

    public void disableMotors() {
        sotaMotion.ServoOff();
        motorsEnabled = false;
    }

    public boolean isMotorsEnabled() {
        return motorsEnabled;
    }

    public void addPoseToActionQueue(CRobotPose pose, int msec) { // only update for non-null entries
        sotaActionQueue.add(new ActionQueueEntry(pose, msec));
//        sotaMotion.play(pose, msec, this.sotaThreadKey);
//        sotaMotion.waitEndinterpAll();
    }

    public double[] getMotorValuesAsRadians() {
        return servoMapper.calcAngles_array(sotaMotion.getReadPose());
    }


    public boolean start() {
        if (!sotaMem.Connect()) { // connect to the robot's subsystem
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

        this.sotaThreadKey = sotaMotion.getThreadkey();  // steal Sota's thread key in the same thread we started the subsystem

        running = true;
        workerThread = new Thread(this, "audio stream player thread");
        workerThread.start();

        return true;
    }

    public void stop() {  //TODO: currently the system has no clean shutdown implemented in SotaSystemController
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join();
            } catch (InterruptedException e) {
                ; // just continue assuming that this finished.
            }
        }
        sotaMem.Disconnect();
    }

    // the Sota action queue managing thread
    @Override
    public void run() {

        while(running) {
            try {
                ActionQueueEntry action = sotaActionQueue.take();

                if (action != null){
                    sotaMotion.play(action.pose, action.msec, this.sotaThreadKey);
                }

            } catch (InterruptedException e) {
                // interrupted for some reason, try again by looping around.
            }
        }

    }
}