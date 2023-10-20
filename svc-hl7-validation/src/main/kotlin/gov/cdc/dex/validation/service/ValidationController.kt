package gov.cdc.dex.validation.service


import com.google.gson.*
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.validation.service.model.ErrorCounts
import gov.cdc.dex.validation.service.model.ErrorInfo
import gov.cdc.dex.validation.service.model.Summary
import gov.cdc.nist.validator.NistReport
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.util.*
import kotlin.jvm.optionals.getOrNull


@Controller("/")
class ValidationController(@Client("redactor") redactorClient: HttpClient, @Client("structure") structureClient: HttpClient ) {
    private var redactorClient: HttpClient
    private var structureClient: HttpClient

    //private HttpClient client;

    companion object {
        private val log = LoggerFactory.getLogger(ValidationController::class.java.name)
        private const val UTF_BOM = "\uFEFF"

    }
    init {
        this.redactorClient = redactorClient
        this.structureClient = structureClient
    }

    @Get(value = "/heartbeat", produces = [MediaType.TEXT_PLAIN])
    @Operation(summary="Used by application startup process. Returns 'hello' if all is well.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success", content = [Content(
            mediaType = "text/plain", schema = Schema(type = "string"),
            examples = [ExampleObject(value = "hello")]
        )]),
        ApiResponse(responseCode =  "400", description = "Bad Request")
    )
    fun getHeartbeatPingResponse() : String {
        return "hello"
    }

    @Post(value = "validate", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary="Action for validating HL7 v2 message(s)", description = """# Validate Function
        
This function accepts either a single HL7 v2 message or a batch of concatenated HL7 v2 messages for validation.  
### Single Message Submission and Response        
If a single message is submitted, then a full validation report is returned that includes counts and descriptions 
        of all errors and warnings encountered, along with an overall status of either "VALID_MESSAGE" if no errors are found or "STRUCTURE_ERRORS" if errors are found.
### Batch Message Submission and Response     
If a batch of messages is submitted, then a summary report is returned that includes the total error count and a breakdown of counts by type, by category, by message location (HL7 path), and by message.
### Message Type and Route Parameters        
HL7 messages submitted are expected to be either CASE (following a CDC Message Mapping Guide) or ELR (lab report) messages. The type of message must be specified in the query parameter **message_type**.

Messages also conform to a specific implementation profile, which this function refers to as the **route**.
- For CASE messages, the MMG used to create the message is determined from the message content and need not be specified in the "route" parameter.
- For ELR messages, the implementation guide used to create the message ***must*** be specified in the "route" parameter.
        
Batch submissions are expected to include messages that are all the same message type.
- For CASE, messages in a batch do not all have to use the same MMG.
- For ELR, all messages in a batch ***do*** have to conform to the same implementation profile.
    """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200",
            description = "Success",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(oneOf = [NistReport::class, Summary::class]),
                    examples = [
                        ExampleObject(name="Single Message Response", value = """{
                          "entries": {
                            "structure": [
                              {
                                "line": 14,
                                "column": 56,
                                "path": "OBX[10]-5[1].2",
                                "description": "test is not a valid Number. The format should be: [+|-]digits[.digits]",
                                "category": "Format",
                                "classification": "Error",
                                "stackTrace": null,
                                "metaData": null
                              }
                            ],
                            "content": [
                              {
                                "line": 3,
                                "column": 99,
                                "path": "OBR[1]-7[1].1",
                                "description": "DateTimeOrAll0s - If TS.1 (Time) is valued then TS.1 (Time) shall follow the date/time pattern 'YYYYMMDDHHMMSS[.S[S[S[S]]]][+/-ZZZZ]]'.",
                                "category": "Constraint Failure",
                                "classification": "Error"
                              }
                            ],
                            "value-set": []
                          },
                          "error-count": {
                            "structure": 1,
                            "value-set": 0,
                            "content": 1
                          },
                          "warning-count": {
                            "structure": 0,
                            "value-set": 0,
                            "content": 0
                          },
                          "status": "STRUCTURE_ERRORS"
                        }""" ),
                    ExampleObject(name="Batch Message Response", value = """{
                        "total_messages": 5,
                        "valid_messages": 2,
                        "invalid_messages": 3,
                        "error_counts": {
                            "total": 4,
                            "by_type": {
                                "structure": 1,
                                "content": 2,
                                "value_set": 0,
                                "other": 1
                            },
                            "by_category": {
                                "Constraint Failure": 2,
                                "Runtime Error": 1,
                                "Usage": 1
                            },
                            "by_path": {
                                "PID[1]-3[1]": 1,
                                "OBR[1]-22[1].1": 1,
                                "MSH-12": 1,
                                "PID[1]-5[1]": 1
                            },
                            "by_message": {
                                "message-1": 2,
                                "message-2": 1,
                                "message-3": 1,
                                "message-4": 0,
                                "message-5": 0
                            }
                        }
                    }
                    """)
                    ]

                )
            ]
        ),
        ApiResponse(responseCode =  "400",
            description = "Bad Request",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorInfo::class)
            )]
        )
    )
    fun validate(
            @Parameter(name="message_type",
                schema = Schema(description = "The type of data contained in the HL7 message",
                    allowableValues = ["ELR", "CASE"], required = true, type = "string"))
                @QueryValue message_type: String,
            @Parameter(name="route", schema = Schema(description = "For ELR only; the profile specification name",
                allowableValues = ["COVID19_ELR", "PHLIP_FLU", "PHLIP_VPD"], type = "string"))
                @QueryValue route:Optional<String>,
            @RequestBody(content = [Content(mediaType = "text/plain", schema = Schema(type = "string") , examples = [
                ExampleObject(name = "Single CASE message", value = """MSH|^~\&|^2.16.840.1.114222.4.1.144.2^ISO|^2.16.840.1.114222.4.1.144^ISO|^^ISO|^2.16.840.1.114222^ISO|20091130133708||ORU^R01^ORU_R01|182012_20091130133708|P|2.5|||||||||NND_ORU_v2.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Gen_Case_Map_v1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO
PID|1||182012^^^&2.16.840.1.114222.4.1.144.2&ISO||~^^^^^^S||19490214000000|M||1002-5^Amer.Ind./Alask.Nat.^2.16.840.1.113883.6.238~2106-3^White^2.16.840.1.113883.6.238|^^^08^^^^^101|||||||||||2186-5^Not Hispanic^2.16.840.1.113883.6.238
OBR|1||182012^^2.16.840.1.114222.4.1.144.2^ISO|PERSUBJ^Person Subject^2.16.840.1.114222.4.5.274|||20091125|||||||||||||||20091130133708|||X||||||10230^Tuleremia^2.16.840.1.114222.4.5.277
OBR|2||182012^^2.16.840.1.114222.4.1.144.2^ISO|NOTF^CASE NOTIFICATION^2.16.840.1.114222.4.5.274|||20091125000000|||||||||||||||20091130133708|||X||||||10230^Tuleremia^2.16.840.1.114222.4.5.277
OBX|1|CWE|INV107^Case Jurisdiction Code^2.16.840.1.114222.4.5.232||101^PUEBLO^2.16.840.1.113883.6.93||||||F
OBX|2|CWE|NOT116^National Reporting Jurisdiction^2.16.840.1.114222.4.5.232||08^Colorado^2.16.840.1.113883.6.92||||||F
OBX|3|CWE|NOT109^Reporting State^2.16.840.1.114222.4.5.232||08^Colorado^2.16.840.1.113883.6.92||||||F
OBX|4|ST|INV173^State Case ID^2.16.840.1.114222.4.5.232||CO182012||||||F
OBX|5|TS|INV147^Investigation Start Date^2.16.840.1.114222.4.5.232||20091125000000||||||F
OBX|6|TS|INV177^Date First Reported PHD^2.16.840.1.114222.4.5.232||20091125000000||||||F
OBX|7|CWE|INV152^Case Disease Imported Code^2.16.840.1.114222.4.5.232||In State^PHC244^2.16.840.1.113883.6.238||||||F
OBX|8|SN|INV2001^Age at case investigation^2.16.840.1.114222.4.5.232||^60|a^Years^2.16.840.1.113883.6.8|||||F
OBX|9|CWE|INV163^Case Class Status Code^2.16.840.1.114222.4.5.232||410605003^Confirmed^2.16.840.1.113883.6.96||||||F
OBX|10|SN|INV165^MMWR Week^2.16.840.1.114222.4.5.232||^test||||||F
OBX|11|TS|INV166^MMWR Year^2.16.840.1.114222.4.5.232||2009||||||F
OBX|12|CWE|INV150^Case outbreak indicator^2.16.840.1.114222.4.5.232||N^No^2.16.840.1.113883.12.136||||||F
OBX|13|CWE|INV151^Case outbreak name^2.16.840.1.114222.4.5.232||||||||F
OBX|14|CWE|INV178^Pregnancy Status^2.16.840.1.114222.4.5.232||UNK^Unknown^2.16.840.1.113883.5.1008||||||F
OBX|15|TS|INV111^Date of Report^2.16.840.1.114222.4.5.232||20091125000000||||||F
OBX|16|TS|INV120^Earliest Date Reported to County^2.16.840.1.114222.4.5.232||20091125000000||||||F
OBX|17|TS|INV121^Earliest Date Reported to State^2.16.840.1.114222.4.5.232||20091125000000||||||F
OBX|18|TS|INV136^Diagnosis date^2.16.840.1.114222.4.5.232||||||||F
OBX|19|TS|INV137^Date of Illness Onset^2.16.840.1.114222.4.5.232||||||||F
OBX|20|CWE|INV128^Was the patient hospitalized as a result of this event^2.16.840.1.114222.4.5.232||N^No^2.16.840.1.113883.12.136||||||F
OBX|21|TS|INV146^Date Of Death^2.16.840.1.114222.4.5.232||||||||F
OBX|22|CWE|INV145^Did the patient die from the condition under investigation^2.16.840.1.114222.4.5.232||N^No^2.16.840.1.113883.12.136||||||F
                """),
                ExampleObject(name = "Single ELR message", value = """MSH|^~\&|USVI.PHL.Horizon.PRO^2.16.840.1.113883.3.8589.4.2.78.1^ISO|USVI.PHL^2.16.840.1.113883.3.8589.4.1.125^ISO|US WHO Collab LabSys^2.16.840.1.114222.4.3.3.7^ISO|CDC-EPI Surv Branch^2.16.840.1.114222.4.1.10416^ISO|20221205134200.000-0500||ORU^R01^ORU_R01|6479|P|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO~PHLIP_ELSM_251^PHLIP_Profile_Flu^2.16.840.1.113883.9.179^ISO
SFT|HorizonLIMS|13.2.0|HORIZON|N/A||20221127172748
PID|1||19348^^^USVI.PHL.Horizon.PRO&2.16.840.1.113883.3.8589.4.2.78.1&ISO^PI||Pid5|^^^^^^M|20070209|M||2106-3^White^HL70005|^^^VI|||||||||||N^Not Hispanic or Latino^HL70189^NH^Not Hispanic^L
ORC|RE||17981001^USVI.PHL.Horizon.PRO^2.16.840.1.113883.3.8589.4.2.78.1^ISO||||||202212051342||||||||||||ChemWare Test Client^D|1324 Hospital Way^^^VI^^USA|^WPN^PH^^^123^4567891
OBR|1||17981001^USVI.PHL.Horizon.PRO^2.16.840.1.113883.3.8589.4.2.78.1^ISO|95422-2^FluAB + SARS-CoV-2 Pnl Resp NAA+prb^LN^MULTIPLEX^Multiplex Influenza SARS-CoV-2^L|||20221116010000.000-0500|||||||||||||||20221205134200.000-0500|||F
OBX|1|CWE|94533-7^SARS-CoV-2 N gene Resp Ql NAA+probe^LN^SC2^SARS-CoV-2^L||260415000^Not detected^SCT^260415000^Not Detected^L||||||F|||20221116010000.000-0500|||Influenza SARS-CoV-2 (Flu SC2) Multiplex Assay_Centers for Disease Control and Prevention (CDC)_EUA^^99ELR^00^PCR^L||20221117113900.000-0500||||US Virgin Islands Department of Health^D^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^XX^^^48D2179122|3500 Richmond Estate^^Christiansted^VI^00820-4370
OBX|2|CWE|92142-9^FLUAV RNA Resp Ql NAA+probe^LN^FLUA^Influenza A^L||260415000^Not detected^SCT^260415000^Not Detected^L||||||F|||20221116010000.000-0500|||Influenza SARS-CoV-2 (Flu SC2) Multiplex Assay_Centers for Disease Control and Prevention (CDC)_EUA^^99ELR^00^PCR^L||20221117113900.000-0500||||US Virgin Islands Department of Health^D^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^XX^^^48D2179122|3500 Richmond Estate^^Christiansted^VI^00820-4370
OBX|3|CWE|92141-1^FLUBV RNA Resp Ql NAA+probe^LN^FLUB^Influenza B^L||260373001^Detected^SCT^260373001^Detected^L||||||F|||20221116010000.000-0500|||Influenza SARS-CoV-2 (Flu SC2) Multiplex Assay_Centers for Disease Control and Prevention (CDC)_EUA^^99ELR^00^PCR^L||20221117113900.000-0500||||US Virgin Islands Department of Health^D^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^XX^^^48D2179122|3500 Richmond Estate^^Christiansted^VI^00820-4370
SPM|1|^17981001&USVI.PHL.Horizon.PRO&2.16.840.1.113883.3.8589.4.2.78.1&ISO||258500001^Nasopharyngeal swab^SCT^SN^Swab - NP^L|||||||||||||20221116010000.000-0500|20221117113500.000-0500                  
                """),
                ExampleObject(name = "Batch of CASE messages", value = """MSH|^~\&|SendAppName^2.16.840.1.114222.1111^ISO|Sending-Facility^2.16.840.1.114222.1111^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~CRS_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO
PID|1||test 7 nyc-CRS^^^SendAppName&2.16.840.1.114222.1111&ISO||~^^^^^^S||20111107|M||2106-3^White^CDCREC|^^^36^10001^^^^36061|||||||||||UNK^unknown^NULLFL
NK1|1||MTH^Mother^HL70063
OBR|1||TEST 7^SendAppName^2.16.840.1.114222.1111^ISO|68991-9^Epidemiologic Information^LN|||20160111143800|||||||||||||||20160111143800|||F||||||10370^Rubella, congenital syndrome^NND
OBX|1|CWE|78746-5^Country of Birth^LN||ESP^SPAIN^ISO3166_1||||||F
OBX|2|CWE|77983-5^Country of Usual Residence^LN||ESP^SPAIN^ISO3166_1||||||F
OBX|3|TS|11368-8^Date of Illness Onset^LN||20160103||||||F
OBX|4|CWE|77996-7^Pregnancy Status^LN||N^No^HL70136||||||F
OBX|5|TS|77975-1^Diagnosis Date^LN||20160103||||||F
OBX|6|CWE|77974-4^Hospitalized^LN||N^No^HL70136||||||F
OBX|7|CWE|77978-5^Subject Died^LN||N^No^HL70136||||||F
OBX|8|SN|77998-3^Age at Case Investigation^LN||^6|a^year [time]^UCUM|||||F
OBX|9|CWE|77982-7^Case Disease Imported Code^LN||C1512888^International^UML||||||F
OBX|10|CWE|INV153^Imported Country^PHINQUESTION||FRA^FRANCE^ISO3166_1||||||F
OBX|11|CWE|77984-3^Country of Exposure^LN|1|FRA^FRANCE^ISO3166_1||||||F
OBX|12|CWE|77984-3^Country of Exposure^LN|2|FRA^FRANCE^ISO3166_1||||||F
OBX|13|CWE|77990-0^Case Class Status Code^LN||410605003^Confirmed present^SCT||||||F
OBX|14|CWE|77965-2^Immediate National Notifiable Condition^LN||Y^Yes^HL70136||||||F
OBX|15|CWE|77980-1^Case Outbreak Indicator^LN||N^No^HL70136||||||F
OBX|16|ST|52831-5^Reporting Source ZIP Code^LN||10001||||||F
OBX|17|DT|77979-3^Case Investigation Start Date^LN||20160103||||||F
OBX|18|DT|77995-9^Date Reported^LN||20160103||||||F
OBX|19|TS|77972-8^Earliest Date Reported to County^LN||20160103||||||F
OBX|20|TS|77973-6^Earliest Date Reported to State^LN||20160103||||||F
OBX|21|SN|77991-8^MMWR Week^LN||^3||||||F
OBX|22|DT|77992-6^MMWR Year^LN||2016||||||F
OBX|23|DT|77970-2^Date First Reported to PHD^LN||20160103||||||F
OBX|24|CWE|77966-0^Reporting State^LN||36^New York^FIPS5_2||||||F
OBX|25|CWE|77967-8^Reporting County^LN||36061^New York, NY^FIPS6_4||||||F
OBX|26|CWE|77968-6^National Reporting Jurisdiction^LN||36^NY^FIPS5_2||||||F
OBX|27|CWE|67187-5^Type of Complication^LN|1|16294009^Enlarged spleen^SCT||||||F
OBX|28|CWE|INV920^Type of Complications Indicator^PHINQUESTION|1|N^No^HL70136||||||F
OBX|29|CWE|67187-5^Type of Complication^LN|2|128306009^Cataract^SCT||||||F
OBX|30|CWE|INV920^Type of Complications Indicator^PHINQUESTION|2|N^No^HL70136||||||F
OBX|31|CWE|85710-2^Does the Mother Know Where She Might Have Been Exposed to Rubella^LN||UNK^Unknown^NULLFL||||||F
OBX|32|CWE|LAB630^Laboratory Testing Performed^PHINQUESTION||Y^Yes^HL70136||||||F
OBX|33|CWE|INV164^Laboratory Confirmed^PHINQUESTION||Y^Yes^HL70136||||||F
OBX|34|CWE|82314-6^Specimen Sent to CDC^LN||Y^Yes^HL70136||||||F
OBX|35|CWE|INV290^Test Type^PHINQUESTION|1|54091-4^Rubella PCR^LN||||||F
OBX|36|CWE|INV291^Test Result^PHINQUESTION|1|10828004^Positive^SCT||||||F
OBX|37|CWE|85690-6^Genotype Sequence^LN|1|OTH^Other^NULLFL^^^^^^B3||||||F
OBX|38|CWE|82771-7^Performing Laboratory Type^LN|1|PHC412^CDC Lab^CDCPHINVS||||||F
MSH|^~\&|SendAppName^2.16.840.1.114222.1111^ISO|Sending-Facility^2.16.840.1.114222.1111^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~CRS_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO
PID|1||test 6 nm-CRS^^^SendAppName&2.16.840.1.114222.1111&ISO||~^^^^^^S||20150720|F||1002-5^American Indian or Alaska Native^CDCREC|^^^35^87029^^^^35039|||||||||||2135-2^Hispanic or Latino^CDCREC
NK1|1||MTH^Mother^HL70063
OBR|1||TEST 6  ^SendAppName^2.16.840.1.114222.1111^ISO|68991-9^Epidemiologic Information^LN|||20160608143800|||||||||||||||20160608143800|||F||||||10370^Rubella, congenital syndrome^NND
OBX|1|CWE|78746-5^Country of Birth^LN||USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|2|CWE|77983-5^Country of Usual Residence^LN||USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|3|TS|11368-8^Date of Illness Onset^LN||20160531||||||F
OBX|4|CWE|77996-7^Pregnancy Status^LN||N^No^HL70136||||||F
OBX|5|TS|77975-1^Diagnosis Date^LN||20160531||||||F
OBX|6|CWE|77974-4^Hospitalized^LN||Y^Yes^HL70136||||||F
OBX|7|TS|8656-1^Admission Date^LN||20160531||||||F
OBX|8|TS|8649-6^Discharge Date^LN||20160606||||||F
OBX|9|SN|78033-8^Duration of Hospital Stay in Days^LN||^7||||||F
OBX|10|CWE|77978-5^Subject Died^LN||N^No^HL70136||||||F
OBX|11|SN|77998-3^Age at Case Investigation^LN||^10|mo^month [time]^UCUM|||||F
OBX|12|CWE|77982-7^Case Disease Imported Code^LN||PHC244^Indigenous^CDCPHINVS||||||F
OBX|13|CWE|77984-3^Country of Exposure^LN|1|USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|14|CWE|77985-0^State or Province of Exposure^LN|1|35^New Mexico^FIPS5_2||||||F
OBX|15|CWE|77984-3^Country of Exposure^LN|2|USA^UNITED STATES OF AMERICA^ISO3166_1||||||F
OBX|16|CWE|77985-0^State or Province of Exposure^LN|2|35^New Mexico^FIPS5_2||||||F
OBX|17|CWE|77990-0^Case Class Status Code^LN||410605003^Confirmed present^SCT||||||F
OBX|18|CWE|77965-2^Immediate National Notifiable Condition^LN||Y^Yes^HL70136||||||F
OBX|19|CWE|77980-1^Case Outbreak Indicator^LN||N^No^HL70136||||||F
OBX|20|CWE|48766-0^Reporting Source Type Code^LN||PHC251^Public Health Clinic^CDCPHINVS||||||F
OBX|21|ST|52831-5^Reporting Source ZIP Code^LN||87029||||||F
OBX|22|DT|77979-3^Case Investigation Start Date^LN||20160531||||||F
OBX|23|DT|77995-9^Date Reported^LN||20160531||||||F
OBX|24|TS|77972-8^Earliest Date Reported to County^LN||20160531||||||F
OBX|25|TS|77973-6^Earliest Date Reported to State^LN||20160531||||||F
OBX|26|SN|77991-8^MMWR Week^LN||^22||||||F
OBX|27|DT|77992-6^MMWR Year^LN||2016||||||F
OBX|28|DT|77970-2^Date First Reported to PHD^LN||20160531||||||F
OBX|29|CWE|77966-0^Reporting State^LN||35^New Mexico^FIPS5_2||||||F
OBX|30|CWE|77967-8^Reporting County^LN||35039^Rio Arriba, NM^FIPS6_4||||||F
OBX|31|CWE|77968-6^National Reporting Jurisdiction^LN||35^NM^FIPS5_2||||||F
OBX|32|CWE|67187-5^Type of Complication^LN|1|15188001^Hearing Impairment^SCT||||||F
OBX|33|CWE|INV920^Type of Complications Indicator^PHINQUESTION|1|Y^Yes^HL70136||||||F
OBX|34|CWE|67187-5^Type of Complication^LN|2|13213009^Congenital heart disease^SCT||||||F
OBX|35|CWE|INV920^Type of Complications Indicator^PHINQUESTION|2|Y^Yes^HL70136||||||F
OBX|36|CWE|85730-0^Did the Mother Have a Fever^LN||Y^Yes^HL70136||||||F
OBX|37|CWE|85794-6^Did the Mother Have Arthralgia/Arthritis^LN||Y^Yes^HL70136||||||F
OBX|38|SN|85720-1^Number of Children Less Than 18 Years of Age Immunized With the Rubella Vaccine^LN||^4||||||F
OBX|39|CWE|85716-9^Was There a Rubella-like Illness During This Pregnancy^LN||Y^Yes^HL70136||||||F
OBX|40|CWE|INV516^US Acquired^PHINQUESTION||PHC465^Import-linked case^CDCPHINVS||||||F
OBX|41|ST|85658-3^Current Occupation^LN|1|Steel worker||||||F
OBX|42|ST|85078-4^Current Industry^LN|1|Oil Refinement||||||F
OBX|43|ST|85658-3^Current Occupation^LN|2|Seamtress||||||F
OBX|44|ST|85078-4^Current Industry^LN|2|Education||||||F
OBX|45|CWE|LAB630^Laboratory Testing Performed^PHINQUESTION||Y^Yes^HL70136||||||F
OBX|46|CWE|INV164^Laboratory Confirmed^PHINQUESTION||Y^Yes^HL70136||||||F
OBX|47|CWE|82314-6^Specimen Sent to CDC^LN||Y^Yes^HL70136||||||F
OBX|48|CWE|85793-8^Specimen From Mother or Infant^LN|1|133931009^Infant^SCT||||||F
OBX|49|CWE|INV290^Test Type^PHINQUESTION|1|54091-4^Rubella PCR^LN||||||F
OBX|50|CWE|INV291^Test Result^PHINQUESTION|1|10828004^Positive^SCT||||||F
OBX|51|CWE|85690-6^Genotype Sequence^LN|1|427559006^Rubella virus genotype 1D^SCT||||||F
OBX|52|CWE|82771-7^Performing Laboratory Type^LN|1|PHC1316^VPD Testing Laboratory^CDCPHINVS||||||F
OBX|53|ST|LAB143^VPD Lab Message Reference Laboratory^PHINQUESTION|1|2.16.840.1.114222.4.1.209842||||||F
OBX|54|CX|LAB598^VPD Lab Message Patient Identifier^PHINQUESTION|1|E442277||||||F
OBX|55|CX|LAB125^VPD Lab Message Specimen Identifier^PHINQUESTION|1|14VR006693||||||F
OBX|56|CWE|85702-9^Did Mother Ever Receive a Vaccine Against This Disease^LN||N^No^HL70136||||||F
                """)])])
                content: String,
            request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        val routeValue = route.orElse("")
        val metadata: HashMap<String, String> = HashMap()
        metadata["message_type"]= message_type
        metadata["route"]= routeValue

        if (message_type.isEmpty()) {
            log.error("Missing Header for message_type")
            return badRequest("BAD REQUEST: Message Type ('CASE' or 'ELR') must be specified using query parameter 'message_type'. Please try again.")
        }
        if (message_type == HL7MessageType.ELR.name && routeValue.isNullOrEmpty()) {
            log.error("Missing Header for route when Message_type == ELR")
            return badRequest("BAD REQUEST: ELR message must specify a route using query parameter 'route'. Please try again.")
        }
        // since content is a required parameter, we can be certain it has a value.
        // otherwise, 'bad request' would have been returned by Micronaut.
        val arrayOfMessages = debatchMessages(content)
        return if (arrayOfMessages.size == 1) {
            val resultData = this.validateMessage(arrayOfMessages[0], metadata)
            if (!resultData.startsWith("Error")) {
                log.info("message successfully redacted and validated")
                HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
            } else {
                log.error(resultData)
                badRequest(resultData)
            }
        } else {
            val resultSummary = this.validateBatch(arrayOfMessages, metadata)
            log.info("batch summary created successfully")
            HttpResponse.ok(resultSummary).contentEncoding(MediaType.APPLICATION_JSON)
        }

    }

    private fun badRequest(responseMessage: String) : HttpResponse<String> {
        val error = ErrorInfo(description = responseMessage)
        return HttpResponse.badRequest(JsonHelper.gson.toJson(error)).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun validateBatch(arrayOfMessages: ArrayList<String>, metadata: Map<String, String>): String {
        val mapOfResults = mutableMapOf<String, String>()
        arrayOfMessages.forEachIndexed { index, message ->
            val result = validateMessage(message, metadata)
            mapOfResults.putIfAbsent("message-${index + 1}", result)
        }
        return prepareSummaryFromMap(mapOfResults)
    }

    private fun prepareSummaryFromMap(mapOfResults: Map<String, String>) : String {
        var runtimeErrorCount = 0
        var structureErrorCount = 0
        var contentErrorCount = 0
        var valueSetErrorCount = 0
        val countsByMessage = mutableMapOf<String, Int>()
        var validMessageCount = 0
        var invalidMessageCount = 0
        val entries = mutableListOf<JsonElement>()
        mapOfResults.forEach { (messageId, report) ->
            // each 'report' with be either a NIST report (JSON) or a runtime error (plain text)
            if (report.startsWith("Error")) {
                runtimeErrorCount++
                invalidMessageCount++
                // extract the path that caused the error, if it exists
                val regex = "[A-Z]{3}-[0-9]{1,2}".toRegex()
                val path = regex.find(report)?.value + ""
                val error = ErrorInfo (description = report, path = path).toJsonElement()
                countsByMessage.putIfAbsent(messageId, 1)
                entries.add(error)
            } else {
                val reportJson = JsonParser.parseString(report).asJsonObject
                if (JsonHelper.getValueFromJson("status", reportJson).asString == "VALID_MESSAGE") {
                    // valid message has 0 errors
                    validMessageCount++
                    countsByMessage.putIfAbsent(messageId, 0)
                } else {
                    invalidMessageCount++
                    val structure = getListOfErrors("entries.structure", reportJson)
                    val content = getListOfErrors("entries.content", reportJson)
                    val valueSet = getListOfErrors("entries.value-set", reportJson)
                    structureErrorCount += structure.size
                    contentErrorCount += content.size
                    valueSetErrorCount += valueSet.size
                    countsByMessage.putIfAbsent(messageId, structure.size + content.size + valueSet.size)
                    entries.addAll(structure + content + valueSet)
                }
            } // .if
        } //.forEach
        val countsByCategory = entries.groupingBy { JsonHelper.getValueFromJson("category", it).asString }.eachCount()
        val countsByPath = entries.groupingBy { JsonHelper.getValueFromJson("path", it).asString }.eachCount()

        val summary = Summary(
            totalMessages = mapOfResults.size,
            validMessages = validMessageCount,
            invalidMessages = invalidMessageCount,
            errors = ErrorCounts(
                totalErrors = runtimeErrorCount + structureErrorCount + contentErrorCount + valueSetErrorCount,
                errorsByType = mapOf( "structure" to structureErrorCount,
                    "content" to contentErrorCount,
                    "value_set" to valueSetErrorCount,
                    "other" to runtimeErrorCount),
                errorsByCategory = countsByCategory,
                errorsByPath = countsByPath,
                errorsByMessage = countsByMessage
            )
        )
        return JsonHelper.gson.toJson(summary)
    }

    private fun getListOfErrors(jsonPath : String, report: JsonObject) : List<JsonElement> {
        return JsonHelper.getValueFromJson(jsonPath, report)
            .asJsonArray.filter { JsonHelper.getValueFromJson("classification", it ).asString == "Error" }
    }

    private fun debatchMessages(messages : String) : ArrayList<String> {
        val messageLines = messages.lines()
        val currentLinesArr = arrayListOf<String>()
        val messagesArr = arrayListOf<String>()
        var mshCount = 0
        messageLines.forEach { line ->
            val lineClean = line.trim().let { if (it.startsWith(UTF_BOM)) it.substring(1) else it }
            if (lineClean.startsWith("FHS") ||
                lineClean.startsWith("BHS") ||
                lineClean.startsWith("BTS") ||
                lineClean.startsWith("FTS")) {

                // batch line --Nothing to do here
            } else if (lineClean.isNotEmpty()) {
                if (lineClean.startsWith("MSH|")) {
                    mshCount++
                    if (mshCount > 1) {
                        messagesArr.add(currentLinesArr.joinToString("\n"))
                    }
                    currentLinesArr.clear()
                } // .if
                currentLinesArr.add(lineClean)
            }
        }
        if (currentLinesArr.isNotEmpty()) {
            messagesArr.add(currentLinesArr.joinToString("\n"))
        }
        return messagesArr
    }

    private fun validateMessage(hl7Content: String, metadata: Map<String, String>): String {
        val redactedMessage = getRedactedContent(hl7Content, metadata)
        return if (redactedMessage.isEmpty()) {
            "Error: Redacted message is empty"
        } else if (redactedMessage.startsWith("Error")) {
            redactedMessage
        } else {
            getStructureReport(redactedMessage, metadata)
        }
    }

    private fun postApiRequest(client: HttpClient, url: String, bodyContent: String, metadata: Map<String, String>) : String {
        val call =
            client.exchange(
                POST(url, bodyContent)
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("x-tp-message_type", metadata["message_type"])
                    .header("x-tp-route", metadata["route"] ?: ""),
                String::class.java
            )
        return try {
            val response = Flux.from(call).blockFirst()
            val message = response?.getBody(String::class.java)
            message?.getOrNull() ?: "Error: No response received from $url"
        } catch (e : Exception) {
            "Error in request to $url : ${e.message}"
        }
    }

    private fun getRedactedContent(hl7Content: String, metadata: Map<String, String>): String {
        log.info("redacting message....")
        val message = postApiRequest(redactorClient, "/api/redactorReport",
            hl7Content, metadata)
        return try {
            val json = JsonParser.parseString(message).asJsonObject
            log.info("message redacted!")
            json.get("_1").asString
        } catch (e: JsonSyntaxException) {
            message
        }

    }

    private fun getStructureReport(hl7Content: String, metadata: Map<String, String>): String {
        log.info("Validating message...")
        val structReport = postApiRequest(structureClient, "/api/structure",
            hl7Content, metadata)
        log.info("message Validated")
        return structReport
    }

}