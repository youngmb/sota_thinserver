package sota.pose;

import java.util.ArrayList;
import java.util.List;

public class PoseSystemStatus {

    public Boolean servosEnabled = false;
    public Boolean talkingLEDEnabled = false;  // TODO: implement

    public List<ServoCapability> servoCapabilities = new ArrayList<>();

    public static class ServoCapability {
        public String servo_id;
        public Double range_min;
        public Double range_max;
        ServoCapability(String servo_id, Double range_min, Double range_max){
            this.servo_id = servo_id; this.range_min = range_min; this.range_max = range_max;
        }
    }
}
