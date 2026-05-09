//package old.dataproviders.network;
//
//import java.io.ObjectInputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//import old.dataproviders.DataProvider;
//import old.datatypes.Data;
//
//public class TCPReceiver extends DataProvider {
//
//    private int port;
//
//    public TCPReceiver(int port) {
//       this.port = port;
//    }
//
//    @Override
//    public void run() {
//        try {
//            ServerSocket serverSocket = new ServerSocket(port);
//
//            System.out.println("TCP Provider listening on port: " + port);
//
//            Socket clientSocket = serverSocket.accept();
//
//            System.out.println("TCP Sender connected");
//
//            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
//
//            while(true) {
//                Data received = (Data)ois.readObject();
//                this.notifyListeners(received);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
