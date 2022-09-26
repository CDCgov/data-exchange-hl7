package com.example

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.azure.messaging.eventhubs.*

import java.util.UUID
import java.io.*

import com.google.gson.*

const val GENV2 = "Generic_MMG_V2.0"
const val GENV1 = "Generic_MMG_V1.0"


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                // TODO:
                eventHubName = "eventhub001",
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        // context.logger.info("message: --> " + message)
        // TODO: change to read message from Even Hub, validate hl7Message.content

        val hl7TestMessage = this::class.java.getResource("/testMessage.hl7").readText() 

        // get profile identifier for the message:
        val hl7Util = HL7Util()
        val profileIdentifier = hl7Util.getProfileIdentifier(hl7TestMessage)

        when ( profileIdentifier ) {
            GENV1 -> {
                // TODO:
                context.logger.info("message profile identifier GENV1: --> " + profileIdentifier) 

            } // GENV1

            GENV2 -> {
                context.logger.info("message profile identifier GENV2: --> " + profileIdentifier) 
                val genV2Mmg = MmgUtil().getGenV2()

                // get event code:
                val eventCode = hl7Util.getEventCode(hl7TestMessage)

                when ( eventCode ) {
                    HL7Error.EVENT_CODE_ERROR.message -> {
                        context.logger.warning("message event code missing: --> " + eventCode) 
                        // TODO: send to Error event hub
                    }
                    else -> {


                        val conditionMmgName = EventCodeUtil().getMmgName(eventCode) 

                        when( conditionMmgName ) {
                            HL7Error.EVENT_CODE_NOT_SUPPORTED_ERROR.message -> {
                                context.logger.warning("message event code not supported: --> " + eventCode) 
                                // TODO: send to Error event hub
                            }
                            else -> {
                                // Finally GenV2 and Condition Mmg:
                                val conditionMmg = MmgUtil().getMmg(conditionMmgName) 

                                context.logger.info("genV2Mmg BLOCKS: --> " + genV2Mmg.result.blocks.size)
                                context.logger.info("conditionMmg BLOCKS: --> " + conditionMmg.result.blocks.size)
                            }
                        }

                    } // .else
                } // .when( eventCode )

            } // .GENV2

            else -> {
                // unknown profile identifier, send to error event hub
                // TODO:
                context.logger.warning("message profile identifie missing: --> " + profileIdentifier) 
            } // .else
        } // .when( profilIdenfier )


        // TODO:...
        // // 
        // // Event Hub -> receive events
        // // -------------------------------------
        // val eventArr = Gson().fromJson(message, Array<HL7Message>::class.java)
       
        // // 
        // // For each Event received
        // // -------------------------------------
        // for (event in eventArr) { 

        //     context.logger.info("event received: --> " + event)

        // } // .for 

    } // .eventHubProcessor

} // .Function

