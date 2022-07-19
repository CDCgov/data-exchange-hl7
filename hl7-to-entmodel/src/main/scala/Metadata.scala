package gov.cdc.dataexchange.entModel

import java.sql.Timestamp

case class Metadata (

    filePath: String,
    fileName: String,
    fileTimestamp: Timestamp,
    fileSize: Int,
    ingestUUID: String,
    ingestTimestamp: Timestamp,
    recordIndex: Int,
    recordUUID: String,

) // .Metadata 
