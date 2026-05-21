package sota.pose;

import java.util.ArrayList;
import java.util.List;

public class PoseCommand {
    public List<PoseStatus.ServoStatus> servoStatus = new ArrayList<>();
    public List<PoseStatus.LEDStatus> LEDStatus = new ArrayList<>();
    public List<PoseStatus.EndpointStatus> endpointStatus = new ArrayList<>();
    
    public Integer move_msec = 100; // how fast it should be moving, with default to 100
    public CommandType command;

    public enum CommandType {
        APPEND,
        PREPEND,
        CLEAR_AND_ADD,
        INTERRUPT_AND_PREPEND;

        static public final CommandType DEFAULT_COMMAND = APPEND;
    }
}
