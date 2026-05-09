//package old.eventsystem;
//
//import old.datatypes.Data;
//
//public class Event {
//
//    private Data data;
//    private EventListener listener;
//    private EventGenerator sender;
//
//    public Event(Data d, EventListener l, EventGenerator s) {
//        this.data = d;
//        this.listener = l;
//        this.sender = s;
//
//        this.data.increaseUserCount();  // when I am registered let the data know that someone is using it.
//    }
//
//    public void handle() {
//        this.listener.handle(this.data, this.sender);
//        this.data.decreaseUserCount(); // this event is now done using it.
//    }
//}