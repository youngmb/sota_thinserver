//package old.dataprocessors.network;
//
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.net.UnknownHostException;
//
//import old.dataprocessors.DataProcessor;
//import old.datatypes.Data;
//
//public class UDPSender extends DataProcessor {
//
//    private String ip;
//    private int port;
//
//    private InetAddress address;
//    private DatagramSocket socket;
//
//    public UDPSender(String ip, int port) {
//        this.ip = ip;
//        this.port = port;
//    }
//
//    private void init() throws SocketException, UnknownHostException {
//        if(this.socket == null) {
//            socket = new DatagramSocket();
//            address = InetAddress.getByName(ip);
//        }
//    }
//
//    @Override
//    protected Data process(Data input, EventGenerator sender) {
//        try {
//            this.init();
//
//            ByteArrayData bytes = (ByteArrayData) input;
//            DatagramPacket packet = new DatagramPacket(bytes.data, bytes.data.length, this.address, this.port);
//            socket.send(packet);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//}
