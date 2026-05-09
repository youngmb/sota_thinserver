//package old.eventsystem;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import old.datatypes.Data;
//
//public class EventGenerator {
//
//    private static EventDispatcher dispatcher = null;
//    private final List<EventListener> listeners = new ArrayList<EventListener>();
//
//    public void notifyListeners(Data d) {
//        if (dispatcher == null)
//            dispatcher = EventDispatcher.getInstance();
//
//        if(d != null) {
//            for(EventListener l : this.listeners) {
//                dispatcher.scheduleEvent(new Event(d, l, this));
//                d.increaseUserCount();
//            }
//        }
//    }
//
//    public void addListener(EventListener l) {
//        this.listeners.add(l);
//    }
//}