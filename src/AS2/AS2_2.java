package student.AS2;

import jp.vstone.RobotLib.*;

public class AS2_2 {
	static final String TAG = "AS3_2";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);;

	final int MOVESPEED = 1500;

	AS2_2() {
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
		Byte[] servoIDs = _sotaMotion.getDefaultIDs();
		ServoRangeTool ranges = ServoRangeTool.Load();
		CRobotUtil.Log(TAG, "Servo Ranges Loaded");
		ranges.printMotorRanges();

		CRobotPose pose = new CRobotPose(); // empty copy that we can continually update
		Short[] angles = ranges.getMidPose().getServoAngles(servoIDs);
		pose.SetPose(servoIDs, angles);

		CRobotUtil.Log(TAG, "Servos On");
		_sotaMotion.ServoOn();

		// go to mid
		CRobotUtil.Log(TAG, "Go To Mid");
		_sotaMotion.play(pose, MOVESPEED);
		_sotaMotion.waitEndinterpAll();

		// go to range
		for (byte i = 0; i < servoIDs.length; i++) {
			CRobotUtil.Log(TAG, "Go To range on index "+ i);

			angles[i] = ranges.getMinPose().getServoAngle(servoIDs[i]);
			pose.SetPose(servoIDs, angles);
			_sotaMotion.play(pose, MOVESPEED/2);
			_sotaMotion.waitEndinterpAll();

			angles[i] = ranges.getMaxPose().getServoAngle(servoIDs[i]);
			pose.SetPose(servoIDs, angles);
			_sotaMotion.play(pose, MOVESPEED);
			_sotaMotion.waitEndinterpAll();

			angles[i] = ranges.getMidPose().getServoAngle(servoIDs[i]);
			pose.SetPose(servoIDs, angles);
			_sotaMotion.play(pose, MOVESPEED/2);
			_sotaMotion.waitEndinterpAll();
		}

		ranges.printMotorRanges(_sotaMotion.getReadpos());
	}

	public static void main(String args[]){
		AS2_2 sota = new AS2_2();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();
			
		CRobotUtil.Log(TAG, "Program End Reached");
	}
}