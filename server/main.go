package main

import (
	"fmt"
	"net"
	"github.com/shjung-dev/multiplayerGame/server/helpers"
)

const SERVER_PORT = 2828
const BUFFER_SIZE = 1024

func main() {
	addr := net.UDPAddr{
		Port: SERVER_PORT,
		IP:   net.ParseIP("0.0.0.0"),
	}

	conn, err := net.ListenUDP("udp", &addr)
	if err != nil {
		panic(err)
	}
	defer conn.Close()

	fmt.Printf("Server listening on port %d\n", SERVER_PORT)

	// Start tick loop
	go helpers.ServerTick(conn)
	
	buf := make([]byte, BUFFER_SIZE)
	for {
		n, clientAddr, err := conn.ReadFromUDP(buf)
		if err != nil {
			fmt.Println("Read error:", err)
			continue
		}

		go helpers.HandlePacket(conn, clientAddr, buf[:n])
	}
}
