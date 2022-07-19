package gov.cdc.dataexchange.entModel

case class MessageInfo(

    sendingFacility: String,
    profileIdentifier: String,
    profileIdentifierAll: Seq[String],
    caseID: String,
    msh7: String,

) // .MessageInfo
