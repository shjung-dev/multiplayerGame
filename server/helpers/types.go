package server

import "net"

// Data sent from client
type PlayerInput struct {
    ID int     `json:"id"`
    Username string `json:"username"`
	Type string `json:"type"`
}

// Authoritative player struct
type Player struct {
    ID string
	Username string
	Addr *net.UDPAddr
}


// PlayerState is what gets sent to clients
type PlayerState struct {
	ID       string  `json:"id"`
	Username string  `json:"username"`
	X        float32 `json:"x"`
	Z        float32 `json:"z"`
}

