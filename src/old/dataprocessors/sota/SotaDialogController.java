//package old.dataprocessors.sota;
//
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//import old.dataprocessors.DataProcessor;
//import old.datatypes.Data;
//import old.datatypes.behaviors.BackchannelEvent;
//import old.datatypes.behaviors.NodBackchannelEvent;
//import old.datatypes.behaviors.UtteranceBackchannelEvent;
//import tools.ServoRangeTool;
//
//import jp.vstone.RobotLib.*;
//
///**
// * A controller for the Sota robot that manages its output for dialog.
// * Interacts with the Sota through the vstone API directly.
// */
//public class SotaDialogController extends DataProcessor {
//    /**
//     * SotaStateData defines the robot's states.
//     * BUSY - the robot is processing input or performing an action
//     * READY - the robot is ready to receive input
//     */
//    public enum SotaState { BUSY, READY }
//    public static class SotaStateData extends Data {
//        private static final long serialVersionUID = 1L;
//        public final SotaState data;
//
//        public SotaStateData(SotaState s) {
//            this.data = s;
//        }
//
//        public String toString() {
//            return data.name();
//        }
//    }
//
//    // ------------- SotaDialogController -------------
//    static final String TAG = "SotaDialogController";
//    private CSotaMotion motion;
//    private CRobotMem mem;
//
//    // robot motor poses
//    private CRobotPose nodNeutral = null;
//    private CRobotPose nodDown = null;
//    private CRobotPose nodUp = null;
//
//    private SotaState state;
//    private long backchannelFinishTimeMs;    // when the robot will finish speaking
//    private static final long MIN_BACKCHANNEL_INTERVAL_MS = 1000; // minimum time between backchannels
//
//    private final ScheduledExecutorService readyNotifier = Executors.newSingleThreadScheduledExecutor();
//
//    public SotaDialogController() {
//        this.mem = new CRobotMem();
//		this.motion = new CSotaMotion(mem);
//        this.backchannelFinishTimeMs = 0;
//        this.state = SotaState.READY;
//
//        mem.Connect();
//        this.motion.InitRobot_Sota();
//        this.motion.ServoOn();
//        this.initNodPoses();
//    }
//
//    @Override
//    protected Data process(Data input, EventGenerator sender) {
//        BackchannelEvent backchannel = (BackchannelEvent) input;
//        return update(backchannel);
//    }
//
//    public SotaStateData update(BackchannelEvent backchannel) {
//        // manage internal Sota state
//        if (this.state == SotaState.BUSY) {
//            long currentTimeMs = System.currentTimeMillis();
//            if (currentTimeMs > this.backchannelFinishTimeMs) {
//                this.state = SotaState.READY;
//            }
//        }
//        if (this.state == SotaState.READY) {
//            executeBackchannel(backchannel);
//            this.state = SotaState.BUSY;
//            scheduleReadyNotification();
//        }
//        return new SotaStateData(this.state);
//    }
//
//    private void scheduleReadyNotification() {
//        long delayMs = Math.max(0, this.backchannelFinishTimeMs - System.currentTimeMillis());
//        readyNotifier.schedule(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (SotaDialogController.this) {
//                    state = SotaState.READY;
//                    SotaDialogController.this.notifyListeners(new SotaStateData(state));
//                }
//            }
//        }, delayMs, TimeUnit.MILLISECONDS);
//    }
//
//    /**
//     * Executes the given backchannel behavior on the robot.
//     * @param backchannel the backchannel event to execute
//     */
//    private void executeBackchannel(BackchannelEvent backchannel) {
//        if (backchannel.getType() == BackchannelEvent.BEHAVIOR_TYPE.NOD) {
//            playNod((NodBackchannelEvent) backchannel);
//        } else if (backchannel.getType() == BackchannelEvent.BEHAVIOR_TYPE.UTTERANCE) {
//            playVerbalBackchannel((UtteranceBackchannelEvent) backchannel);
//        } else if (backchannel.getType() == BackchannelEvent.BEHAVIOR_TYPE.LOOK) {
//            System.out.println("Look backchannel received.");
//        }
//    }
//
//    // plays a backchannel
//    private void playVerbalBackchannel(UtteranceBackchannelEvent utteranceEvent) {
//        long playTime = CPlayWave.getPlayTime("../resources/utterances/test_hmm.wav");
//        long currentTimeMs = System.currentTimeMillis();
//        this.backchannelFinishTimeMs = currentTimeMs + playTime + MIN_BACKCHANNEL_INTERVAL_MS;
//        CPlayWave.PlayWave("../resources/utterances/test_hmm.wav");
//    }
//
//    // Adjust head pitch to make Sota nod using the nod poses
//    private void playNod(NodBackchannelEvent nodEvent) {
//        long playTime = 1000;
//        long currentTimeMs = System.currentTimeMillis();
//        this.backchannelFinishTimeMs = currentTimeMs + playTime + MIN_BACKCHANNEL_INTERVAL_MS;
//
//        int speed = NodBackchannelEvent.SPEED_MIN_MS + (NodBackchannelEvent.SPEED_MAX_MS - NodBackchannelEvent.SPEED_MIN_MS) / nodEvent.getSpeed();
//        int amplitude = nodEvent.getAmplitude();
//
//        if (amplitude <= 33) {
//            // small nod
//            this.smallNod(speed);
//        } else if (amplitude <= 66) {
//            // medium nod
//            this.mediumNod(speed);
//        } else {
//            // intense nod
//            this.intenseNod(speed);
//        }
//    }
//
//    private void mediumNod(int speed) {
//        this.motion.play(nodDown, speed);
//        this.motion.waitEndinterpAll();
//
//        this.motion.play(nodUp, speed*2);
//        this.motion.waitEndinterpAll();
//
//        this.motion.play(nodNeutral, speed);
//        this.motion.waitEndinterpAll();
//    }
//
//    private void smallNod(int speed) {
//        this.motion.play(nodDown, speed);
//        this.motion.waitEndinterpAll();
//
//        this.motion.play(nodNeutral, speed);
//        this.motion.waitEndinterpAll();
//    }
//
//    private void intenseNod(int speed) {
//        this.motion.play(nodUp, speed*2);
//        this.motion.waitEndinterpAll();
//
//        this.motion.play(nodDown, speed);
//        this.motion.waitEndinterpAll();
//
//        this.motion.play(nodNeutral, speed);
//        this.motion.waitEndinterpAll();
//    }
//
//    /**
//     * Initializes the motor poses for head nodding (head pitch motor positions).
//     * Ensure the robot starts in a neutral position with its head facing forward.
//     */
//    private void initNodPoses() {
//        ServoRangeTool ranges = ServoRangeTool.Load("../resources/servo/head_nod_motor_positions");
//        CRobotPose minPose = ranges.getMinPose();
//        CRobotPose maxPose = ranges.getMaxPose();
//        CRobotPose midPose = ranges.getMidPose();
//
//        this.nodNeutral = this.motion.getReadPose();
//        this.nodDown = this.motion.getReadPose();
//        this.nodUp = this.motion.getReadPose();
//
//        // extract just the head pitch motor position from saved ServoRangeTool obj
//        // and don't change the rest of the pose
//        Short minHeadPitch = minPose.getServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P));
//        Short maxHeadPitch = maxPose.getServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P));
//        Short midHeadPitch = midPose.getServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P));
//
//        this.nodUp.addServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P), minHeadPitch);
//        this.nodNeutral.addServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P), midHeadPitch);
//        this.nodDown.addServoAngle(Byte.valueOf(CSotaMotion.SV_HEAD_P), maxHeadPitch);
//    }
//}