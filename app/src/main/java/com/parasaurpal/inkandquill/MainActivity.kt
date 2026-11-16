package com.parasaurpal.inkandquill

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import com.parasaurpal.inkandquill.data.*
import com.parasaurpal.inkandquill.ui.RichTextEditor
import kotlinx.coroutines.delay
import java.util.UUID

private val Plum=Color(0xFFC9B6E4)
private val Night=Color(0xFF17141E)
private val Paper=Color(0xFFF2EAF8)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{InkAndQuillApp(InkDatabase(this))}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InkAndQuillApp(db:InkDatabase){
    val context=LocalContext.current
    var screen by remember{mutableStateOf("Home")}
    var selected by remember{mutableStateOf<InkRecord?>(null)}
    var refresh by remember{mutableIntStateOf(0)}
    var query by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("All")}
    var create by remember{mutableStateOf(false)}
    var settings by remember{mutableStateOf(false)}
    var fontSize by remember{mutableFloatStateOf(db.setting("fontSize","18").toFloatOrNull()?:18f)}
    var appearance by remember{mutableStateOf(db.setting("appearance","dark"))}
    val dark=when(appearance){"light"->false;"dark"->true;else->isSystemInDarkTheme()}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(db.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{r->db.importJson(r.readText())};refresh++}}}
    MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Plum,background=Night,surface=Color(0xFF25212F),onBackground=Paper,onSurface=Paper)else lightColorScheme(primary=Color(0xFF72558F),background=Color(0xFFF5F0E8),surface=Color.White)){
        Scaffold(
            containerColor=MaterialTheme.colorScheme.background,
            topBar={TopAppBar(title={Text(if(selected!=null)selected!!.title.ifBlank{"Writing"} else "Ink & Quill",fontWeight=FontWeight.SemiBold)},navigationIcon={if(selected!=null)IconButton(onClick={selected=null;refresh++}){Icon(Icons.Default.ArrowBack,"Back")}},actions={if(selected==null)IconButton(onClick={settings=true}){Icon(Icons.Default.Settings,"Settings")}})},
            bottomBar={if(selected==null)NavigationBar{listOf("Home","Library","Worlds","Templates","Settings").forEach{tab->NavigationBarItem(selected=screen==tab,onClick={if(tab=="Settings")settings=true else screen=tab},icon={Icon(when(tab){"Home"->Icons.Default.Home;"Library"->Icons.Default.MenuBook;"Worlds"->Icons.Default.Public;"Templates"->Icons.Default.Dashboard;else->Icons.Default.Settings},tab)},label={Text(tab)})}}}
        ){padding->
            Box(Modifier.fillMaxSize().padding(padding)){
                if(selected!=null)EditorScreen(db,selected!!,fontSize,onSaved={record->db.save(record);selected=db.get(record.id);refresh++},onDelete={id->db.delete(id);selected=null;refresh++},onLinkScan={record->val hits=LocalLinkEngine(db).scan(record);refresh++;hits.size}) else when(screen){
                    "Home"->HomeScreen(db,refresh,{selected=it},{create=true})
                    "Library"->LibraryScreen(db,query,{query=it},filter,{filter=it},refresh){selected=it}
                    "Worlds"->WorldScreen(db,refresh,{selected=it},{create=true})
                    "Templates"->TemplateScreen(db,refresh,{selected=it},{template->selected=InkRecord(title=template.title,body=template.body,kind="Document")})
                    else->HomeScreen(db,refresh,{selected=it},{create=true})
                }
            }
        }
        if(create)CreateDialog(onDismiss={create=false},onChoose={kind->val record=InkRecord(title="Untitled $kind",kind=kind);db.save(record);selected=record;create=false;refresh++})
        if(settings)SettingsDialog(db,appearance,{appearance=it;db.setSetting("appearance",it)},fontSize,{fontSize=it;db.setSetting("fontSize",it.toInt().toString())},{export.launch("ink-and-quill-backup.json")},{importLauncher.launch(arrayOf("application/json","text/*"))}){settings=false}
    }
}

