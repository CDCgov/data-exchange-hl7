package cdc.xlr.structurevalidator


import scala.io.Source
import java.io.{File, PrintWriter}

import scala.util.{Try, Failure, Success}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

object Main {

    def main(args: Array[String]): Unit = {

        println("Structure Validator - with local profiles...")

        // load test file from resources
        val fileName = "PERT_V1.0.1_TM_TC04"
        val hl7TestMessage = "src/main/resources/hl7messages/" + fileName + ".txt"
        val testMsg = Source.fromFile(hl7TestMessage).getLines.mkString("\n")
        
        val numMsgs = 1
        1 to numMsgs foreach { i => 
            
            time {
                // the async Validator
                val validator = StructureValidator()

                Try( Await.result(validator.validate(testMsg), Duration(2, TimeUnit.SECONDS)) ) match {

                    case Success( report ) => {
                        println("report", report)
                        println("Structure Validator - writing report to file: src/main/resources/hl7messages/validation_report.json")
                        // writeToFile("src/main/resources/hl7messages/" + fileName + "_validation_report.json", report)
                    } // .Success 

                    case Failure( err ) => {
                        println("Error structure validation: ", err.getMessage )
                    } // .Failure

                } // .Try  

                // // the sync Validator
                // val validator = StructureValidatorSync()
                // val result = validator.validate(testMsg)
            } // .time
        
        } // .foreach

        println("validated: " + numMsgs +" messages")
 
         
        //println("Structure Validator - writing result to file: src/main/resources/hl7messages/validation_result.json")
        //writeToFile("src/main/resources/hl7messages/" + fileName + "_validation.json", result)

    } // .main 


    def writeToFile(p: String, s: String): Unit = {
        val pw = new PrintWriter(new File(p))
        try pw.write(s) finally pw.close()
    } // .writeToFile

      def time[R](block: => R): R = {
        val t0 = System.currentTimeMillis()
        val result = block    // call-by-name
        val t1 = System.currentTimeMillis()
        println("Elapsed time: " + (t1 - t0) + "ms")
        result
    } // .time


} // .StructureValidator
