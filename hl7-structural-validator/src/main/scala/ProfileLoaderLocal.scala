package cdc.xlr.structurevalidator

import java.io.{BufferedInputStream, FileInputStream}
import scala.io.Source

class ProfileLoaderLocal extends ProfileLoader {

    val constraintsFileLoc = "/Constraints.xml"
    val profileFileLoc = "/Profile.xml"
    val valueSetsFileLoc = "/ValueSets.xml"


    def constraints(): BufferedInputStream = {
        readFileToBufStr(constraintsFileLoc)
    } // .constraints
    

    def profile(): BufferedInputStream = {
        readFileToBufStr(profileFileLoc)
    } // .profile


    def valueSets(): BufferedInputStream = {
        readFileToBufStr(valueSetsFileLoc)
    } // .valueSets
    

    def readFileToBufStr(fileLocation:String):BufferedInputStream = {

        new BufferedInputStream( getClass.getResourceAsStream( fileLocation ) )

    } // .read


} // .ProfileLoaderLocal