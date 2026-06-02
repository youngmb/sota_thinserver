package Udp;

import main.Properties;
import main.PropertyKey;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class UdpReceiver extends UdpStream implements Runnable {

    private final BlockingQueue<byte[]> queue;
    private Thread workerThread = null;

    private volatile boolean running = true;

    // track expected next packet sequence
    private int expectedSeq = -1;  // neg impossible so means no expectation yet

    private final int waitBufferSize = Properties.getPropAsInt(PropertyKey.KEY_NET_UDP_WAITBUFFER_SIZE);
    private final int[] waitBufferSeq = new int[waitBufferSize];
    private final byte[][] waitBufferData = new byte[waitBufferSize][];
    private int dataWaiting = 0; // number of packets saved
    private int expectedBufferSize = 0;

    public UdpReceiver(int port,
                       BlockingQueue<byte[]> queue,
                       int expectedBufferSize) // expected size of incoming data
            throws SocketException {
        super(port);
        this.queue = queue;
        this.port = port;
        this.expectedBufferSize = expectedBufferSize;
    }

    public void start() {
        try {
            this.socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        workerThread = new Thread(this, "udp audio stream receiver thread");
        workerThread.start();
    }

    public void stop() {
        running = false;
        socket.close(); // interrupts the blocking receive.

        workerThread.interrupt();
        try {
            workerThread.join(1000); // wait 1 second to join
        } catch (InterruptedException e) {
            System.err.println("unable to join thread when closing.");
            e.printStackTrace();
        }
    }

    // modulo arithmetic version to check newer counter ints.
    boolean isNewer(int a, int b) {   // is a > b
        int diff = (a - b + SEQUENCE_MOD) % SEQUENCE_MOD;  // the plus is to keep it positive
        return diff > 0 && diff <= (SEQUENCE_MOD / 2);  // bottom half the space, a>b
    }

    private void waitBufferInsert(int seq, byte[] data) {
        if (dataWaiting >= waitBufferSize) { // should never happen, for safety
            System.err.println("Error: UDP Receiver Wait Buffer overflow, shouldn't happen");
            return;
        }

        // insertion-sort add to queue.
        int i = dataWaiting - 1; // start at end

        // shift elements to the right until we find insert point
        while (i >= 0 && isNewer(waitBufferSeq[i], seq)) {
            waitBufferSeq[i + 1] = waitBufferSeq[i];
            waitBufferData[i + 1] = waitBufferData[i];
            i--;
        }

        // insert new packet
        waitBufferSeq[i + 1] = seq;
        waitBufferData[i + 1] = data;
        dataWaiting++;
    }

    // flush the in-order packets to the queue, with a fallback on full buffer to just move ahead
    private void waitBufferFlush() {
        // queue all items that arrived in order
        int queued=0;
        while (queued < dataWaiting && expectedSeq == waitBufferSeq[queued]) {
            addToQueue(waitBufferData[queued]);
            expectedSeq = (expectedSeq + 1) % SEQUENCE_MOD;
            queued++;
        }

        // remove added items
        int remaining = dataWaiting - queued;
        System.arraycopy(waitBufferSeq, queued, waitBufferSeq, 0, remaining);
        System.arraycopy(waitBufferData, queued, waitBufferData, 0, remaining);
        dataWaiting = remaining;

        if (dataWaiting == waitBufferSize) { // we waited too long for the missing packet, just move on
            System.err.println("Warning: missed UDP packet and full buffer, so skipping ahead");
            for (int i = 0; i < dataWaiting; i++)
                addToQueue(waitBufferData[i]);
            expectedSeq = waitBufferSeq[waitBufferSize - 1];
            expectedSeq = (expectedSeq + 1) % SEQUENCE_MOD;
            dataWaiting = 0;
        }
    }

    void addToQueue(byte[] data) {
        if (!queue.offer(data)) // non-blocking enqueue but can fail
            System.err.println("Warning: UDP receive code is dropping packet due to failure to add to queue");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[expectedBufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            while (running) {

                socket.receive(packet); // blocking
                int len = packet.getLength();

                ByteBuffer buf = ByteBuffer.wrap(buffer, 0, len);
                int sequence = buf.getInt();
                if (expectedSeq == -1)
                    expectedSeq = sequence;

//                System.out.println("got: "+sequence);
                byte[] audioData = new byte[len - SEQUENCE_RESERVED_BYTES]; // strip sequence int
                buf.get(audioData);

                waitBufferInsert(sequence, audioData);  // Add to wait buffer. should have room
                waitBufferFlush();  // send in-seq packets or skip ahead of we missed one for too long
            }
        } catch (SocketException e) {
            // expected on socket.close()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}