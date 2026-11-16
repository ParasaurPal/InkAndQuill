package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedCloudBackupTest {
    private val password = "correct horse battery staple"
    private val sample = """{"format":"inkandquill","version":4,"records":[{"title":"Café 🌙"}]}"""

    @Test
    fun encryptedBackupRoundTripsUnicodeJson() {
        val encrypted = EncryptedCloudBackup.encrypt(sample, password)
        assertEquals(sample, EncryptedCloudBackup.decrypt(encrypted, password))
    }

    @Test
    fun encryptionUsesFreshSaltAndIvForEveryBackup() {
        val first = EncryptedCloudBackup.encrypt(sample, password)
        val second = EncryptedCloudBackup.encrypt(sample, password)
        assertFalse(first.contentEquals(second))
        assertEquals(sample, EncryptedCloudBackup.decrypt(first, password))
        assertEquals(sample, EncryptedCloudBackup.decrypt(second, password))
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongPasswordIsRejected() {
        EncryptedCloudBackup.decrypt(EncryptedCloudBackup.encrypt(sample, password), "different password")
    }

    @Test(expected = IllegalArgumentException::class)
    fun modifiedCiphertextIsRejected() {
        val encrypted = EncryptedCloudBackup.encrypt(sample, password)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 0x01).toByte()
        EncryptedCloudBackup.decrypt(encrypted, password)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedBackupHeaderIsRejected() {
        EncryptedCloudBackup.decrypt(ByteArray(80) { 0x41 }, password)
    }

    @Test(expected = IllegalArgumentException::class)
    fun weakPasswordIsRejected() {
        EncryptedCloudBackup.encrypt(sample, "short")
    }
}
