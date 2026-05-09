//package old.datatypes;
//
//import java.util.Arrays;
//
//public class ShortArrayData extends Data {
//    public final short[] data;
//
//    public ShortArrayData(short[] data) {
//        this.data = data;
//    }
//
//    public String toString() {
//        return Arrays.toString(this.data);
//    }
//
//    public double[] asDoubleArray() {
//        double[] output = new double[data.length];
//        for(int i = 0; i < data.length; i++) {
//            output[i] = (double)data[i];
//        }
//        return output;
//    }
//}