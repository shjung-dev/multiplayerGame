package server


import (
	"crypto/sha256"
	"encoding/hex"
)

// GenerateUserID creates a unique ID from username
func GenerateUserID(username string) string {
	hash := sha256.Sum256([]byte(username))
	return hex.EncodeToString(hash[:])
}


