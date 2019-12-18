package utils

import io.jsonwebtoken.Jwts
import okhttp3.Headers
import secrets.AppStoreSecrets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES
import javax.xml.bind.DatatypeConverter

object AppStoreApiRequester {

    suspend inline fun <reified T> get(url: String): T {
        return ApiRequester.request("GET", url, createHeaders())
    }

    fun createHeaders(): Headers {
        return Headers.of(mapOf("Authorization" to "Bearer ${getJwtToken()}"))
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

    private fun loadPrivateKey(): PrivateKey {
//        val f = File(PRIVATE_KEY_FILE_PATH)
//        println(f.canonicalFile)
//        println(f.absolutePath)
//        val fis = FileInputStream(f)
//        val dis = DataInputStream(fis)
//        val keyBytes = ByteArray(f.length().toInt())
//        dis.readFully(keyBytes)
//        dis.close()

//        val temp = String(keyBytes)
        val temp = AppStoreSecrets.JWT_PRIVATE_KEY
        var privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----", "")
        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "")
        //System.out.println("Private key\n"+privKeyPEM);

        val spec = PKCS8EncodedKeySpec(DatatypeConverter.parseBase64Binary(privKeyPEM))
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }
}