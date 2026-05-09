//package old.eventsystem;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
/////  SINGLETON class - only one globally
//public class EventDispatcher {
//
//    private static EventDispatcher instance = null;
//    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>();
//
//    private volatile boolean running;
//
//    public EventDispatcher() {
//        instance = this;
//    }
//
//    public static EventDispatcher getInstance() { return instance; }
//
//    public void scheduleEvent(Event event) {
//        eventQueue.add(event);
//    }
//
//    public void run() {
//        this.running = true;
//        try {
//
//            while(this.running)
//                eventQueue.take().handle();
//
//        } catch (InterruptedException ie) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    // signal the dispatcher to stop
//    public void signalStop() {
//        this.running = false;
//    }
//}
