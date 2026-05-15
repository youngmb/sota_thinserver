package sota.motors;

import sota.kinematics.Frames;
import sota.kinematics.MatrixHelp;
import sota.kinematics.SotaForwardK;

public class SingleMotorStatus {
    float jointSpaceRad;
    double[] leftHand = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.L_HAND)).toArray();
    double[] rightCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.R_HAND)).toArray();
    double[] headCenter = MatrixHelp.getTrans(FK.frames.get(Frames.FrameKeys.HEAD)).toArray();
}
