package sota.kinematics;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import java.util.Map;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CSotaMotion;

public class SotaMappingTools implements Serializable {
    private static final long serialVersionUID = 1L;

    final public static String FALLBACK_DEFAULT_FILENAME = "../resources/motorranges-default.dat";
    final public static String LOCAL_FILENAME = "./motorranges.dat";

    final public static int SERVO_COUNT = 8;
    final public static int LED_ENTRY_COUNT = 10; // RGB lights each count as 3

    public static TreeMap<Byte, Byte> IDtoIndex = null;

    private Byte[] _servoIDs = null;   // Store the IDs of sota motors. Receive from sota subsystem

    // map to support type-safe String-> motor ID
    public static final Map<String, Byte> motorIdByName = new HashMap<>();
    static {
        motorIdByName.put("BODY_Y", CSotaMotion.SV_BODY_Y);
        motorIdByName.put("L_SHOULDER", CSotaMotion.SV_L_SHOULDER);
        motorIdByName.put("L_ELBOW", CSotaMotion.SV_L_ELBOW);
        motorIdByName.put("R_SHOULDER", CSotaMotion.SV_R_SHOULDER);
        motorIdByName.put("R_ELBOW", CSotaMotion.SV_R_ELBOW);
        motorIdByName.put("HEAD_Y", CSotaMotion.SV_HEAD_Y);
        motorIdByName.put("HEAD_P", CSotaMotion.SV_HEAD_P);
        motorIdByName.put("HEAD_R", CSotaMotion.SV_HEAD_R);
    }

    public SotaMappingTools(Byte[] servoIDs) {
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

    ///==================== LED Mapping support functions
    ///====================
    public enum LEDs { // start index of light. note that RGB has 3 entries
        POWER("power", 0, 3), // RGB
        ///  I wonder what 3-7 are mapped to
        EYE_L("leftEye", 8, 3), // RGB
        EYE_R("rightEye", 11, 3),
        MOUTH("mouth", 14, 1);

        public final byte index;
        public final int count;
        public final String label;
        LEDs(String label, int index, int count) {
            this.index = (byte)index; this.count = count; this.label = label;
        }

        static public LEDs fromLabel(String label) {
            for (LEDs led: LEDs.values()) {
                if (label.equals(led.label))
                    return led;
            }
            return null;
        }
    }

    public static Color getLEDColor(Map<Byte, Short> LEDData, LEDs led) {
        if (led.count == 1) { // monochrome
            Short c = LEDData.get(led.index);
            return new Color(c, c, c);

        } else if (led.count == 3) {
            Short r = LEDData.get(led.index);
            Short g = LEDData.get((byte)(led.index+1));
            Short b = LEDData.get((byte)(led.index+1));
            return new Color(r, g, b);
        }

        // should never happen
        return null;
    }

    public static void setLED(Map<Byte, Short> LEDMap, String LED, Color color){ setLED(LEDMap, LEDs.fromLabel(LED), color); }

    public static void setLED(Map<Byte, Short> LEDMap, LEDs LED, Color color){
        if (LEDMap == null || LED == null || color == null) {
            System.err.println("ERROR: setting LED with null");
            return;
        }

        boolean flipGreen = LED == LEDs.POWER; // special case for power button

        float[] rgb = color.getRGBColorComponents(null);
        for (int i = 0; i < LED.count; i++) {
            short c = (short) (rgb[i] * 255);
            if (flipGreen && i==1) // GREEN
                c = (short)(255-c);
            LEDMap.put((byte) (LED.index + i), c);
        }
    }

    ///==================== Manage min/max/etc.
    ///====================
    private Short[] _minpos = null;  // internal arrays for precalcualted values
    private Short[] _maxpos = null;
    private Short[] _midpos = null;
    boolean _calibrationInitialized = false;

    public CRobotPose getMinPose() { return makePose_fromPositions(_minpos);}
    public CRobotPose getMaxPose() { return makePose_fromPositions(_maxpos);}
    public CRobotPose getMidPose() { return makePose_fromPositions(_midpos);}

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
    private CRobotPose makePose_fromPositions(Short[] pos) {
        CRobotPose pose = new CRobotPose();
        pose.SetPose(_servoIDs, pos);
        return pose;
    }

    public CRobotPose makePose_fromRadians(double[] pos) {
        Short[] motorPositions = new Short[pos.length];

        for (int i=0; i<motorPositions.length; i++)
            motorPositions[i] = radToPos(_servoIDs[i], pos[i]);
        CRobotPose pose = new CRobotPose();
        pose.SetPose(_servoIDs, motorPositions);
        return pose;
    }

    public RealVector calcAngles_vector(CRobotPose pose) { return MatrixUtils.createRealVector(calcAngles_array(pose));}
    public double[] calcAngles_array(CRobotPose pose) { // convert pose in motor positions to radians
        Short[] rawAngles = pose.getServoAngles(_servoIDs);
        double[] angles = new double[rawAngles.length];

        for (int i=0; i<rawAngles.length; i++)
            angles[i] = posToRad(_servoIDs[i], rawAngles[i]);

        return angles;
    }

    public CRobotPose calcMotorValues_vector(RealVector angles) { // convert pose in angles to motor positions
        Short[] rawAngles = new Short[angles.getDimension()];

        for (int i=0; i<rawAngles.length; i++)
            rawAngles[i] = radToPos(_servoIDs[i], angles.getEntry(i));
       return makePose_fromPositions(rawAngles);
    }

    private double posToRad(Byte servoID, Short pos) { // convert motor position to angle, in radians
        double[] radRange = _motorRanges_rad.get(servoID);
        Byte index = IDtoIndex.get(servoID);
        double percent = 1; // avoid div by 0. no range, use 100%.
        double range = ( _maxpos[index] - _minpos[index]);
        if (range!=0)
            percent = (pos - _minpos[index]) / (double)range;

        return ( percent*(radRange[1]-radRange[0]) )+radRange[0];
    }

    public short radToPos(Byte servoID, double angle) { // convert angles, in radians, to motor position
        Byte index = IDtoIndex.get(servoID);
        double[] radRange = _motorRanges_rad.get(servoID);
        double radPercent = (angle - radRange[0]) / (radRange[1]-radRange[0]);
        return (short)( (radPercent *  (_maxpos[index] - _minpos[index])) + _minpos[index] );
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

    public static SotaMappingTools Load(){ return SotaMappingTools.Load(findFile());}
    public static SotaMappingTools Load(String filename){
        if (filename == null) return null;

        SotaMappingTools s = null;
        try(
            FileInputStream fout = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fout);
        ){
            s = (SotaMappingTools) ois.readObject();
            ois.close();
            System.out.println("Loaded Servo Motor Ranges from "+filename);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        s.setupMotorRanges();
        return s;
    }

    public void save() { save(findFile());}
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