package server.helpers;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;

/**
 * Marker class to group Go-style types together.
 * (Java requires one public class per file)
 */
public class Types {
    // intentionally empty
}

/* ================================
   Data sent from client
   ================================ */
class PlayerInput {
    public String username;
    public String type;
    public float x;
    public float y;
    public float z;
    public float yaw;
    public float pitch;
}

/* ================================
   World snapshot
   ================================ */
class WorldSnapshot {
    public String type;
    public long seq; // uint32 -> long
    public Map<String, PlayerState> players;
}

/* ================================
   Authoritative player struct
   ================================ */
class Player {
    public String username;
    public InetSocketAddress addr;
    public Instant lastSeen;

    public Player(String username, InetSocketAddress addr) {
        this.username = username;
        this.addr = addr;
        this.lastSeen = Instant.now();
    }
}

/* ================================
   Player state sent to clients
   ================================ */
class PlayerState {
    public String username;
    public float x;
    public float y;
    public float z;
    public float yaw;
    public float pitch;

    public PlayerState(String username, float x, float y, float z) {
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0;
        this.pitch = 0;
    }
}