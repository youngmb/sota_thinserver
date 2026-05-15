package sota.servos;

import java.util.ArrayList;
import java.util.List;

public class ServoSystemStatus {

    public Boolean enabled = false;
    public List<MotorCapability> motorCapabilities = new ArrayList<>();

    public static class MotorCapability {
        public String motor_id;
        public Double range_min;
        public Double range_max;
        MotorCapability(String motor_id, Double range_min, Double range_max){
            this.motor_id = motor_id; this.range_min = range_min; this.range_max = range_max;
        }
    }
}
