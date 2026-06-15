package com.example.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SecurityHelper {
    private const val PREFS_NAME = "secure_hand_cricket_prefs"
    private const val ALGORITHM = "AES"
    
    private val KEY_PHRASE = byteArrayOf(
        0x48, 0x61, 0x6e, 0x64, 0x43, 0x72, 0x69, 0x63, 
        0x6b, 0x65, 0x74, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x4b, 0x65, 0x79
    ) // "HandCricketSecureKey"

    private fun getSecretKeySpec(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(KEY_PHRASE)
        return SecretKeySpec(keyBytes.copyOfRange(0, 16), ALGORITHM)
    }

    fun encrypt(clearText: String): String {
        if (clearText.isEmpty()) return ""
        return try {
            val keySpec = getSecretKeySpec()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encryptedBytes = cipher.doFinal(clearText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            clearText // Fallback on error
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = getSecretKeySpec()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback on error
        }
    }

    fun secureSave(context: Context, key: String, value: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedValue = encrypt(value)
        sharedPrefs.edit().putString(key, encryptedValue).apply()
    }

    fun secureGet(context: Context, key: String, defaultValue: String = ""): String {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedValue = sharedPrefs.getString(key, null) ?: return defaultValue
        return decrypt(encryptedValue)
    }
    
    fun clearSecureCache(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }
}
