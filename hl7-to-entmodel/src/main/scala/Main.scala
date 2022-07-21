package gov.cdc.dataexchange.entModel

import scala.io.Source
import java.io.{File, PrintWriter}


import spray.json._


object Main extends App with MmgJsonProtocol {
  println("running app..")

  val hl7MessageLoc = "src/main/resources/" + "Lyme_TBRD_1.txt" // "Genv2_2-0-1_TC01.txt" // "Lyme_TBRD_1.txt"
  val hl7MessageContent = Source.fromFile(hl7MessageLoc).getLines.mkString("\n")

  val hl7 = new MessageHL7(hl7MessageContent)
  val hl7WithReport1 = hl7.validateStructure(hl7)
  val hl7AtLake = hl7WithReport1.transformToObxLake(hl7WithReport1)

  val mmgProfile = "Lyme_TBRD_MMG_V1.0" // "Lyme_TBRD_MMG_V1.0" // "Generic_MMG_V2.0" // "RIBD_MMG_V1.1" // 
  val mmgLoc = "src/main/resources/" + mmgProfile + ".json"
  val mmgJson = Source.fromFile(mmgLoc).getLines.mkString("\n")

  val mmgRoot = mmgJson.parseJson.convertTo[MmgRoot]

  val mmgSeq = mmgRoot.blocks.flatMap( block => {
    block.elements.map( el => {
      new Mmg(mmgRoot.profileIdentifier, block.name, block.blockType, block.ordinal, block.id, 
              el.name, el.ordinal, el.dataType, el.isRepeat, el.valueSetCode, el.valueSetVersionNumber, 
              el.mappings.hl7v251.identifier, el.mappings.hl7v251.segmentType, el.mappings.hl7v251.fieldPosition, 
              el.mappings.hl7v251.componentPosition,
              el.mappings.hl7v251.cardinality, el.mappings.hl7v251.dataType, el.codeSystem ).toSeqLine()
    })
  })

  
  // val (singles, repeats) = mmgSeq.partition(el => {
  //   el.blockType == "Single"
  // })
  // println(singles.size + ", " + repeats.size)
  // singles.sortBy(_.elemNameStd).foreach(el => println(el.elemNameStd))
  // println("-----")
  // repeats.sortBy(_.blkNameStd).foreach(el => println(el.blkNameStd + " --> " + el.elemNameStd))
  

  val hl7EntModel = hl7AtLake.transformToEntModel(hl7AtLake, mmgSeq) 
  println(JsonUtil.toJson(hl7EntModel))
  writeToFile("src/main/resources/entModel_Lyme_1.json", JsonUtil.toJson(hl7EntModel))


  def writeToFile(p: String, s: String): Unit = {
      val pw = new PrintWriter(new File(p))
      try pw.write(s) finally pw.close()
  } // 

} // .App



