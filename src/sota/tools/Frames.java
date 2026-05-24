package sota.tools;

import jp.vstone.RobotLib.CSotaMotion;
import static sota.tools.ServoMapper.IDtoIndex;

public class Frames {
    public enum FrameKeys{

        L_HAND("leftHand", CSotaMotion.SV_BODY_Y, CSotaMotion.SV_L_SHOULDER, CSotaMotion.SV_L_ELBOW),
        R_HAND("rightHand", CSotaMotion.SV_BODY_Y, CSotaMotion.SV_R_SHOULDER, CSotaMotion.SV_R_ELBOW),
        HEAD("head", CSotaMotion.SV_BODY_Y, CSotaMotion.SV_HEAD_Y, CSotaMotion.SV_HEAD_P, CSotaMotion.SV_HEAD_R);

        public final Byte[] motorIndices;
        public final Byte[] motorIDs;
        public final String label;

        FrameKeys(String label, Byte... motorIDs){
            this.motorIDs = motorIDs;
            this.motorIndices = new Byte[motorIDs.length];
            for (int i=0; i<motorIDs.length; i++) {
                this.motorIndices[i] = IDtoIndex.get(motorIDs[i]);
            }
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