package com.parasaurpal.inkandquill.data

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-encrypted portable backup.
 * Encryption happens on-device before bytes are handed to the selected Android document provider.
 * The app never receives a provider's login credentials.
 */
object EncryptedCloudBackup {
    private val magic = "INKQBK01".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    fun encrypt(plainText: String, password: String): ByteArray {
        require(password.length >= 10) { "Use a backup password with at least 10 characters." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = derive(password.toCharArray(), salt)
        val encrypted = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        } finally {
            key.fill(0)
        }
        return ByteArrayOutputStream().apply {
            write(magic)
            write(salt)
            write(iv)
            write(encrypted)
        }.toByteArray()
    }

    fun decrypt(payload: ByteArray, password: String): String {
        require(password.isNotEmpty()) { "Enter the backup password." }
        require(payload.size > magic.size + SALT_BYTES + IV_BYTES) { "This backup file is incomplete." }
        require(payload.copyOfRange(0, magic.size).contentEquals(magic)) { "This is not an Ink & Quill encrypted backup." }
        var offset = magic.size
        val salt = payload.copyOfRange(offset, offset + SALT_BYTES); offset += SALT_BYTES
        val iv = payload.copyOfRange(offset, offset + IV_BYTES); offset += IV_BYTES
        val key = derive(password.toCharArray(), salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(payload.copyOfRange(offset, payload.size)).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("Unable to decrypt this backup. Check the password or file.")
        } finally {
            key.fill(0)
        }
    }

    fun write(resolver: ContentResolver, uri: Uri, json: String, password: String) {
        val encrypted = encrypt(json, password)
        resolver.openOutputStream(uri)?.use { it.write(encrypted) }
            ?: throw IllegalStateException("The selected backup location could not be opened.")
    }

    fun read(resolver: ContentResolver, uri: Uri, password: String): String {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("The selected backup file could not be opened.")
        return decrypt(bytes, password)
    }
}
