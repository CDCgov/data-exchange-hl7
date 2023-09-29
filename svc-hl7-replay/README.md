# Replay POC

POC Replay API is designed to introduce already processed messages ( both validated and errored ) back into the HL7 pipeline. At the time of this documentation, the final architecture has not been finalized so please reach out to leadership to verify.

## Description

To begin, the overall workflow of the Replay Functionality should be as follows:

1. API receives request 
	1.a Request must contain attributes to generate a query ( more details below )
2. Query Event Storage for the message(s)
3. Append message(s) with Replay Metadata ( more details below )
4. Submit message(s) into Event Hub Topic

### API Request

The incoming request to the API needs following required values:

1. Reason for replay
2. Event Hub Topic to place replayed messages
3. Message query method
- Single message w/ message_uuid  
- Single message w/ file_uuid  
- Multiple messages with a combination of the following:
    -   By date
    -   By jurisdiction
    -   By Route
 
 ### Replay Metadata

To distinguish replayed messages, a replay object will be attached that will contain the following values:

1. Reason for replay
2. Timestamp
3. Event Hub topic entry
4. Filter - The query string that was used
5. User

### Contact

If there are any questions, please reach out to Marcelo Caldas (mcq1@cdc.gov), Sai Valluripalli (ucn2@cdc.gov), Sebastian Clavijo (uux3@cdc.gov) or Jesus Aguilar (csb0@cdc.gov) 

## Diagram

```mermaid
graph TD
A[User] -- Replay Request --> B{Replay API}
B -- Query --> C[Storage]
C --Messages--> B
B --Message with Replay Metadata--> D[Event Topic]

