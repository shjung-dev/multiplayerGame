package server

import (
	"encoding/json"
	"net"
	"time"
)

const TICK_RATE = 20 // server updates per second

// Broadcast game state to all players
func ServerTick(conn *net.UDPConn) {
	ticker := time.NewTicker(time.Second / TICK_RATE)
	defer ticker.Stop()

	for range ticker.C {
		playersMu.Lock()

		// Prepare serializable state
		state := make(map[string]PlayerState)
		for id, player := range players {
			state[id] = PlayerState{
				ID:       player.ID,
				Username: player.Username,
			}
		}
		
		packet, _ := json.Marshal(state)

		// Send to all connected players
		for _, player := range players {
			if player.Addr != nil {
				conn.WriteToUDP(packet, player.Addr)
			}
		}

		playersMu.Unlock()
	}
}
