package main

import (
	"time"
)

type Message struct {
	// file metadata
	FileSource string
    FileName string
    FileModTime time.Time 
    FileSize int64
    FileIngestUUID string
    FileIngestTime time.Time 
    
	// message metadate
    MessageUUID string
	MessageIndex int 
	MessageContent string
} // .Message