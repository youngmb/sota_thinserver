package sota.tools;

import jp.vstone.RobotLib.CSotaMotion;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.linear.*;

import sota.tools.Frames.FrameKeys;

public class SotaForwardK {

    final static private RealVector L_SHOULDER_DIR = MatrixHelp.normalizeH(MatrixUtils.createRealVector(new double[]{ .0225, -.03897, 0, 1})); 
    final static private RealVector R_SHOULDER_DIR = MatrixHelp.normalizeH(MatrixUtils.createRealVector(new double[]{ -.0225, -.03897, 0, 1})); 
    final static private double ARMLENGTH = 0.064; // 6cm 

    public final Map<FrameKeys, RealMatrix> frames = new HashMap<>();

    public SotaForwardK(double[] angles) { this(MatrixUtils.createRealVector(angles)); }
    public SotaForwardK(RealVector angles) {
        //======== setup Transformation matrices
        RealMatrix _base_to_origin          = MatrixUtils.createRealIdentityMatrix(4);
        RealMatrix _body_to_base 			= MatrixHelp.T(MatrixHelp.rotZ(angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_BODY_Y))), 		0, 0, 0.005);
        RealMatrix _neck_to_body 			= MatrixHelp.T(MatrixHelp.rotZ(angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_HEAD_Y))), 		0, 0, .19);
        RealMatrix _headroll_to_neck 		= MatrixHelp.T(MatrixHelp.rotY(angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_HEAD_R))), 		0, 0, 0);
        RealMatrix _headpitch_to_headroll 	= MatrixHelp.T(MatrixHelp.rotX(angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_HEAD_P))), 		0, 0, 0);
        RealMatrix _head_to_headpitch 		= MatrixUtils.createRealIdentityMatrix(4); 		// head_to_headpitch - no change since the headpitch is the actual head.
        RealMatrix _r_shoulder_to_body 		= MatrixHelp.T(MatrixHelp.rotX(angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_R_SHOULDER))),	 -.039, 0, .1415);
        RealMatrix _l_shoulder_to_body 		= MatrixHelp.T(MatrixHelp.rotX(-angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_L_SHOULDER))), 	.039, 0, .1415);
        RealMatrix _r_elbow_to_shoulder 	= MatrixHelp.T(MatrixHelp.rotRodrigues(-0.6258053, .329192519, 0.707106769,  angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_R_ELBOW))),
                                             -.0225, -.03897, 0);
        RealMatrix _l_elbow_to_shoulder		= MatrixHelp.T(MatrixHelp.rotRodrigues(0.6258053, .329192519, 0.707106769, angles.getEntry(ServoMapper.IDtoIndex.get(CSotaMotion.SV_L_ELBOW))),
                                            .0225, -.03897, 0);
        RealMatrix _r_hand_to_elbow 		= MatrixHelp.trans(R_SHOULDER_DIR.mapMultiply(ARMLENGTH));
        RealMatrix _l_hand_to_elbow			= MatrixHelp.trans(L_SHOULDER_DIR.mapMultiply(ARMLENGTH));

        //========== precalculate combined chains
        RealMatrix _body_to_origin          = _base_to_origin.multiply(_body_to_base);   
        RealMatrix _neck_to_origin          = _body_to_origin.multiply(_neck_to_body); 
        RealMatrix _head_to_origin          = _neck_to_origin.multiply(_headroll_to_neck).multiply(_headpitch_to_headroll).multiply(_head_to_headpitch);
        RealMatrix _r_shoulder_to_origin    = _body_to_origin.multiply(_r_shoulder_to_body);
        RealMatrix _r_elbow_to_origin       = _r_shoulder_to_origin.multiply(_r_elbow_to_shoulder);
        RealMatrix _r_hand_to_origin        = _r_elbow_to_origin.multiply(_r_hand_to_elbow);
        RealMatrix _l_shoulder_to_origin    = _body_to_origin.multiply(_l_shoulder_to_body);
        RealMatrix _l_elbow_to_origin       = _l_shoulder_to_origin.multiply(_l_elbow_to_shoulder);
        RealMatrix _l_hand_to_origin        = _l_elbow_to_origin.multiply(_l_hand_to_elbow);

        frames.put(FrameKeys.HEAD, _head_to_origin);
        frames.put(FrameKeys.R_HAND, _r_hand_to_origin);
        frames.put(FrameKeys.L_HAND, _l_hand_to_origin);
    }

    public RealVector getPointDir(FrameKeys frame) {
        RealVector dir = null;
        RealVector dir2 = null;

        switch (frame) {
            case HEAD: // just extract the y axis from the head frame
                dir = frames.get(frame)
                        .getColumnVector(1)// 0 is x axis, 1 is y, 2 is z, 3 is trans
                        .mapMultiplyToSelf(-1); // y axis is pointing behind robot, we want -Y for look dir
                break;

            case R_HAND:
                dir = calcHandPointDirInWorld(FrameKeys.R_HAND, R_SHOULDER_DIR);
                break;

            case L_HAND:
                dir = calcHandPointDirInWorld(FrameKeys.L_HAND, L_SHOULDER_DIR);
                break;
        }
        return dir;
    }

    private RealVector calcHandPointDirInWorld(FrameKeys frame, RealVector dir) {
        RealMatrix R = frames.get(frame);
        R.setColumn(3, new double[]{0,0,0,1});// kill T, get R only.
        return R.operate(dir).unitVector();
    }
}