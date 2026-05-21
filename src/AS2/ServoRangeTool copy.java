package student.AS2;// package AS3;

// import java.io.FileInputStream;
// import java.io.FileOutputStream;
// import java.io.ObjectInputStream;
// import java.io.ObjectOutputStream;
// import java.io.Serializable;
// import java.util.TreeMap;
// import org.apache.commons.math3.linear.MatrixUtils;
// import org.apache.commons.math3.linear.RealVector;
// import java.util.Map;
// import jp.vstone.RobotLib.CRobotPose;
// import jp.vstone.RobotLib.CSotaMotion;

// public class ServoRangeTool implements Serializable {
//     private static final long serialVersionUID = 1L;
    
//     public static TreeMap<Byte, Byte> IDtoIndex = null;

//     private Short[] _minpos = null;  // internal arrays for precalcualted values
//     private Short[] _maxpos = null;
//     private Short[] _midpos = null;

//     // caching of creating pose objects, to accelerate multiple accesses
//     boolean _initialized = false;
//     boolean _dirty = true;  // updates since last gen of pose objects

//     private CRobotPose _minpose = null;  // pose objects generates from internal arrays
//     private CRobotPose _maxpose = null;
//     private CRobotPose _midpose = null;

//     private Byte[] _servoIDs = null;   

//     final static String FILENAME = "motorranges.dat";

//     ServoRangeTool(Byte[] servoIDs) {
//         this._servoIDs = servoIDs;
//         setupMotorRanges();
//     }

//     private Map<Byte, double[]> _motorRanges_rad = null;
//     private void setupMotorRanges() {
//         if (IDtoIndex == null) {
//             IDtoIndex = new TreeMap<>();
//             for (int i=0; i < _servoIDs.length; i++)
//                 IDtoIndex.put(_servoIDs[i], Byte.valueOf((byte)i));
//         }        
        
//         this._motorRanges_rad = new TreeMap<Byte, double[]>() {{   // lower and upper radian range
//             put(CSotaMotion.SV_BODY_Y, new double[]{-1.077363736, 1.077363736} );
//             put(CSotaMotion.SV_L_ELBOW, new double[]{-1.745329252, 1.221730476} );
//             put(CSotaMotion.SV_L_SHOULDER, new double[]{2.617993878, -1.745329252});
//             put(CSotaMotion.SV_R_ELBOW, new double[]{-1.221730476, 1.745329252});
//             put(CSotaMotion.SV_R_SHOULDER, new double[]{-1.745329252, 2.617993878});
//             put(CSotaMotion.SV_HEAD_Y, new double[]{-1.495996502, 1.495996502});
//             put(CSotaMotion.SV_HEAD_P, new double[]{-1.495996502, 1.495996502});
//             put(CSotaMotion.SV_HEAD_R, new double[]{-2.617993878, 2.617993878});
//         }};
//     }

//     public void register(CRobotPose pose) {register(pose.getServoAngles(_servoIDs));}
//     public void register(Short[] pos) {
//         if (!_initialized) {
//             _minpos = (Short[])pos.clone();
//             _maxpos = (Short[])pos.clone();
//             _midpos = (Short[])pos.clone();
//             _initialized = true;
//         } else 
//         for (int i=0; i < pos.length; i++) {
//             _minpos[i] = (short)Math.min(_minpos[i], pos[i]);
// 			_maxpos[i] = (short)Math.max(_maxpos[i], pos[i]);
// 			_midpos[i] = (short) ( (_minpos[i] + _maxpos[i])/2 );
//         }
//         _dirty = true;
//     }

//     private CRobotPose makePose(Short[] pos) {
//         CRobotPose pose = new CRobotPose();
//         pose.SetPose(_servoIDs, pos);
//         return pose; 
//     }

//     private void makePoses() {
//         if (_dirty) {
//             _minpose = makePose(_minpos);
//             _maxpose = makePose(_maxpos);
//             _midpose = makePose(_midpos);
//             _dirty = false;
//         }
//     }

//     public CRobotPose getMinPose() { makePoses(); return _minpose;}
//     public CRobotPose getMaxPose() { makePoses(); return _maxpose;}
//     public CRobotPose getMidPose() { makePoses(); return _midpose;}

//     ///==================== Angle <-> motor pos conversions
//     ///====================
//     public RealVector calcAngles(CRobotPose pose) {
//         Short[] rawAngles = pose.getServoAngles(_servoIDs);
//         double[] angles = new double[rawAngles.length];

