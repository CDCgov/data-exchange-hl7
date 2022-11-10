package gov.cdc.dex.mrr

import com.google.gson.Gson
import com.google.gson.JsonObject
import gov.cdc.dex.util.StringUtils
import java.io.BufferedReader
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
    val GUIDANCE_STATUS_UAT  = "UserAcceptanceTesting"
    val GUIDANCE_STATUS_FINAL  = "Final"

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
            println(e.printStackTrace())
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
                // System.out.println(line );
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
            url = URL("https://mmgat.services.cdc.gov/api/guide/$id?includeGenV2=true")
            conn = url!!.openConnection() as HttpURLConnection
            conn!!.requestMethod = "GET"
            conn!!.setRequestProperty("Accept", "application/json")


            if (conn!!.responseCode != 200) {
                throw RuntimeException("Failed : HTTP error code : " + conn!!.responseCode)
            }

            val br = BufferedReader(InputStreamReader((conn!!.inputStream)))


            var line: String?

            // System.out.println("Output from Server mmgat1.... \n");

            // System.out.println("Output from Server mmgat1.... \n");
            while ((br.readLine().also { line = it }) != null) {
                sb.append(line)
            }
        } catch (e: Exception) {
            throw Exception("Error in getGuideById method: +${e.printStackTrace()}")
        }
        return sb
    }

    fun loadLegacyMmgat() {
        val url = Thread.currentThread().contextClassLoader.getResource("legacy_mmgs")
        if (url != null) {
            if (url.protocol == "jar") {
                val dirname: String = "legacy_mmgs" + "/"
                val path = url.path
                val jarPath = path.substring(5, path.indexOf("!"))
                JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name())).use { jar ->
                    val entries: Enumeration<JarEntry> = jar.entries()
                    while (entries.hasMoreElements()) {
                        val entry: JarEntry = entries.nextElement()
                        val name: String = entry.name

                        if (name.startsWith(dirname) && dirname != name) {
                            val resource = Thread.currentThread().contextClassLoader.getResource(name)

                            val instream: InputStream = jar.getInputStream(entry)
                            val inputReader = InputStreamReader(instream)
                            val fileOutputJson = Gson().fromJson(inputReader, JsonObject::class.java)
                            var filename = StringUtils.normalizeString(
                                resource.toString().substring(resource.toString().lastIndexOf("/") + 1)
                            )
                            filename = "mmg:" + filename.substring(0,filename.lastIndexOf("."))
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
                    }
                }
            }
        }


    }
}