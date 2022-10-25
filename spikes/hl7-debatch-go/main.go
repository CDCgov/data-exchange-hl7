package main

import (
	"log"
	"strings"
	"os"
	"bufio"
	"github.com/google/uuid"
	"time"
)

func main() {

	start := time.Now()

	f, err := os.Open("batch.txt")
	if err != nil {
		log.Fatal(err) 
	} // .if 
	defer f.Close()

	fstat, err := f.Stat()
	if err != nil {
		log.Fatal(err) 
	} // .if

	fileSource := "TODO - read from bucket"
	fileName := fstat.Name()
	fileModTime := fstat.ModTime().UTC()
	fileSize := fstat.Size()
	fileIngestUUID := uuid.New().String()
	fileIngestTime := time.Now().UTC()

	var messages []Message
	var lines []string 
	index := 0

	scanner := bufio.NewScanner(f) 
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())

		if strings.HasPrefix(line, "FHS") || strings.HasPrefix(line, "BHS") || strings.HasPrefix(line, "BTS") || strings.HasPrefix(line, "FTS") {
			continue 
		} // .if 

		if strings.HasPrefix(line, "MSH") {
			// first message at index 1 
			if (index > 0) {

				message := Message{
					FileSource: fileSource,
					FileName: fileName,
					FileModTime: fileModTime,
					FileSize: fileSize,

					FileIngestUUID: fileIngestUUID,
					FileIngestTime: fileIngestTime, 
					
					// message metadate
					MessageUUID: uuid.New().String(),

					MessageIndex: index,
					MessageContent: strings.Join(lines, "\n"),
				} // .Message
				
				// TODO: do not append and fire off message/s to Event Hub
				messages = append(messages, message)
			} // .if 

			index++
			// empty for a new message
			lines = lines[:0]
		 } // .if 
			
		lines = append(lines, line) 
    } // .for scanner.Scan()
	
	// append last message
	message := Message{
		FileSource: fileSource,
		FileName: fileName,
		FileModTime: fileModTime,
		FileSize: fileSize,

		FileIngestUUID: fileIngestUUID,
		FileIngestTime: fileIngestTime, 
		
		// message metadate
		MessageUUID: uuid.New().String(),

		MessageIndex: index,
		MessageContent: strings.Join(lines, "\n"),
	} // .Message
	// TODO: do not append and fire off message/s to Event Hub
	messages = append(messages, message)

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	} // .if

	log.Printf("file has: %d messages", len(messages))
	log.Printf("execution duration: %v", time.Since(start))
} // .main