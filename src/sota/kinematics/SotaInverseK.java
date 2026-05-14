package sota.kinematics;

import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import sota.kinematics.Frames.FrameKeys;

public class SotaInverseK {
    private static double NUMERICAL_DELTA_rad = 1e-10;
    private static double DISTANCE_THRESH = 1e-3; // 1mm

    public enum JType {  // We separate the jacobians into origin and rotation components to simplify the problem
        O, // origin
        R; // rotation / orientation
        
        public static final int OUT_DIM = 3; // each has 3 outputs
    }

    public TreeMap<FrameKeys, RealMatrix>[] J;
    public TreeMap<FrameKeys, RealMatrix>[] Jinv;

    @SuppressWarnings("unchecked")
    SotaInverseK(RealVector currentAngles, FrameKeys frameType) {
        J = new TreeMap[JType.values().length];
        Jinv = new TreeMap[JType.values().length];
        for (int i=0; i < JType.values().length; i++) {
            J[i] = new TreeMap<FrameKeys, RealMatrix>();
            Jinv[i] = new TreeMap<FrameKeys, RealMatrix>();
        }
       makeJacobian(currentAngles, frameType);
    }

    // Makes both the jacobian and inverse from the current configuration for the
    // given frame type. Creates both JTypes.
    private void makeJacobian(RealVector currentAngles, FrameKeys frameType) {
        final int JACOBIANS = JType.values().length; // 2 because each frame produces an O and an R
        final int JOINT_DIM = frameType.motorIndices.length; // inputs.

        SotaForwardK currentFK = new SotaForwardK(currentAngles);

        double[] deltaAngles = currentAngles.toArray();
        RealMatrix[] J = new RealMatrix[JACOBIANS];
        for (int i=0; i<JACOBIANS;i++) J[i] = MatrixUtils.createRealMatrix( JType.OUT_DIM, JOINT_DIM );

        for (int motor = 0; motor < frameType.motorIndices.length; motor++) { // go through each joint. inputs
            int j = frameType.motorIndices[motor];
            
            Double angleBackup = deltaAngles[j];  // perturb that joint and calculate the new FK result.
            deltaAngles[j] += NUMERICAL_DELTA_rad;
            SotaForwardK deltaFK = new SotaForwardK(deltaAngles);

            for (JType jtype: JType.values()) {
                RealVector v = getFKOutput(deltaFK.frames, frameType, jtype).subtract(getFKOutput(currentFK.frames, frameType, jtype))
                        .mapDivideToSelf(NUMERICAL_DELTA_rad);
                J[jtype.ordinal()].setColumnVector(motor, v);
            }

            deltaAngles[j] = angleBackup; // restore this angle
        }
        for (JType jtype: JType.values()) {
            this.J[jtype.ordinal()].put(frameType, J[jtype.ordinal()]);
            this.Jinv[jtype.ordinal()].put(frameType, MatrixHelp.pseudoInverse( J[jtype.ordinal()]) );
        }
    }

    static private RealVector getFKOutput(Map<FrameKeys, RealMatrix> frames, FrameKeys frameType, JType outputType) {
        RealMatrix frame = frames.get(frameType);
        return outputType == JType.O ? MatrixHelp.getTrans(frame).getSubVector(0, 3) : MatrixHelp.getYPRVec(frame);
    }
    // solves for the target pose on the given frame and type, starting at the current angle configuration.
    static public RealVector solve(FrameKeys frameType, JType jtype, RealVector targetPose, RealVector curMotorAngles) {
        double error = Double.MAX_VALUE;
        RealVector theta = curMotorAngles.copy();
        int tries = 0;

        double smallestError = error;
        RealVector solution = theta;
        RealVector simEndPose = targetPose;

        while (error > DISTANCE_THRESH && tries < 20) {
            SotaForwardK FK = new SotaForwardK(theta);
            simEndPose = getFKOutput(FK.frames, frameType, jtype); // update end pos with new info.
            
            RealVector errorVec = targetPose.subtract(simEndPose); // calc. error from target.
            error = errorVec.getNorm();
            if (error < smallestError) {  // save best answer
                smallestError = error;
                solution = theta.copy();
            }
 
            SotaInverseK IK = new SotaInverseK(theta, frameType);  // resolve IK at updated theta
            RealVector deltaTheta = IK.Jinv[jtype.ordinal()].get(frameType).operate(errorVec); // update target with jacobian
            for (int j = 0; j < frameType.motorIndices.length; j++)
                theta.addToEntry(frameType.motorIndices[j], deltaTheta.getEntry(j));

            tries++;
        }
        return solution;
    }   
}