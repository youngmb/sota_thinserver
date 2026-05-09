//package old.test;
//
//import old.dataprocessors.sota.SotaDialogController;
//import old.dataproviders.DataProvider;
//import old.datatypes.behaviors.BackchannelEvent;
//import old.datatypes.behaviors.NodBackchannelEvent;
//import old.datatypes.behaviors.UtteranceBackchannelEvent;
//import old.eventsystem.EventDispatcher;
//
///**
// * Test SotaDialogController and surrounding infrastructure using a stub HTTP command provider.
// * Must be run on a VStone Sota Edison robot.
// */
//public class TestSotaDialogController {
//  public static void main(String [] args) {
//
//    System.out.println("------------- Testing SotaDialogController -------------");
//
//    System.out.println("Test one event");
//    BackchannelEvent[] testOneEvent = new BackchannelEvent[] {
//      new UtteranceBackchannelEvent("right"),
//    };
//    test(testOneEvent);
//
//    System.out.println("Test multiple events");
//    BackchannelEvent[] testMultipleEvents = new BackchannelEvent[] {
//      new NodBackchannelEvent(5, 3),
//      new UtteranceBackchannelEvent("uh-huh"),
//      new NodBackchannelEvent(7, 4),
//      new UtteranceBackchannelEvent("I see"),
//    };
//    test(testMultipleEvents);
//  }
//
//  public static void test(BackchannelEvent[] events) {
//    final EventDispatcher dispatcher = new EventDispatcher();
//    final StubHttpCommandProvider commandProvider = new StubHttpCommandProvider(events);
//    SotaDialogController controller = new SotaDialogController();
//
//    commandProvider.addListener(controller);
//    commandProvider.start();
//
//    // when the controller is ready, tell HttpCommandProvider to request the next command
//    controller.addListener(new old.eventsystem.EventListener() {
//      @Override
//      public void handle(old.datatypes.Data d, old.eventsystem.EventGenerator sender) {
//        if (d instanceof SotaDialogController.SotaStateData) {
//          SotaDialogController.SotaStateData sd = (SotaDialogController.SotaStateData) d;
//          if (sd.data == SotaDialogController.SotaState.READY) {
//            if (!commandProvider.requestOnce()) {
//              dispatcher.stop();
//            }
//          }
//        }
//      }
//    });
//
//    // trigger an initial request
//    commandProvider.requestOnce();
//
//    dispatcher.run();
//  }
//}
//
///**
// * Test stub class to feed predefined backchannel events to the SotaDialogController.
// */
//class StubHttpCommandProvider extends DataProvider {
//
//  private BackchannelEvent[] events;
//  private int index;
//
//  public StubHttpCommandProvider(BackchannelEvent[] events) {
//    this.events = events;
//    this.index = 0;
//  }
//
//  @Override
//  public void run() {
//    // no continuous polling - only respond to requestOnce calls
//  }
//
//  // modified for testing purposes
//  public boolean requestOnce() {
//    if (this.index < events.length) {
//      this.notifyListeners(events[this.index]);
//      this.index++;
//      return true;
//    }
//    return false;
//  }
//
//  public boolean isRunning() {
//    return this.index < events.length;
//  }
//}