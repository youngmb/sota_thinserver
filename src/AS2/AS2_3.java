package student.AS2;

import java.util.Arrays;
import org.apache.commons.math3.linear.RealVector;
import jp.vstone.RobotLib.*;

public class AS2_3 {
	static final String TAG = "AS3_3";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);

	AS2_3() {
		CRobotUtil.Log(TAG, "Start " + TAG);
	}

	boolean connect() {		
		if(!_sotaMem.Connect()) { // connect to the robot's subsystem
			CRobotUtil.Log(TAG, "Sota connection failure " + TAG);
			return false;
		}

		CRobotUtil.Log(TAG, "connected " + TAG);
		_sotaMotion.InitRobot_Sota();  // initialize the Sota VSMD			
		CRobotUtil.Log(TAG, "Rev. " + _sotaMem.FirmwareRev.get());
		return true;
	}

	void run() {
		ServoRangeTool ranges = ServoRangeTool.Load();
		CRobotUtil.Log(TAG, "Servo Ranges Loaded");
		ranges.printMotorRanges(_sotaMotion.getReadpos());

		CRobotUtil.Log(TAG, "Servos Off"); // initialize in off state
		_sotaMotion.ServoOff();
			
		// clear screen and move to origin.
		System.out.print("\033[H\033[2J"); System.out.flush();

		while (!_sotaMotion.isButton_Power()) {
			System.out.print("\033[H");  // move to origin
			CRobotPose pose = _sotaMotion.getReadPose();
			Short[] rawAngles = pose.getServoAngles(_sotaMotion.getDefaultIDs());
			ranges.printMotorRanges(rawAngles);
			
			System.out.println("---"+ Arrays.toString(rawAngles));

			RealVector angles = ranges.calcAngles(pose);
			MatrixHelp.printVector(angles);

			pose = ranges.calcMotorValues(angles);
			System.out.println("---"+ Arrays.toString(pose.getServoAngles(_sotaMotion.getDefaultIDs())));

			System.out.flush();
			CRobotUtil.wait(100);
		}
	}

	public static void main(String args[]){
		AS2_3 sota = new AS2_3();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();
			
		CRobotUtil.Log(TAG, "Program End Reached");
	}
}

