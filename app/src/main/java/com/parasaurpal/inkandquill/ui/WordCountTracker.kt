package com.parasaurpal.inkandquill.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parasaurpal.inkandquill.data.*

@Composable
fun LiveWordCountBar(
    report: HierarchyWordReport,
    sessionStartWords: Int,
    compactFocusMode: Boolean = false,
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = report.currentStats
    val sessionDelta = stats.totalWords - sessionStartWords
    val deltaText = when {
        sessionDelta > 0 -> "+$sessionDelta"
        sessionDelta < 0 -> "$sessionDelta"
        else -> null
    }

    val tierPills = remember(report) {
        buildList {
            report.sceneTier?.let { add("Scene" to it.wordCount) }
            report.chapterTier?.let { add("Ch" to it.wordCount) }
            report.bookTier?.let { add("Book" to it.wordCount) }
            report.seriesTier?.let { add("Series" to it.wordCount) }
            report.worldTier?.let { add("World" to it.wordCount) }
        }
    }

    Surface(
        onClick = onOpenDashboard,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (compactFocusMode) 0.35f else 0.52f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_word_count_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compactFocusMode) 5.dp else 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Word count tracker",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "%,d words".format(stats.totalWords),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (stats.sheetWords > 0 && stats.proseWords > 0) {
                    Text(
                        text = "(${WordCountEngine.formatCompact(stats.proseWords)} prose + ${WordCountEngine.formatCompact(stats.sheetWords)} sheet)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (deltaText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (sessionDelta >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = deltaText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionDelta >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (!compactFocusMode && stats.readingMinutes > 0) {
                    Text(
                        text = "· ~${stats.readingMinutes}m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                if (report.currentGoal > 0) {
                    Text(
                        text = "${((stats.totalWords.toFloat() / report.currentGoal) * 100).toInt()}% of %,d".format(report.currentGoal),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Details",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!compactFocusMode && tierPills.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tierPills.forEach { (label, count) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = "$label: ${WordCountEngine.formatCompact(count)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (report.currentGoal > 0) {
                LinearProgressIndicator(
                    progress = { (stats.totalWords.toFloat() / report.currentGoal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
        }
    }
}

@Composable
fun WordCountDashboardDialog(
    db: InkDatabase,
    report: HierarchyWordReport,
    sessionStartWords: Int,
    onGoalUpdated: () -> Unit,
    onOpenRecord: (InkRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var editingGoalTarget by remember { mutableStateOf<Pair<InkRecord, Int>?>(null) }
    var goalInput by remember { mutableStateOf("") }

    val stats = report.currentStats
    val sessionDelta = stats.totalWords - sessionStartWords
    val tiers = remember(report) {
        listOfNotNull(
            report.sceneTier,
            report.chapterTier,
            report.bookTier,
            report.seriesTier,
            report.worldTier
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("word_count_dashboard_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Word Count Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${report.currentRecord.title.ifBlank { "Untitled" }} · ${report.currentRecord.kind}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close word count tracker")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // 1. Live Document Metrics
                    item(key = "doc_metrics") {
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Current Document",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            editingGoalTarget = report.currentRecord to report.currentGoal
                                            goalInput = if (report.currentGoal > 0) report.currentGoal.toString() else ""
                                        },
                                        modifier = Modifier.testTag("set_current_word_goal_button")
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (report.currentGoal > 0) "Goal: %,d".format(report.currentGoal) else "Set Goal")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatTile("Total Words", "%,d".format(stats.totalWords), Modifier.weight(1f))
                                    StatTile("Prose", "%,d".format(stats.proseWords), Modifier.weight(1f))
                                    StatTile("Sheet", "%,d".format(stats.sheetWords), Modifier.weight(1f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatTile(
                                        "Session",
                                        if (sessionDelta >= 0) "+%,d".format(sessionDelta) else "%,d".format(sessionDelta),
                                        Modifier.weight(1f)
                                    )
                                    StatTile("Characters", "%,d".format(stats.charactersWithSpaces), Modifier.weight(1f))
                                    StatTile("Read Time", "${stats.readingMinutes} min", Modifier.weight(1f))
                                }

                                if (report.currentGoal > 0) {
                                    val pct = ((stats.totalWords.toFloat() / report.currentGoal) * 100).toInt()
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Document Goal Progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$pct% (%,d / %,d)".format(stats.totalWords, report.currentGoal), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        LinearProgressIndicator(
                                            progress = { (stats.totalWords.toFloat() / report.currentGoal.toFloat()).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Hierarchy Roll-up (Scene -> Chapter -> Book -> Series -> World)
                    item(key = "hierarchy_header") {
                        Text(
                            text = "Hierarchy Progress (Scene → World)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(tiers, key = { "${it.tierLabel}_${it.record?.id ?: "all"}" }) { tier ->
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { mod ->
                                    if (tier.record != null && tier.record.id != report.currentRecord.id) {
                                        mod.clickable { onOpenRecord(tier.record) }
                                    } else mod
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = tier.tierLabel.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = tier.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "%,d w".format(tier.wordCount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (tier.tierLabel.contains("World", ignoreCase = true) && (tier.storyWords > 0 || tier.loreWords > 0)) {
                                    Text(
                                        text = "Manuscript & story: %,d w · Worldbuilding & lore: %,d w".format(tier.storyWords, tier.loreWords),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (tier.childCount > 0) {
                                    Text(
                                        text = "${tier.childCount} child item(s) · %,d own words".format(tier.ownWords),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (tier.goalWords > 0) {
                                    LinearProgressIndicator(
                                        progress = { tier.progress },
                                        modifier = Modifier.fillMaxWidth().height(5.dp)
                                    )
                                }

                                if (tier.record != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                editingGoalTarget = tier.record to tier.goalWords
                                                goalInput = if (tier.goalWords > 0) tier.goalWords.toString() else ""
                                            }
                                        ) {
                                            Text(
                                                if (tier.goalWords > 0) "Target: %,d".format(tier.goalWords) else "Set Target",
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (tier.record.id != report.currentRecord.id) {
                                            TextButton(onClick = { onOpenRecord(tier.record) }) {
                                                Text("Open ${tier.tierLabel}", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Direct Child Breakdown (e.g. Scenes in Chapter, Chapters in Book, Books in Series)
                    if (report.childBreakdowns.isNotEmpty()) {
                        item(key = "children_header") {
                            Text(
                                text = "Inside ${report.currentRecord.title.ifBlank { report.currentRecord.kind }} (${report.childBreakdowns.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(report.childBreakdowns, key = { "child_${it.record.id}" }) { child ->
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenRecord(child.record) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = child.record.title.ifBlank { "Untitled ${child.record.kind}" },
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = child.record.kind,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "%,d w".format(child.subtreeWords),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Overall Worlds Summary
                    if (report.worldSummaryList.isNotEmpty()) {
                        item(key = "all_worlds_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Worlds Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Library total: %,d w".format(report.allWorldsTotalWords),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        items(report.worldSummaryList, key = { "world_sum_${it.record?.id}" }) { wTier ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let { mod ->
                                        if (wTier.record != null && wTier.record.id != report.currentRecord.id) {
                                            mod.clickable { onOpenRecord(wTier.record) }
                                        } else mod
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(wTier.title, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Story: %,d w · Lore: %,d w".format(wTier.storyWords, wTier.loreWords),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "%,d w".format(wTier.wordCount),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingGoalTarget?.let { (targetRecord, currentGoalValue) ->
        AlertDialog(
            onDismissRequest = { editingGoalTarget = null },
            title = { Text("Word Target · ${targetRecord.title.ifBlank { targetRecord.kind }}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target word count (e.g. 2500)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("word_goal_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = when (targetRecord.kind) {
                            "Scene" -> listOf(500, 1000, 1500, 2500)
                            "Chapter" -> listOf(1500, 2500, 4000, 6000)
                            "Book", "Novel", "Story" -> listOf(30000, 50000, 80000, 100000)
                            "Series" -> listOf(100000, 200000, 300000)
                            else -> listOf(1000, 5000, 25000, 100000)
                        }
                        presets.forEach { preset ->
                            FilterChip(
                                selected = goalInput == preset.toString(),
                                onClick = { goalInput = preset.toString() },
                                label = { Text(WordCountEngine.formatCompact(preset)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = goalInput.toIntOrNull() ?: 0
                    db.setSetting("wordGoal_${targetRecord.id}", if (parsed > 0) parsed.toString() else "0")
                    editingGoalTarget = null
                    onGoalUpdated()
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (currentGoalValue > 0) {
                        TextButton(onClick = {
                            db.setSetting("wordGoal_${targetRecord.id}", "0")
                            editingGoalTarget = null
                            onGoalUpdated()
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { editingGoalTarget = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
