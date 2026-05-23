package sota.pose;

import httpserver.ActionResult;
import jp.vstone.RobotLib.CRobotPose;
import org.apache.commons.math3.linear.RealVector;
import sota.SotaConnector;
import sota.tools.Frames.FrameKeys;
import sota.tools.LEDHelp;
import sota.tools.LEDHelp.LEDs;
import sota.tools.ServoMapper;

import java.util.HashMap;
import java.util.Map;

public class PoseService {

    boolean servosEnabled = false;
    boolean talkingLEDEnabled = false; // we don't know the initial state for sure
    SotaConnector sota;

    public PoseService(SotaConnector sota) {
        this.sota = sota;
    }

    public PoseSystemStatus getSystemStatus() {
        PoseSystemStatus status = new PoseSystemStatus();
        status.servosEnabled = sota.isServosEnabled();
        status.talkingLEDEnabled = this.talkingLEDEnabled;

        for (String key : ServoMapper.motorIdByName.keySet()) {
            status.servoCapabilities.add(new PoseSystemStatus.ServoCapability(
                key, sota.servoMapper.getMinRad(key), sota.servoMapper.getMaxRad(key)
            ));
        }

        return status;
    }

    public ActionResult postSystemStatus(PoseSystemStatus status, String _unused) {
        if (status.servosEnabled != null && status.servosEnabled != this.servosEnabled) {  // 'enabled' changed
            if (status.servosEnabled) { // turn on
                sota.enableMotors();
                this.servosEnabled = true;
            } else { // request to disable
                sota.disableMotors();
                this.servosEnabled = false;
            }
        }
        if (status.talkingLEDEnabled != null && (status.talkingLEDEnabled != this.talkingLEDEnabled)) {
            sota.setTalkingLED(status.talkingLEDEnabled);
            this.talkingLEDEnabled = status.talkingLEDEnabled;
        }
        return ActionResult.ok();
    }

    public PoseStatus getPoseStatus() {
        PoseStatus status = new PoseStatus();
        double[] angles = sota.getMotorAngles();

        // motors
        for (Map.Entry<String, Byte> entry : ServoMapper.motorIdByName.entrySet()) {
            int motorIndex = ServoMapper.IDtoIndex.get(entry.getValue());
            status.servoStatus.add(new PoseStatus.ServoStatus(entry.getKey(), angles[motorIndex]));
        }

        // endpoints (world space)
        sota.updateFK();
        for (FrameKeys frame: FrameKeys.values())
            status.endpointStatus.add( sota.getEndpointStatus(frame) );

        // leds
        Map<Byte, Short> colors = sota.getTargetPose().getLed();
        for (LEDs led: LEDs.values()) {
            status.LEDStatus.add(new PoseStatus.LEDStatus( led.label, LEDHelp.getLEDColor(colors, led)));
        }

        return status;
    }

    public ActionResult postPoseStatus(PoseCommand command, String _unused) {
        CRobotPose pose = new CRobotPose();

        // set LEDs
        if (!command.LEDStatus.isEmpty()) {
            Map<Byte, Short> LEDMap = new HashMap<>();
            for (PoseStatus.LEDStatus ledEntry : command.LEDStatus)
                if (ledEntry.led_id != null && ledEntry.color != null)  //TODO: consider if we should put err message response here
                    LEDHelp.setLED(LEDMap, ledEntry.led_id, ledEntry.color);
            pose.SetLed(LEDMap);
        }

        // set direct motor positions
        if (!command.servoStatus.isEmpty()) { // only if we have motor commands

            Map<Byte, Short> poseMap = new HashMap<>();
            for (PoseStatus.ServoStatus servo : command.servoStatus) {
                if (servo.servo_id != null) {  // otherwise skip  //TODO: consider if we should put err message responss here
                    Byte id = ServoMapper.motorIdByName.get(servo.servo_id);
                    if (servo.radians != null) {
                        short motorPosition = sota.servoMapper.radToPos(id, servo.radians);
                        poseMap.put(id, motorPosition);
                    }
                }
            }
            pose.SetPose(poseMap);

        }  else { // only check IK if we didn't have direct motor commands. they conflict
            if (!command.endpointStatus.isEmpty()) {
                // set motors using IK to solve for given endpoints
                RealVector theta = null;
                for (PoseStatus.EndpointStatus endpoint : command.endpointStatus)
                    theta = sota.solveIK(endpoint.endpoint_id, endpoint.position, theta);

                pose.SetPose(sota.makePose(theta).getPose());
            }
        }
        
        if (command.command == null) command.command = PoseCommand.CommandType.DEFAULT_COMMAND;

        sota.addPoseToActionQueue(pose, command.command, command.move_msec);
        return ActionResult.ok();
    }
}