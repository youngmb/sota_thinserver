package sota.pose;

import httpserver.ActionResult;
import jp.vstone.RobotLib.CRobotPose;
import sota.SotaConnector;
import sota.kinematics.SotaMappingTools;
import sota.kinematics.SotaMappingTools.LEDs;

import java.util.HashMap;
import java.util.Map;

public class PoseService {

    boolean servosEnabled = false;
    SotaConnector sota;

    public PoseService(SotaConnector sota) {
        this.sota = sota;
    }

    public PoseSystemStatus getSystemStatus() {
        PoseSystemStatus status = new PoseSystemStatus();
        status.servosEnabled = sota.isMotorsEnabled();

        for (String key : SotaMappingTools.motorIdByName.keySet()) {
            status.servoCapabilities.add(new PoseSystemStatus.ServoCapability(
                key, sota.servoMapper.getMinRad(key), sota.servoMapper.getMaxRad(key)
            ));
        }

        return status;
    }

    public ActionResult postSystemStatus(PoseSystemStatus status, String _unused) {
        if (status.servosEnabled != null && status.servosEnabled != this.servosEnabled) {  // 'enabled' changed
            if (status.servosEnabled) { // turn on
                sota.enableMotors();
                this.servosEnabled = true;
            } else { // request to disable
                sota.disableMotors();
                this.servosEnabled = false;
            }
        }
        return ActionResult.ok();
    }

    private void addLEDStatus(CRobotPose pose, PoseStatus status) {
        Map<Byte, Short> colors = pose.getLed();
        for (LEDs led: LEDs.values()) {
            status.LEDStatus.add(new PoseStatus.LEDStatus( led.label, SotaMappingTools.getLEDColor(colors, led)));
        }
    }

    public PoseStatus getJointSpaceStatus() {
        PoseStatus status = new PoseStatus();
        double[] angles = sota.getMotorValuesAsRadians();

        for (Map.Entry<String, Byte> entry : SotaMappingTools.motorIdByName.entrySet()) {
            int motorIndex = SotaMappingTools.IDtoIndex.get(entry.getValue());
            status.servoStatus.add(new PoseStatus.ServoStatus(entry.getKey(), angles[motorIndex]));
        }

        addLEDStatus(sota.getTargetPose(), status);
        return status;
    }

    public ActionResult postJointSpaceStatus(PoseCommand command, String _unused) {
        Map<Byte, Short> poseMap = new HashMap<>();
        Map<Byte, Short> LEDMap = new HashMap<>();

        for (PoseStatus.ServoStatus servo: command.servoStatus) {
            if (servo.servo_id != null) {  // otherwise skip  //TODO: consider if we should put err message responss here
                Byte id = SotaMappingTools.motorIdByName.get(servo.servo_id);
                if (servo.radians != null) {
                    short motorPosition = sota.servoMapper.radToPos(id, servo.radians);
                    poseMap.put(id, motorPosition);
                }
            }
        }

        for (PoseStatus.LEDStatus ledEntry: command.LEDStatus)
            if (ledEntry.led_id != null && ledEntry.color != null)  //TODO: consider if we should put err message response here
                SotaMappingTools.setLED(LEDMap, ledEntry.led_id, ledEntry.color);

        CRobotPose pose = new CRobotPose();
        pose.SetPose(poseMap);
        pose.SetLed(LEDMap);

        if (command.command == null) command.command = PoseCommand.CommandType.DEFAULT_COMMAND;

        sota.addPoseToActionQueue(pose, command.command, command.move_msec);
        return ActionResult.ok();
    }

    public PoseStatus getWorldSpaceStatus() {
//        SotaForwardK FK = new SotaForwardK(sota.servoMapper.calcAngles(_sotaMotion.getReadPose()));
//        double[] leftCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.L_HAND)).toArray();
//        double[] rightCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.R_HAND)).toArray();
//        double[] headCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.HEAD)).toArray();
        return null;
    }

    public ActionResult postWorldSpaceStatus(PoseStatus status, String _unused) {
        return null;
    }
}
