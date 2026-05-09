//package old.dataprocessors.network;
//
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//import java.net.Socket;
//import java.net.UnknownHostException;
//
//import old.dataprocessors.DataProcessor;
//import old.datatypes.Data;
//
//public class TCPSender extends DataProcessor {
//
//    private String ip;
//    private int port;
//
//    private Socket socket;
//    private ObjectOutputStream oos;
//
//    public TCPSender(String ip, int port) {
//        this.ip = ip;
//        this.port = port;
//    }
//
//    private void init() throws IOException, UnknownHostException {
//        if(this.socket == null) {
//            this.socket = new Socket(this.ip, this.port);
//            this.oos = new ObjectOutputStream(socket.getOutputStream());
//        }
//    }
//
//    @Override
//    protected Data process(Data input, EventGenerator sender) {
//        try {
//            this.init();
//            oos.writeObject(input);
//            oos.reset();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//}
