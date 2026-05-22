package Udp;

import main.Properties;
import main.PropertyKey;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;


public class UdpStream {

    protected DatagramSocket socket;
    protected int port = -1;

    // agreed upon maximum seq number to wrap around.
    protected final int SEQUENCE_MOD = Properties.getPropAsInt(PropertyKey.KEY_NET_SEQ_MOD);
    protected final int SEQUENCE_RESERVED_BYTES = 4;  // currently using an int

    public UdpStream(int port){//}, int dataBufferSize){
        this.port = port;
    }

    public int getPort() {return this.port; }
}
