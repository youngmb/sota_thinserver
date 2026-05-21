package sota.tools;

import jp.vstone.RobotLib.CSotaMotion;
import static sota.tools.ServoMapper.IDtoIndex;

public class Frames {
    public enum FrameKeys{
        L_HAND("leftHand", IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_L_SHOULDER), IDtoIndex.get(CSotaMotion.SV_L_ELBOW)),
        R_HAND("rightHand", IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_R_SHOULDER), IDtoIndex.get(CSotaMotion.SV_R_ELBOW)),
        HEAD("head", IDtoIndex.get(CSotaMotion.SV_BODY_Y), IDtoIndex.get(CSotaMotion.SV_HEAD_Y),
             IDtoIndex.get(CSotaMotion.SV_HEAD_P), IDtoIndex.get(CSotaMotion.SV_HEAD_R));
                
        public final int[] motorIndices;
        public final String label;

        FrameKeys(String label, int... motorIndices){
            this.motorIndices = motorIndices;
            this.label = label;
        }

        static public FrameKeys fromLabel(String label) {
            for (FrameKeys key: FrameKeys.values())
                if (label.equals(key.label))
                    return key;
            return null;
        }
    }
}