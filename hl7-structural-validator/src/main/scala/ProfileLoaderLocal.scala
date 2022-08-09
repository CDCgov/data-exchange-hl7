package cdc.xlr.structurevalidator

import hl7.v2.validation.vs.{ValueSetLibrary, ValueSetLibraryImpl}
import hl7.v2.validation.content.Constraint
import hl7.v2.validation.content.{ConformanceContext, DefaultConformanceContext}
import hl7.v2.profile.{Profile, XMLDeserializer}

import java.io.{BufferedInputStream, FileInputStream}
import scala.io.Source

class ProfileLoaderLocal(constraintsFileLoc: String,
                        profileFileLoc: String,
                        valueSetsFileLoc: String) extends ProfileLoader {

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
    } // .reads

} // .ProfileLoaderLocal


object ProfileLoaderLocal {
    
  def apply() = new ProfileLoaderLocal(
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_CONSTRAINTS_DEFAULT_FILE_NAME,
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_PROFILE_DEFAULT_FILE_NAME,
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_VALUESETS_DEFAULT_FILE_NAME)

                        
  def apply(profilesSpecName: String) = {

    profilesSpecName match {

        case PROFILES_PHIN_SPEC_3_1 =>  new ProfileLoaderLocal(
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_CONSTRAINTS_DEFAULT_FILE_NAME,
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_PROFILE_DEFAULT_FILE_NAME,
                        PROFILES_LOCAL_PHIN_SPEC_3_1_FOLDER + PROFILES_VALUESETS_DEFAULT_FILE_NAME)

        case _ => throw new Exception("Profiles for this specification are not available")

    } // .match
  } // .apply


  def apply(profilesLocation: String, 
            constraintsFileName: String, 
            profileFileName: String,
            valueSetsFileName: String) = new ProfileLoaderLocal(
                                                profilesLocation + constraintsFileName,
                                                profilesLocation + profileFileName,
                                                profilesLocation + valueSetsFileName)

} // .ProfileLoaderLocal