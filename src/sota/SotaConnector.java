package sota;

import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CSotaMotion;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import sota.tools.*;
import sota.pose.PoseCommand;
import sota.pose.PoseStatus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SotaConnector implements Runnable {

    private final String TAG = "Sota Thinserver Connector";

    CRobotMem sotaMem = new CRobotMem();
    CSotaMotion sotaMotion = new CSotaMotion(sotaMem);

    public ServoMapper servoMapper = null;
    private SotaForwardK FK = null;
    boolean servosEnabled = false;
    String sotaThreadKey = null;   // steal Sota's internal lock key so we can work across a different thread

    private Thread workerThread = null;
    private volatile boolean running = false;



    private final Deque<ActionQueueEntry> sotaActionQueue = new ArrayDeque<>(); // TODO: NAME AND THINK ABOUT QUEUE SIZE

    private final ReentrantLock actionQueueLock = new ReentrantLock(); // lock to enforce order of operations
    private final Condition hasItem = actionQueueLock.newCondition();
    private final Condition doneHandlingItem = actionQueueLock.newCondition();
    private volatile boolean handlingItem = false;

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
        servosEnabled = true;
    }

    public void disableMotors() {
        sotaMotion.ServoOff();
        servosEnabled = false;
    }

    public boolean isServosEnabled() {
        return servosEnabled;
    }
    public void updateFK() {
        FK = new SotaForwardK( servoMapper.extractAngles( sotaMotion.getReadPose() ) );
    }

    public PoseStatus.EndpointStatus getEndpointStatus(Frames.FrameKeys frame) {
        if (FK==null) updateFK();

        return new PoseStatus.EndpointStatus(
                frame.label,
                MatrixHelp.getTrans( FK.frames.get(frame)).toArray(),
                FK.frames.get(frame).getColumn(0)  // 0 is x axis
        );
    }

    public RealVector solveIK(String endpoint_id, double[] position, RealVector startPosition){
        if (startPosition == null) // if no start position given, start from current robot position
            startPosition = MatrixUtils.createRealVector(servoMapper.extractAngles( sotaMotion.getReadPose() ));

        return SotaInverseK.solve(Frames.FrameKeys.fromLabel(endpoint_id), SotaInverseK.JType.O, MatrixUtils.createRealVector(position), startPosition);
    }

    public CRobotPose getTargetPose() { return sotaMotion.getTargetPose();}

    public double[] getMotorAngles() {
        return servoMapper.extractAngles(sotaMotion.getReadPose());
    }

    public CRobotPose makePose (RealVector angles) { return servoMapper.makePose(servoMapper.radToPos( angles ));}

    public boolean start() {
        if (!sotaMem.Connect()) { // connect to the robot's subsystem
            CRobotUtil.Log(TAG, "Sota connection failure " + TAG);
            return false;
        }

        CRobotUtil.Log(TAG, "connected " + TAG);
        sotaMotion.InitRobot_Sota();  // initialize the Sota VSMD
        CRobotUtil.Log(TAG, "Rev. " + sotaMem.FirmwareRev.get());

        servoMapper = ServoMapper.Load();
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
            actionQueueLock.lock();
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
                            doneHandlingItem.await(); // release lock and wait until signalled

                        hasItem.signalAll();  // if it was blocked waiting, let it know that an item is available.

                    } catch (InterruptedException e) {
                        ; // why would this happen? shutdown?
                    }
                    break;
            }

        } finally {
            actionQueueLock.unlock();
        }
    }

    // the Sota action queue managing thread
    @Override
    public void run() {

        while (running) {
            actionQueueLock.lock();
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
                actionQueueLock.unlock(); // unlock while doing work
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
                    actionQueueLock.lock();
                    handlingItem = false;
                    doneHandlingItem.signalAll();

                } finally {
                    actionQueueLock.unlock();
                }
            }
        }
    }
}