package cdc.ede.hl7.structuralvalidator

import com.azure.messaging.eventhubs._

import net.liftweb.json.DefaultFormats
import net.liftweb.json._

import scala.util.{Try, Failure, Success}
import expression.EvalResult

import cdc.xlr.structurevalidator._

object PhinProfileUtil {

  def extract(message: String ): Try[String] = {

    try {
        // extract PHIN profile from message:
            val msh21_1 = message.split("\n")(0).split("\\|")(20).split("\\^")(0) 
            msh21_1 match {
                // TODO: define constants needed for each profile
                // TODO: move to constants in tandem with hl7 structural validator (folder names)
                case "NOTF_ORU_v2.0" => Success(PROFILES_PHIN_SPEC_2_0)
                case "NOTF_ORU_v3.0" => Success(PROFILES_PHIN_SPEC_3_0)
                case "NOTF_ORU_v3.1" => Success(PROFILES_PHIN_SPEC_3_1)
                // case "NOTF_ORU_v3.2" => Success(PROFILES_PHIN_SPEC_3_2)
                case _ => Failure(throw new Exception("Unknown extracted profile MSH[21][1]: " + msh21_1))
            }

    } catch {
        case e: Exception => Failure(e)
    }

  } // .extract


} // .PhinProfileUtil


