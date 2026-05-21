package sota.supportPrograms;

import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import jp.vstone.RobotLib.*;
import sota.tools.MatrixHelp;
import sota.tools.ServoMapper;

public class ServoAngleReader {
	static final String TAG = "MotorAngleReader";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);

	ServoAngleReader() {
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
		ServoMapper ranges = ServoMapper.Load();
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

			double[] angles = ranges.extractAngles(pose);
			MatrixHelp.printVector(MatrixUtils.createRealVector( angles));

			pose = ranges.makePose( ranges.radToPos(angles) );
			System.out.println("---"+ Arrays.toString(pose.getServoAngles(_sotaMotion.getDefaultIDs())));

			System.out.flush();
			CRobotUtil.wait(100);
		}
	}

	public static void main(String args[]){
		ServoAngleReader sota = new ServoAngleReader();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();

		CRobotUtil.Log(TAG, "Program End Reached");
	}
}

