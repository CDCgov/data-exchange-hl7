package gov.cdc.dataexchange.entModel

case class Mmg (
    val profileIdentifier: String,
    val blockName: String,
    val blockType: String,
    val blockOrdinal: Int,
    val blockID: String,
    val elemName: String,
    val elemOrdinal: Int,
    val elemDataType: String,
    val elemIsRepeat: Boolean,
    val elemValueSetCode: Option[String],
    val elemValueSetVersionNumber: Option[Int],
    val elemIdentifier: String, 
    val elemSegmentType: String,
    val elemFieldPosition: Int,
    val elemComponentPosition: Int,
    val elemCardinality: String,
    val elemHl7v2DataType: String,
    val elemCodeSystem: String,
    // val blkNameStd: String,
    // val elemNameStd: String,
) {
    val blkNameStd: String = renameStrHeader(this.blockName)
    val elemNameStd: String = renameStrHeader(this.elemName)

    def toSeqLine(): Seq[String] = {
        
        // TODO: remove once GoldTemp changes
        // mmgSeq
        // 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
        // 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
        // 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
        // 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 
      
        Seq(this.profileIdentifier, 
            this.blockType,
            this.blockName,
            this.blockOrdinal.toString,
            this.blockID,
            //
            this.elemName,
            this.elemOrdinal.toString,
            this.elemDataType,
            this.elemIsRepeat.toString,
            this.elemValueSetCode.toString,
            //
            this.elemValueSetVersionNumber.toString,
            this.elemIdentifier,
            this.elemSegmentType,
            this.elemFieldPosition.toString,
            this.elemComponentPosition.toString,
            //
            this.elemCardinality,
            this.elemHl7v2DataType,
            this.elemCodeSystem,
            this.blkNameStd,
            this.elemNameStd,
            )
    } // .toSeqLine

    def renameStrHeader(s: String): String = {
      val rr1 = s.toLowerCase.trim 
      var rr2 = rr1.replaceAll("\\s", "_")
      val rr3 = rr2.replaceAll("-", "_") 
      val rr4 = rr3.replaceAll("/", "")
      val rr5 = rr4.replaceAll("&", "and")
      val rr6 = rr5.replaceAll("(_)\\1+", "_")
      val rr7 = rr6.replaceAll("[^A-Z a-z 0-9 _]", "" )
      rr7      
    } // renameStrHeader 

} // .Mmg

