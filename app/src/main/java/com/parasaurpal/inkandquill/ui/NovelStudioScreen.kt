package com.parasaurpal.inkandquill.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var worldTitle by remember{mutableStateOf("")}
    var bookTitle by remember{mutableStateOf("")}
    var chapterTitle by remember{mutableStateOf("")}
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
    var sidebarOpen by remember{mutableStateOf(false)}
    var sidebarSearch by remember{mutableStateOf("")}
    var sidebarSelectedId by remember{mutableStateOf("")}
    var sidebarSelectedTitle by remember{mutableStateOf("")}
    var sidebarSelectedType by remember{mutableStateOf("")}
    var sidebarSelectedDescription by remember{mutableStateOf("")}
    var sidebarSelectedFields by remember{mutableStateOf("")}
    var sidebarDrag by remember{mutableFloatStateOf(0f)}
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
    var editingSeriesId by remember{mutableStateOf("")}
    var editingBookId by remember{mutableStateOf("")}
    var expandedSeries by remember{mutableStateOf(setOf<String>())}
    var seriesMenuExpanded by remember{mutableStateOf(false)}
    var connectedMenuExpanded by remember{mutableStateOf(false)}
    val bannerPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};seriesBanner=it.toString();if(editingSeriesId.isNotBlank()){store.updateSeriesBanner(editingSeriesId,it.toString());refresh++}}}
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};coverUri=it.toString();if(editingBookId.isNotBlank()){store.updateBookCover(editingBookId,it.toString());refresh++}}}
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{out->out.write(store.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->store.importJson(reader.readText())};refresh++}}}
    val textExportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use{writer->writer.write(prose)}}}
    val sidebarContextType=if(level==NovelLevel.EDITOR) "scene" else "chapter"
    val sidebarContextId=if(level==NovelLevel.EDITOR) sceneId else chapterId
    val resourceUnion = """
        SELECT id,'entity' AS kind,entity_type AS type,title,description,fields_json AS fields FROM entities WHERE universe_id='$worldId'
        UNION ALL SELECT id,'timeline','Timeline',name,custom_description,'{}' FROM timelines WHERE universe_id='$worldId'
        UNION ALL SELECT m.id,'moment','Timeline moment',m.title,COALESCE(m.date_or_era_text,'')||' '||COALESCE(m.custom_text,''),'{}' FROM moments m JOIN timelines t ON t.id=m.timeline_id WHERE t.universe_id='$worldId'
        UNION ALL SELECT id,'board','Board',title,board_type,'{}' FROM boards WHERE universe_id='$worldId'
        UNION ALL SELECT bo.id,'board_card','Board card',substr(bo.payload_json,1,80),b.title,bo.payload_json FROM board_objects bo JOIN boards b ON b.id=bo.board_id WHERE b.universe_id='$worldId'
        UNION ALL SELECT id,'prompt','Writing prompt',prompt_text,source,'{}' FROM prompt_entries WHERE universe_id='$worldId' OR universe_id IS NULL
        UNION ALL SELECT id,'goal','Writing goal',period||' writing goal',CAST(target_words AS TEXT)||' words','{}' FROM writing_goals WHERE universe_id='$worldId'
    """.trimIndent()
    val linkedWorldItems=remember(worldId,sidebarContextType,sidebarContextId,refresh,sidebarOpen){
        if(sidebarContextId.isBlank()) emptyList() else store.readableDatabase.rawQuery(
            "SELECT r.id,r.kind,r.type,r.title,r.description,r.fields FROM entity_links l JOIN ($resourceUnion) r ON ((l.target_type=r.kind AND l.target_id=r.id) OR (l.source_type=r.kind AND l.source_id=r.id)) WHERE (l.source_type=? AND l.source_id=?) OR (l.target_type=? AND l.target_id=?) ORDER BY r.title",
            arrayOf(sidebarContextType,sidebarContextId,sidebarContextType,sidebarContextId)
        ).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("kind",c.getString(1)).put("type",c.getString(2)).put("title",c.getString(3)).put("description",c.getString(4)).put("fields",c.getString(5)))}}
    }
    val sidebarSearchResults=remember(worldId,sidebarSearch,refresh,sidebarOpen){
        if(sidebarSearch.isBlank()) emptyList() else store.readableDatabase.rawQuery(
            "SELECT id,kind,type,title,description,fields FROM ($resourceUnion) WHERE title LIKE ? OR description LIKE ? OR type LIKE ? OR fields LIKE ? ORDER BY title LIMIT 80",
            arrayOf("%$sidebarSearch%","%$sidebarSearch%","%$sidebarSearch%","%$sidebarSearch%")
        ).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("kind",c.getString(1)).put("type",c.getString(2)).put("title",c.getString(3)).put("description",c.getString(4)).put("fields",c.getString(5)))}}
    }
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

    // Keep standalone novels and short stories visibly distinct from series.
    // Stable book IDs preserve existing manuscripts and chapter/scene links.
    val standaloneNovels=remember(nodes){
        nodes.filter { node ->
            val meta=runCatching{JSONObject(node.metadata)}.getOrDefault(JSONObject())
            meta.optString("series_id").isBlank() && meta.optString("story_type")!="SHORT_STORY"
        }
    }
    val standaloneStories=remember(nodes){
        nodes.filter { node ->
            val meta=runCatching{JSONObject(node.metadata)}.getOrDefault(JSONObject())
            meta.optString("series_id").isBlank() && meta.optString("story_type")=="SHORT_STORY"
        }
    }
    fun loadScene(node:StudioNode){
        sceneId=node.id;title=node.title
        store.readableDatabase.rawQuery("SELECT s.summary,s.goal,s.conflict,s.hook,d.content_json,d.plain_text FROM scenes s LEFT JOIN documents d ON d.scene_id=s.id WHERE s.id=?",arrayOf(node.id)).use{c->
            if(c.moveToFirst()){
                summary=c.getString(0)?: "";goal=c.getString(1)?: "";conflict=c.getString(2)?: "";hook=c.getString(3)?: ""
                prose=c.getString(5)?: ""
                fontChoice=runCatching{JSONObject(c.getString(4)?: "{}").optString("font","Literata")}.getOrDefault("Literata").ifBlank{"Literata"}
                val stored=runCatching{JSONObject(c.getString(4)?: "{}")}.getOrDefault(JSONObject())
                editorHtml=stored.optString("html").ifBlank{android.text.Html.escapeHtml(prose).replace("\n","<br>")}
            }
        }
        level=NovelLevel.EDITOR;saved=false
    }
    LaunchedEffect(sceneId,title,editorHtml,summary,goal,conflict,hook,fontChoice) {
        if(sceneId.isNotBlank()) {
            kotlinx.coroutines.delay(650)
            val meta=JSONObject().put("summary",summary).put("goal",goal).put("conflict",conflict).put("hook",hook)
            val plain=DocumentExport.plain(editorHtml)
            prose=plain
            val content=JSONObject().put("format_version",2).put("html",editorHtml).put("font",fontChoice)
            store.saveScene(sceneId,title,meta,content,plain)
            saved=true
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pointerInput(sidebarOpen){
        detectHorizontalDragGestures(onHorizontalDrag={change,amount->if(!sidebarOpen && (level==NovelLevel.EDITOR || level==NovelLevel.SCENES))sidebarDrag+=amount},onDragEnd={if(sidebarDrag>85f && (level==NovelLevel.EDITOR || level==NovelLevel.SCENES))sidebarOpen=true;sidebarDrag=0f})
    }){
    Column(Modifier.fillMaxSize()){
        if(!focus) TopAppBar(
            title={Column{Text("Novel Studio",fontWeight=FontWeight.Bold);Text(when(level){NovelLevel.WORLDS->"Universes";NovelLevel.BOOKS,NovelLevel.CHAPTERS,NovelLevel.SCENES->parentTitle;NovelLevel.EDITOR->title},style=MaterialTheme.typography.labelSmall)}},
            navigationIcon={IconButton(onClick={
                when(level){NovelLevel.WORLDS->onClose();NovelLevel.BOOKS->level=NovelLevel.WORLDS;NovelLevel.CHAPTERS->level=NovelLevel.BOOKS;NovelLevel.SCENES->level=NovelLevel.CHAPTERS;NovelLevel.EDITOR->level=NovelLevel.SCENES}
            }){Icon(if(level==NovelLevel.WORLDS)Icons.Default.Close else Icons.Default.ArrowBack,"Back")}},
            actions={
                if(level==NovelLevel.WORLDS){IconButton(onClick={exportLauncher.launch("InkAndQuill.fortelling.json")}){Icon(Icons.Default.FileDownload,"Export project")};IconButton(onClick={importLauncher.launch(arrayOf("application/json","text/*"))}){Icon(Icons.Default.FileUpload,"Import project")}}
                if(level==NovelLevel.BOOKS){IconButton(onClick={seriesTitle="";seriesBanner="";editingSeriesId="";showSeriesCreate=true}){Icon(Icons.Default.CollectionsBookmark,"New series")}}
                if(level!=NovelLevel.WORLDS){IconButton(onClick={toolsOpen=true}){Icon(Icons.Default.Explore,"Worldbuilding and planning")}}
                if(level==NovelLevel.EDITOR){IconButton(onClick={textExportLauncher.launch((title.ifBlank{"Scene"}).replace(Regex("[^a-zA-Z0-9_-]"),"_")+".txt")}){Icon(Icons.Default.TextSnippet,"Export scene as text")};IconButton(onClick={focus=!focus}){Icon(Icons.Default.CenterFocusStrong,if(focus)"Exit focus mode" else "Focus mode")}}
                if(level!=NovelLevel.EDITOR) IconButton(onClick={newTitle="";extra="";showCreate=true}){Icon(Icons.Default.Add,"Add")}
            }
        ) else if(level==NovelLevel.EDITOR){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.End){TextButton(onClick={focus=false}){Text("Exit focus mode")}}}
        if(!focus && level!=NovelLevel.WORLDS){
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=14.dp, vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Text("Universes",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium,modifier=Modifier.clickable{level=NovelLevel.WORLDS})
                Text("›",color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(worldTitle.ifBlank{"World"},color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium,modifier=Modifier.clickable{level=NovelLevel.BOOKS})
                if(level==NovelLevel.CHAPTERS || level==NovelLevel.SCENES || level==NovelLevel.EDITOR){
                    Text("›",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(bookTitle.ifBlank{"Books"},color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium,modifier=Modifier.clickable{level=NovelLevel.CHAPTERS})
                }
                if(level==NovelLevel.SCENES || level==NovelLevel.EDITOR){
                    Text("›",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(chapterTitle.ifBlank{"Chapters"},color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium,modifier=Modifier.clickable{level=NovelLevel.SCENES})
                }
                if(level==NovelLevel.EDITOR){Text("›",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(title.ifBlank{"Scene"},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
        if(toolsOpen){NovelStudioToolsScreen(worldId,store){toolsOpen=false;refresh++}} else when(level){
            NovelLevel.WORLDS->{
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    item{Text("Build a world. Write its stories.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Offline-first. Your work stays on this device.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    items(worlds){w->
                        Surface(Modifier.fillMaxWidth().clickable{worldId=w.getString("id");worldTitle=w.getString("title");parentTitle=worldTitle;level=NovelLevel.BOOKS},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface){
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
                                            IconButton(onClick={editingSeriesId=sid;bannerPicker.launch(arrayOf("image/*"))},modifier=Modifier.align(Alignment.TopEnd)){Icon(Icons.Default.Image,"Change banner",tint=Color.White)}
                                        }
                                    }
                                    if(expanded) grouped.forEach { book -> StudioBookCard(book,nodes,{bookId=book.id;bookTitle=book.title;parentTitle=book.title;level=NovelLevel.CHAPTERS},{editingBookId=book.id;coverPicker.launch(arrayOf("image/*"))}) }
                                }
                            }
                        }
                        if(standaloneNovels.isNotEmpty()){
                            item(key="standalone-novels-heading"){
                                Text("Standalone novels",modifier=Modifier.padding(top=10.dp,bottom=2.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            }
                            items(standaloneNovels,key={it.id}){node->
                                StudioBookCard(node,nodes,{bookId=node.id;bookTitle=node.title;parentTitle=node.title;level=NovelLevel.CHAPTERS},{editingBookId=node.id;coverPicker.launch(arrayOf("image/*"))})
                            }
                        }
                        if(standaloneStories.isNotEmpty()){
                            item(key="standalone-stories-heading"){
                                Text("Standalone stories",modifier=Modifier.padding(top=10.dp,bottom=2.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            }
                            items(standaloneStories,key={it.id}){node->
                                StudioBookCard(node,nodes,{bookId=node.id;bookTitle=node.title;parentTitle=node.title;level=NovelLevel.CHAPTERS},{editingBookId=node.id;coverPicker.launch(arrayOf("image/*"))})
                            }
                        }
                    } else {
                        items(nodes,key={it.id}){node->
                            Surface(Modifier.fillMaxWidth().clickable{
                                parentTitle=node.title
                                when(level){NovelLevel.CHAPTERS->{chapterId=node.id;chapterTitle=node.title;level=NovelLevel.SCENES};NovelLevel.SCENES->loadScene(node);else->Unit}
                            },shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
                                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                                    Column(Modifier.weight(1f)){Text(node.title,fontWeight=FontWeight.Medium);if(node.customStatus.isNotBlank()||node.status.isNotBlank())Text(node.customStatus.ifBlank{node.status},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                                    Icon(Icons.Default.ChevronRight,"Open")
                                }
                            }
                        }
                    }
                    if(nodes.isEmpty()&&series.isEmpty())item{Text("Nothing here yet. Add the first item.")}
                }
                Button(onClick={newTitle="";extra="";showCreate=true},modifier=Modifier.fillMaxWidth().padding(16.dp)){Text(when(level){NovelLevel.BOOKS->"Add novel or story";NovelLevel.CHAPTERS->"Add chapter";else->"Add scene"})}
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
                    if(!focus) Row(verticalAlignment=Alignment.CenterVertically){
                        Text("TYPEFACE",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Box{
                            TextButton(onClick={fontMenuExpanded=true}){Text(fontChoice);Icon(Icons.Default.ArrowDropDown,null)}
                            DropdownMenu(expanded=fontMenuExpanded,onDismissRequest={fontMenuExpanded=false}){
                                StudioFonts.forEach{font->DropdownMenuItem(text={Text(font)},onClick={fontChoice=font;fontMenuExpanded=false;saved=false})}
                            }
                        }
                    }
                    RichTextEditor(
                        value=editorHtml,
                        onValueChange={editorHtml=it;prose=DocumentExport.plain(it);saved=false},
                        modifier=Modifier.weight(1f).fillMaxWidth(),
                        fontSize=18f,
                        fontFamily=fontChoice,
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
                OutlinedButton(onClick={editingSeriesId="";bannerPicker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.Image,null);Spacer(Modifier.width(8.dp));Text(if(seriesBanner.isBlank())"Choose banner image" else "Change banner image")
                }
            }},
            confirmButton={TextButton(onClick={if(seriesTitle.isNotBlank()){store.createSeries(worldId,seriesTitle,seriesBanner);refresh++;showSeriesCreate=false}}){Text("Create series")}},
            dismissButton={TextButton(onClick={showSeriesCreate=false}){Text("Cancel")}}
        )
    }
    if(sidebarOpen && (level==NovelLevel.EDITOR || level==NovelLevel.SCENES)){
        Surface(Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max=380.dp).fillMaxWidth(.88f),color=MaterialTheme.colorScheme.surface,shadowElevation=18.dp,shape=RoundedCornerShape(topStart=20.dp,bottomStart=20.dp)){
            Column(Modifier.fillMaxSize().padding(14.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    if(sidebarSelectedId.isNotBlank())IconButton(onClick={sidebarSelectedId=""}){Icon(Icons.Default.ArrowBack,"Back to linked items")}
                    Column(Modifier.weight(1f)){Text(if(sidebarSelectedId.isBlank())"Worldbuilding" else sidebarSelectedTitle,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text(if(sidebarSelectedId.isBlank())"Quick reference · "+worldTitle else sidebarSelectedType,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                    IconButton(onClick={sidebarOpen=false;sidebarSelectedId=""}){Icon(Icons.Default.Close,"Close sidebar")}
                }
                if(sidebarSelectedId.isNotBlank()){
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(sidebarSelectedDescription.isNotBlank())Text(sidebarSelectedDescription,style=MaterialTheme.typography.bodyMedium);if(sidebarSelectedFields.isNotBlank() && sidebarSelectedFields!="{}")Text(runCatching{JSONObject(sidebarSelectedFields).toString(2)}.getOrDefault(sidebarSelectedFields),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }else{
                    Text(if(sidebarContextId.isBlank())"Open a chapter or scene to see its linked worldbuilding items." else "Added to this "+(if(sidebarContextType=="scene")"scene" else "chapter"),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        items(linkedWorldItems,key={it.optString("id")}){entry->
                            Surface(Modifier.fillMaxWidth().clickable{sidebarSelectedId=entry.optString("id");sidebarSelectedTitle=entry.optString("title");sidebarSelectedType=entry.optString("type");sidebarSelectedDescription=entry.optString("description");sidebarSelectedFields=entry.optString("fields");sidebarSelectedFields=entry.optString("fields")},shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant){
                                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(entry.optString("title"),fontWeight=FontWeight.SemiBold);Text(entry.optString("type"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)};Icon(Icons.Default.ChevronRight,null)}
                            }
                        }
                        if(linkedWorldItems.isEmpty())item{Text("No worldbuilding items linked here yet. Search below to add characters, locations, factions, objects and more.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        if(sidebarSearch.isNotBlank())item{Text("Search results",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)}
                        items(sidebarSearchResults,key={"search-"+it.optString("id")}){entry->
                            val alreadyLinked=linkedWorldItems.any{it.optString("id")==entry.optString("id") && it.optString("kind")==entry.optString("kind")}
                            Surface(Modifier.fillMaxWidth().clickable{sidebarSelectedId=entry.optString("id");sidebarSelectedTitle=entry.optString("title");sidebarSelectedType=entry.optString("type");sidebarSelectedDescription=entry.optString("description");sidebarSelectedFields=entry.optString("fields")},shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceVariant){
                                Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(entry.optString("title"),fontWeight=FontWeight.Medium);Text(entry.optString("type"),style=MaterialTheme.typography.labelSmall)};if(!alreadyLinked)TextButton(onClick={store.link(sidebarContextType,sidebarContextId,entry.optString("kind"),entry.optString("id"),"Appears in");refresh++}){Text("Add")}else Icon(Icons.Default.Check,"Added")}
                            }
                        }
                    }
                    OutlinedTextField(sidebarSearch,{sidebarSearch=it},modifier=Modifier.fillMaxWidth().padding(top=10.dp),singleLine=true,label={Text("Search all worldbuilding")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(sidebarSearch.isNotBlank())IconButton(onClick={sidebarSearch=""}){Icon(Icons.Default.Close,"Clear search")}})
                }
            }
        }
    }
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
                    OutlinedButton(onClick={editingBookId="";coverPicker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()){
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


@Composable
private fun StudioArtwork(uri:String,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val bitmap=remember(uri){runCatching{context.contentResolver.openInputStream(Uri.parse(uri))?.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()}
    if(bitmap!=null) Image(bitmap=bitmap,contentDescription=null,modifier=modifier,contentScale=ContentScale.Crop)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoStories,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}
}

@Composable
private fun StudioBookCard(node:StudioNode,allBooks:List<StudioNode>,onOpen:()->Unit,onChangeCover:()->Unit){
    val meta=remember(node.metadata){runCatching{JSONObject(node.metadata)}.getOrDefault(JSONObject())}
    val cover=meta.optString("cover_uri")
    val type=if(meta.optString("story_type")=="SHORT_STORY")"Short story" else "Novel"
    val connected=allBooks.firstOrNull{it.id==meta.optString("connected_book_id")}
    Surface(Modifier.fillMaxWidth().clickable(onClick=onOpen),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.width(82.dp).height(116.dp).clip(RoundedCornerShape(7.dp))){
                if(cover.isNotBlank())StudioArtwork(cover,Modifier.fillMaxSize()) else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center){Icon(Icons.Default.MenuBook,null,modifier=Modifier.size(30.dp))}
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(node.title,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
                Text(type,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)
                if(connected!=null)Text("Side story connected to ${connected.title}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Open manuscript →",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick=onChangeCover){Icon(Icons.Default.Image,"Change cover")}
        }
    }
}
