package im.bigs.pg.external

import java.security.MessageDigest

object PgKeyDeriver {

    fun deriveAes256Key(apiKey: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(apiKey.toByteArray(Charsets.UTF_8))
    }
}
