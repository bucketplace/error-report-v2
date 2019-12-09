package utils

import io.jsonwebtoken.Jwts
import io.ktor.util.InternalAPI
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import secrets.AppStoreSecrets
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES
import javax.xml.bind.DatatypeConverter

object AppStoreApiRequester {

        private const val PRIVATE_KEY_FILE_PATH = "/home/bsscco/error-report-v2/appstore-connect-api-private-key.p8"
//    private const val PRIVATE_KEY_FILE_PATH = "./appstore-connect-api-private-key.p8"

    fun get(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${getJwtToken()}")
            .build()
        return OkHttpClient().newCall(request).execute()
    }

    private fun getJwtToken(): String {
        return Jwts.builder()
            .setHeaderParam("alg", "ES256")
            .setHeaderParam("kid", AppStoreSecrets.JWT_PRIVATE_KEY_ID)
            .setHeaderParam("typ", "JWT")
            .setIssuer(AppStoreSecrets.JWT_ISSUER)
            .setAudience("appstoreconnect-v1")
            .setExpiration(Date(System.currentTimeMillis() + MINUTES.toMillis(1)))
            .signWith(loadPrivateKey())
            .compact()
    }

    @UseExperimental(InternalAPI::class)
    private fun loadPrivateKey(): PrivateKey {
        val f = File(PRIVATE_KEY_FILE_PATH)
        println(f.canonicalFile)
        println(f.absolutePath)
        val fis = FileInputStream(f)
        val dis = DataInputStream(fis)
        val keyBytes = ByteArray(f.length().toInt())
        dis.readFully(keyBytes)
        dis.close()

        val temp = String(keyBytes)
        var privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----", "")
        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "")
        //System.out.println("Private key\n"+privKeyPEM);

        val spec = PKCS8EncodedKeySpec(DatatypeConverter.parseBase64Binary(privKeyPEM))
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }
}