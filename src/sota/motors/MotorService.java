package sota.motors;

import httpserver.ActionResult;
import sota.SotaConnector;

public class MotorService {

    SotaConnector sota;

    public MotorService(SotaConnector sota) {
        this.sota = sota;
    }

    public MotorSystemStatus getSystemStatus() {
        MotorSystemStatus status = new MotorSystemStatus();
        status.enabled = sota.isMotorsEnabled();

        for (String key : SotaConnector.motorIdByName.keySet()) {
            status.motorCapabilities.add(new MotorSystemStatus.MotorCapability(
                key, sota.ranges.getMinRad(key), sota.ranges.getMaxRad(key)
            ));
        }
        return status;
    }

    public ActionResult postSystemStatus(MotorSystemStatus status, String _unused) {
        return null;
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
