package Tcp;

import main.Properties;
import main.PropertyKey;

import java.net.DatagramSocket;


public class TcpStream {

    protected DatagramSocket socket;
    protected int port = -1;

    // agreed upon maximum seq number to wrap around.
    protected final int SEQUENCE_MOD = Properties.getPropAsInt(PropertyKey.KEY_NET_SEQ_MOD);
    protected final int SEQUENCE_RESERVED_BYTES = 4;  // currently using an int

    public TcpStream(int port){//}, int dataBufferSize){
        this.port = port;
    }

    public int getPort() {return this.port; }
}
