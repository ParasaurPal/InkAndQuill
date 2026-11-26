package com.parasaurpal.inkandquill.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.parasaurpal.inkandquill.data.NovelStudioStore
import com.parasaurpal.inkandquill.data.StudioNode
import org.json.JSONObject

private enum class NovelLevel { WORLDS, BOOKS, CHAPTERS, SCENES, EDITOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelStudioScreen(onClose: () -> Unit) {
    val context=LocalContext.current
    val store= remember(context){NovelStudioStore(context)}
    var level by remember{mutableStateOf(NovelLevel.WORLDS)}
    var worldId by remember{mutableStateOf("")}
    var bookId by remember{mutableStateOf("")}
    var chapterId by remember{mutableStateOf("")}
    var sceneId by remember{mutableStateOf("")}
    var parentTitle by remember{mutableStateOf("")}
    var refresh by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var newTitle by remember{mutableStateOf("")}
    var extra by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var prose by remember{mutableStateOf("")}
    var summary by remember{mutableStateOf("")}
    var goal by remember{mutableStateOf("")}
    var conflict by remember{mutableStateOf("")}
    var hook by remember{mutableStateOf("")}
    var focus by remember{mutableStateOf(false)}
    var toolsOpen by remember{mutableStateOf(false)}
    var saved by remember{mutableStateOf(false)}
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{out->out.write(store.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->store.importJson(reader.readText())};refresh++}}}
    val textExportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use{writer->writer.write(prose)}}}
    val worlds= remember(level,refresh){store.universes()}
    val nodes= remember(level,worldId,bookId,chapterId,refresh){
        when(level){
            NovelLevel.BOOKS->store.orderedNodes(worldId,"Book")
            NovelLevel.CHAPTERS->store.orderedNodes(bookId,"Chapter")
            NovelLevel.SCENES->store.orderedNodes(chapterId,"Scene")
            else-> emptyList()
        }
    }
    fun loadScene(node:StudioNode){
        sceneId=node.id;title=node.title
        store.readableDatabase.rawQuery("SELECT s.summary,s.goal,s.conflict,s.hook,d.plain_text FROM scenes s LEFT JOIN documents d ON d.scene_id=s.id WHERE s.id=?",arrayOf(node.id)).use{c->
            if(c.moveToFirst()){summary=c.getString(0)?: "";goal=c.getString(1)?: "";conflict=c.getString(2)?: "";hook=c.getString(3)?: "";prose=c.getString(4)?: ""}
        }
        level=NovelLevel.EDITOR;saved=false
    }
    LaunchedEffect(sceneId,title,prose,summary,goal,conflict,hook) {
        if(sceneId.isNotBlank()) {
            kotlinx.coroutines.delay(650)
            val meta=JSONObject().put("summary",summary).put("goal",goal).put("conflict",conflict).put("hook",hook)
            val content=JSONObject().put("blocks",org.json.JSONArray().put(JSONObject().put("type","paragraph").put("text",prose)))
            store.saveScene(sceneId,title,meta,content,prose)
            saved=true
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)){
        if(!focus) TopAppBar(
            title={Column{Text("Novel Studio",fontWeight=FontWeight.Bold);Text(when(level){NovelLevel.WORLDS->"Universes";NovelLevel.BOOKS,NovelLevel.CHAPTERS,NovelLevel.SCENES->parentTitle;NovelLevel.EDITOR->title},style=MaterialTheme.typography.labelSmall)}},
            navigationIcon={IconButton(onClick={
                when(level){NovelLevel.WORLDS->onClose();NovelLevel.BOOKS->level=NovelLevel.WORLDS;NovelLevel.CHAPTERS->level=NovelLevel.BOOKS;NovelLevel.SCENES->level=NovelLevel.CHAPTERS;NovelLevel.EDITOR->level=NovelLevel.SCENES}
            }){Icon(if(level==NovelLevel.WORLDS)Icons.Default.Close else Icons.Default.ArrowBack,"Back")}},
            actions={
                if(level==NovelLevel.WORLDS){IconButton(onClick={exportLauncher.launch("InkAndQuill.fortelling.json")}){Icon(Icons.Default.FileDownload,"Export project")};IconButton(onClick={importLauncher.launch(arrayOf("application/json","text/*"))}){Icon(Icons.Default.FileUpload,"Import project")}}
                if(level!=NovelLevel.WORLDS&&level!=NovelLevel.EDITOR){IconButton(onClick={toolsOpen=true}){Icon(Icons.Default.Explore,"Worldbuilding and planning")}}
                if(level==NovelLevel.EDITOR){IconButton(onClick={textExportLauncher.launch((title.ifBlank{"Scene"}).replace(Regex("[^a-zA-Z0-9_-]"),"_")+".txt")}){Icon(Icons.Default.TextSnippet,"Export scene as text")};IconButton(onClick={focus=!focus}){Icon(Icons.Default.CenterFocusStrong,if(focus)"Exit focus mode" else "Focus mode")}}
                if(level!=NovelLevel.EDITOR) IconButton(onClick={newTitle="";extra="";showCreate=true}){Icon(Icons.Default.Add,"Add")}
            }
        ) else if(level==NovelLevel.EDITOR){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.End){TextButton(onClick={focus=false}){Text("Exit focus mode")}}}
        if(toolsOpen){NovelStudioToolsScreen(worldId,store){toolsOpen=false;refresh++}} else when(level){
            NovelLevel.WORLDS->{
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    item{Text("Build a world. Write its stories.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Offline-first. Your work stays on this device.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    items(worlds){w->
                        Surface(Modifier.fillMaxWidth().clickable{worldId=w.getString("id");parentTitle=w.getString("title");level=NovelLevel.BOOKS},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
                            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(w.getString("title"),Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Icon(Icons.Default.ChevronRight,"Open")}
                        }
                    }
                    if(worlds.isEmpty())item{Text("Create your first universe.")}
                }
                Button(onClick={newTitle="";extra="";showCreate=true},modifier=Modifier.fillMaxWidth().padding(16.dp)){Text("Create universe")}
            }
            NovelLevel.BOOKS,NovelLevel.CHAPTERS,NovelLevel.SCENES->{
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items(nodes){node->
                        Surface(Modifier.fillMaxWidth().clickable{
                            parentTitle=node.title
                            when(level){NovelLevel.BOOKS->{bookId=node.id;level=NovelLevel.CHAPTERS};NovelLevel.CHAPTERS->{chapterId=node.id;level=NovelLevel.SCENES};NovelLevel.SCENES->loadScene(node);else->Unit}
                        },shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
                            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){Text(node.title,fontWeight=FontWeight.Medium);if(node.customStatus.isNotBlank()||node.status.isNotBlank())Text(node.customStatus.ifBlank{node.status},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                                Icon(Icons.Default.ChevronRight,"Open")
                            }
                        }
                    }
                    if(nodes.isEmpty())item{Text("Nothing here yet. Add the first item.")}
                }
                Button(onClick={newTitle="";extra="";showCreate=true},modifier=Modifier.fillMaxWidth().padding(16.dp)){Text(when(level){NovelLevel.BOOKS->"Add book";NovelLevel.CHAPTERS->"Add chapter";else->"Add scene"})}
            }
            NovelLevel.EDITOR->{
                Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    if(!focus){
                        OutlinedTextField(title,{title=it;saved=false},label={Text("Scene title")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                        OutlinedTextField(summary,{summary=it;saved=false},label={Text("Summary")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(goal,{goal=it;saved=false},label={Text("Goal")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(conflict,{conflict=it;saved=false},label={Text("Conflict")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(hook,{hook=it;saved=false},label={Text("Hook")},modifier=Modifier.fillMaxWidth())
                        Text("MANUSCRIPT",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(prose,{prose=it;saved=false},modifier=Modifier.weight(1f).fillMaxWidth(),placeholder={Text("Write your scene…")},textStyle=MaterialTheme.typography.bodyLarge.copy(fontSize=18.sp),minLines=12)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        Text(prose.trim().split(Regex("\\s+")).count{it.isNotBlank()}.toString()+" words",style=MaterialTheme.typography.labelSmall)
                        Button(onClick={
                            val meta=JSONObject().put("summary",summary).put("goal",goal).put("conflict",conflict).put("hook",hook)
                            val content=JSONObject().put("blocks",org.json.JSONArray().put(JSONObject().put("type","paragraph").put("text",prose)))
                            store.saveScene(sceneId,title,meta,content,prose)
                            store.saveVersion("scene",sceneId,"Manual checkpoint",content)
                            saved=true
                        }){Icon(Icons.Default.Save,null);Spacer(Modifier.width(6.dp));Text("Save")}
                    }
                    if(saved)Text("Saved on this device",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if(showCreate){
        AlertDialog(
            onDismissRequest={showCreate=false},
            title={Text(when(level){NovelLevel.WORLDS->"New universe";NovelLevel.BOOKS->"New book";NovelLevel.CHAPTERS->"New chapter";else->"New scene"})},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(newTitle,{newTitle=it},label={Text("Name")},singleLine=true)
                if(level==NovelLevel.WORLDS)OutlinedTextField(extra,{extra=it},label={Text("Description")})
                if(level==NovelLevel.CHAPTERS)OutlinedTextField(extra,{extra=it},label={Text("Custom status label")})
            }},
            confirmButton={TextButton(onClick={
                if(newTitle.isNotBlank()){
                    when(level){
                        NovelLevel.WORLDS->{worldId=store.createUniverse(newTitle,description=extra);parentTitle=newTitle;level=NovelLevel.BOOKS}
                        NovelLevel.BOOKS->store.createBook(worldId,newTitle)
                        NovelLevel.CHAPTERS->store.createChapter(bookId,newTitle,extra)
                        NovelLevel.SCENES->store.createScene(chapterId,newTitle)
                        else->Unit
                    }
                    refresh++;showCreate=false
                }
            }){Text("Create")}},
            dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}}
        )
    }
}
