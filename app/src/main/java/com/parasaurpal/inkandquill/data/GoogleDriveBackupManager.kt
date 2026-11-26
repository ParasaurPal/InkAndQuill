package com.parasaurpal.inkandquill.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.parasaurpal.inkandquill.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DriveBackupFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String,
    val sizeBytes: Long,
    val isEncrypted: Boolean
)

data class DriveBackupUiState(
    val connectedEmail: String = "",
    val hasSessionToken: Boolean = false,
    val lastBackupAt: Long = 0L,
    val lastStatus: String = "Not backed up to Google Drive yet",
    val scheduledEnabled: Boolean = false,
    val scheduledEncrypted: Boolean = false,
    val history: List<Pair<Long, String>> = emptyList()
)

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String, val email: String = "") : DriveAuthOutcome
    data class NeedsResolution(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Failed(val message: String) : DriveAuthOutcome
}

object GoogleDriveBackupManager {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val USERINFO_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
    private const val BACKUP_FOLDER_NAME = "Ink & Quill Backups"
    private const val AUTO_BACKUP_BASENAME = "InkAndQuill-AutoBackup"

    private const val PREFS = "ink_quill_google_drive_backup"
    private const val KEYSTORE_ALIAS = "ink_quill_drive_backup_key"
    private const val WORK_NAME = "ink_quill_google_drive_scheduled_backup"

