package Udp;

import main.Properties;
import main.PropertyKey;

import java.net.*;
import java.nio.ByteBuffer;

public class UdpSender extends UdpStream {

    private DatagramSocket socket;
    private final InetAddress target;
    private final int port;

    private int sequence = 0; // give each packet a unique, in sequence number
    private final int SEQUENCE_MOD = Properties.getPropAsInt(PropertyKey.KEY_NET_SEQ_MOD);

    private final ByteBuffer buffer = ByteBuffer.allocate(
            Properties.getPropAsInt(PropertyKey.KEY_MIC_BUFFER_SIZE)
                    + 4);  // +4 to add the int at the beginning for sequence number

    public UdpSender(String targetIP,
                     int port,
                     int dataBufferSize // expected size of incoming data
        ) throws UnknownHostException {
        super(port, dataBufferSize);

        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Err creating new UDP send socket. Shouldn't happen.");
            e.printStackTrace();
        }

        this.target = InetAddress.getByName(targetIP);
        this.port = port;
    }

    public String getIP() { return target.getHostAddress(); }

    public void send(byte[] data) {
        buffer.clear();
        buffer.putInt(sequence);  //WARNING: match size of reserved bytes, fragile coupling here
        buffer.put(data);

        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.position(), target, port);
        try {
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Err sending datagram: ");
            e.printStackTrace();
        }

        sequence = (sequence+1)%SEQUENCE_MOD;
    }
}
