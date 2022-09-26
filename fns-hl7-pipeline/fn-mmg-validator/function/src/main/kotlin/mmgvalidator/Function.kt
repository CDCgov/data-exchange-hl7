package com.example

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.azure.messaging.eventhubs.*

import java.util.UUID
import java.io.*

import com.google.gson.*
// import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

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

        // read the local MMGs
        val mmgGenV2Json = this::class.java.getResource("/genV2.json").readText()
        val mmgTbrdJson = this::class.java.getResource("/tbrd.json").readText()

        // val mapper = jacksonObjectMapper()
        val mmgGenV2 =  Gson().fromJson(mmgGenV2Json, MMG::class.java)//  mapper.readValue(mmgGenV2Json, MMG::class.java)
        val mmgTbrd = Gson().fromJson(mmgTbrdJson, MMG::class.java) // mapper.readValue(mmgTbrdJson, MMG::class.java) 

        val mmgFullBlocks = mmgGenV2.result.blocks + mmgTbrd.result.blocks

        // val validator = MMGValidator()
        // val report= validator.validate(msg, mmgFromJson)
        // context.logger.info("TYPE: --> " + mmgGenV2::class.java.typeName)
        context.logger.info("mmgGenV2Blocks BLOCKS: --> " + mmgGenV2.result.blocks.size)
        context.logger.info("mmgTbrdBlocks BLOCKS: --> " + mmgTbrd.result.blocks.size)

        context.logger.info("mmgFullBlocks BLOCKS: --> " + mmgFullBlocks.size)

        context.logger.info("BLOCKS: --> " + mmgFullBlocks)


        val hl7TestMessage = this::class.java.getResource("/testMessage.hl7").readText()

        val eventCode = HL7Util().getEventCode(hl7TestMessage)
        val profileIdentifier = HL7Util().getProfileIdentifier(hl7TestMessage)
        
        context.logger.info("event code: --> " + eventCode)
        context.logger.info("profile identifier: --> " + profileIdentifier)
        // push to JSON



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