    private val DRIVE_ID_REGEX = Regex("^[A-Za-z0-9_-]{8,128}$")
    private const val TOKEN_CACHE_TTL_MS = 45 * 60 * 1000L
    private const val MAX_BACKUP_DOWNLOAD_BYTES = 64 * 1024 * 1024

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedTokenAt: Long = 0L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun protectPassword(password: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    internal fun unprotectPassword(context: Context): String? {
        val encoded = prefs(context).getString("scheduled_password", null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        if (bytes.size <= 12) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun hasCachedToken(): Boolean {
        val token = cachedToken
        return !token.isNullOrBlank() && (System.currentTimeMillis() - cachedTokenAt) < TOKEN_CACHE_TTL_MS
    }

    fun currentAccessTokenOrNull(): String? = if (hasCachedToken()) cachedToken else null

    fun uiState(context: Context): DriveBackupUiState {
        val p = prefs(context)
        return DriveBackupUiState(
            connectedEmail = p.getString("account_email", "").orEmpty(),
            hasSessionToken = hasCachedToken(),
            lastBackupAt = p.getLong("last_backup_at", 0L),
            lastStatus = p.getString("last_status", "Not backed up to Google Drive yet")
                ?: "Not backed up to Google Drive yet",
            scheduledEnabled = p.getBoolean("scheduled_enabled", false),
            scheduledEncrypted = p.getBoolean("scheduled_encrypted", false),
            history = history(context)
        )
    }

    fun history(context: Context): List<Pair<Long, String>> {
        val raw = prefs(context).getString("backup_history", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val at = obj.optLong("at", 0L)
                if (at > 0L) at to obj.optString("status", "Drive backup") else null
            }.asReversed()
        }.getOrDefault(emptyList())
    }

    @Synchronized
    internal fun recordEvent(context: Context, status: String, markBackupSuccess: Boolean = false) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val arr = runCatching { JSONArray(p.getString("backup_history", "[]") ?: "[]") }
            .getOrDefault(JSONArray())
        arr.put(JSONObject().put("at", now).put("status", status))
        while (arr.length() > 25) arr.remove(0)
        p.edit().apply {
            putString("backup_history", arr.toString())
            putString("last_status", status)
            if (markBackupSuccess) putLong("last_backup_at", now)
            apply()
        }
    }

    private fun buildAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE), Scope(USERINFO_EMAIL_SCOPE)))
            .build()
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    suspend fun signInWithGoogle(activityContext: Context): String? {
        val clientId = activityContext.getString(R.string.google_oauth_client_id)
        if (clientId.isBlank()) return null
        return runCatching {
            val manager = CredentialManager.create(activityContext)
            val option = GetSignInWithGoogleOption.Builder(clientId)
                .setNonce(generateNonce())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = manager.getCredential(activityContext, request)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idCred = GoogleIdTokenCredential.createFrom(cred.data)
                val email = idCred.id
                if (email.isNotBlank()) {
                    prefs(activityContext).edit().putString("account_email", email).apply()
                }
                email
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun authorizeDrive(context: Context): DriveAuthOutcome {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .authorize(buildAuthorizationRequest())
                .awaitResult()
            val pending = result.pendingIntent
            if (result.hasResolution() && pending != null) {
                DriveAuthOutcome.NeedsResolution(pending)
            } else {
                val token = result.accessToken.orEmpty()
                if (token.isBlank()) {
                    DriveAuthOutcome.Failed("Google Drive authorization did not return an access token.")
                } else {
                    cachedToken = token
                    cachedTokenAt = System.currentTimeMillis()
                    val email = withContext(Dispatchers.IO) { fetchUserEmail(token) }
                    if (email.isNotBlank()) {
                        prefs(context).edit().putString("account_email", email).apply()
                    }
                    DriveAuthOutcome.Authorized(
                        accessToken = token,
                        email = email.ifBlank { prefs(context).getString("account_email", "").orEmpty() }
                    )
                }
            }
        } catch (e: ApiException) {
            DriveAuthOutcome.Failed(
                "Google authorization unavailable (code ${e.statusCode}). Check Google Play Services or use the Drive file picker below."
            )
        } catch (e: Exception) {
            DriveAuthOutcome.Failed(e.message ?: "Unable to authorize Google Drive.")
        }
    }

    suspend fun handleAuthorizationIntent(context: Context, data: Intent?): DriveAuthOutcome {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken.orEmpty()
            if (token.isBlank()) {
                DriveAuthOutcome.Failed("Google Drive permission was not granted.")
            } else {
                cachedToken = token
                cachedTokenAt = System.currentTimeMillis()
                val email = withContext(Dispatchers.IO) { fetchUserEmail(token) }
                if (email.isNotBlank()) {
                    prefs(context).edit().putString("account_email", email).apply()
                }
                DriveAuthOutcome.Authorized(
                    accessToken = token,
                    email = email.ifBlank { prefs(context).getString("account_email", "").orEmpty() }
                )
            }
        } catch (e: ApiException) {
            DriveAuthOutcome.Failed("Google Drive authorization cancelled or failed (code ${e.statusCode}).")
        } catch (e: Exception) {
            DriveAuthOutcome.Failed(e.message ?: "Unable to complete Google Drive authorization.")
        }
    }

    suspend fun disconnect(context: Context) {
        cachedToken = null
        cachedTokenAt = 0L
        disableScheduledBackup(context)
        prefs(context).edit()
            .remove("account_email")
            .remove("backup_folder_id")
            .remove("scheduled_file_id")
            .apply()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        recordEvent(context, "Disconnected Google Drive")
    }

    fun enableScheduledBackup(context: Context, encrypt: Boolean, password: String) {
        if (encrypt) {
            require(password.length >= 10) { "Use a backup password with at least 10 characters." }
        }
        prefs(context).edit().apply {
            putBoolean("scheduled_enabled", true)
            putBoolean("scheduled_encrypted", encrypt)
            if (encrypt) putString("scheduled_password", protectPassword(password))
            else remove("scheduled_password")
            apply()
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        val manager = WorkManager.getInstance(context.applicationContext)
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        manager.enqueueUniqueWork(
            "${WORK_NAME}_initial",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<GoogleDriveBackupWorker>().setConstraints(constraints).build()
        )
        recordEvent(context, "Scheduled 12-hour Google Drive backup enabled")
    }

    fun disableScheduledBackup(context: Context) {
        prefs(context).edit()
            .putBoolean("scheduled_enabled", false)
            .putBoolean("scheduled_encrypted", false)
            .remove("scheduled_password")
            .apply()
        val manager = WorkManager.getInstance(context.applicationContext)
        manager.cancelUniqueWork(WORK_NAME)
        manager.cancelUniqueWork("${WORK_NAME}_initial")
    }

    suspend fun backupNow(
        context: Context,
        db: InkDatabase,
        accessToken: String,
        encrypt: Boolean,
        password: String,
        overwriteAutoFile: Boolean = false
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val json = db.exportJson()
        val payloadBytes: ByteArray
        val fileName: String
        val mimeType: String
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())

        if (encrypt) {
            payloadBytes = EncryptedCloudBackup.encrypt(json, password)
            fileName = if (overwriteAutoFile) "$AUTO_BACKUP_BASENAME.inkq" else "InkAndQuill-$stamp.inkq"
            mimeType = "application/octet-stream"
        } else {
            payloadBytes = json.toByteArray(Charsets.UTF_8)
            fileName = if (overwriteAutoFile) "$AUTO_BACKUP_BASENAME.json" else "InkAndQuill-$stamp.json"
            mimeType = "application/json"
        }

        val folderId = findOrCreateBackupFolder(context, accessToken)
        val existingId = if (overwriteAutoFile) {
            prefs(context).getString("scheduled_file_id", null)
                ?.takeIf { DRIVE_ID_REGEX.matches(it) }
        } else {
            null
        }

        val uploaded = uploadMultipartFile(
            accessToken = accessToken,
            folderId = folderId,
            fileName = fileName,
            mimeType = mimeType,
            bytes = payloadBytes,
            existingFileId = existingId
        )

        if (overwriteAutoFile) {
            prefs(context).edit().putString("scheduled_file_id", uploaded.id).apply()
        }
        val modeTag = if (encrypt) "encrypted" else "JSON"
        recordEvent(context, "Backed up ${uploaded.name} ($modeTag)", markBackupSuccess = true)
        uploaded
    }

    suspend fun listBackups(accessToken: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val q = "trashed = false and mimeType != 'application/vnd.google-apps.folder'"
        val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(q, "UTF-8")}" +
            "&orderBy=modifiedTime%20desc&pageSize=30&fields=files(id,name,mimeType,modifiedTime,size)"
        val body = executeJsonGet(url, accessToken)
        val filesArr = JSONObject(body).optJSONArray("files") ?: JSONArray()
        buildList {
            for (i in 0 until filesArr.length()) {
                val item = filesArr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val name = item.optString("name", "Backup")
                if (!DRIVE_ID_REGEX.matches(id)) continue
                add(
                    DriveBackupFile(
                        id = id,
                        name = name,
                        mimeType = item.optString("mimeType", "application/octet-stream"),
                        modifiedTime = item.optString("modifiedTime", ""),
                        sizeBytes = item.optString("size", "0").toLongOrNull() ?: 0L,
                        isEncrypted = name.endsWith(".inkq", ignoreCase = true)
                    )
                )
            }
        }
    }

    suspend fun downloadBackupBytes(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        require(DRIVE_ID_REGEX.matches(fileId)) { "Invalid Google Drive file identifier." }
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException(extractApiError(code, err))
            }
            conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_BACKUP_DOWNLOAD_BYTES) {
                        throw IOException("Backup file exceeds maximum supported size.")
                    }
                    out.write(buf, 0, read)
                }
                out.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    fun isEncryptedPayload(bytes: ByteArray): Boolean {
        return bytes.size >= 8 && String(bytes.copyOfRange(0, 8), Charsets.US_ASCII) == "INKQBK01"
    }

    fun decodeBackupToJson(bytes: ByteArray, password: String): String {
        return if (isEncryptedPayload(bytes)) {
            EncryptedCloudBackup.decrypt(bytes, password)
        } else {
            val json = bytes.toString(Charsets.UTF_8)
            val parsed = JSONObject(json)
            require(parsed.optJSONArray("records") != null || parsed.optJSONArray("items") != null) {
                "The selected Google Drive file is not a valid Ink & Quill backup."
            }
            json
        }
    }

    suspend fun restoreFromDrive(
        context: Context,
        db: InkDatabase,
        accessToken: String,
        file: DriveBackupFile,
        password: String,
        replaceLocal: Boolean
    ) = withContext(Dispatchers.IO) {
        val rawBytes = downloadBackupBytes(accessToken, file.id)
        val json = decodeBackupToJson(rawBytes, password)
        if (replaceLocal) db.replaceWithJson(json) else db.importJson(json)
        recordEvent(context, "Restored from ${file.name}")
    }

    suspend fun deleteBackup(accessToken: String, fileId: String) = withContext(Dispatchers.IO) {
        require(DRIVE_ID_REGEX.matches(fileId)) { "Invalid Google Drive file identifier." }
        val conn = (URL("https://www.googleapis.com/drive/v3/files/$fileId").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299 && code != 404) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException(extractApiError(code, err))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchUserEmail(accessToken: String): String {
        return runCatching {
            val body = executeJsonGet("https://www.googleapis.com/oauth2/v3/userinfo", accessToken)
            JSONObject(body).optString("email", "")
        }.getOrDefault("")
    }

    private fun findOrCreateBackupFolder(context: Context, accessToken: String): String {
        val cachedId = prefs(context).getString("backup_folder_id", null)
        if (!cachedId.isNullOrBlank() && DRIVE_ID_REGEX.matches(cachedId)) {
            val check = runCatching {
                val raw = executeJsonGet(
                    "https://www.googleapis.com/drive/v3/files/$cachedId?fields=id,trashed",
                    accessToken
                )
                val obj = JSONObject(raw)
                if (!obj.optBoolean("trashed", false)) obj.optString("id") else ""
            }.getOrDefault("")
            if (check.isNotBlank()) return check
        }

        val query = "name = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)&pageSize=5"
        val existingArr = JSONObject(executeJsonGet(searchUrl, accessToken)).optJSONArray("files")
        val existingId = existingArr?.optJSONObject(0)?.optString("id").orEmpty()
        if (existingId.isNotBlank() && DRIVE_ID_REGEX.matches(existingId)) {
            prefs(context).edit().putString("backup_folder_id", existingId).apply()
            return existingId
        }

        val createConn = (URL("https://www.googleapis.com/drive/v3/files?fields=id").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        try {
            val meta = JSONObject()
                .put("name", BACKUP_FOLDER_NAME)
                .put("mimeType", "application/vnd.google-apps.folder")
                .toString()
            createConn.outputStream.use { it.write(meta.toByteArray(Charsets.UTF_8)) }
            val code = createConn.responseCode
            val stream = if (code in 200..299) createConn.inputStream else createConn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException(extractApiError(code, resp))
            val folderId = JSONObject(resp).getString("id")
            prefs(context).edit().putString("backup_folder_id", folderId).apply()
            return folderId
        } finally {
            createConn.disconnect()
        }
    }

    private fun uploadMultipartFile(
        accessToken: String,
        folderId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        existingFileId: String?
    ): DriveBackupFile {
        val isPatch = !existingFileId.isNullOrBlank() && DRIVE_ID_REGEX.matches(existingFileId)
        val endpoint = if (isPatch) {
            "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart&fields=id,name,mimeType,modifiedTime,size"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,modifiedTime,size"
        }

        val boundary = "inkquill_${UUID.randomUUID().toString().replace("-", "")}"
        val metadata = JSONObject().apply {
            put("name", fileName)
            put("mimeType", mimeType)
            if (!isPatch) put("parents", JSONArray().put(folderId))
        }.toString()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = if (isPatch) "PATCH" else "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 45_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        try {
            conn.outputStream.use { out ->
                val header1 = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n"
                val header2 = "--$boundary\r\nContent-Type: $mimeType\r\n\r\n"
                val footer = "\r\n--$boundary--\r\n"
                out.write(header1.toByteArray(Charsets.UTF_8))
                out.write(header2.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.write(footer.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val code = conn.responseCode
            if (isPatch && code == 404) {
                conn.disconnect()
                return uploadMultipartFile(accessToken, folderId, fileName, mimeType, bytes, existingFileId = null)
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException(extractApiError(code, resp))
            val obj = JSONObject(resp)
            val uploadedName = obj.optString("name", fileName)
            return DriveBackupFile(
                id = obj.getString("id"),
                name = uploadedName,
                mimeType = obj.optString("mimeType", mimeType),
                modifiedTime = obj.optString("modifiedTime", ""),
                sizeBytes = obj.optString("size", bytes.size.toString()).toLongOrNull() ?: bytes.size.toLong(),
                isEncrypted = uploadedName.endsWith(".inkq", ignoreCase = true)
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun executeJsonGet(url: String, accessToken: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                if (code == 401) {
                    cachedToken = null
                    cachedTokenAt = 0L
                }
                throw IOException(extractApiError(code, body))
            }
            body
        } finally {
            conn.disconnect()
        }
    }

    private fun extractApiError(code: Int, rawBody: String): String {
        val parsedMsg = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return when {
            code == 401 -> "Google Drive session expired. Please reconnect your Google Account."
            parsedMsg.isNotBlank() -> "Google Drive error ($code): $parsedMsg"
            else -> "Google Drive request failed (HTTP $code)."
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}

class GoogleDriveBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val uiState = GoogleDriveBackupManager.uiState(applicationContext)
        if (!uiState.scheduledEnabled) return Result.success()

        return try {
            when (val auth = GoogleDriveBackupManager.authorizeDrive(applicationContext)) {
                is DriveAuthOutcome.Authorized -> {
                    val password = if (uiState.scheduledEncrypted) {
                        GoogleDriveBackupManager.unprotectPassword(applicationContext)
                            ?: run {
                                GoogleDriveBackupManager.recordEvent(
                                    applicationContext,
                                    "Scheduled Drive backup failed: missing encryption password"
                                )
                                return Result.failure()
                            }
                    } else {
                        ""
                    }
                    val db = InkDatabase(applicationContext)
                    GoogleDriveBackupManager.backupNow(
                        context = applicationContext,
                        db = db,
                        accessToken = auth.accessToken,
                        encrypt = uiState.scheduledEncrypted,
                        password = password,
                        overwriteAutoFile = true
                    )
                    Result.success()
                }
                is DriveAuthOutcome.NeedsResolution -> {
                    GoogleDriveBackupManager.recordEvent(
                        applicationContext,
                        "Google Drive re-authorization needed"
                    )
                    Result.failure()
                }
                is DriveAuthOutcome.Failed -> {
                    GoogleDriveBackupManager.recordEvent(
                        applicationContext,
                        "Drive backup retry: ${auth.message.take(90)}"
                    )
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            GoogleDriveBackupManager.recordEvent(
                applicationContext,
                "Drive backup failed: ${(e.message ?: "Network error").take(90)}"
            )
            Result.retry()
        }
    }
}
