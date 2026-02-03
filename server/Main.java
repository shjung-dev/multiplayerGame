package server;

import java.net.*;
import java.util.concurrent.*;

import server.helpers.*;

public class Main {
    private static final int SERVER_PORT = 2828;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(SERVER_PORT);
            System.out.println("Server listening on port " + SERVER_PORT);

            // Executor for concurrent packet handling
            ExecutorService packetExecutor = Executors.newCachedThreadPool();

            // Start server tick loop (25 ticks per second)
            ScheduledExecutorService tickExecutor = Executors.newScheduledThreadPool(1);
            tickExecutor.scheduleAtFixedRate(() -> {
                ServerHelper.serverTick(socket);
            }, 0, 1000 / ServerHelper.TICK_RATE, TimeUnit.MILLISECONDS);

            byte[] buf = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet); // blocking call

                // Copy packet data to avoid overwriting buffer
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                // Handle packet concurrently
                packetExecutor.submit(() -> {
                    Handlers.handlePacket(socket, (InetSocketAddress) packet.getSocketAddress(), data);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}