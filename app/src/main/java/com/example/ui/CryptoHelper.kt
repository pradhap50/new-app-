package com.example.ui

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    // 16-byte key for 128-bit AES
    private val keyBytes = "MillSecretKey123".toByteArray(StandardCharsets.UTF_8)
    private val secretKeySpec = SecretKeySpec(keyBytes, ALGORITHM)

    /**
     * Encrypts plain text string with AES algorithm and outputs Base64 encoded string.
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText // Fallback to plain if fails
        }
    }

    /**
     * Decrypts Base64 AES cipher text to normal readable string.
     */
    fun decrypt(cipherText: String): String {
        return try {
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText // Fallback
        }
    }
}
