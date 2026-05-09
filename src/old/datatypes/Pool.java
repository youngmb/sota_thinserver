//package old.datatypes;
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//public class Pool<T> {
//
//    private final Deque<T> pool;
//    private final int maxSize;
//    private final Supplier<T> factory;
//    private final Consumer<T> resetter;
//
//    // maxSize to avoid memory overruns
//    // factory - function to generate a new object
//    // resetter - function to erase the data before re-use (if needed, use null if not
//    public Pool(int maxSize, Supplier<T> factory, Consumer<T> resetter) {
//        if (maxSize <= 0) {
//            throw new IllegalArgumentException("maxSize must be > 0");
//        }
//        this.pool = new ArrayDeque<>(maxSize);
//        this.maxSize = maxSize;
//        this.factory = factory;
//        this.resetter = resetter;
//    }
//
//    public T acquire() {
//        synchronized (pool) {
//            T obj = pool.pollFirst();  // null if empty
//            if (obj != null) {
//                return obj;
//            }
//        }
//
//        // was empty, get create a new one
//        return factory.get();
//    }
//
//    public void release(T obj) {
//        if (obj == null) return;
//
//        // clean up data before putting in pool, if needed
//        if (resetter != null)
//            resetter.accept(obj);
//
//        synchronized (pool) {
//            if (pool.size() < maxSize) {
//                pool.offerFirst(obj);
//            }
//            // else discard, nowhere to put it!!
//        }
//    }
//
//    public int size() {
//        synchronized (pool) {
//            return pool.size();
//        }
//    }
//}