@Composable private fun HomeScreen(db:InkDatabase,refresh:Int,open:(InkRecord)->Unit,create:()->Unit){
    val recent=remember(refresh){db.all().take(8)}
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item{Text("A quiet place for big ideas.",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Your words, worlds, and everything between.",color=MaterialTheme.colorScheme.onSurface.copy(alpha=.7f))}
        item{Button(onClick=create,Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text("  Create new")}}
        item{Text("Your studio",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)}
        items(listOf("Novels & Stories","Poetry","Journal","Documents","Notes","Creative Projects","Worldbuilding")){label->Card(Modifier.fillMaxWidth().clickable{create()}){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null)}}}
        item{Text("Recently edited",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)}
        if(recent.isEmpty())item{Text("Your writing appears here. Create a document, poem, or world to begin.")}
        items(recent){RecordCard(it,open)}
    }
}

@Composable private fun LibraryScreen(db:InkDatabase,query:String,setQuery:(String)->Unit,filter:String,setFilter:(String)->Unit,refresh:Int,open:(InkRecord)->Unit){
    val records=remember(query,filter,refresh){db.all(query,filter)}
    Column(Modifier.fillMaxSize().padding(14.dp)){
        OutlinedTextField(query,setQuery,Modifier.fillMaxWidth(),label={Text("Search your writing")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("All","Document","Novel","Story","Poem","Journal","Note","World","Series","Book","Chapter","Scene","Character","Location").forEach{FilterChip(filter==it,{setFilter(it)},label={Text(it)})}}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(records){RecordCard(it,open)}}
    }
}
@Composable private fun RecordCard(record:InkRecord,open:(InkRecord)->Unit){
    Card(Modifier.fillMaxWidth().clickable{open(record)}){
        Column(Modifier.padding(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Text(record.title.ifBlank{"Untitled"},Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Text(record.kind,color=Plum,fontSize=11.sp)}
            if(record.body.isNotBlank())Text(android.text.Html.fromHtml(record.body,android.text.Html.FROM_HTML_MODE_COMPACT).toString().take(150).replace('\n',' '),maxLines=2,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.7f))
            if(record.tags.isNotBlank())Text(record.tags,color=Plum,fontSize=11.sp)
        }
    }
}

@Composable private fun WorldScreen(db:InkDatabase,refresh:Int,open:(InkRecord)->Unit,create:()->Unit){
    val worlds=remember(refresh){db.all().filter{it.kind=="World"}}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("Worlds",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("Each world connects its series, standalone stories, characters, places, cultures, lore, and timelines.")
        Button(onClick=create){Icon(Icons.Default.Add,null);Text(" Create a world")}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(worlds){world->RecordCard(world,open);db.all().filter{it.parentId==world.id}.forEach{child->Row(Modifier.padding(start=18.dp)){RecordCard(child,open)}}}}
        if(worlds.isEmpty())Text("No worlds yet. Create a World to begin your story universe.",Modifier.padding(12.dp))
    }
}

@Composable private fun TemplateScreen(db:InkDatabase,refresh:Int,edit:(InkRecord)->Unit,use:(InkRecord)->Unit){
    val templates=remember(refresh){db.templates()}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Button(onClick={edit(InkRecord(title="My custom template",kind="Template",body="Section 1:\\n"))}){Text("Create custom template")}
        Text("Template gallery",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("Start with a structure, use it for a new record, or edit the template itself.")
        LazyColumn{items(templates){t->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(t.title,fontWeight=FontWeight.SemiBold);Text(t.body.take(120).replace('\n',' '),fontSize=12.sp);Row{TextButton(onClick={use(t)}){Text("Use")};TextButton(onClick={edit(t)}){Text("Edit template")}}}}}}
    }
}

