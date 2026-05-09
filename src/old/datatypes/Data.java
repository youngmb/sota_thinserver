//package old.datatypes;
//import java.io.Serializable;
//
//public abstract class Data implements Serializable {
//    private int users = 0;
//
//    public void increaseUserCount() { users++; }
//    public void decreaseUserCount() {
//        if (users <= 0) {
//            throw new IllegalStateException("Data user count already zero");
//        }
//        users--;
//        if (users == 0) cleanup();
//    }
//
//    // this function can be overridden to have data cleanup code if needed
//    protected void cleanup() {}
//}
