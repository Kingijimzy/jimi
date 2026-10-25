package com.jimi.app.prefs

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts provider API keys at rest with an AES key held by Android Keystore.
 * If encryption/decryption fails, callers receive no plaintext fallback.
 */
class ApiKeyVault(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("jimi_encrypted_keys", Context.MODE_PRIVATE)

    @Synchronized
    fun get(providerId: String): String {
        val packed = prefs.getString(keyName(providerId), null) ?: return ""
        return try {
            val parts = packed.split(':', limit = 2)
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    @Synchronized
    fun put(providerId: String, value: String): Boolean {
        return try {
            if (value.isBlank()) {
                prefs.edit().remove(keyName(providerId)).apply()
                true
            } else {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
                val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encrypted, Base64.NO_WRAP)
                prefs.edit().putString(keyName(providerId), packed).commit()
            }
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun remove(providerId: String) {
        prefs.edit().remove(keyName(providerId)).apply()
    }

    private fun keyName(providerId: String): String =
        "provider_" + providerId.lowercase().replace(Regex("[^a-z0-9_-]"), "")

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "jimi_provider_keys_aes_gcm_v1"
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
