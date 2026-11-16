package com.parasaurpal.inkandquill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17141E)
private val Panel = Color(0xFF24202D)
private val Lavender = Color(0xFFC9B6E4)
private val Muted = Color(0xFFAAA2B5)
private val Paper = Color(0xFFF2EAF8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InkAndQuillApp() }
    }
}

@Composable
fun InkAndQuillApp() {
    var selected by remember { mutableStateOf("Home") }
    var showCreate by remember { mutableStateOf(false) }
    val sections = listOf("Novels & Stories", "Poetry", "Journal", "Documents", "Notes", "Creative Projects")
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Lavender,
        onBackground = Paper, onSurface = Paper, onPrimary = Ink
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Lavender) {
                    listOf("Home", "Library", "Create", "History", "Settings").forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { if (item == "Create") showCreate = true else selected = item },
                            icon = {
                                Icon(when (item) {
                                    "Home" -> Icons.Default.Home
                                    "Library" -> Icons.Default.Book
                                    "Create" -> Icons.Default.AddCircle
                                    "History" -> Icons.Default.History
                                    else -> Icons.Default.Settings
                                }, contentDescription = item)
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Home" -> HomeScreen(sections, Modifier.padding(padding), onCreate = { showCreate = true })
                "Library" -> LibraryScreen(sections, Modifier.padding(padding))
                "History" -> SimpleScreen("Writing history", "Your recent edits and recoverable versions will appear here.", Modifier.padding(padding))
                "Settings" -> SimpleScreen("Your writing space", "Appearance, typography, editor layout, backups, templates, and automatic connections.", Modifier.padding(padding))
            }
        }
        if (showCreate) {
            CreateDialog(onDismiss = { showCreate = false }, onChoose = { showCreate = false; selected = "Library" })
        }
    }
}

@Composable
private fun HomeScreen(sections: List<String>, modifier: Modifier = Modifier, onCreate: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("INK & QUILL", color = Lavender, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text("Your stories,
your worlds.", color = Paper, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp)
            Spacer(Modifier.height(8.dp))
            Text("A quiet place for every kind of writing.", color = Muted, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onCreate)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Begin something new", color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Create a story, journal entry, poem, or document.", color = Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.Create, null, tint = Lavender, modifier = Modifier.size(30.dp))
                }
            }
        }
        item { Text("YOUR LIBRARY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        items(sections) { section ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { }) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = Lavender)
                    Spacer(Modifier.width(14.dp))
                    Text(section, color = Paper, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(sections: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Library", color = Paper, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("Everything you create, gathered in one place.", color = Muted)
        Spacer(Modifier.height(20.dp))
        sections.forEach { Text(it, color = Paper, modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp)) }
    }
}

@Composable
private fun SimpleScreen(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Paper, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted)
    }
}

@Composable
private fun CreateDialog(onDismiss: () -> Unit, onChoose: (String) -> Unit) {
    val types = listOf("World", "Series", "Standalone Story", "Poem", "Journal Entry", "Document", "Note", "Creative Project")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Create", color = Paper) },
        text = {
            Column {
                types.forEach { type ->
                    TextButton(onClick = { onChoose(type) }, modifier = Modifier.fillMaxWidth()) {
                        Text(type, color = Lavender, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Muted) } }
    )
}
