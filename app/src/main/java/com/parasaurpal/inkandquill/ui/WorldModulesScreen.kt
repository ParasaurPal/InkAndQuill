package com.parasaurpal.inkandquill.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.InkDatabase
import com.parasaurpal.inkandquill.data.InkRecord

private data class WorldModule(
    val title: String,
    val icon: ImageVector,
    val kinds: List<String>,
    val primaryKind: String
)

private val characterKinds = listOf("Character", "NPC", "Dynasty", "Lineage")
private val groupKinds = listOf("Faction", "Culture", "Military", "Government", "Economy", "Festival")
private val locationKinds = listOf("Location", "Region", "Kingdom", "Settlement", "Dungeon", "Stronghold", "Realm", "Biome")
private val itemKinds = listOf("Item", "Artifact", "Weapon", "Potion", "Material", "Vessel")
private val languageKinds = listOf("Language", "Script")
private val floraKinds = listOf("Flora", "Herb", "Fungus")
private val faunaKinds = listOf("Fauna", "Monster", "Species", "Dragon")

@Composable
fun WorldModulesScreen(
    db: InkDatabase,
    world: InkRecord,
    refresh: Int,
    onOpenKinds: (title: String, kinds: List<String>, primaryKind: String) -> Unit,
    onOpenMode: (String) -> Unit,
    onOpenFiles: () -> Unit
) {
    val descendants = remember(world.id, refresh) { db.descendants(world.id) }
    val hasCharacter = descendants.any { it.kind in characterKinds }
    val hasLocation = descendants.any { it.kind in locationKinds }
    val hasTimelineEvent = remember(world.id, refresh) { db.timeline(world.id).isNotEmpty() }
    val hasBoard = remember(world.id, refresh) { db.boards(world.id).isNotEmpty() }
    val stepsDone = listOf(hasCharacter, hasLocation, hasTimelineEvent, hasBoard).count { it }
    val nextStep: Pair<String, () -> Unit>? = when {
        !hasCharacter -> "create a character" to { onOpenKinds("Characters", characterKinds, "Character") }
        !hasLocation -> "create a location" to { onOpenKinds("Locations", locationKinds, "Location") }
        !hasTimelineEvent -> "add a timeline event" to { onOpenMode("Timeline") }
        !hasBoard -> "create a board" to { onOpenMode("Boards") }
        else -> null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Card(
            Modifier.fillMaxWidth().clickable(enabled = nextStep != null) { nextStep?.second?.invoke() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { stepsDone / 4f },
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Build your world · $stepsDone of 4",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (nextStep != null) "Next up: ${nextStep.first}" else "All starter steps complete",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                if (nextStep != null) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Next step",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        ModuleSection("Encyclopedia") {
            ModuleRow("World Encyclopedia", Icons.Default.LibraryBooks) { onOpenMode("Encyclopedia") }
        }

        ModuleSection("Story") {
            ModuleRow("Timeline", Icons.Default.Timeline) { onOpenMode("Timeline") }
        }
        ModuleSection("Characters") {
            ModuleRow("Characters", Icons.Default.Person) { onOpenKinds("Characters", characterKinds, "Character") }
            ModuleRow("Groups", Icons.Default.Groups) { onOpenKinds("Groups", groupKinds, "Faction") }
        }
        ModuleSection("World") {
            ModuleRow("Locations", Icons.Default.LocationCity) { onOpenKinds("Locations", locationKinds, "Location") }
            ModuleRow("Items", Icons.Default.Diamond) { onOpenKinds("Items", itemKinds, "Item") }
            ModuleRow("Languages", Icons.Default.Language) { onOpenKinds("Languages", languageKinds, "Language") }
            ModuleRow("Flora", Icons.Default.Spa) { onOpenKinds("Flora", floraKinds, "Flora") }
            ModuleRow("Fauna", Icons.Default.Pets) { onOpenKinds("Fauna", faunaKinds, "Fauna") }
        }
        ModuleSection("Additional Tools") {
            ModuleRow("Boards", Icons.Default.PushPin) { onOpenMode("Boards") }
            ModuleRow("Calendar", Icons.Default.CalendarMonth) { onOpenMode("Calendar") }
            ModuleRow("Connections", Icons.Default.Hub) { onOpenMode("Connections") }
            ModuleRow("Files", Icons.Default.Folder) { onOpenFiles() }
            ModuleRow("All Resources", Icons.Default.LibraryBooks) { onOpenMode("Resources") }
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun ModuleSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ModuleRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, title, Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun WorldModuleListScreen(
    db: InkDatabase,
    world: InkRecord,
    title: String,
    kinds: List<String>,
    primaryKind: String,
    refresh: Int,
    open: (InkRecord) -> Unit,
    onBack: () -> Unit
) {
    val records = remember(world.id, kinds, refresh) {
        db.descendants(world.id).filter { it.kind in kinds }.sortedBy { it.title.lowercase() }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to modules") }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = {
                val record = InkRecord(title = "Untitled $primaryKind", kind = primaryKind, parentId = world.id, scope = "World")
                db.save(record)
                open(record)
            }) { Text("Add") }
        }
        if (records.isEmpty()) {
            Text(
                "No ${title.lowercase()} in this world yet. Tap Add to create the first one.",
                Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                records.forEach { record ->
                    Card(Modifier.fillMaxWidth().clickable { open(record) }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(record.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold)
                            Text(record.kind, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        }
                    }
                }
            }
        }
    }
}
