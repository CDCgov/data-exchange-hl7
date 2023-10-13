# Replay API POC

Replay API is designed to introduce already processed messages (both validated and error queued) back into the HL7 pipeline.


### API Calls

| HTTP Request  | Description  |
|:-------------:| -----:|
| POST /replay/{mesaage_uuid}|Replayes a single message by uuid and returns process_id|
| POST /replay/{file_uuid} |Replays messages associated to file uuid and returns process_id |
| GET /replay/{process_id} |Returns the status of replay|



 ### Replay Request Object for POST /replay/{file_uuid}
 | Parameter Name  | Parameter Type  | Example | Description
 |:-------------:| -----:| ---------:|--------------:|
 | reason|String|Message is error queued| Reason for replay
 | user|String|ewl0|User requesting to replay message(s)|
 | message_query |enum|jurisdiction|User can filter messages to be replayed by jurisdiction code, start and end dates, and route|



To distinguish replayed messages, a replay object will be attached to a message that will contain the following:

1. Reason
2. Timestamp
3. Event Hub topic entry
4. message_query
   
### Replay Response Object
TBD

### Contact

If there are any questions, please reach out to Marcelo Caldas (mcq1@cdc.gov), Sai Valluripalli (ucn2@cdc.gov)

Note: This API is still under development and will be updated as needed.

## Diagram

![MicrosoftTeams-image (3)](https://github.com/CDCgov/data-exchange-hl7/assets/137535421/4c9402db-afa6-4aa9-97ce-018f0d6febd0)



