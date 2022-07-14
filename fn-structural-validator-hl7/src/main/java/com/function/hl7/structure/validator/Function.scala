package com.function.hl7.structure.validator;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

import cdc.xlr.structurevalidator._;


/**
 * Azure Functions with HTTP Trigger.
 */
class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    def run(
        @HttpTrigger(
          name = "req",
          methods = Array(HttpMethod.GET, HttpMethod.POST),
          authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage[Optional[String]],
        context: ExecutionContext): HttpResponseMessage = {
        context.getLogger.info("Scala HTTP trigger processed a request.")
        
        // Genv2_2-0-1_TC01 from https://ndc.services.cdc.gov/mmgpage/generic-v2-0-message-mapping-guide/
        val testMsg = """MSH|^~\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|TM_CN_TC01_GENV2|T|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO
PID|1||GenV2_TC01^^^SendAppName&2.16.840.1.114222.nnnn&ISO||~^^^^^^S||19640502|F||2106-3^White^CDCREC|^^^48^77018^^^^48201|||||||||||2135-2^Hispanic or Latino^CDCREC|||||||20140302
OBR|1||GenV2_TC01^SendAppName^2.16.840.1.114222.nnnn^ISO|68991-9^Epidemiologic Information^LN|||20140227170100|||||||||||||||20140227170100|||F||||||11550^Hemolytic uremic syndrome postdiarrheal^NND
OBX|1|ST|32624-9^Other Race Text^LN||Apache||||||F
OBX|2|CWE|78746-5^Country of Birth^LN||USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|3|CWE|77983-5^Country of Usual Residence^LN||USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|4||11368-8^Date of Illness Onset^LN||20140224||||||F"""
        val validator = StructureValidatorSync()
        val result = validator.validate(testMsg)

        request.createResponseBuilder(HttpStatus.OK).body(result).build
    } // .HttpTrigger
}
