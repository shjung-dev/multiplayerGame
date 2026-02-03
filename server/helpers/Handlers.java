package server.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Handlers {

    // ===============================
    // Global state (thread-safe)
    // ===============================
    static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, PlayerState> playerPositions = new ConcurrentHashMap<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    // ===============================
    // Handle incoming packet
    // ===============================
    public static void handlePacket(
            DatagramSocket socket,
            InetSocketAddress clientAddr,
            byte[] data
    ) {
        PlayerInput input;

        try {
            input = mapper.readValue(data, PlayerInput.class);
        } catch (Exception e) {
            System.out.println("Invalid packet: " + new String(data));
            return;
        }

        // Update last-seen timestamp
        Player p = players.get(input.username);
        if (p != null && p.addr.equals(clientAddr)) {
            p.lastSeen = Instant.now();
        }

            switch (input.type) {
                case "connect": handleConnect(socket, clientAddr, input); break;
                case "movement": updateMovement(input); break;
                default: System.out.println("Unknown packet type: " + input.type);
            }
    }

    // ===============================
    // Handle new client connection
    // ===============================
    private static void handleConnect(
            DatagramSocket socket,
            InetSocketAddress clientAddr,
            PlayerInput input
    ) {
        String username = input.username;

        // Save player info
        Player player = new Player(username, clientAddr);
        players.put(username, player);

        // Initialize spawn position
        playerPositions.put(username, new PlayerState(username, 0, 0, 0));

        System.out.println("New player connected: " + username);

        // Send initial state to new player
        sendInitialState(socket, player);

        // Notify other players
        broadcastNewPlayer(socket, player);
    }

    // ===============================
    // Update player movement
    // ===============================
    private static void updateMovement(PlayerInput input) {
        PlayerState state = playerPositions.get(input.username);
        if (state == null) {
            System.out.println("Player not found: " + input.username);
            return;
        }

        state.x = input.x;
        state.y = input.y;
        state.z = input.z;
        state.yaw = input.yaw;
        state.pitch = input.pitch;
    }

    // ===============================
    // Send initial snapshot
    // ===============================
    private static void sendInitialState(
            DatagramSocket socket,
            Player newPlayer
    ) {
        List<PlayerState> existingPlayers = new ArrayList<>();

        for (Map.Entry<String, PlayerState> entry : playerPositions.entrySet()) {
            if (!entry.getKey().equals(newPlayer.username)) {
                existingPlayers.add(entry.getValue());
            }
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ack");
            payload.put("username", newPlayer.username);
            payload.put("players", existingPlayers);

            byte[] data = mapper.writeValueAsBytes(payload);
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    newPlayer.addr
            );

            socket.send(packet);
        } catch (Exception e) {
            System.out.println("Failed to send initial state: " + e.getMessage());
        }
    }

    // ===============================
    // Broadcast new player
    // ===============================
    private static void broadcastNewPlayer(
            DatagramSocket socket,
            Player newPlayer
    ) {
        PlayerState state = playerPositions.get(newPlayer.username);
        if (state == null) return;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "player_joined");
            payload.put("player", state);

            byte[] data = mapper.writeValueAsBytes(payload);

            for (Player p : players.values()) {
                if (!p.username.equals(newPlayer.username)) {
                    DatagramPacket packet = new DatagramPacket(
                            data,
                            data.length,
                            p.addr
                    );
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to broadcast new player: " + e.getMessage());
        }
    }
}