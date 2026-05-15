package sota.servos;

import java.util.ArrayList;
import java.util.List;

public class ServosCommand {
    public List<ServosStatus.ServoStatus> motorsStatus = new ArrayList<>();
    public Integer move_msec = 100; // how fast it should be moving, with default to 100
    public CommandType command;

    public enum CommandType {
        APPEND,
        PREPEND,
        REPLACE
    }
}
