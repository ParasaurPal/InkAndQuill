package com.parasaurpal.inkandquill.data

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CloudSyncManager {
    private const val PREFS = "ink_quill_cloud_sync"
    private const val ALIAS = "ink_quill_sync_password_key"
    private const val WORK = "ink_quill_scheduled_encrypted_backup"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    private fun protect(password: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    internal fun unprotect(context: Context): String {
        val encoded = prefs(context).getString("password", null)
            ?: throw IllegalStateException("Scheduled backup password is not configured.")
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 12) { "Stored sync credentials are invalid." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun configure(context: Context, uri: Uri, password: String) {
        require(password.length >= 10) { "Use a password with at least 10 characters." }
        prefs(context).edit().putString("uri", uri.toString())
            .putString("password", protect(password)).putBoolean("enabled", true).apply()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints).addTag(WORK).build()
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniqueWork(WORK + "_initial", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CloudBackupWorker>().setConstraints(constraints).build())
        manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun disable(context: Context) {
        prefs(context).edit().clear().apply()
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    fun isEnabled(context: Context) = prefs(context).getBoolean("enabled", false)
    internal fun target(context: Context): Uri =
        Uri.parse(prefs(context).getString("uri", null)
            ?: throw IllegalStateException("Choose a cloud backup destination first."))
}

class CloudBackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (!CloudSyncManager.isEnabled(applicationContext)) Result.success()
            else {
                val password = CloudSyncManager.unprotect(applicationContext)
                val uri = CloudSyncManager.target(applicationContext)
                val json = InkDatabase(applicationContext).exportJson()
                EncryptedCloudBackup.write(applicationContext.contentResolver, uri, json, password)
                Result.success()
            }
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: IllegalArgumentException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
