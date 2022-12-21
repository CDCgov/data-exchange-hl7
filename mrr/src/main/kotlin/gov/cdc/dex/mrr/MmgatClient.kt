package gov.cdc.dex.mrr

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.util.StringUtils
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import javax.net.ssl.*

class MmgatClient {
    var url: URL? = null
    var conn: HttpURLConnection? = null
    val GUIDANCE_STATUS_UAT = "useracceptancetesting"
    val GUIDANCE_STATUS_FINAL = "final"

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

    fun getGuideAll(): StringBuilder {
        val sb = StringBuilder()
        try {
            trustAllHosts()
            url = URL("https://mmgat.services.cdc.gov/api/guide/all?type=0")
            conn = url!!.openConnection() as HttpURLConnection

            conn!!.requestMethod = "GET"
            conn!!.setRequestProperty("Accept", "application/json")


            if (conn!!.responseCode != 200) {
                throw RuntimeException(
                    "Failed : HTTP error code : " + conn!!.responseCode
                )
            }
            val br = BufferedReader(
                InputStreamReader(conn!!.inputStream)
            )

            var line = ""

            while (br.readLine().also {
                    if (it != null) {
                        line = it
                    }
                } != null) {
                sb.append(line)
            }

            conn!!.disconnect()

        } catch (e: Exception) {
            println("exception:${e.printStackTrace()}")
            throw Exception("Error in getGuideAll method: +${e.printStackTrace()}")

        }
        return sb
    }

    fun getGuideById(id: String): StringBuilder {
        val sb = StringBuilder()
        try {
            url = URL("https://mmgat.services.cdc.gov/api/guide/$id?includeGenV2=false")
            conn = url!!.openConnection() as HttpURLConnection
            conn!!.requestMethod = "GET"
            conn!!.setRequestProperty("Accept", "application/json")


            if (conn!!.responseCode != 200) {
                throw RuntimeException("Failed : HTTP error code : " + conn!!.responseCode)
            }

            val br = BufferedReader(InputStreamReader((conn!!.inputStream)))


            var line: String?

            while ((br.readLine().also { line = it }) != null) {
                sb.append(line)
            }
        } catch (e: Exception) {
            throw Exception("Error in getGuideById method: +${e.printStackTrace()}")
        }
        return sb
    }

    fun loadLegacyMmgat() {
//        val url = Thread.currentThread().contextClassLoader.getResource("legacy_mmgs")
//        if (url != null) {
//            if (url.protocol == "jar") {
        //val dirname: String = "legacy_mmgs" + "/"
        // val path = url.path
        // val jarPath = path.substring(5, path.indexOf("!"))

        val url = this::class.java.getResource("/legacy_mmgs")
        val dir = File(url.file)
        dir.walk().forEach {
            val legacyContent = this::class.java.getResource("/legacy_mmgs/${it.name}").readText()
            //val fileOutputJson = Gson().fromJson(legacyContent, JsonObject::class.java)
            val fileOutputJson = JsonParser.parseString(legacyContent)
            var filename = StringUtils.normalizeString(it.name)
            filename = "mmg:" + filename.substring(0, filename.lastIndexOf("."))
            //println("MMGAT name2:$filename");
            RedisUtility().redisConnection().use { jedis ->
                try {
                    if (jedis.exists(filename))
                        jedis.del(filename)
                    jedis.set(filename, fileOutputJson.toString())
                } catch (e: Exception) {
                    throw Exception("Problem in setting Legacy MMGAT's to Redis:${e.printStackTrace()}")
                } finally {
                    jedis.close()
                }

            }
        }
//            }
//        }


    }
}