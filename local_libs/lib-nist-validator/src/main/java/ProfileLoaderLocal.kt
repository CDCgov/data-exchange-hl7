//package gov.cdc.nist.validator
//
//import hl7.v2.profile.Profile
//import hl7.v2.profile.XMLDeserializer
//import hl7.v2.validation.content.ConformanceContext
//import hl7.v2.validation.content.DefaultConformanceContext
//import hl7.v2.validation.vs.ValueSetLibrary
//import hl7.v2.validation.vs.ValueSetLibraryImpl
//import scala.collection.mutable.Seq
//import java.io.BufferedInputStream
//
//class ProfileLoaderLocal(
//    private val constraintsFileLoc: String,
//    private val profileFileLoc: String,
//    private val valueSetsFileLoc: String
//) : ProfileLoader {
//    override fun conformanceContext(): ConformanceContext {
//        val buff: BufferedInputStream = readFileToBufStr(constraintsFileLoc)
//        return DefaultConformanceContext(buff).get()
//    }
//
//    override fun profile(): Profile {
//        val buff: BufferedInputStream = readFileToBufStr(profileFileLoc)
//        return XMLDeserializer.deserialize(buff).get()
//    }
//
//    override fun valueSets(): ValueSetLibrary {
//        val buff: BufferedInputStream = readFileToBufStr(valueSetsFileLoc)
//        return ValueSetLibraryImpl(buff).get()
//    }
//
//    private fun readFileToBufStr(fileLocation: String): BufferedInputStream {
//        return BufferedInputStream(javaClass.getResourceAsStream(fileLocation))
//    }
//}
//
//object ProfileLoaderLocal {
//    fun apply(profilesSpecName: String): ProfileLoaderLocal {
//        return when (profilesSpecName) {
//            PROFILES_PHIN_SPEC_2_0 -> ProfileLoaderLocal(
//                "/" + PROFILES_PHIN_SPEC_2_0 + "/" + PROFILES_CONSTRAINTS_DEFAULT_FILE_NAME,
//                "/" + PROFILES_PHIN_SPEC_2_0 + "/" + PROFILES_PROFILE_DEFAULT_FILE_NAME,
//                "/" + PROFILES_PHIN_SPEC_2_0 + "/" + PROFILES_VALUESETS_DEFAULT_FILE_NAME
//            )
//            PROFILES_PHIN_SPEC_3_0 -> TODO()
//            else -> TODO()
//        }
//    }
//}