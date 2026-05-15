package sota.servos;

import httpserver.ActionResult;
import jp.vstone.RobotLib.CRobotPose;
import sota.SotaConnector;
import sota.kinematics.ServoMappingTools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServoService {

    boolean motorsEnabled = false;
    SotaConnector sota;

    public ServoService(SotaConnector sota) {
        this.sota = sota;
    }

    public ServoSystemStatus getSystemStatus() {
        ServoSystemStatus status = new ServoSystemStatus();
        status.enabled = sota.isMotorsEnabled();

        for (String key : ServoMappingTools.motorIdByName.keySet()) {
            status.motorCapabilities.add(new ServoSystemStatus.MotorCapability(
                key, sota.servoMapper.getMinRad(key), sota.servoMapper.getMaxRad(key)
            ));
        }
        return status;
    }

    public ActionResult postSystemStatus(ServoSystemStatus status, String _unused) {
        if (status.enabled != null && status.enabled != this.motorsEnabled) {  // 'enabled' changed
            if (status.enabled) { // turn on
                sota.enableMotors();
                this.motorsEnabled = true;
            } else { // request to disable
                sota.disableMotors();
                this.motorsEnabled = false;
            }
        }
        return ActionResult.ok();
    }

    public ServosStatus getJointSpaceStatus() {
        ServosStatus status = new ServosStatus();

        double[] angles = sota.getMotorValuesAsRadians();

        for (Map.Entry<String, Byte> entry : ServoMappingTools.motorIdByName.entrySet()) {
            int motorIndex = ServoMappingTools.IDtoIndex.get(entry.getValue());

            status.motorsStatus.add(new ServosStatus.ServoStatus(entry.getKey(), angles[motorIndex]));
        }
        return status;
    }

    public ActionResult postJointSpaceStatus(ServosCommand command, String _unused) {
        Map<Byte, Short> poseMap = new HashMap<>();

        for (ServosStatus.ServoStatus motor: command.motorsStatus) {
            if (motor.motor_id != null) {  // otherwise skip
                Byte id = ServoMappingTools.motorIdByName.get(motor.motor_id);
                if (motor.radians != null) {
                    short motorPosition = sota.servoMapper.radToPos(id, motor.radians);
                    poseMap.put(id, motorPosition);
                }
            }
        }

        CRobotPose pose = new CRobotPose();
        pose.SetPose(poseMap);
        sota.addPoseToActionQueue(pose, command.move_msec);
        return ActionResult.ok();
    }

    public ServosStatus getWorldSpaceStatus() {
//        SotaForwardK FK = new SotaForwardK(sota.servoMapper.calcAngles(_sotaMotion.getReadPose()));
//        double[] leftCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.L_HAND)).toArray();
//        double[] rightCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.R_HAND)).toArray();
//        double[] headCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.HEAD)).toArray();
        return null;
    }

    public ActionResult postWorldSpaceStatus(ServosStatus status, String _unused) {
        return null;
    }
}
