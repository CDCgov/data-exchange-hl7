package gov.cdc.dataexchange.entModel

case class Blocks (
  id: String,
  guideId: String,
  ordinal: Int,
  blockType: String,
  name: String,
  // startingDescription: String,
  // endingDescription: String,
  // shouldDisplayName: Boolean,
  elements: Seq[Elements]
)

// case class Concepts (
//   valueSetConceptId: String,
//   codeSystemConceptName: String,
//   valueSetConceptStatusCode: String,
//   valueSetConceptStatusDate: String,
//   valueSetConceptDefinitionText: String,
//   cdcPreferredDesignation: String,
//   scopeNoteText: String,
//   valueSetVersionId: String,
//   codeSystemOid: String,
//   conceptCode: String,
//   hL70396Identifier: String
// )

// case class DefaultValue (
//   value: String
// )

case class Elements (
  id: String,
  guideId: String,
  guideInternalVersion: Int,
  blockId: String,
  ordinal: Int,
  name: String,
  // description: String,
  category: Option[String],
  comments: String,
  status: String,
  dataType: String,
  businessRules: String,
  isUnitOfMeasure: Boolean,
  codeSystem: String,
  legacyPriority: String,
  priority: String,
  isRepeat: Boolean,
  mayRepeat: String,
  valueSetCode: Option[String],
  mappings: Mappings,
  // defaultValue: DefaultValue,
  valueSetVersionNumber: Option[Int],
  // relatedElementId: Option[String],
  // legacyCodeSystem: Option[String]
)

case class Hl7V251 (
  legacyIdentifier: String,
  identifier: String,
  // messageContext: String,
  dataType: String,
  segmentType: String,
  obrPosition: Int,
  fieldPosition: Int,
  componentPosition: Int,
  usage: String,
  cardinality: String,
  // literalFieldValues: Any,
  repeatingGroupElementType: String,
  // implementationNotes: String,
  // sampleSegment: String
)

case class Mappings (
  hl7v251: Hl7V251
)

case class MmgRoot (
  id: String,
  mmgType: String,
  guideStatus: Option[String],
  templateStatus: Option[String],
  name: String,
  shortName: String,
  // description: String,
  isActive: Boolean,
  createdBy: String,
  ownedBy: String,
  internalVersion: Int,
  createdDate: String,
  lastUpdatedDate: String,
  publishVersion: String,
  profileIdentifier: String,
  blocks: Seq[Blocks],
  // testScenarios: Seq[String],
  // testCaseScenarioWorksheetColumns: Seq[TestCaseScenarioWorksheetColumns],
  // columns: Seq[TestCaseScenarioWorksheetColumns],
  // valueSets: Seq[ValueSets]
) 

// case class TestCaseScenarioWorksheetColumns (
//   label: String,
//   path: String
// )

// case class ValueSet (
//   valueSetId: String,
//   valueSetOid: String,
//   valueSetName: String,
//   valueSetCode: String,
//   status: String,
//   statusDate: String,
//   definitionText: String,
//   scopeNoteText: String,
//   assigningAuthorityId: String,
//   legacyFlag: String
// )

// case class ValueSetVersion (
//   valueSetVersionId: String,
//   valueSetVersionNumber: Int,
//   valueSetVersionDescriptionText: String,
//   statusCode: String,
//   statusDate: String,
//   assigningAuthorityVersionText: String,
//   noteText: String,
//   effectiveDate: String,
//   valueSetOid: String
// )

// case class ValueSets (
//   valueSet: ValueSet,
//   valueSetVersion: ValueSetVersion,
//   conceptsCount: Int,
//   concepts: Seq[Concepts]
// )

import spray.json._
import DefaultJsonProtocol._ 

trait MmgJsonProtocol extends DefaultJsonProtocol {

  implicit val hl7V251Format: JsonFormat[Hl7V251] = jsonFormat10(Hl7V251)
  implicit val mappingsFormat: JsonFormat[Mappings] = jsonFormat1(Mappings)
  implicit val elementsFormat: JsonFormat[Elements] = jsonFormat20(Elements)

  implicit val blocksFormat: JsonFormat[Blocks] = jsonFormat6(Blocks)
  implicit val mmgRootFormat: JsonFormat[MmgRoot] = jsonFormat15(MmgRoot)

  // implicit val conceptsFormat: JsonFormat[Concepts] = jsonFormat11(Concepts)
  // implicit val defaultValueFormat: JsonFormat[DefaultValue] = jsonFormat1(DefaultValue)
  // implicit val testCaseScenarioWorksheetColumnsFormat: JsonFormat[TestCaseScenarioWorksheetColumns] = jsonFormat2(TestCaseScenarioWorksheetColumns)
  // implicit val valueSetFormat: JsonFormat[ValueSet] = jsonFormat10(ValueSet)
  // implicit val valueSetVersionFormat: JsonFormat[ValueSetVersion] = jsonFormat9(ValueSetVersion)
  // implicit val valueSetsFormat: JsonFormat[ValueSets] = jsonFormat4(ValueSets)

} // .MmgJsonProtocol