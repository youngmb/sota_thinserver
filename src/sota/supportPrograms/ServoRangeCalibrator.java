package sota.supportPrograms;

import jp.vstone.RobotLib.*;
import sota.kinematics.ServoMappingTools;

public class ServoRangeCalibrator {
	static final String TAG = "MotorRangeCalibrator";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);

	ServoRangeCalibrator() {
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
		CRobotUtil.Log(TAG, "Servos Off");
		_sotaMotion.ServoOff();

		ServoMappingTools ranges = new ServoMappingTools(_sotaMotion.getDefaultIDs());

		System.out.print("\033[H\033[2J"); System.out.flush();
		while (!_sotaMotion.isButton_Power()) {
			System.out.print("\033[H");  // move to origin
			System.out.println("-- move each joint, one at a time, to one extreme then another");
			System.out.println("-- press the power button to stop and safe the motor values\n----\n");
			ranges.register(_sotaMotion.getReadpos());
			ranges.printMotorRanges(_sotaMotion.getReadpos());
			System.out.flush();
			CRobotUtil.wait(100);
		}
		ranges.save();
		CRobotUtil.Log(TAG, "Ranges saved in default file "+ ServoMappingTools.DEFAULT_FILENAME);
	}

	public static void main(String args[]){
		ServoRangeCalibrator sota = new ServoRangeCalibrator();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();
			
		CRobotUtil.Log(TAG, "Program End Reached");
	}
}
