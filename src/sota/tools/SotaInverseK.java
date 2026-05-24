package sota.tools;

import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import sota.tools.Frames.FrameKeys;

public class SotaInverseK {
    private static final double NUMERICAL_DELTA_rad = 1e-10;
    private static final double DISTANCE_THRESH = 2e-3; // 2mm

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
        return outputType == JType.O ? MatrixHelp.getTrans(frame).getSubVector(0, 3) : MatrixHelp.getZYXRot_vec(frame);
    }

    // solves for the target pose on the given frame and type, starting at the current angle configuration.
    // updated with two tuning parameters to keep the search local and avoid huge jumps:
    // -  alpha is learning parameter, scales the output from the IK each turn. 0.1 alpha is reasonable
    // - maxAngleStep is how much we let an angle move each step. 3 degrees / 0.05 radians is reasonable
    static public RealVector solve(FrameKeys frameType, JType jtype,
                                   RealVector targetPose, RealVector curMotorAngles,
                                   ServoMapper mapper) {
        return solve(frameType, jtype, targetPose, curMotorAngles, mapper, 0.1, 0.05);
    }
    static public RealVector solve(FrameKeys frameType, JType jtype,
                                   RealVector targetPose, RealVector curMotorAngles,
                                   ServoMapper mapper,
                                   double alpha, double maxAngleStep) {
        double error = Double.MAX_VALUE;
        RealVector theta = curMotorAngles.copy();
        int tries = 0;

        double smallestError = error;
        RealVector solution = theta;
        RealVector simEndPose = targetPose;

        while (error > DISTANCE_THRESH && tries < 25) {
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
            deltaTheta.mapMultiply(alpha);  // learning parameter

            for (int j = 0; j < frameType.motorIndices.length; j++) {
                Byte motorIndex = frameType.motorIndices[j];
                Byte motorID = frameType.motorIDs[j];
                double delta = clamp(deltaTheta.getEntry(j), -maxAngleStep, maxAngleStep);  // clamp to local movement

                double newValue = theta.getEntry(motorIndex) + deltaTheta.getEntry(j);
                newValue = clamp(newValue, mapper.getMinRad(motorID), mapper.getMaxRad(motorID) );  // clamp to motor capability
//                theta.setEntry(motorIndex, newValue);

                  theta.setEntry(motorIndex, newValue);
//                  theta.addToEntry(frameType.motorIndices[j], deltaTheta.getEntry(j));
            }
            tries++;
        }
        return solution;
    }
    public static double clamp(double value, double min, double max) {return Math.max(min, Math.min(max, value));}
}