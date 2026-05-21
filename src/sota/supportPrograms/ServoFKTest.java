package sota.supportPrograms;

import jp.vstone.RobotLib.*;
import sota.tools.MatrixHelp;
import sota.tools.ServoMapper;
import sota.tools.SotaForwardK;
import sota.tools.Frames.FrameKeys;

public class ServoFKTest {
	static final String TAG = "MotorFKTest";   // set this to support the Sota logging system

	// private variables
	CRobotPose _sotaPose = new CRobotPose();
	CRobotMem _sotaMem = new CRobotMem();
	CSotaMotion _sotaMotion = new CSotaMotion(_sotaMem);

	ServoFKTest() {
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
			// SotaForwardK sotaforwardK = new SotaForwardK(ranges.calcAngles(_sotaMotion.getReadPose()));
			SotaForwardK FK = new SotaForwardK(ranges.extractAngles(_sotaMotion.getReadPose()));

			ranges.printMotorRanges(_sotaMotion.getReadpos());
			// MatrixHelp.printFrame("body", FK.frames.get(Frames.BODY));
			MatrixHelp.printFrame("head", FK.frames.get(FrameKeys.HEAD));
			MatrixHelp.printFrame("lhand", FK.frames.get(FrameKeys.L_HAND));
			MatrixHelp.printFrame("rhand", FK.frames.get(FrameKeys.R_HAND));

			// System.out.println("Distance between hands: "+sotaforwardK.lHandFrame.origin.getDistance(sotaforwardK.rHandFrame.origin)*100);
			double dist = MatrixHelp.getTrans(FK.frames.get(FrameKeys.L_HAND))
					.getDistance(MatrixHelp.getTrans(FK.frames.get(FrameKeys.R_HAND)));
			System.out.println("Distance between hands: "+dist*100);

			System.out.flush();
			CRobotUtil.wait(100);
		}
	}

	public static void main(String args[]){
		ServoFKTest sota = new ServoFKTest();
		if (!sota.connect())
			return;
		CRobotUtil.Log(TAG, "Startup Successful");
		sota.run();

		CRobotUtil.Log(TAG, "Program End Reached");
	}
}

