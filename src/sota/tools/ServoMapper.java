package sota.tools;

import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import java.util.Map;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CSotaMotion;

public class ServoMapper implements Serializable {
    private static final long serialVersionUID = 1L;

    final public static String FALLBACK_DEFAULT_FILENAME = "../resources/motorranges-default.dat";
    final public static String LOCAL_FILENAME = "./motorranges.dat";

    public static TreeMap<Byte, Byte> IDtoIndex = null;

    private Byte[] _servoIDs = null;   // Store the IDs of sota motors. Receive from sota subsystem

    // map to support type-safe String-> motor ID
    public static final Map<String, Byte> motorIdByName = new HashMap<>();
    static {
        motorIdByName.put("body_yaw", CSotaMotion.SV_BODY_Y);
        motorIdByName.put("left_shoulder", CSotaMotion.SV_L_SHOULDER);
        motorIdByName.put("left_elbow", CSotaMotion.SV_L_ELBOW);
        motorIdByName.put("right_shoulder", CSotaMotion.SV_R_SHOULDER);
        motorIdByName.put("right_elbow", CSotaMotion.SV_R_ELBOW);
        motorIdByName.put("head_yaw", CSotaMotion.SV_HEAD_Y);
        motorIdByName.put("head_pitch", CSotaMotion.SV_HEAD_P);
        motorIdByName.put("head_roll", CSotaMotion.SV_HEAD_R);
    }

    public ServoMapper(Byte[] servoIDs) {
        this._servoIDs = servoIDs;
        setupMotorRanges();
    }

    private Map<Byte, double[]> _motorRanges_rad = null;
    private void setupMotorRanges() {
        if (IDtoIndex == null) {
            IDtoIndex = new TreeMap<>();
            for (int i=0; i < _servoIDs.length; i++)
                IDtoIndex.put(_servoIDs[i], Byte.valueOf((byte)i));
        }

        this._motorRanges_rad = new TreeMap<Byte, double[]>() {{   // lower and upper radian range
            put(CSotaMotion.SV_BODY_Y, new double[]{-1.077363736, 1.077363736} );
            put(CSotaMotion.SV_L_ELBOW, new double[]{-1.745329252, 1.221730476} );
            put(CSotaMotion.SV_L_SHOULDER, new double[]{2.617993878, -1.745329252});
            put(CSotaMotion.SV_R_ELBOW, new double[]{-1.221730476, 1.745329252});
            put(CSotaMotion.SV_R_SHOULDER, new double[]{-1.745329252, 2.617993878});
            put(CSotaMotion.SV_HEAD_Y, new double[]{-1.495996502, 1.495996502});
            put(CSotaMotion.SV_HEAD_P, new double[]{-1.495996502, 1.495996502});
            put(CSotaMotion.SV_HEAD_R, new double[]{-2.617993878, 2.617993878});
        }};
    }

    ///==================== Manage min/max/etc.
    ///====================
    private Short[] _minpos = null;  // internal arrays for precalcualted values
    private Short[] _maxpos = null;
    private Short[] _midpos = null;
    boolean _calibrationInitialized = false;

    public CRobotPose getMinPose() { return makePose(_minpos);}
    public CRobotPose getMaxPose() { return makePose(_maxpos);}
    public CRobotPose getMidPose() { return makePose(_midpos);}

    public double getMinRad(String servoID) { return getMinRad(motorIdByName.get(servoID));}
    public double getMinRad(Byte servoID) {return _motorRanges_rad.get(servoID)[0];}

    public double getMaxRad(String servoID) { return getMaxRad(motorIdByName.get(servoID));}
    public double getMaxRad(Byte servoID) {return _motorRanges_rad.get(servoID)[1];}

    public void registerCalibrationEntry(Short[] pos) {
        if (!_calibrationInitialized) {
            _minpos = (Short[])pos.clone();
            _maxpos = (Short[])pos.clone();
            _midpos = (Short[])pos.clone();
            _calibrationInitialized = true;
        } else
            for (int i=0; i < pos.length; i++) {
                _minpos[i] = (short)Math.min(_minpos[i], pos[i]);
                _maxpos[i] = (short)Math.max(_maxpos[i], pos[i]);
                _midpos[i] = (short) ( (_minpos[i] + _maxpos[i])/2 );
            }
    }

    ///==================== Angle <-> motor pos conversions
    ///====================  mostly utility functions to support working in different units
    ///
    public CRobotPose makePose(Short[] pos) {
        CRobotPose pose = new CRobotPose();
        pose.SetPose(_servoIDs, pos);
        return pose;
    }

