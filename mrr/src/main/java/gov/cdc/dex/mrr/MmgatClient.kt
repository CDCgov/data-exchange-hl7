package gov.cdc.dex.mrr

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MmgatClient {
    var url: URL? = null
    var conn : HttpURLConnection ? = null
    val guidanceStatusUAT = "UserAcceptanceTesting"
    val guidanceStatusFINAL = "Final"

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
        } catch(e:Exception){
           println(e.printStackTrace())
        }
    }

 public  fun getGuideAll() :StringBuilder {
     val sb = StringBuilder()
     try {
         trustAllHosts()
          url = URL("https://mmgat.services.cdc.gov/api/guide/all?type=0")
          conn = url!!.openConnection() as HttpURLConnection

         conn!!.requestMethod = "GET"
         conn!!.setRequestProperty("Accept", "application/json")


         if (conn!!.responseCode != 200) {
             throw RuntimeException(
                 "Failed : HTTP error code : " + conn!!.responseCode)
         }
         val br = BufferedReader(
             InputStreamReader(conn!!.inputStream)
         )

         var line: String = ""
         while (br.readLine().also { line = it } != null) {
             // System.out.println(line );
             sb.append(line)
         }

         conn!!.disconnect()

     }catch(e:Exception){

     }
     return sb
 }

    public fun getGuideById(id:String): StringBuilder{
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
        } catch(e:Exception){

        }
       return sb
    }
}