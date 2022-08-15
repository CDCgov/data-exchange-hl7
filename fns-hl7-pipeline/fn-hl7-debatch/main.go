package main

import (
	"log"
	"net/http"
	"os"
)

func uploadFileEvent(w http.ResponseWriter, r *http.Request) {
    w.Write([]byte("UploadFileEvent route"))
} // .UploadFileEvent


func main() {
	listenAddr := ":8080"

	if val, ok := os.LookupEnv("FUNCTIONS_CUSTOMHANDLER_PORT"); ok {
		listenAddr = ":" + val
	} // .if 

	http.HandleFunc("/api/UploadFileEvent", uploadFileEvent)
	log.Printf("About to listen on %s. Go to https://127.0.0.1%s/", listenAddr, listenAddr)
	log.Fatal(http.ListenAndServe(listenAddr, nil))
} // .main