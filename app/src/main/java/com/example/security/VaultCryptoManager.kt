package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class EncryptedEnvelope(
    val ciphertextBase64: String,
    val ivBase64: String,
    val saltBase64: String,
    val keyMode: String
)

object VaultCryptoManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "tabiguide_e2ee_vault_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_BITS = 256

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun derivePassphraseKey(passphrase: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: String, passphrase: String? = null): EncryptedEnvelope {
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        return if (!passphrase.isNullOrBlank()) {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val key = derivePassphraseKey(passphrase, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(plainBytes)
            EncryptedEnvelope(
                ciphertextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
                keyMode = "PASSPHRASE"
            )
        } else {
            val key = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(plainBytes)
            EncryptedEnvelope(
                ciphertextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                saltBase64 = "",
                keyMode = "KEYSTORE"
            )
        }
    }

    fun decrypt(envelope: EncryptedEnvelope, passphrase: String? = null): String {
        val cipherBytes = Base64.decode(envelope.ciphertextBase64, Base64.NO_WRAP)
        val ivBytes = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_BITS, ivBytes)

        val key = if (envelope.keyMode == "PASSPHRASE") {
            require(!passphrase.isNullOrBlank()) { "Passphrase required to decrypt this document." }
            val salt = Base64.decode(envelope.saltBase64, Base64.NO_WRAP)
            derivePassphraseKey(passphrase, salt)
        } else {
            getOrCreateKeystoreKey()
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}

/**
 * RFC 6238 TOTP (Time-based One-Time Password) helper for Multi-Factor Vault Protection.
 */
object TotpManager {
    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6

    fun generateSecretBase32(byteCount: Int = 20): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return encodeBase32(bytes)
    }

    fun generateCurrentCode(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        val key = decodeBase32(secretBase32)
        val counter = (timeMillis / 1000L) / TIME_STEP_SECONDS
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % (10.0.pow(CODE_DIGITS).toInt())
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    fun verifyCode(
        secretBase32: String,
        inputCode: String,
        timeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val clean = inputCode.trim()
        if (clean.length != CODE_DIGITS) return false
        for (window in -1L..1L) {
            val candidate = generateCurrentCode(
                secretBase32,
                timeMillis + window * TIME_STEP_SECONDS * 1000L
            )
            if (candidate == clean) return true
        }
        return false
    }

    fun secondsRemaining(timeMillis: Long = System.currentTimeMillis()): Int {
        val elapsed = ((timeMillis / 1000L) % TIME_STEP_SECONDS).toInt()
        return (TIME_STEP_SECONDS - elapsed).toInt()
    }

    private fun encodeBase32(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val idx = (buffer shr (bitsLeft - 5)) and 0x1F
                out.append(BASE32_CHARS[idx])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val idx = (buffer shl (5 - bitsLeft)) and 0x1F
            out.append(BASE32_CHARS[idx])
        }
        return out.toString()
    }

    private fun decodeBase32(input: String): ByteArray {
        val clean = input.uppercase().filter { it in BASE32_CHARS }
        val result = ArrayList<Byte>(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (c in clean) {
            val val5 = BASE32_CHARS.indexOf(c)
            buffer = (buffer shl 5) or val5
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return result.toByteArray()
    }
}
