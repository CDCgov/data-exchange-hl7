package cdc.xlr.structurevalidator

import hl7.v2.validation.vs.{ValueSetLibrary, ValueSetLibraryImpl}
import hl7.v2.validation.content.Constraint
import hl7.v2.validation.content.{ConformanceContext, DefaultConformanceContext}
import hl7.v2.profile.{Profile, XMLDeserializer}

import java.io.{BufferedInputStream, FileInputStream}
import scala.io.Source

class ProfileLoaderLocal extends ProfileLoader {

    // local profiles location in src/main/resources
    val constraintsFileLoc = "/Constraints.xml"
    val profileFileLoc = "/Profile.xml"
    val valueSetsFileLoc = "/ValueSets.xml"

    def conformanceContext(): ConformanceContext= {
        val buff: BufferedInputStream = readFileToBufStr(constraintsFileLoc)
        DefaultConformanceContext.apply(buff).get
    } // .constraints
    

    def profile(): Profile = {
        val buff: BufferedInputStream = readFileToBufStr(profileFileLoc)
        XMLDeserializer.deserialize(buff).get
    } // .profile


    def valueSets(): ValueSetLibrary = {
        val buff: BufferedInputStream = readFileToBufStr(valueSetsFileLoc)
        ValueSetLibraryImpl.apply(buff).get
    } // .valueSets
    

    def readFileToBufStr(fileLocation:String):BufferedInputStream = {
        new BufferedInputStream( getClass.getResourceAsStream( fileLocation ) )
    } // .read

} // .ProfileLoaderLocal
