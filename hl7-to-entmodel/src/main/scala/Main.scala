package gov.cdc.dataexchange.entModel

import scala.io.Source

import spray.json._


object Main extends App with MmgJsonProtocol {
  println("running app..")

  val hl7MessageLoc = "src/main/resources/" + "Genv2_2-0-1_TC01.txt"
  val hl7MessageContent = Source.fromFile(hl7MessageLoc).getLines.mkString("\n")

  val hl7 = new MessageHL7(hl7MessageContent)
  val hl7WithReport1 = hl7.validateStructure(hl7)
  println(JsonUtil.toJson(hl7WithReport1))
  
  // val mmgProfile = "RIBD_MMG_V1.1" // "Generic_MMG_V2.0"
  // val mmgLoc = "src/main/resources/" + mmgProfile + ".json"
  // val mmgJson = Source.fromFile(mmgLoc).getLines.mkString("\n")

  // println(mmgJson.parseJson.convertTo[MmgRoot])


} // .App

