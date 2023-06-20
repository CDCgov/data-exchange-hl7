package gov.cdc.dex.mrr

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.Condition2MMGMapping
import gov.cdc.dex.redisModels.Profile
import gov.cdc.dex.redisModels.SpecialCase
import gov.cdc.dex.util.StringUtils.Companion.normalize
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.system.measureTimeMillis

/*
    Class containing Azure functions that load records from CSV into redis.
    - loadGroups loads records as key : set pairs.
    Data in CSV maps group name to a space-delimited set of members.

    - loadEventMaps loads records as key : json (string) pairs
    Data in CSV maps NND event codes to MMGs, including special cases.
    Data in columns 4 through 8 are pipe-delimited when there are multiple profiles.
    Data in columns 7 and 8 are further delimited by ^ when there are multiple special cases per profile.
    Columns:
          0 Category
          1 Program
          2 Condition Code
          3 Condition Name
          4 Profiles
          5 Profile Has Special Case (yes/no)
          6 MMGs	(list, delimited with ;)
          7 Special Case Group
          8 Special Case MMGs (list, delimited with ;)
 */
class EventCodeClient {
    private val CONDITION_NAMESPACE = "condition:"
    private val GROUP_NAMESPACE = "group:"
    private val MMG_NAMESPACE = "mmg:"
    private val logger = LogManager.getLogger()
    //TODO: Configure logging level via environment variables (currently using xml config file)
    fun loadGroups(redisProxy: RedisProxy) {
        // for each csv in resources/groups:
        // load csv file
        // get the group key and members
        // insert into redis as a set
        val dir = ClientUtils.getResourcePath("groups")
        logger.debug("Found directory ${dir.pathString}")
        Files.walk(dir).filter{ it.isRegularFile() && it.extension.lowercase() == "csv"}.forEach {
            f -> run {
                logger.debug("filename: $f")
                val parser = CSVParser.parse(
                    f, Charsets.UTF_8, CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                )

                for (row: CSVRecord in parser) {
                    val groupName = "$GROUP_NAMESPACE${row.get("Group Name").trim()}"
                    val members = row.get("Jurisdiction Codes").split(" ")
                    // remove group if it already exists
                    redisProxy.getJedisClient().del(groupName)
                    for (member in members) {
                        // if member is already in group, this will do nothing
                        redisProxy.getJedisClient().sadd(groupName, member)
                    }
                }
                parser.close()
            } //.run
        } //.foreach

    }

    fun loadEventMaps(redisProxy: RedisProxy) {
        // for each csv file in resources/event_codes:
        // load csv file
        // for each row in csv file:
        // load row as new data object
        // save data object as JSON string
        // put the record into redis
        val timeInMillis = measureTimeMillis {
            val gson = Gson()
            println("PING Redis: ${redisProxy.getJedisClient().ping()}")
            val dir = ClientUtils.getResourcePath("event_codes")
            logger.debug("Found directory ${dir.pathString}")
            val pipeline = redisProxy.getJedisClient().pipelined()
            Files.walk(dir).filter{ it.isRegularFile() && it.extension.lowercase() == "csv"}.forEach {
                f -> run {
                    logger.debug("filename: $f")
                    val parser = CSVParser.parse(
                        f, Charsets.UTF_8, CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                    )

                    for (row: CSVRecord in parser) {
                        val conditionCode = row.get("Condition Code").trim()
                        val profilesString: String = row.get("Profiles").trim()
                        var profileObjList: MutableList<Profile>? = null
                        // condition may not have a profile, but we still need a record for provisioning.
                        if (profilesString.isNotEmpty()) {
                            profileObjList = mutableListOf()
                            val profileList: List<String> = profilesString.trim().split("|")
                            val specialCaseIndicators: List<String> =
                                row.get("Profile Has Special Case").trim().split("|")
                            val profileMmgLists: List<String> = row.get("MMGs").trim().split("|")
                            val specialCaseGroups: List<String> =
                                row.get("Special Case Group").trim().split("|")
                            val specialCaseMmgLists: List<String> =
                                row.get("Special Case MMGs").trim().split("|")
                            // for each profile in the profile list, build the list of profile objects
                            for ((profileIdx, profile: String) in profileList.withIndex()) {
                                val profileName = profile.trim().normalize()
                                val mmgList: List<String> = profileMmgLists[profileIdx].trim().split(";")
                                val normMmgList = mmgList.map { "$MMG_NAMESPACE${it.normalize()}"}
                                val hasSpecial: String = specialCaseIndicators[profileIdx].trim()
                                var specialCaseObjList: MutableList<SpecialCase>? = null
                                if (hasSpecial == "Yes") {
                                    specialCaseObjList = mutableListOf()
                                    val groups: List<String> = specialCaseGroups[profileIdx].trim().split("^")
                                    val specialMmgsByGroup: List<String> =
                                        specialCaseMmgLists[profileIdx].trim().split("^")
                                    for ((groupIdx, group) in groups.withIndex()) {
                                        val specialMmgList: List<String> =
                                            specialMmgsByGroup[groupIdx].trim().split(";")
                                        val normSpecialMmgList = specialMmgList.map { "$MMG_NAMESPACE${it.normalize()}" }
                                        specialCaseObjList.add(
                                            SpecialCase(
                                                appliesTo = "$GROUP_NAMESPACE$group",
                                                mmgs = normSpecialMmgList
                                            )
                                        )
                                    }

                                }
                                profileObjList.add(
                                    Profile(
                                        name = profileName,
                                        mmgs = normMmgList,
                                        specialCases = specialCaseObjList
                                    )
                                )
                            } //.for profile
                        } // .if
                        // add everything to the event record and save as JSON
                        val eventRecord = Condition2MMGMapping(
                            eventCode = conditionCode.toLong(),
                            name = row.get("Condition Name").trim(),
                            program = row.get("Program").trim(),
                            category = row.get("Category").trim(),
                            profiles = profileObjList
                        )
                        val eventString = gson.toJson(eventRecord)
                        // Add record to redis pipeline
                        logger.debug("Adding key $CONDITION_NAMESPACE$conditionCode to REDIS")
                        pipeline.set("$CONDITION_NAMESPACE$conditionCode", eventString)
                    } //.for row
                    // insert all the rows into redis
                    pipeline.sync()
                    parser.close()
                } //.run for this f
            } //.walk
            pipeline.close()
        }
        logger.info("Loading Event Maps took $timeInMillis ms.")

    }


}