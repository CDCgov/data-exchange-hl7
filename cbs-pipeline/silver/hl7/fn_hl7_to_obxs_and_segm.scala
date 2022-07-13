// Databricks notebook source
// returns tuple3 of: ( obxsEpi, obxNonEpi, otherSegments )
// obxsEpi -> obxs under the Epi obr OBR-4.1 = 68991-9
// obxsNonEpi -> these are the obx/s under other obr/s, they keep the line numbers for obr and obx (the position in the message)
// otherSegments -> these are segments other than msh, pid, obr, or obx, they keep the line numbers for the segment position in the message
def HL7ToObxsAndSegments( hl7: String): 
  (Seq[(String, String, String, String)],                // obxsEpi       // (MSH, PID, OBR, OBX)
   Seq[(String, String, (String, Int), (String, Int))],  // obxsNonEpi    // (MSH, PID, (OBR, index), (OBX, index))
   Seq[(String, String, (String, Int))]) = {             // otherSegments // (MSH, PID, (Segment, index))
  // split the hl7 message into list of lines
  val newLineDelimiter = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
  val lines = hl7.split(newLineDelimiter).toSeq

  val linesWithIndex = lines.zipWithIndex
  // linesWithIndex.foreach {
  //     case(line, index) => println(s"$line is $index")
  // }

  val msh = linesWithIndex.filter(_._1.startsWith("MSH")).head._1 // there is only one, take only line no index
  val pid = linesWithIndex.filter(_._1.startsWith("PID")).head._1

  val obrs = linesWithIndex.filter(_._1.startsWith("OBR"))
  val obxs = linesWithIndex.filter(_._1.startsWith("OBX"))

  // map the msh, pid, respective obr to an obx
  val obxLake = obxs.map( obx => {

    // find the respective obr 
    val obrForObx = obrs.filter( obr => {
      obr._2 < obx._2
    }).sortWith(_._2 > _._2).head

    ( msh, pid, obrForObx, obx )
  }) // .obxLake

  // Epi Obr = the OBR where OBR-4.1 = 68991-9
  val obrEpiID = "68991-9"

  // OBX LAKE
  val obxsPartitioned = obxLake.partition( el => { 

    val obrParts = el._3._1.split("\\|")

    obrParts.size match {
      case x if x >= 5 => obrParts(4).contains(obrEpiID) // Epi Obr = 68991-9
      case _ => false
    } // .match 
  }) // .obxsPartitioned
  
  // OBX LAKE OF EPI
  val obxsEpi = obxsPartitioned._1.map( ob => {
    // discarding line numbers for epi obr and epi obxs
    (ob._1, ob._2, ob._3._1, ob._4._1)
  }) // .obxsEpi

  // OBX LAKE OF NON EPI
  val obxsNonEpi = obxsPartitioned._2

  // OTHER SEGMENTS
  val otherSegments = linesWithIndex.filter( ln => !( ln._1.startsWith("MSH") || ln._1.startsWith("PID") || ln._1.startsWith("OBR") || ln._1.startsWith("OBX") ) )
                        .filter( line => line._1.trim.nonEmpty ).map( line => (msh, pid, line) )

  // obxsEpi       ->   Seq[(String, String, String, String)]                ->  (MSH, PID, OBR, OBX)
  // obxsNonEpi    ->   Seq[(String, String, (String, Int), (String, Int))]  ->  (MSH, PID, (OBR, index), (OBX, index))
  // otherSegments ->   Seq[(String, String, (String, Int))])                ->  (MSH, PID, (Segment, index))
  ( obxsEpi, obxsNonEpi, otherSegments )

} // .HL7ToObxsAndSegments

// COMMAND ----------


