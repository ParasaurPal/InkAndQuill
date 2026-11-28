package com.parasaurpal.inkandquill.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.parasaurpal.inkandquill.data.NovelStudioStore
import com.parasaurpal.inkandquill.data.DocumentExport
import com.parasaurpal.inkandquill.data.StudioNode
import org.json.JSONObject

private enum class NovelLevel { WORLDS, BOOKS, CHAPTERS, SCENES, EDITOR }
private val StudioFonts = listOf("Literata", "Source Serif 4", "Lora", "Crimson Pro", "Merriweather")

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
    var editorHtml by remember{mutableStateOf("")}
    var summary by remember{mutableStateOf("")}
    var goal by remember{mutableStateOf("")}
    var conflict by remember{mutableStateOf("")}
    var hook by remember{mutableStateOf("")}
    var focus by remember{mutableStateOf(false)}
    var toolsOpen by remember{mutableStateOf(false)}
    var planningOpen by remember{mutableStateOf(false)}
    var saved by remember{mutableStateOf(false)}
    var fontChoice by remember{mutableStateOf("Literata")}
    var fontMenuExpanded by remember{mutableStateOf(false)}
    var showSeriesCreate by remember{mutableStateOf(false)}
    var seriesTitle by remember{mutableStateOf("")}
    var seriesBanner by remember{mutableStateOf("")}
    var storyType by remember{mutableStateOf("NOVEL")}
    var selectedSeriesId by remember{mutableStateOf("")}
    var connectedBookId by remember{mutableStateOf("")}
    var coverUri by remember{mutableStateOf("")}
    var expandedSeries by remember{mutableStateOf(setOf<String>())}
    var seriesMenuExpanded by remember{mutableStateOf(false)}
    var connectedMenuExpanded by remember{mutableStateOf(false)}
    val bannerPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};seriesBanner=it.toString()}}
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};coverUri=it.toString()}}
    val bannerPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};seriesBanner=it.toString()}}
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};coverUri=it.toString()}}
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{out->out.write(store.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->store.importJson(reader.readText())};refresh++}}}
    val textExportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use{writer->writer.write(prose)}}}
    val worlds= remember(level,refresh){store.universes()}
    val series= remember(level,worldId,refresh){if(worldId.isBlank()) emptyList() else store.series(worldId)}
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
        store.readableDatabase.rawQuery("SELECT s.summary,s.goal,s.conflict,s.hook,d.content_json,d.plain_text FROM scenes s LEFT JOIN documents d ON d.scene_id=s.id WHERE s.id=?",arrayOf(node.id)).use{c->
            if(c.moveToFirst()){
                summary=c.getString(0)?: "";goal=c.getString(1)?: "";conflict=c.getString(2)?: "";hook=c.getString(3)?: ""
                prose=c.getString(5)?: ""
                val stored=runCatching{JSONObject(c.getString(4)?: "{}")}.getOrDefault(JSONObject())
                editorHtml=stored.optString("html").ifBlank{android.text.Html.escapeHtml(prose).replace("\n","<br>")}
            }
        }
        level=NovelLevel.EDITOR;saved=false
    }
    LaunchedEffect(sceneId,title,editorHtml,summary,goal,conflict,hook) {
        if(sceneId.isNotBlank()) {
            kotlinx.coroutines.delay(650)
            val meta=JSONObject().put("summary",summary).put("goal",goal).put("conflict",conflict).put("hook",hook)
            val plain=DocumentExport.plain(editorHtml)
            prose=plain
            val content=JSONObject().put("format_version",2).put("html",editorHtml)
            store.saveScene(sceneId,title,meta,content,plain)
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
                if(level==NovelLevel.BOOKS){IconButton(onClick={seriesTitle="";seriesBanner="";showSeriesCreate=true}){Icon(Icons.Default.CollectionsBookmark,"New series")}}
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
                    if(level==NovelLevel.BOOKS){
                        series.forEach { seriesRow ->
                            val sid=seriesRow.optString("id")
                            val expanded=sid in expandedSeries
                            val grouped=nodes.filter{runCatching{JSONObject(it.metadata).optString("series_id")==sid}.getOrDefault(false)}
                            item(key="series-$sid"){
                                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                                    Surface(Modifier.fillMaxWidth().clickable{expandedSeries=if(expanded)expandedSeries-sid else expandedSeries+sid},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceVariant){
                                        Box(Modifier.fillMaxWidth().height(118.dp)){
                                            if(seriesRow.optString("banner_uri").isNotBlank()) StudioArtwork(seriesRow.optString("banner_uri"),Modifier.fillMaxSize())
                                            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha=.62f)).padding(12.dp)){
                                                Text(seriesRow.optString("title"),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                                                Text("${grouped.size} novels and stories · ${if(expanded)"Tap to collapse" else "Tap to expand"}",color=Color.White,style=MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(onClick={bannerPicker.launch(arrayOf("image/*"))},modifier=Modifier.align(Alignment.TopEnd)){Icon(Icons.Default.Image,"Change banner",tint=Color.White)}
                                        }
                                    }
                                    if(expanded) grouped.forEach { book -> StudioBookCard(book,store,{bookId=book.id;parentTitle=book.title;level=NovelLevel.CHAPTERS}) }
                                }
                            }
                        }
                    }
                    items(if(level==NovelLevel.BOOKS) nodes.filter{runCatching{JSONObject(it.metadata).optString("series_id").isBlank()}.getOrDefault(true)} else nodes){node->
                        if(level==NovelLevel.BOOKS){
                            StudioBookCard(node,store,{bookId=node.id;parentTitle=node.title;level=NovelLevel.CHAPTERS})
                        } else Surface(Modifier.fillMaxWidth().clickable{
                            parentTitle=node.title
                            when(level){NovelLevel.BOOKS->{bookId=node.id;level=NovelLevel.CHAPTERS};NovelLevel.CHAPTERS->{chapterId=node.id;level=NovelLevel.SCENES};NovelLevel.SCENES->loadScene(node);else->Unit}
                        },shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
                            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){Text(node.title,fontWeight=FontWeight.Medium);if(node.customStatus.isNotBlank()||node.status.isNotBlank())Text(node.customStatus.ifBlank{node.status},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                                Icon(Icons.Default.ChevronRight,"Open")
                            }
                        }
                    }
                    if(nodes.isEmpty()&&series.isEmpty())item{Text("Nothing here yet. Add the first item.")}
                }
                Button(onClick={newTitle="";extra="";showCreate=true},modifier=Modifier.fillMaxWidth().padding(16.dp)){Text(when(level){NovelLevel.BOOKS->"Add book";NovelLevel.CHAPTERS->"Add chapter";else->"Add scene"})}
            }
            NovelLevel.EDITOR->{
                Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedTextField(
                        title,
                        {title=it;saved=false},
                        label={Text("Scene title")},
                        modifier=Modifier.fillMaxWidth(),
                        singleLine=true
                    )
                    if(!focus){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Text("MANUSCRIPT",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.weight(1f))
                            TextButton(onClick={planningOpen=!planningOpen}){
                                Icon(if(planningOpen)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,modifier=Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if(planningOpen)"Hide planning" else "Planning notes")
                            }
                        }
                        if(planningOpen)Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Text("Optional scene planning",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(summary,{summary=it;saved=false},label={Text("Summary")},modifier=Modifier.fillMaxWidth(),minLines=2)
                            OutlinedTextField(goal,{goal=it;saved=false},label={Text("Scene goal")},modifier=Modifier.fillMaxWidth(),minLines=2)
                            OutlinedTextField(conflict,{conflict=it;saved=false},label={Text("Conflict")},modifier=Modifier.fillMaxWidth(),minLines=2)
                            OutlinedTextField(hook,{hook=it;saved=false},label={Text("Hook / turning point")},modifier=Modifier.fillMaxWidth(),minLines=2)
                        }
                    }
                    RichTextEditor(
                        value=editorHtml,
                        onValueChange={editorHtml=it;prose=DocumentExport.plain(it);saved=false},
                        modifier=Modifier.weight(1f).fillMaxWidth(),
                        fontSize=18f,
                        enabledCommands=if(focus) emptySet() else null
                    )
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        Text(prose.trim().split(Regex("\\s+")).count{it.isNotBlank()}.toString()+" words",style=MaterialTheme.typography.labelSmall)
                        Button(onClick={
                            val meta=JSONObject().put("summary",summary).put("goal",goal).put("conflict",conflict).put("hook",hook)
                            val plain=DocumentExport.plain(editorHtml)
                            prose=plain
                            val content=JSONObject().put("format_version",2).put("html",editorHtml)
                            store.saveScene(sceneId,title,meta,content,plain)
                            store.saveVersion("scene",sceneId,"Manual checkpoint",content)
                            saved=true
                        }){Icon(Icons.Default.Save,null);Spacer(Modifier.width(6.dp));Text("Save")}
                    }
                    if(saved)Text("Saved on this device",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if(showSeriesCreate){
        AlertDialog(
            onDismissRequest={showSeriesCreate=false},
            title={Text("New series")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedTextField(seriesTitle,{seriesTitle=it},label={Text("Series name")},singleLine=true)
                OutlinedButton(onClick={bannerPicker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Image,null);Spacer(Modifier.width(8.dp));Text(if(seriesBanner.isBlank())"Choose banner image" else "Change banner image")
                }
            }},
            confirmButton={TextButton(onClick={if(seriesTitle.isNotBlank()){store.createSeries(worldId,seriesTitle,seriesBanner);refresh++;showSeriesCreate=false}}){Text("Create series")}},
            dismissButton={TextButton(onClick={showSeriesCreate=false}){Text("Cancel")}}
        )
    }
    if(showCreate){
        AlertDialog(
            onDismissRequest={showCreate=false},
            title={Text(when(level){NovelLevel.WORLDS->"New universe";NovelLevel.BOOKS->if(storyType=="SHORT_STORY")"New short story" else "New novel";NovelLevel.CHAPTERS->"New chapter";else->"New scene"})},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(newTitle,{newTitle=it},label={Text("Name")},singleLine=true)
                if(level==NovelLevel.WORLDS)OutlinedTextField(extra,{extra=it},label={Text("Description")})
                if(level==NovelLevel.BOOKS){
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        FilterChip(selected=storyType=="NOVEL",onClick={storyType="NOVEL"},label={Text("Novel")})
                        FilterChip(selected=storyType=="SHORT_STORY",onClick={storyType="SHORT_STORY"},label={Text("Short story")})
                    }
                    Box{
                        OutlinedButton(onClick={seriesMenuExpanded=true},modifier=Modifier.fillMaxWidth()){
                            Text("Series: ${series.firstOrNull{it.optString("id")==selectedSeriesId}?.optString("title")?: "Standalone"}")
                        }
                        DropdownMenu(expanded=seriesMenuExpanded,onDismissRequest={seriesMenuExpanded=false}){
                            DropdownMenuItem(text={Text("Standalone · no series")},onClick={selectedSeriesId="";seriesMenuExpanded=false})
                            series.forEach{ser->DropdownMenuItem(text={Text(ser.optString("title"))},onClick={selectedSeriesId=ser.optString("id");seriesMenuExpanded=false})}
                        }
                    }
                    Box{
                        OutlinedButton(onClick={connectedMenuExpanded=true},modifier=Modifier.fillMaxWidth()){
                            Text("Connected to: ${nodes.firstOrNull{it.id==connectedBookId}?.title?: "No book"}")
                        }
                        DropdownMenu(expanded=connectedMenuExpanded,onDismissRequest={connectedMenuExpanded=false}){
                            DropdownMenuItem(text={Text("No connected book")},onClick={connectedBookId="";connectedMenuExpanded=false})
                            nodes.filter{it.id!=bookId}.forEach{other->DropdownMenuItem(text={Text(other.title)},onClick={
                                connectedBookId=other.id;connectedMenuExpanded=false
                                if(selectedSeriesId.isBlank())selectedSeriesId=runCatching{JSONObject(other.metadata).optString("series_id")}.getOrDefault("")
                            })}
                        }
                    }
                    OutlinedButton(onClick={coverPicker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Default.Image,null);Spacer(Modifier.width(8.dp));Text(if(coverUri.isBlank())"Choose cover artwork" else "Change cover artwork")
                    }
                }
                if(level==NovelLevel.CHAPTERS)OutlinedTextField(extra,{extra=it},label={Text("Custom status label")})
            }},
            confirmButton={TextButton(onClick={
                if(newTitle.isNotBlank()){
                    when(level){
                        NovelLevel.WORLDS->{worldId=store.createUniverse(newTitle,description=extra);parentTitle=newTitle;level=NovelLevel.BOOKS}
                        NovelLevel.BOOKS->store.createBook(worldId,newTitle,storyType=storyType,seriesId=selectedSeriesId.takeIf{it.isNotBlank()},connectedBookId=connectedBookId.takeIf{it.isNotBlank()},coverUri=coverUri)
                        NovelLevel.CHAPTERS->store.createChapter(bookId,newTitle,extra)
                        NovelLevel.SCENES->store.createScene(chapterId,newTitle)
                        else->Unit
                    }
                    refresh++;showCreate=false;storyType="NOVEL";selectedSeriesId="";connectedBookId="";coverUri=""
                }
            }){Text("Create")}},
            dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}}
        )
    }
}
