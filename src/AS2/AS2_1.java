package student.AS2;

import jp.vstone.RobotLib.*;

public class AS2_1 {
	static final String TAG = "AS3_1";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);;

	AS2_1() {
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

		ServoRangeTool ranges = new ServoRangeTool(_sotaMotion.getDefaultIDs());

		System.out.print("\033[H\033[2J"); System.out.flush();
		while (!_sotaMotion.isButton_Power()) {
			System.out.print("\033[H");  // move to origin
			ranges.register(_sotaMotion.getReadpos());
			ranges.printMotorRanges(_sotaMotion.getReadpos());
			System.out.flush();
			CRobotUtil.wait(100);
		}
		ranges.save();
	}

	public static void main(String args[]){
		AS2_1 sota = new AS2_1();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();
			
		CRobotUtil.Log(TAG, "Program End Reached");
	}
}
