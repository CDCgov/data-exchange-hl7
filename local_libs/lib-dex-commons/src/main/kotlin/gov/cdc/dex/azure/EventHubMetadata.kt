package gov.cdc.dex.azure

//These attributes need to be CApitalized to work with AZ functions BindingName
data class EventHubMetadata(
    var SequenceNumber:Int,
    var Offset:Long,
    var PartitionKey:String?,
    var EnqueuedTimeUtc:String
)