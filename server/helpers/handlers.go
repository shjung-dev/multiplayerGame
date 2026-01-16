package server

import (
	"encoding/json"
	"fmt"
	"log"
	"net"
	"sync"
)

// Global player map (keep protected with mutex)
var (
	players   = make(map[string]*Player)
	playersMu sync.Mutex
)

// Handle incoming packet from a client
func HandlePacket(conn *net.UDPConn, clientAddr *net.UDPAddr, data []byte) {
	var input PlayerInput
	if err := json.Unmarshal(data, &input); err != nil {
		fmt.Println("Invalid packet:", string(data))
		return
	}

	switch input.Type {
	case "connect":
		handleConnect(conn, clientAddr, input)
	default:
		fmt.Println("Unknown packet type:", input.Type)
	}
}

// Handle new client connection
func handleConnect(conn *net.UDPConn, clientAddr *net.UDPAddr, input PlayerInput) {
	userID := GenerateUserID(input.Username)

	// Save player info
	playersMu.Lock()
	player := &Player{
		ID:       userID,
		Username: input.Username,
		Addr:     clientAddr,
	}
	players[userID] = player
	playersMu.Unlock()

	fmt.Println("New player connected:", input.Username)

	// Send ack
	payload, err := json.Marshal(map[string]interface{}{
		"type":    "ack",
		"user_id": userID,
	})
	if err != nil {
		log.Println("Failed to marshal payload:", err)
		return
	}

	_, err = conn.WriteToUDP(payload, clientAddr)
	if err != nil {
		log.Println("Failed to send ack:", err)
	} else {
		fmt.Println("Ack sent to", clientAddr)
	}
}

