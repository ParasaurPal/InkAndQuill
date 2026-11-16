package com.parasaurpal.inkandquill.data

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
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

    private fun digest(value: String): String =
        Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    fun configure(context: Context, uri: Uri, password: String) {
        require(password.length >= 10) { "Use a password with at least 10 characters." }
        prefs(context).edit().putString("uri", uri.toString())
            .putString("password", protect(password)).putBoolean("enabled", true)
            .putBoolean("conflict", false).remove("last_hash").apply()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val manager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints).build()
        manager.enqueueUniqueWork(WORK + "_initial", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CloudBackupWorker>().setConstraints(constraints).build())
        manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun syncNow(context: Context) {val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();val request=OneTimeWorkRequestBuilder<CloudBackupWorker>().setConstraints(constraints).build();WorkManager.getInstance(context).enqueueUniqueWork(WORK+"_manual",ExistingWorkPolicy.REPLACE,request)}
    fun resolveConflict(context: Context, keepPhone: Boolean) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data = Data.Builder().putString("resolution", if (keepPhone) "local" else "cloud").build()
        val request = OneTimeWorkRequestBuilder<CloudBackupWorker>()
            .setConstraints(constraints).setInputData(data).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK + "_resolve", ExistingWorkPolicy.REPLACE, request)
    }

    fun disable(context: Context) {
        prefs(context).edit().clear().apply()
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(WORK)
        manager.cancelUniqueWork(WORK + "_initial")
        manager.cancelUniqueWork(WORK + "_resolve")
        manager.cancelUniqueWork(WORK + "_manual")
    }

    fun isEnabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun hasConflict(context: Context) = prefs(context).getBoolean("conflict", false)
    fun lastSyncStatus(context: Context) = prefs(context).getString("last_status", "Not synced yet") ?: "Not synced yet"
    fun lastSyncAt(context: Context) = prefs(context).getLong("last_sync_at", 0L)
    internal fun target(context: Context): Uri =
        Uri.parse(prefs(context).getString("uri", null)
            ?: throw IllegalStateException("Choose a cloud backup destination first."))

    internal fun sync(context: Context, db: InkDatabase, uri: Uri, password: String, resolution: String?): String {
        val preferences = prefs(context)
        fun finish(status:String):String { preferences.edit().putString("last_status",status).putLong("last_sync_at",System.currentTimeMillis()).apply();return status }
        if (resolution == "cloud") {
            val remote = EncryptedCloudBackup.read(context.contentResolver, uri, password)
            db.replaceWithJson(remote)
            preferences.edit().putString("last_hash", digest(remote)).putBoolean("conflict", false).apply()
            return finish("Downloaded cloud copy")
        }
        val localJson = db.exportJson()
        val localHash = digest(localJson)
        if (resolution == "local") {
            EncryptedCloudBackup.write(context.contentResolver, uri, localJson, password)
            preferences.edit().putString("last_hash", localHash).putBoolean("conflict", false).apply()
            return finish("Uploaded phone copy")
        }

        val remoteBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cloud provider could not open the backup destination.")
        if (remoteBytes.isEmpty()) {
            EncryptedCloudBackup.write(context.contentResolver, uri, localJson, password)
            preferences.edit().putString("last_hash", localHash).putBoolean("conflict", false).apply()
            return finish("Uploaded phone copy")
        }
        val remoteJson = EncryptedCloudBackup.decrypt(remoteBytes, password)
        val remoteHash = digest(remoteJson)
        val previousHash = preferences.getString("last_hash", null)

        if (remoteHash == localHash) {
            preferences.edit().putString("last_hash", localHash).putBoolean("conflict", false).apply()
            return finish("Already up to date")
        }
        if (previousHash == null) {
            preferences.edit().putBoolean("conflict", true).apply()
            return finish("Conflict needs your choice")
        }
        if (remoteHash == previousHash) {
            EncryptedCloudBackup.write(context.contentResolver, uri, localJson, password)
            preferences.edit().putString("last_hash", localHash).putBoolean("conflict", false).apply()
            return finish("Uploaded phone copy")
        }
        if (localHash == previousHash) {
            db.replaceWithJson(remoteJson)
            preferences.edit().putString("last_hash", remoteHash).putBoolean("conflict", false).apply()
            return finish("Downloaded cloud copy")
        }
        preferences.edit().putBoolean("conflict", true).apply()
        return finish("Conflict needs your choice")
    }
}

class CloudBackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (!CloudSyncManager.isEnabled(applicationContext)) Result.success()
            else {
                val password = CloudSyncManager.unprotect(applicationContext)
                val uri = CloudSyncManager.target(applicationContext)
                val db = InkDatabase(applicationContext)
                CloudSyncManager.sync(applicationContext, db, uri, password, inputData.getString("resolution"))
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
