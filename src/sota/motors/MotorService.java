package sota.motors;

import httpserver.ActionResult;
import main.SotaSystemController;
import sota.SotaConnector;

import java.util.Map;

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

    public SingleMotorStatus getJointStatus() {
        return null;
    }

    public ActionResult postJointStatus(SingleMotorStatus status, String _unused) {
        return null;
    }

    public SingleMotorStatus getWorldStatus() {
        return null;
    }

    public ActionResult postWorldStatus(SingleMotorStatus status, String _unused) {
        return null;
    }
}
