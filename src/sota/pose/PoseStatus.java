package sota.pose;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import httpserver.ColorSerializer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PoseStatus {
    public List<ServoStatus> servoStatus = new ArrayList<>();
    public List<LEDStatus> LEDStatus = new ArrayList<>();

    public static class ServoStatus {
        public String servo_id;
        public Double radians;
        ServoStatus(String servo_id, Double radians){
            this.servo_id = servo_id; this.radians = radians;
        }
        ServoStatus() {}
    }

    public static class LEDStatus {
        public String led_id;

        @JsonSerialize(using = ColorSerializer.Serializer.class)
        @JsonDeserialize(using = ColorSerializer.Deserializer.class)
        public Color color;

        LEDStatus(String led_id, Color color){
            this.led_id = led_id; this.color = color;
        }
        LEDStatus() {}
    }
}
