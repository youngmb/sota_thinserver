package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import sota.kinematics.*;
import sota.pose.PoseCommand;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SotaConnector implements Runnable {

    private final String TAG = "Sota Thinserver Connector";

    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);
    boolean motorsEnabled = false;
    String sotaThreadKey = null;   // steal Sota's internal lock key so we can work across a different thread

    private Thread workerThread = null;
    private volatile boolean running = false;

    public SotaMappingTools servoMapper = null;

    private final Deque<ActionQueueEntry> sotaActionQueue = new ArrayDeque<>(); // TODO: NAME AND THINK ABOUT QUEUE SIZE

    private final ReentrantLock waitLock = new ReentrantLock(); // lock to enforce order of operations
    private final Condition hasItem = waitLock.newCondition();
    private Condition condDoneHandlingItem = waitLock.newCondition();
    private volatile boolean handlingItem = false;
    private volatile boolean waitForItem = false;

    static private class ActionQueueEntry {
        CRobotPose pose;
        int msec;

        ActionQueueEntry(CRobotPose pose, int msec) {  this.pose = pose;  this.msec = msec; }
    }

    public SotaConnector() {
        ; // pass
    }

    public void enableMotors() {
        sotaMotion.play(sotaMotion.getReadPose(), 100, this.sotaThreadKey);  // make sure the target loc is current when enabling
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

    public double[] getMotorValuesAsRadians() {
        return servoMapper.calcAngles_array(sotaMotion.getReadPose());
    }

    public CRobotPose getTargetPose() { return sotaMotion.getTargetPose();}
    public CRobotPose getCurrentPose() { return sotaMotion.getReadPose();} // does not include color :(

    public boolean start() {
        if (!sotaMem.Connect()) { // connect to the robot's subsystem
            CRobotUtil.Log(TAG, "Sota connection failure " + TAG);
            return false;
        }

        CRobotUtil.Log(TAG, "connected " + TAG);
        sotaMotion.InitRobot_Sota();  // initialize the Sota VSMD
        CRobotUtil.Log(TAG, "Rev. " + sotaMem.FirmwareRev.get());

        servoMapper = SotaMappingTools.Load();
        if (servoMapper == null) {
            CRobotUtil.Log(TAG, "Failed to load servo ranges");
            return false;
        }
        CRobotUtil.Log(TAG, "Servo Ranges Loaded");

        this.sotaThreadKey = sotaMotion.getThreadkey();  // steal Sota's thread key in the same thread we started the subsystem

        running = true;
        workerThread = new Thread(this, "Sota Connector action queue draining worker thread");
        workerThread.start();

        return true;
    }

    public void stop() {
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

    public void addPoseToActionQueue(CRobotPose pose, PoseCommand.CommandType command, int msec) {
        ActionQueueEntry entry = new ActionQueueEntry(pose, msec);

        try {
            waitLock.lock();
            switch (command) {
                case APPEND:
                    sotaActionQueue.addLast(entry);
                    hasItem.signalAll();
                    break;

                case PREPEND:
                    sotaActionQueue.addFirst(entry);
                    hasItem.signalAll();
                    break;

                case CLEAR_AND_ADD:   // doesn't interrupt. finishes current
                    sotaActionQueue.clear();
                    sotaActionQueue.addLast(entry);
                    hasItem.signalAll();
                    break;

                case INTERRUPT_AND_PREPEND:
                    try {
                        sotaActionQueue.addFirst(entry);  // prepend before interrupting, while we have the lock

                        if (handlingItem) {  // currently doing an action, so
                            workerThread.interrupt(); // wakeup the timer
                            System.out.println("called for interruption");
                        }

                        while (handlingItem) // wait until its done, protected for spurious wakeups
                            condDoneHandlingItem.await(); // release lock and wait until signalled

                        hasItem.signalAll();  // if it was blocked waiting, let it know that an item is available.

                    } catch (InterruptedException e) {
                        ; // why would this happen? shutdown?
                    }
                    break;
            }

        } finally {
            waitLock.unlock();
        }
    }

    // the Sota action queue managing thread
    @Override
    public void run() {

        while (running) {
            waitLock.lock();
            ActionQueueEntry action = null;
            try {
                while (sotaActionQueue.isEmpty())
                    hasItem.await();  // wait until we have an item

                System.out.println("Size "+sotaActionQueue.size());
                action = sotaActionQueue.removeFirst();  // never blocks
                System.out.println(action.msec + "  ");
                handlingItem = true;

            } catch (InterruptedException e) {
                ; // probably end of program.
            } finally {
                waitLock.unlock(); // unlock while doing work
            }

            // real work
            if (action != null && running) {
                sotaMotion.play(action.pose, action.msec, this.sotaThreadKey);
                try {
                    Thread.sleep(action.msec);
                } catch (InterruptedException e) {
                    System.out.println(e);
                    ; // woken up or end of program
                }
            }

            if (action != null && running) {
                try {
                    waitLock.lock();
                    handlingItem = false;
                    condDoneHandlingItem.signalAll();

                } finally {
                    waitLock.unlock();
                }
            }
        }
    }
}