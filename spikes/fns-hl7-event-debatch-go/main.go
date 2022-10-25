package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"
)

func uploadFileEvent(w http.ResponseWriter, r *http.Request) {

	switch r.Method {
		case http.MethodGet: {
			w.Write([]byte( time.Now().String()))
			return
		} // .http.MethodGet

		case http.MethodPost: {
			aegEventType := r.Header.Get("aeg-event-type")
			log.Println("UploadFileEvent called with POST request, header aeg-event-type: ", aegEventType)
			
            switch aegEventType {

                case "SubscriptionValidation": {

                    type ValidationDetails []struct {
                        Data struct {
                            ValidationCode string `json:"validationCode"`
                        } `json:"data"`
                    }
                    var vd ValidationDetails
                    err := json.NewDecoder(r.Body).Decode(&vd)
                    if err != nil {
                        w.Write([]byte("error decoding validation json payload"))
                        return 
                    } // .if 

                    code := vd[0].Data.ValidationCode

                    w.Header().Set("Content-Type", "application/json")
                    json.NewEncoder(w).Encode(struct{
                        ValidationResponse string `json:"validationResponse"`
                    }{ValidationResponse: code})
					return
                } // .case "SubscriptionValidation"

                case "Notification": {

                    var payload AzBlobStorageEvent 
                    err := json.NewDecoder(r.Body).Decode(&payload)
                    if err != nil {
                        log.Printf("error decode payload: %+v", err)
                        w.Write([]byte("error decoding json payload"))
                        return 
                    } // .if 
        
                    log.Printf("received payload: %+v", payload)
                    log.Printf("received file location: %v", payload[0].Data.URL)
					// TODO: send EventHub notification
                        
        
                    w.Header().Set("Content-Type", "application/json")
                    json.NewEncoder(w).Encode(payload)
					return 
                } // .case "Notification"

                default: {
                    log.Println("UploadFileEvent called with POST request, header aeg-event-type: ", aegEventType)
                } // .default 

            } // .switch
		} // .http.MethodPost
	} // .switch

} // .uploadFileEvent


func main() {
	listenAddr := ":8080"

	if val, ok := os.LookupEnv("FUNCTIONS_CUSTOMHANDLER_PORT"); ok {
		listenAddr = ":" + val
	} // .if 

	http.HandleFunc("/api/UploadFileEvent", uploadFileEvent)
	log.Printf("About to listen on %s. Go to https://127.0.0.1%s/", listenAddr, listenAddr)
	log.Fatal(http.ListenAndServe(listenAddr, nil))
} // .main


type AzBlobStorageEvent []struct {
	Topic     string    `json:"topic"`
	Subject   string    `json:"subject"`
	EventType string    `json:"eventType"`
	EventTime time.Time `json:"eventTime"`
	ID        string    `json:"id"`
	Data      struct {
		API                string `json:"api"`
		ClientRequestID    string `json:"clientRequestId"`
		RequestID          string `json:"requestId"`
		ETag               string `json:"eTag"`
		ContentType        string `json:"contentType"`
		ContentLength      int    `json:"contentLength"`
		BlobType           string `json:"blobType"`
		URL                string `json:"url"`
		Sequencer          string `json:"sequencer"`
		StorageDiagnostics struct {
			BatchID string `json:"batchId"`
		} `json:"storageDiagnostics"`
	} `json:"data"`
	DataVersion     string `json:"dataVersion"`
	MetadataVersion string `json:"metadataVersion"`
} // .AzBlobStorageEvent