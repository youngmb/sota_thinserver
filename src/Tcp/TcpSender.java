package Tcp;

import main.Properties;
import main.PropertyKey;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.LockSupport;

public class TcpSender extends TcpStream {

    private DatagramSocket socket;
    private final InetAddress target;
    private final int port;

    private int sequence = 0; // give each packet a unique, in sequence number
    private final int SEQUENCE_MOD = Properties.getPropAsInt(PropertyKey.KEY_NET_SEQ_MOD);


    final int MAX_DATA_PAYLOAD = 1200;  // 1300-1500 byte mtu
    private ByteBuffer buffer = null;

    public TcpSender(String targetIP,
                     int port
        ) throws UnknownHostException {
        super(port);

        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Err creating new UDP send socket. Shouldn't happen.");
            e.printStackTrace();
        }

        this.buffer = ByteBuffer.allocate(MAX_DATA_PAYLOAD * 2); // safe over

        this.target = InetAddress.getByName(targetIP);
        this.port = port;
    }

    public String getIP() { return target.getHostAddress(); }

    public void send(byte[] data) {
        buffer.clear();
        buffer.putInt(sequence);  //WARNING: match size of reserved bytes, fragile coupling here
        buffer.put(data);
        buffer.flip();

        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.limit(), target, port);
        try {
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Err sending datagram: ");
            e.printStackTrace();
        }

        sequence = (sequence+1)%SEQUENCE_MOD;
    }

    public void send_in_chunks(byte[] data, int dataSize) { // split our data into appropriate size chunks and put in a new header.
        int bytesSent = 0;
        long delayNs = 100_000; // 0.1ms baseline for pacing UDP packets. >100Mb (!)

        short packets = (short) ((dataSize + MAX_DATA_PAYLOAD - 1) / MAX_DATA_PAYLOAD);
        for (int packet_num = 0; packet_num < packets; packet_num++) {

            int thisPacketLen = dataSize - bytesSent;
            if (thisPacketLen > MAX_DATA_PAYLOAD) thisPacketLen = MAX_DATA_PAYLOAD;

            buffer.clear();
            buffer.putInt(sequence);
            buffer.putShort( (short)packet_num);
            buffer.putShort(packets);
            buffer.put(data, bytesSent, thisPacketLen);
            buffer.flip();

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.limit(), target, port);
            try {
                socket.send(packet);
                LockSupport.parkNanos(delayNs);
            } catch (Exception e) {
                System.err.println("Err sending datagram: ");
                e.printStackTrace();
            }

            bytesSent += thisPacketLen;
        }
        sequence = (sequence+1)%SEQUENCE_MOD;
    }
}