@Composable private fun EditorScreen(db:InkDatabase,record:InkRecord,fontSize:Float,onSaved:(InkRecord)->Unit,onDelete:(String)->Unit,onLinkScan:(InkRecord)->Int){
    var title by remember(record.id){mutableStateOf(record.title)}
    var html by remember(record.id){mutableStateOf(record.body)}
    var tags by remember(record.id){mutableStateOf(record.tags)}
    var aliases by remember(record.id){mutableStateOf(record.aliases)}
    var parent by remember(record.id){mutableStateOf(record.parentId)}
    var scope by remember(record.id){mutableStateOf(record.scope)}
    var details by remember{mutableStateOf(false)}
    var history by remember{mutableStateOf(false)}
    var linkCount by remember{mutableIntStateOf(0)}
    val context=LocalContext.current
    val txtExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(DocumentExport.plain(html).toByteArray())}}}
    val mdExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(DocumentExport.markdown(html).toByteArray())}}}
    val htmlExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")){uri->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(DocumentExport.htmlDocument(title,html).toByteArray())}}}
    val pdfExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->uri?.let{runCatching{PdfExport.write(context.contentResolver,it,title,html)}}}
    val docxExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")){uri->uri?.let{runCatching{PortableExport.docx(context.contentResolver,it,title,html)}}}
    val epubExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")){uri->uri?.let{runCatching{PortableExport.epub(context.contentResolver,it,title,html)}}}
    val documentImport=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{val raw=context.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->reader.readText()}?:return@let;val mime=context.contentResolver.getType(it).orEmpty();html=when{mime.contains("wordprocessingml")->DocumentExport.plainToHtml(PortableExport.extractDocx(context.contentResolver,it));mime.contains("epub")->DocumentExport.plainToHtml(PortableExport.extractEpub(context.contentResolver,it));mime.contains("markdown")->DocumentExport.markdownToHtml(raw);mime.contains("html")->raw;else->DocumentExport.plainToHtml(raw)}}}}
    LaunchedEffect(record.id,title,html,tags,aliases,parent,scope){delay(700);val updated=record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope);onSaved(updated);if(db.setting("autoLink","suggest")=="auto")LocalLinkEngine(db).scan(updated)}
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(record.kind,color=Plum,modifier=Modifier.weight(1f))
            TextButton(onClick={onSaved(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope))}){Text("Save")}
            IconButton(onClick={onSaved(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope,favorite=!record.favorite))}){Icon(if(record.favorite)Icons.Default.Star else Icons.Default.StarBorder,"Favorite")}
            IconButton(onClick={details=!details}){Icon(Icons.Default.Tune,"Details")}
            IconButton(onClick={history=!history}){Icon(Icons.Default.History,"History")}
            IconButton(onClick={onDelete(record.id)}){Icon(Icons.Default.Delete,"Delete")}
        }
        BasicTextField(title,{title=it},Modifier.fillMaxWidth().padding(vertical=8.dp),textStyle=MaterialTheme.typography.headlineMedium.copy(color=MaterialTheme.colorScheme.onBackground,fontWeight=FontWeight.Bold),decorationBox={inner->if(title.isEmpty())Text("Untitled",style=MaterialTheme.typography.headlineMedium,color=Color.Gray);inner()})
        Row(verticalAlignment=Alignment.CenterVertically){Text("Word count: ${android.text.Html.fromHtml(html,android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim().split(Regex("\\s+")).count{it.isNotBlank()}}",fontSize=11.sp,color=Plum);Spacer(Modifier.weight(1f));TextButton(onClick={linkCount=onLinkScan(record.copy(title=title,body=html,aliases=aliases))}){Text("Scan mentions")};if(linkCount>0)Text("$linkCount found",fontSize=11.sp,color=Plum)}
        HorizontalDivider()
        RichTextEditor(value=html,onValueChange={html=it},modifier=Modifier.weight(1f).fillMaxWidth(),fontSize=fontSize)
        if(details)Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
            OutlinedTextField(tags,{tags=it},Modifier.fillMaxWidth(),label={Text("Tags (comma separated)")})
            OutlinedTextField(aliases,{aliases=it},Modifier.fillMaxWidth(),label={Text("Aliases (comma separated)")})
            OutlinedTextField(parent,{parent=it},Modifier.fillMaxWidth(),label={Text("Parent record ID (World/Series/Story/Chapter)")})
            OutlinedTextField(scope,{scope=it},Modifier.fillMaxWidth(),label={Text("Sharing scope: World / Series / Story")})
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={documentImport.launch(arrayOf("text/plain","text/markdown","text/html","application/xhtml+xml","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/epub+zip"))}){Text("Import")};TextButton(onClick={txtExport.launch(title.ifBlank{"Untitled"}+".txt")}){Text("TXT")};TextButton(onClick={mdExport.launch(title.ifBlank{"Untitled"}+".md")}){Text("Markdown")};TextButton(onClick={htmlExport.launch(title.ifBlank{"Untitled"}+".html")}){Text("HTML")};TextButton(onClick={pdfExport.launch(title.ifBlank{"Untitled"}+".pdf")}){Text("PDF")};TextButton(onClick={docxExport.launch(title.ifBlank{"Untitled"}+".docx")}){Text("DOCX")};TextButton(onClick={epubExport.launch(title.ifBlank{"Untitled"}+".epub")}){Text("EPUB")}}
            Text("Backlinks",fontWeight=FontWeight.SemiBold)
            db.backlinks(record.id).forEach{Text("• ${it.first} — ${it.second}",fontSize=12.sp)}
        }
        if(history)Column{Text("Recent versions",fontWeight=FontWeight.SemiBold);db.revisions(record.id).take(5).forEach{rev->Row(verticalAlignment=Alignment.CenterVertically){Text("• ${rev.first} · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(rev.third))}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={title=rev.first;html=rev.second}){Text("Restore")}}}}
        Text("Offline · stored on this device",Modifier.padding(vertical=5.dp),fontSize=11.sp,color=Plum)
    }
}

@Composable private fun CreateDialog(onDismiss:()->Unit,onChoose:(String)->Unit){
    val kinds=listOf("Document","Novel","Series","Story","Book","Chapter","Scene","Poem","Journal","Note","World","Character","Location","Culture","Faction","Organization","Species","Creature","Magic System","Artifact","Technology","Religion","Deity","Language","Timeline Event","Research Note")
    AlertDialog(onDismissRequest=onDismiss,title={Text("Create something")},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState())){kinds.forEach{TextButton(onClick={onChoose(it)}){Text(it)}}}},confirmButton={TextButton(onClick=onDismiss){Text("Close")}})
}

@Composable private fun SettingsDialog(db:InkDatabase,appearance:String,setAppearance:(String)->Unit,size:Float,setSize:(Float)->Unit,export:()->Unit,importBackup:()->Unit,close:()->Unit){
    AlertDialog(onDismissRequest=close,title={Text("Studio settings")},text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("Appearance");Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("system","light","dark").forEach{FilterChip(appearance==it,{setAppearance(it)},label={Text(it.replaceFirstChar(Char::uppercase))})}}
        Text("Editor size: ${size.toInt()}sp");Slider(size,setSize,valueRange=14f..30f)
        Text("Automatic mention links");var mode by remember{mutableStateOf(db.setting("autoLink","suggest"))};Row{listOf("off","suggest","auto").forEach{FilterChip(mode==it,{mode=it;db.setSetting("autoLink",it)},label={Text(it)})}}
        Text("Your writing remains in the app's local database. Internet is not needed.")
        TextButton(onClick=export){Text("Export JSON backup")};TextButton(onClick=importBackup){Text("Import JSON backup")}
    }},confirmButton={TextButton(onClick=close){Text("Done")}})
}
