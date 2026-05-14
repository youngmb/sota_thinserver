package sota.kinematics;

import jp.vstone.RobotLib.CSotaMotion;

public class Frames {
    public enum FrameKeys{
        L_HAND(ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_BODY_Y), ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_L_SHOULDER), ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_L_ELBOW)),
        R_HAND(ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_BODY_Y), ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_R_SHOULDER), ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_R_ELBOW)),
        HEAD(ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_BODY_Y), ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_HEAD_Y), 
             ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_HEAD_P),ServoRangeTool.IDtoIndex.get(CSotaMotion.SV_HEAD_R)); 
                
        public int[] motorindices;
        FrameKeys(int... motorindices){
            this.motorindices = motorindices;
        }
    }
}