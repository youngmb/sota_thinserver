package sota.kinematics;

import jp.vstone.RobotLib.CSotaMotion;
import static sota.kinematics.ServoMappingTools.IDtoIndex;

public class Frames {
    public enum FrameKeys{
        L_HAND(IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_L_SHOULDER), IDtoIndex.get(CSotaMotion.SV_L_ELBOW)),
        R_HAND(IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_R_SHOULDER), IDtoIndex.get(CSotaMotion.SV_R_ELBOW)),
        HEAD(IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_HEAD_Y),
             IDtoIndex.get(CSotaMotion.SV_HEAD_P), IDtoIndex.get(CSotaMotion.SV_HEAD_R));
                
        public final int[] motorIndices;
        FrameKeys(int... motorIndices){
            this.motorIndices = motorIndices;
        }
    }
}