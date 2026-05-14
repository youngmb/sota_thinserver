package sota.motors;

import httpserver.ActionResult;
import sota.SotaConnector;
import sota.kinematics.ServoRangeTool;

public class MotorService {

    boolean motorsEnabled = false;
    SotaConnector sota;

    public MotorService(SotaConnector sota) {
        this.sota = sota;
    }

    public MotorSystemStatus getSystemStatus() {
        MotorSystemStatus status = new MotorSystemStatus();
        status.enabled = sota.isMotorsEnabled();

        for (String key : ServoRangeTool.motorIdByName.keySet()) {
            status.motorCapabilities.add(new MotorSystemStatus.MotorCapability(
                key, sota.ranges.getMinRad(key), sota.ranges.getMaxRad(key)
            ));
        }
        return status;
    }

    public ActionResult postSystemStatus(MotorSystemStatus status, String _unused) {
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

    public SingleMotorStatus getJointSpaceStatus() {
        return null;
    }

    public ActionResult postJointSpaceStatus(SingleMotorStatus status, String _unused) {
        return null;
    }

    public SingleMotorStatus getWorldSpaceStatus() {
        return null;
    }

    public ActionResult postWorldSpaceStatus(SingleMotorStatus status, String _unused) {
        return null;
    }
}
