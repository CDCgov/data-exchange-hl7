package gov.cdc.dataexchange.entModel

trait StructureValidator {
    def validateStructure(message: Message): Message
    def isValidStructure(message: Message): Option[Message]
} // .StructureValidator

trait ContentValidator {
    def validateContent(message: Message): Message
    def isValidContent(message: Message): Option[Message]
} // .StructureValidator
 
trait Transformer {
    def transformToSegmLake(message: Message): Message
    def transformToEntModel(message: Message): Message
} // .StructureValidator

abstract class Message extends MessageSimple
                                with StructureValidator
                                with ContentValidator
                                with Transformer {
} // .Message