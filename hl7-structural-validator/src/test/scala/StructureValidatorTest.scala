package cdc.xlr.structurevalidator

import org.scalatest._
import flatspec._
import matchers._

import scala.io.Source
import scala.util.{Try, Failure, Success}

class StructureValidatorTest extends AnyFlatSpec with should.Matchers {

    "Test message: PERT_V1.0.1_TM_TC04" should "have 6 structure errors, no content and no value set errors with concurrent validator" in {

        // load test file from resources
        val fileName = "PERT_V1.0.1_TM_TC04"
        val hl7TestMessage = "src/main/resources/hl7messages/" + fileName + ".txt"
        val testMsg = Source.fromFile(hl7TestMessage).getLines.mkString("\n")

        val validator = StructureValidatorConc()

        validator.reportMap(testMsg) match {

            case Success(report) => {
                report("contentErrors").size shouldBe(0) 
                report("valueSetErrors").size shouldBe(0) 
                report("structureErrors").size shouldBe(6) 
            } // .Success
            case Failure(e) => {
                println("error: " + e.getMessage() )
                fail("validate test message should not throw exception")
            } // .Failure 

        } // .match

    } // .in

    "Test message empty or without MSH segment" should "throw with message: No MSH Segment found in the message." in {

        val validator = StructureValidatorConc()

        validator.reportMap("some not valid message") match {

            case Success(report) => {
                fail("validate empty message should throw exception")
            } // .Success
            case Failure(e) => {
                e.getMessage() shouldBe("No MSH Segment found in the message.")
            } // .Failure 

        } // .match

    } // .in

    


} // .StructureValidatorTest
