package gov.cdc.dataexchange.entModel

import scala.io.Source
import java.io.{File, PrintWriter}


import spray.json._


object Main extends App with MmgJsonProtocol {
  println("running app..")

  val hl7MessageLoc = "src/main/resources/" + "Genv2_2-0-1_TC01.txt"
  val hl7MessageContent = Source.fromFile(hl7MessageLoc).getLines.mkString("\n")

  val hl7 = new MessageHL7(hl7MessageContent)
  val hl7WithReport1 = hl7.validateStructure(hl7)
  val hl7AtLake = hl7WithReport1.transformToObxLake(hl7WithReport1)
  println(JsonUtil.toJson(hl7AtLake))


   writeToFile("src/main/resources/entModel.json", JsonUtil.toJson(hl7AtLake))
  
  // val mmgProfile = "RIBD_MMG_V1.1" // "Generic_MMG_V2.0"
  // val mmgLoc = "src/main/resources/" + mmgProfile + ".json"
  // val mmgJson = Source.fromFile(mmgLoc).getLines.mkString("\n")

  // println(mmgJson.parseJson.convertTo[MmgRoot])
  def writeToFile(p: String, s: String): Unit = {
      val pw = new PrintWriter(new File(p))
      try pw.write(s) finally pw.close()
  } // 
  
} // .App



