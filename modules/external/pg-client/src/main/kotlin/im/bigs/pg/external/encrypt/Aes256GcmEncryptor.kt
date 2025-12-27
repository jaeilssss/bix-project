package im.bigs.pg.external.encrypt

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Aes256GcmEncryptor {

    private const val AES_KEY_SIZE = 32
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(plainText: String, secretKey: ByteArray): String {
        require(secretKey.size == AES_KEY_SIZE) {
            "secretKey의 사이즈가 32바이트가 아닙니다."
        }

//        val iv = ByteArray(GCM_IV_LENGTH)
//        SecureRandom().nextBytes(iv)
        val iv = ByteArray(GCM_IV_LENGTH)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(secretKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val cipherTextWithTag = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
//
//        val result = ByteBuffer.allocate(iv.size + cipherTextWithTag.size)
//            .put(cipherTextWithTag)
//            .array()

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(cipherTextWithTag)
    }
}