//         for (int i=0; i<rawAngles.length; i++)
//             angles[i] = posToRad(_servoIDs[i], rawAngles[i]);
       
//         return MatrixUtils.createRealVector(angles);
//     }

//     public CRobotPose calcMotorValues(RealVector angles) {
//         Short[] rawAngles = new Short[angles.getDimension()];

//         for (int i=0; i<rawAngles.length; i++)
//             rawAngles[i] = radToPos(_servoIDs[i], angles.getEntry(i));
//         CRobotPose pose = new CRobotPose();
//         pose.SetPose(_servoIDs, rawAngles);
//         return pose;
//     }

//     private double posToRad(Byte servoID, Short pos) {
//         double[] radRange = _motorRanges_rad.get(servoID);
//         Byte index = IDtoIndex.get(servoID);
//         double percent = 1; // avoid div by 0. no range, use 100%.
//         double range = ( _maxpos[index] - _minpos[index]);
//         if (range!=0)
//             percent = (pos - _minpos[index]) / (double)range;

//         return ( percent*(radRange[1]-radRange[0]) )+radRange[0];
//     }

//     private short radToPos(Byte servoID, double angle) {
//         Byte index = IDtoIndex.get(servoID);
//         double[] radRange = _motorRanges_rad.get(servoID);
//         double radPercent = (angle - radRange[0]) / (radRange[1]-radRange[0]);
//         return (short)( (radPercent *  (_maxpos[index] - _minpos[index])) + _minpos[index] );
//     }


//     ///==================== Pretty Print
//     /// ///====================
// 	private String formattedLine(String title, Byte servoID, Short[] minpos, Short[] maxpos, Short[] middle, Short[] pos) {
// 		int i = IDtoIndex.get(servoID);
//         double rad = 0;
//         if (pos != null) rad = posToRad(servoID, pos[i]);
// 		String format = "%14s %8d %8d %8d    %.2f rad";
// 		return String.format(format, title, minpos[i], middle[i], maxpos[i], rad);
// 	}

//     public void printMotorRanges() {printMotorRanges(null);}
// 	public void printMotorRanges(Short[] pos) {  // will print the current position as given by the pos array
// 		System.out.println("-------------");
// 		System.out.println( formattedLine("Body Y: ", CSotaMotion.SV_BODY_Y, _minpos, _maxpos, _midpos, pos));
// 		System.out.println( formattedLine("L Shoulder: ", CSotaMotion.SV_L_SHOULDER, _minpos, _maxpos, _midpos, pos));
//         System.out.println( formattedLine("L Elbow: ", CSotaMotion.SV_L_ELBOW, _minpos, _maxpos, _midpos, pos));
// 		System.out.println( formattedLine("R Shoulder: ", CSotaMotion.SV_R_SHOULDER, _minpos, _maxpos, _midpos, pos));
// 		System.out.println( formattedLine("R Elbow: ", CSotaMotion.SV_R_ELBOW, _minpos, _maxpos, _midpos, pos));
//         System.out.println( formattedLine("Head Y: ", CSotaMotion.SV_HEAD_Y, _minpos, _maxpos, _midpos, pos));
// 		System.out.println( formattedLine("Head P: ", CSotaMotion.SV_HEAD_P, _minpos, _maxpos, _midpos, pos));
//         System.out.println( formattedLine("Head R: ", CSotaMotion.SV_HEAD_R, _minpos, _maxpos, _midpos, pos));
// 	}

//     ///==================== LOAD AND SAVE
//     ///====================
//     public static ServoRangeTool Load(){ return ServoRangeTool.Load(FILENAME);}
//     public static ServoRangeTool Load(String filename){
//         ServoRangeTool s = null;
//         try(
//             FileInputStream fout = new FileInputStream(filename);
//             ObjectInputStream ois = new ObjectInputStream(fout);
//         ){
//             s = (ServoRangeTool) ois.readObject();
//             ois.close();
//         } catch (Exception ex) {
//             ex.printStackTrace();
//         }
//         s.setupMotorRanges();
//         return s;
//     }

//     public void save() { save(FILENAME);}
//     public void save(String filename) {
//         try(
//             FileOutputStream fout = new FileOutputStream(filename);
//             ObjectOutputStream oos = new ObjectOutputStream(fout);
//         ){
//             oos.writeObject(this);
//             oos.flush();
//             oos.close();
//         } catch (Exception ex) {
//             ex.printStackTrace();
//         }
//     }
// }