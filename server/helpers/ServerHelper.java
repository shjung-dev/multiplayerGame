package server.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ServerHelper {

    public static final int TICK_RATE = 25; // updates per second
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(15);

    private static long serverSeq = 0;

    private static final ObjectMapper mapper = new ObjectMapper();

    // ===============================
    // Server tick loop
    // ===============================
    public static void serverTick(DatagramSocket socket) {
        serverSeq++;

        removeDisconnectedPlayers(socket);
        broadcastPlayerStates(socket, serverSeq);
    }

    // ===============================
    // Broadcast world snapshot
    // ===============================
    private static void broadcastPlayerStates(
            DatagramSocket socket,
            long seq
    ) {
        // Snapshot player positions
        Map<String, PlayerState> snapshot =
                new HashMap<>(Handlers.playerPositions);

        WorldSnapshot worldSnapshot = new WorldSnapshot();
        worldSnapshot.type = "snapshot";
        worldSnapshot.seq = seq;
        worldSnapshot.players = snapshot;

        byte[] payload;
        try {
            payload = mapper.writeValueAsBytes(worldSnapshot);
        } catch (Exception e) {
            System.out.println("Failed to marshal snapshot: " + e.getMessage());
            return;
        }

        // Snapshot player addresses
        List<Player> playersSnapshot =
                new ArrayList<>(Handlers.players.values());

        // Send snapshot
        for (Player p : playersSnapshot) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        payload,
                        payload.length,
                        p.addr
                );
                socket.send(packet);
            } catch (Exception e) {
                System.out.println("Failed to send snapshot: " + e.getMessage());
            }
        }
    }

    // ===============================
    // Remove disconnected players
    // ===============================
    private static void removeDisconnectedPlayers(
            DatagramSocket socket
    ) {
        Instant now = Instant.now();
        List<String> disconnected = new ArrayList<>();

        // Detect disconnected players
        for (Map.Entry<String, Player> entry : Handlers.players.entrySet()) {
            Player p = entry.getValue();
            if (Duration.between(p.lastSeen, now).compareTo(CLIENT_TIMEOUT) > 0) {
                disconnected.add(entry.getKey());
                Handlers.players.remove(entry.getKey());
                System.out.println("player: " + entry.getKey() + " is removed");
            }
        }

        if (disconnected.isEmpty()) return;

        // Remove from playerPositions
        for (String username : disconnected) {
            Handlers.playerPositions.remove(username);
        }

        // Notify remaining players
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "player_left");
            payload.put("players", disconnected);

            byte[] data = mapper.writeValueAsBytes(payload);

            for (Player p : Handlers.players.values()) {
                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        p.addr
                );
                socket.send(packet);
            }
        } catch (Exception e) {
            System.out.println("Failed to notify player removal: " + e.getMessage());
        }
    }
}