package com.parasaurpal.inkandquill.ui

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parasaurpal.inkandquill.data.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun GoogleDriveBackupDialog(
    db: InkDatabase,
    onRestoredFromDrive: () -> Unit,
    onFallbackSafBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(GoogleDriveBackupManager.uiState(context)) }
    var accessToken by remember { mutableStateOf(GoogleDriveBackupManager.currentAccessTokenOrNull()) }
    var backups by remember { mutableStateOf<List<DriveBackupFile>>(emptyList()) }
    var isBusy by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf("") }

    var encryptBackup by remember { mutableStateOf(uiState.scheduledEncrypted) }
    var backupPassword by remember { mutableStateOf("") }

    var pendingActionAfterAuth by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<DriveBackupFile?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoreReplaceLocal by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }

    fun refreshLocalState() {
        uiState = GoogleDriveBackupManager.uiState(context)
    }

    suspend fun loadDriveBackups(token: String) {
        isBusy = true
        runCatching {
            GoogleDriveBackupManager.listBackups(token)
        }.onSuccess { list ->
            backups = list
            refreshLocalState()
        }.onFailure { err ->
            statusBanner = err.message ?: "Could not list Google Drive backups."
        }
        isBusy = false
    }

    suspend fun performUpload(token: String) {
        if (encryptBackup && backupPassword.length < 10) {
            statusBanner = "Enter a backup password of at least 10 characters."
            return
        }
        isBusy = true
        statusBanner = "Uploading backup to Google Drive..."
        runCatching {
            GoogleDriveBackupManager.backupNow(
                context = context,
                db = db,
                accessToken = token,
                encrypt = encryptBackup,
                password = backupPassword,
                overwriteAutoFile = false
            )
        }.onSuccess { uploaded ->
            statusBanner = "Backed up to Google Drive: ${uploaded.name}"
            refreshLocalState()
            loadDriveBackups(token)
            Toast.makeText(context, "Google Drive backup complete.", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
            statusBanner = err.message ?: "Google Drive upload failed."
        }
        isBusy = false
    }

    val authResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        scope.launch {
            isBusy = true
            when (val outcome = GoogleDriveBackupManager.handleAuthorizationIntent(context, activityResult.data)) {
                is DriveAuthOutcome.Authorized -> {
                    accessToken = outcome.accessToken
                    refreshLocalState()
                    statusBanner = if (outcome.email.isNotBlank()) {
                        "Connected to ${outcome.email}"
                    } else {
                        "Google Drive connected."
                    }
                    when (pendingActionAfterAuth) {
                        "backup" -> performUpload(outcome.accessToken)
                        "list", "connect" -> loadDriveBackups(outcome.accessToken)
                    }
                    pendingActionAfterAuth = null
                }
                is DriveAuthOutcome.Failed -> {
                    statusBanner = outcome.message
                    pendingActionAfterAuth = null
                }
                is DriveAuthOutcome.NeedsResolution -> {
                    pendingActionAfterAuth = null
                }
            }
            isBusy = false
        }
    }

    fun startAuthorization(afterAction: String) {
        scope.launch {
            isBusy = true
            pendingActionAfterAuth = afterAction
            if (afterAction == "connect" && uiState.connectedEmail.isBlank()) {
                GoogleDriveBackupManager.signInWithGoogle(context)
                refreshLocalState()
            }
            when (val outcome = GoogleDriveBackupManager.authorizeDrive(context)) {
                is DriveAuthOutcome.Authorized -> {
                    accessToken = outcome.accessToken
                    refreshLocalState()
                    statusBanner = if (outcome.email.isNotBlank()) {
                        "Connected to ${outcome.email}"
                    } else {
                        "Google Drive authorized."
                    }
                    when (afterAction) {
                        "backup" -> performUpload(outcome.accessToken)
                        "list", "connect" -> loadDriveBackups(outcome.accessToken)
                    }
                    pendingActionAfterAuth = null
                }
                is DriveAuthOutcome.NeedsResolution -> {
                    authResolutionLauncher.launch(
                        IntentSenderRequest.Builder(outcome.pendingIntent.intentSender).build()
                    )
                }
                is DriveAuthOutcome.Failed -> {
                    statusBanner = outcome.message
                    pendingActionAfterAuth = null
                }
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        val existingToken = accessToken
        if (!existingToken.isNullOrBlank()) {
            loadDriveBackups(existingToken)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Google Drive Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Back up your worlds, fantasy sheets, templates & prose to Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("close_google_drive_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Google Drive backup")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (accessToken != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (uiState.connectedEmail.isNotBlank()) uiState.connectedEmail
                                            else if (accessToken != null) "Google Drive Authorized"
                                            else "Not connected to Google Drive",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = uiState.lastStatus,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.lastBackupAt > 0L) {
                                            Text(
                                                text = "Last Drive backup: ${DateFormat.getDateTimeInstance().format(Date(uiState.lastBackupAt))}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (accessToken == null) {
                                        Button(
                                            onClick = { startAuthorization("connect") },
                                            enabled = !isBusy,
                                            modifier = Modifier.testTag("connect_google_drive_button")
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Connect Google Drive")
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { accessToken?.let { token -> scope.launch { loadDriveBackups(token) } } },
                                            enabled = !isBusy
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Refresh")
                                        }
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    GoogleDriveBackupManager.disconnect(context)
                                                    accessToken = null
                                                    backups = emptyList()
                                                    refreshLocalState()
                                                    startAuthorization("connect")
                                                }
                                            },
                                            enabled = !isBusy
                                        ) {
                                            Text("Switch")
                                        }
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    GoogleDriveBackupManager.disconnect(context)
                                                    accessToken = null
                                                    backups = emptyList()
                                                    refreshLocalState()
                                                    statusBanner = "Disconnected from Google Drive."
                                                }
                                            },
                                            enabled = !isBusy
                                        ) {
                                            Text("Disconnect")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (statusBanner.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = statusBanner,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Create Backup in Google Drive",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Encrypt backup with password", fontWeight = FontWeight.Medium)
                                        Text(
                                            if (encryptBackup) "AES-256 encrypted (.inkq) before upload"
                                            else "Portable JSON backup (.json) inside Ink & Quill Backups",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = encryptBackup,
                                        onCheckedChange = { encryptBackup = it }
                                    )
                                }

                                if (encryptBackup) {
                                    OutlinedTextField(
                                        value = backupPassword,
                                        onValueChange = { backupPassword = it },
                                        modifier = Modifier.fillMaxWidth().testTag("drive_backup_password_input"),
                                        label = { Text("Backup encryption password (10+ characters)") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true
                                    )
                                }

                                Button(
                                    onClick = {
                                        val token = accessToken
                                        if (token.isNullOrBlank()) {
                                            startAuthorization("backup")
                                        } else {
                                            scope.launch { performUpload(token) }
                                        }
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.fillMaxWidth().testTag("drive_backup_now_button")
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Working...")
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Back Up to Google Drive Now")
                                    }
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-backup every 12 hours", fontWeight = FontWeight.Medium)
                                        Text(
                                            "Updates InkAndQuill-AutoBackup on Wi-Fi/cellular",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.scheduledEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                if (encryptBackup && backupPassword.length < 10) {
                                                    statusBanner = "Enter a 10+ character password before enabling encrypted auto-backup."
                                                } else {
                                                    GoogleDriveBackupManager.enableScheduledBackup(
                                                        context,
                                                        encryptBackup,
                                                        backupPassword
                                                    )
                                                    refreshLocalState()
                                                    statusBanner = "12-hour Google Drive auto-backup enabled."
                                                }
                                            } else {
                                                GoogleDriveBackupManager.disableScheduledBackup(context)
                                                refreshLocalState()
                                                statusBanner = "Scheduled Google Drive backup disabled."
                                            }
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onFallbackSafBackup()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Or save via Android Drive file picker")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Google Drive Snapshots (${backups.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (accessToken == null) {
                        item {
                            Text(
                                "Connect Google Drive above to browse and restore your Drive backups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (backups.isEmpty()) {
                        item {
                            Text(
                                "No Ink & Quill backups found in Google Drive yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(backups, key = { it.id }) { file ->
                            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (file.isEncrypted) Icons.Default.Lock else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeLabel = Formatter.formatShortFileSize(context, file.sizeBytes)
                                        val timeLabel = file.modifiedTime.take(16).replace('T', ' ')
                                        Text(
                                            text = "$sizeLabel · $timeLabel · ${if (file.isEncrypted) "Encrypted" else "JSON"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            restoreTarget = file
                                            restorePassword = ""
                                        },
                                        enabled = !isBusy
                                    ) {
                                        Text("Restore")
                                    }
                                    IconButton(
                                        onClick = {
                                            val token = accessToken ?: return@IconButton
                                            scope.launch {
                                                isBusy = true
                                                runCatching {
                                                    GoogleDriveBackupManager.deleteBackup(token, file.id)
                                                }.onSuccess {
                                                    backups = backups.filterNot { it.id == file.id }
                                                    statusBanner = "Deleted ${file.name} from Google Drive."
                                                }.onFailure { err ->
                                                    statusBanner = err.message ?: "Failed to delete backup."
                                                }
                                                isBusy = false
                                            }
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete backup")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        TextButton(
                            onClick = { showHistory = !showHistory },
                            modifier = Modifier.testTag("toggle_drive_history_button")
                        ) {
                            Icon(
                                imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (showHistory) "Hide Drive activity (${uiState.history.size})"
                                else "View Drive activity (${uiState.history.size})"
                            )
                        }
                    }

                    if (showHistory) {
                        if (uiState.history.isEmpty()) {
                            item {
                                Text(
                                    "No Google Drive backup events recorded yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(uiState.history.take(15)) { entry ->
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(entry.second, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        DateFormat.getDateTimeInstance().format(Date(entry.first)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    restoreTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text("Restore ${target.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose whether to replace your current device library or merge entries from this Google Drive backup.",
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = restoreReplaceLocal,
                            onClick = { restoreReplaceLocal = true },
                            label = { Text("Replace all") }
                        )
                        FilterChip(
                            selected = !restoreReplaceLocal,
                            onClick = { restoreReplaceLocal = false },
                            label = { Text("Merge") }
                        )
                    }
                    if (target.isEncrypted) {
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            label = { Text("Backup decryption password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val token = accessToken
                        if (token.isNullOrBlank()) {
                            restoreTarget = null
                            startAuthorization("list")
                            return@TextButton
                        }
                        if (target.isEncrypted && restorePassword.isBlank()) {
                            Toast.makeText(context, "Enter the backup password.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val pwd = restorePassword
                        val replace = restoreReplaceLocal
                        restoreTarget = null
                        scope.launch {
                            isBusy = true
                            runCatching {
                                GoogleDriveBackupManager.restoreFromDrive(
                                    context = context,
                                    db = db,
                                    accessToken = token,
                                    file = target,
                                    password = pwd,
                                    replaceLocal = replace
                                )
                            }.onSuccess {
                                onRestoredFromDrive()
                                refreshLocalState()
                                statusBanner = "Restored library from ${target.name}."
                                Toast.makeText(context, "Restored from Google Drive.", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                statusBanner = err.message ?: "Restore failed."
                            }
                            isBusy = false
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
