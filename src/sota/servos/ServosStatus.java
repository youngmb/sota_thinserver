package sota.servos;

import java.util.ArrayList;
import java.util.List;

public class ServosStatus {
    public List<ServoStatus> motorsStatus = new ArrayList<>();

    public static class ServoStatus {
        public String motor_id;
        public Double radians;
        ServoStatus(String motor_id, Double radians){
            this.motor_id = motor_id; this.radians = radians;
        }
        ServoStatus() {}
    }
}
