package cdc.xlr.structurevalidator

import hl7.v2.validation.vs.ValueSetLibrary
import hl7.v2.validation.content.ConformanceContext
import hl7.v2.profile.Profile

trait ProfileLoader {

    def profile(): Profile
    def valueSets(): ValueSetLibrary
    def conformanceContext(): ConformanceContext

} // .ProfileLoader