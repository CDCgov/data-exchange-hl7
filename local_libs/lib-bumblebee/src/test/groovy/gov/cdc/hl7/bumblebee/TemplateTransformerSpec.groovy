package gov.cdc.hl7.bumblebee

import gov.cdc.hl7.bumblebee.TemplateTransformer
import groovy.json.JsonSlurper

import spock.lang.*
/**
 *
 *
 * @Created - 4/11/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
class TemplateTransformerSpec extends Specification {

    def "initializing with resource files"() {
        when:
          def bumblebee = TemplateTransformer.getTransformerWithResource("/exampleTemplate.json", "/BasicProfile.json")
        then:
          bumblebee != null
    }

    def "initializing with content"() {
        when:
            def template = getClass().getResource('/exampleTemplate.json').text

            def profile = getClass().getResource('/BasicProfile.json').text
            def bumblebee = TemplateTransformer.getTransformerWithContent(template, profile)

        then:
            bumblebee != null
    }

    def "transforming example template"() {
        when:
          def bumblebee = TemplateTransformer.getTransformerWithResource("/exampleTemplate.json", "/BasicProfile.json")
          def message = getClass().getResource("/exampleHL7Message.txt").text
          def newMessage = bumblebee.transformMessage(message, "DONT")

          print(newMessage)
        then:
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(newMessage)

            assert object instanceof Map
            assert object.specimen_id == '214MP000912'
            assert object.message_profile == "PHLabReport-NoAck"
            assert object.message_controller_id == "ARLN_GC_DupASTmOBR_ELR"
            assert object.meta_organization == "MD.BALTIMORE.SPHL"
            assert object.phl == "2.16.840.1.114222.4.1.95290"
            assert object.meta_program == "CDC.ARLN.GC"
            assert object.message_status == "F"
            assert object.race[0] == "Native Hawaiian"
            assert object.race[1] == "White"
            assert object.OBR[1].Group == "33"
            assert object.OBR[1].type == "SN"
            assert object.OBR[1].question.code == "35659-2"
            assert object.OBR[1].question.label == "Age at specimen collection"
            assert object.OBR[1].answer == "^7"
//            assert object.emptyValue == ""
    }

    def "transforming single template"() {
        when:
        def bumblebee = TemplateTransformer.getTransformerWithResource("/simpleTemplate.json", "/BasicProfile.json")
        def message = getClass().getResource("/exampleHL7Message.txt").text
        def newMessage = bumblebee.transformMessage(message, "DONT")

        print(newMessage)
        then:
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(newMessage)
//        print(object)
//            assert object.emptyValue == ""
    }

    def "test flattening dhqp"() {
        when:
        def bumblebee = TemplateTransformer.getTransformerWithResource("/dhqpFlat.json", "/BasicProfile.json")
        def message = getClass().getResource("/exampleConcatNotes.txt").text
        def newMessage = bumblebee.transformMessage(message, "\n")

        print(newMessage)
        then:
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(newMessage)
    }

    def "transforming example no matches"() {
        when:
        def bumblebee = TemplateTransformer.getTransformerWithResource("/exampleTemplateNoMatch.json", "/BasicProfile.json")
        def message = getClass().getResource("/exampleHL7Message.txt").text
        def newMessage = bumblebee.transformMessage(message, "DONT")

        print(newMessage)
        then:
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(newMessage)

        assert object instanceof Map
        assert object.specimen_id == null

    }

    def "transforming covid test"() {
        when:
        def bumblebee = TemplateTransformer.getTransformerWithResource("/covidExample.json", "/BasicProfile.json")
        def message = getClass().getResource("/testMessage.txt").text
        def newMessage = bumblebee.transformMessage(message, "DONT")

        print(newMessage)
        then:
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(newMessage)
    }


}