    public Short[] extractPositions(CRobotPose pose) { return pose.getServoAngles(_servoIDs);}
    public double[] extractAngles(CRobotPose pose) { return posToRad(pose.getServoAngles(_servoIDs));}

    public double posToRad(Byte servoID, Short pos) { // convert motor position to angle, in radians
        double[] radRange = _motorRanges_rad.get(servoID);
        Byte index = IDtoIndex.get(servoID);
        double percent = 1; // avoid div by 0. no range, use 100%.
        double range = ( _maxpos[index] - _minpos[index]);
        if (range!=0)
            percent = (pos - _minpos[index]) / (double)range;

        return ( percent*(radRange[1]-radRange[0]) )+radRange[0];
    }

    public double[] posToRad(Short[] positions) {
        double[] angles = new double[positions.length];

        for (int i=0; i<positions.length; i++)
            angles[i] = posToRad(_servoIDs[i], positions[i]);

        return angles;
    }

    public short radToPos(Byte servoID, double angle) { // convert angles, in radians, to motor position
        Byte index = IDtoIndex.get(servoID);
        double[] radRange = _motorRanges_rad.get(servoID);
        double radPercent = (angle - radRange[0]) / (radRange[1]-radRange[0]);
        return (short)( (radPercent *  (_maxpos[index] - _minpos[index])) + _minpos[index] );
    }

    public Short[] radToPos(RealVector angles) { return radToPos(angles.toArray());}
    public Short[] radToPos(double[] angles) {
        Short[] positions = new Short[angles.length];
        for (int i=0; i<angles.length; i++)
            positions[i] = radToPos(_servoIDs[i], angles[i]);
        return positions;
    }

    ///==================== Pretty Print
    /// ///====================
	private String formattedLine(String title, Byte servoID, Short[] minpos, Short[] maxpos, Short[] middle, Short[] pos) {
		int i = IDtoIndex.get(servoID);
        double rad = 0;
        if (pos != null) rad = posToRad(servoID, pos[i]);
		String format = "%14s %8d %8d %8d    %.2f rad        ";
		return String.format(format, title, minpos[i], middle[i], maxpos[i], rad);
	}

    public void printMotorRanges() {printMotorRanges(null);}
	public void printMotorRanges(Short[] pos) {  // will print the current position as given by the pos array
		System.out.println("-------------");
		System.out.println( formattedLine("Body Y: ", CSotaMotion.SV_BODY_Y, _minpos, _maxpos, _midpos, pos));
		System.out.println( formattedLine("L Shoulder: ", CSotaMotion.SV_L_SHOULDER, _minpos, _maxpos, _midpos, pos));
        System.out.println( formattedLine("L Elbow: ", CSotaMotion.SV_L_ELBOW, _minpos, _maxpos, _midpos, pos));
		System.out.println( formattedLine("R Shoulder: ", CSotaMotion.SV_R_SHOULDER, _minpos, _maxpos, _midpos, pos));
		System.out.println( formattedLine("R Elbow: ", CSotaMotion.SV_R_ELBOW, _minpos, _maxpos, _midpos, pos));
        System.out.println( formattedLine("Head Y: ", CSotaMotion.SV_HEAD_Y, _minpos, _maxpos, _midpos, pos));
		System.out.println( formattedLine("Head P: ", CSotaMotion.SV_HEAD_P, _minpos, _maxpos, _midpos, pos));
        System.out.println( formattedLine("Head R: ", CSotaMotion.SV_HEAD_R, _minpos, _maxpos, _midpos, pos));
	}

    ///==================== LOAD AND SAVE
    ///====================

    private static String findFile() {
        File f = new File(LOCAL_FILENAME);
        if (f.exists() && !f.isDirectory())
            return LOCAL_FILENAME;

        f = new File(FALLBACK_DEFAULT_FILENAME);
        if (f.exists() && !f.isDirectory())
            return FALLBACK_DEFAULT_FILENAME;

        System.err.println("Error: cannot find motor ranges mapping file, checked both: \n\t"+LOCAL_FILENAME+"\n\t"+FALLBACK_DEFAULT_FILENAME);
        return null;
    }

    public static ServoMapper Load(){ return ServoMapper.Load(findFile());}
    public static ServoMapper Load(String filename){
        if (filename == null) return null;

        ServoMapper s = null;
        try(
            FileInputStream fout = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fout);
        ){
            s = (ServoMapper) ois.readObject();
            ois.close();
            System.out.println("Loaded Servo Motor Ranges from "+filename);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        s.setupMotorRanges();
        return s;
    }

    public void save() { save(LOCAL_FILENAME);}
    public void save(String filename) {
        if (filename==null) return;

        try(
            FileOutputStream fout = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
        ){
            oos.writeObject(this);
            oos.flush();
            oos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}