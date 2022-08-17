package main

import (
	"io"
	"log"
	"os"
)


func main() {
	fhs := []byte("FHS this is the file header\n")
	bhs := []byte("BHS this is the batch header")
	bts := []byte("\nBTS this is the batch end\n")
	fts := []byte("BHS this is the file end")

	f, err := os.Open("Genv2_2-0-1_TC01.txt")
	if err != nil {
		log.Fatalf("error open file: %v", err)
	} // .err
	defer f.Close()

	msg, err := io.ReadAll(f) 
	if err != nil {
		log.Fatalf("error read file: %v", err)
	} // .if 
	
	messages := append(fhs, bhs...)
	var lineEnding []byte = []byte("\n") 

	for i := 1; i <= 20000; i++ { 
		
		//     20,000 HL7 batched messages  ->    74 MB file size	   	    -> Az explorer upload    7 sed 			   ->      0.3 sec debatch local desktop
		//    200,000 HL7 batched messages  ->   740 MB (0.7 GB) file size  -> Az explorer upload   28 sec   		   ->      2.1 sec debatch local desktop
		//  2,000,000 HL7 batched messages  ->  7.45 GB file size 		    -> Az explorer upload  520 sec (8.7 min)   ->      21  sec debatch local desktop
		// 10,000,000 HL7 batched messages  -> 37.26 GB file size 		    -> Az explorer upload 1,227 s (20.45 min)  -> 1min 50  sec debatch local desktop

		messages = append(messages, lineEnding...)
		messages = append(messages, msg...)
	} // .for

	messages = append(messages, bts...)
	messages = append(messages, fts...)

	bigBatch, err := os.OpenFile("bigbatch.txt", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0777)
    if err != nil {
        log.Fatalf("error while opening the file. %v", err)
    } // .if 
    defer bigBatch.Close()

    if _, err := bigBatch.Write(messages); err != nil {
        log.Fatalf("error while writing the file. %v", err)
    } // .if 

} // .main