//package old.datatypes;
//
//public class DataFromPool<T> extends Data {
//  public final T data;
//  public final int dataSize;
//  private final Pool<T> pool;
//
//    public DataFromPool(Pool<T> pool, T data, int dataSize) {
//      this.pool = pool;
//      this.data = data;
//      this.dataSize = dataSize;
//    }
//
//    @Override
//    protected void cleanup() {   // called when data no longer needed
//      this.pool.release(this.data);
//    }
//}