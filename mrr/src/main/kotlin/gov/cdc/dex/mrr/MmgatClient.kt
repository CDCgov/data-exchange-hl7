package gov.cdc.dex.mrr

import com.google.gson.*
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.util.StringUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import javax.net.ssl.*

class MmgatClient {
    val GUIDANCE_STATUS_UAT = "useracceptancetesting"
    val GUIDANCE_STATUS_FINAL = "final"

    var MMG_AT_ROOT_URL = "https://mmgat.services.cdc.gov/api/guide/"

    private fun trustAllHosts() {
        try {

            /* Start of certificates fix */
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            // Create all-trusting host name verifier
            val allHostsValid = HostnameVerifier { hostname, session -> true }

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
            /* End of certificates fix*/
        } catch (e: Exception) {
            throw Exception("Error in trustAllHosts method: +${e.printStackTrace()}")
        }
    }

    fun getGuideAll(): String {
        try {
            trustAllHosts()
            val url = URL("${MMG_AT_ROOT_URL}all?type=0")
            return getContent(url)
        } catch (e: Exception) {
            println("exception:${e.printStackTrace()}")
            throw Exception("Error in getGuideAll method: +${e.message}")

        }
    }

    fun getGuideById(id: String): String {
        try {
            val url = URL("${MMG_AT_ROOT_URL}$id?includeGenV2=false")
            return getContent(url)
        } catch (e: Exception) {
            throw Exception("Error in getGuideById method: +${e.message}")
        }
    }

    fun getContent(url: URL): String {
        val sb = StringBuilder()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw RuntimeException("Failed : HTTP error code : " + conn.responseCode)
        }
        val br = BufferedReader(InputStreamReader((conn.inputStream)))
        var line: String?
        while ((br.readLine().also { line = it }) != null) {
            sb.append(line)
        }
        return sb.toString()
    }

    fun loadLegacyMmgat(redisProxy: RedisProxy) {
        val legacyMMGFolder = this::class.java.getResource("/legacy_mmgs")
        val dir = File(legacyMMGFolder.file)
        dir.walk().filter{ it.isFile }.forEach {
            val legacyContent = this::class.java.getResource("/legacy_mmgs/${it.name}").readText()
            val fileOutputJson = JsonParser.parseString(legacyContent)
            var filename = StringUtils.normalizeString(it.name)
            filename = "mmg:" + filename.substring(0, filename.lastIndexOf("."))
            try {
                var jedis = redisProxy.getJedisClient()
                if (jedis.exists(filename))
                    jedis.del(filename)
                jedis.set(filename, fileOutputJson.toString())
            } catch (e: Exception) {
                throw Exception("Problem in setting Legacy MMGATs to Redis:${e.printStackTrace()}")
            } finally {
                //jedis.close()
            }

        }
    }


    fun loadMMGAT(redisProxy: RedisProxy) {
        try {
            println("STARTING MMGATRead services")

            val mmgaGuide = this.getGuideAll().toString()

            val elem: JsonElement = JsonParser.parseString(mmgaGuide)
            //context.logger.info("Json Array size:" + elem.asJsonObject.getAsJsonArray("result").size())
            val mmgatJArray = elem.asJsonObject.getAsJsonArray("result")
            println("Json Array size:" + mmgatJArray.size())
            val gson = GsonBuilder().create()

            for (mmgatjson in mmgatJArray) {

                val mj = mmgatjson.asJsonObject
                //if (mj.get("guideStatus").asString.toLowerCase() in listOf(mmgaClient.GUIDANCE_STATUS_UAT, mmgaClient.GUIDANCE_STATUS_FINAL) )
                if (mj.get("guideStatus").asString
                        .equals(this.GUIDANCE_STATUS_UAT,true) || mj.get("guideStatus")
                        .asString.equals(this.GUIDANCE_STATUS_FINAL,true)
                ) {
                    val id = (mj.get("id").asString)
                    // context.logger.info("MMGAT id:$id")
                    val mGuide = this.getGuideById(id)
                    val melement = JsonParser.parseString(mGuide.toString())
                    val mresult = melement.asJsonObject.get("result")

                    mresult.asJsonObject.remove("testScenarios")
                    mresult.asJsonObject.remove("testCaseScenarioWorksheetColumns")
                    mresult.asJsonObject.remove("columns")
                    mresult.asJsonObject.remove("templates")
                    mresult.asJsonObject.remove("valueSets")

                    val key = "mmg:"+ StringUtils.normalizeString(mj.get("name").asString)
                    print("MMGAT name: $key")
                    if (redisProxy.getJedisClient().exists(key))
                        redisProxy.getJedisClient().del(key)
                    try {
                         var jedis = redisProxy.getJedisClient()
                        if(jedis.exists(key))
                            jedis.del(key)
                        jedis.set(key, gson.toJson(mresult))
                        println("...Done!")
                    } catch (e: Throwable) {
                        println("... ERRORED OUT")
                    }
                }
            }
        } catch (e: Exception) {
            println("Failure in MMGATREAD function : ${e.printStackTrace()} ")
            throw e
        }
        println("MMGATREAD Function executed at: " + LocalDateTime.now())

    }


}