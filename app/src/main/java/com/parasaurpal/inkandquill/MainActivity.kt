package com.parasaurpal.inkandquill

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
    var cloudPassword by remember{mutableStateOf("")}
    var fontSize by remember{mutableFloatStateOf(db.setting("fontSize","18").toFloatOrNull()?:18f)}
    var appearance by remember{mutableStateOf(db.setting("appearance","dark"))}
    var palette by remember{mutableStateOf(db.setting("palette","Plum"))}
    val accent=when(palette){"Ocean"->Color(0xFF8DB9D8);"Forest"->Color(0xFF9DBD9A);"Rose"->Color(0xFFE0A8B8);"Amber"->Color(0xFFD9B66F);else->Plum}
    val dark=when(appearance){"light"->false;"dark"->true;else->isSystemInDarkTheme()}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(db.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{r->db.importJson(r.readText())};refresh++}}}
    val cloudExportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri:Uri?->uri?.let{
        val result=runCatching{EncryptedCloudBackup.write(context.contentResolver,it,db.exportJson(),cloudPassword)}
        cloudPassword=""
        Toast.makeText(context,result.fold({"Encrypted cloud backup saved."},{it.message?: "Backup failed."}),Toast.LENGTH_LONG).show()
    }}
    val cloudImportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{
        val result=runCatching{db.importJson(EncryptedCloudBackup.read(context.contentResolver,it,cloudPassword));refresh++}
        cloudPassword=""
        Toast.makeText(context,result.fold({"Encrypted backup restored."},{it.message?: "Restore failed."}),Toast.LENGTH_LONG).show()
    }}
    MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=accent,background=Night,surface=Color(0xFF25212F),onBackground=Paper,onSurface=Paper)else lightColorScheme(primary=accent,background=Color(0xFFF5F0E8),surface=Color.White)){
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
                    "Templates"->TemplateScreen(db,refresh,{selected=it},{template->val fields=db.templateFields(template.id);val structured=fields.joinToString("\n\n"){f->"## ${f.section}\n${f.name}:\n"};selected=InkRecord(title=template.title,body=if(fields.isEmpty())template.body else structured,kind="Document")})
                    else->HomeScreen(db,refresh,{selected=it},{create=true})
                }
            }
        }
        if(create)CreateDialog(onDismiss={create=false},onChoose={kind->val record=InkRecord(title="Untitled $kind",kind=kind);db.save(record);selected=record;create=false;refresh++})
        if(settings)SettingsDialog(db,appearance,{appearance=it;db.setSetting("appearance",it)},palette,{palette=it;db.setSetting("palette",it)},fontSize,{fontSize=it;db.setSetting("fontSize",it.toInt().toString())},{export.launch("ink-and-quill-backup.json")},{importLauncher.launch(arrayOf("application/json","text/*"))},{password->cloudPassword=password;cloudExportLauncher.launch("InkAndQuill-encrypted-backup.inkq")},{password->cloudPassword=password;cloudImportLauncher.launch(arrayOf("application/octet-stream","application/*","*/*"))}){settings=false}
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
    var folderFilter by remember{mutableStateOf("")}
    var folderMenu by remember{mutableStateOf(false)}
    var createFolder by remember{mutableStateOf(false)}
    var newFolderName by remember{mutableStateOf("")}
    val folders=db.folders()
    val records=remember(query,filter,refresh,folderFilter){db.all(query,filter).filter{folderFilter.isBlank()||db.folderFor(it.id)==folderFilter}}
    Column(Modifier.fillMaxSize().padding(14.dp)){
        OutlinedTextField(query,setQuery,Modifier.fillMaxWidth(),label={Text("Search your writing")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        Row(verticalAlignment=Alignment.CenterVertically){Box{OutlinedButton(onClick={folderMenu=true}){Text("Folder: "+(folders.firstOrNull{it.first==folderFilter}?.second?: "All"))};DropdownMenu(expanded=folderMenu,onDismissRequest={folderMenu=false}){DropdownMenuItem(text={Text("All folders")},onClick={folderFilter="";folderMenu=false});folders.forEach{f->DropdownMenuItem(text={Text(f.second)},onClick={folderFilter=f.first;folderMenu=false})}}};Spacer(Modifier.weight(1f));TextButton(onClick={createFolder=true}){Text("+ New folder")}}
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("All","Document","Novel","Story","Poem","Journal","Note","World","Series","Book","Chapter","Scene","Character","Location").forEach{FilterChip(filter==it,{setFilter(it)},label={Text(it)})}}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(records){RecordCard(it,open)}}
    }
    if(createFolder)AlertDialog(onDismissRequest={createFolder=false},title={Text("New folder")},text={OutlinedTextField(newFolderName,{newFolderName=it},label={Text("Folder name")},singleLine=true)},confirmButton={TextButton(onClick={if(newFolderName.isNotBlank()){db.createFolder(newFolderName);newFolderName="";createFolder=false}}){Text("Create")}},dismissButton={TextButton(onClick={createFolder=false}){Text("Cancel")}})
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
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(worlds){world->RecordCard(world,open);WorldTreeChildren(db,world.id,1,open)}}
        if(worlds.isEmpty())Text("No worlds yet. Create a World to begin your story universe.",Modifier.padding(12.dp))
    }
}

@Composable private fun WorldTreeChildren(db:InkDatabase,parentId:String,depth:Int,open:(InkRecord)->Unit){
    if(depth>8)return
    val children=db.all().filter{it.parentId==parentId}
    children.forEach{child->Column(Modifier.padding(start=(depth*12).dp)){RecordCard(child,open);WorldTreeChildren(db,child.id,depth+1,open)}}
}

@Composable private fun TemplateScreen(db:InkDatabase,refresh:Int,edit:(InkRecord)->Unit,use:(InkRecord)->Unit){
    val templates=remember(refresh){db.templates()}
    var selectedTemplate by remember{mutableStateOf<InkRecord?>(null)}
    var fieldRefresh by remember{mutableIntStateOf(0)}
    var addField by remember{mutableStateOf(false)}
    var fieldName by remember{mutableStateOf("")}
    var fieldSection by remember{mutableStateOf("General")}
    var fieldOptions by remember{mutableStateOf("")}
    var fieldHint by remember{mutableStateOf("")}
    var fieldType by remember{mutableStateOf("Long text")}
    var required by remember{mutableStateOf(false)}
    var typeMenu by remember{mutableStateOf(false)}
    val types=listOf("Short text","Long text","Rich text","Number","Date","Dropdown","Multi-select","Checkbox","Image","Gallery","Attachment","Color","Rating","Table","Relationship","Repeatable group","Calculated")
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Template gallery",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Button(onClick={val t=InkRecord(title="My custom template",kind="Template",body="");db.save(t);selectedTemplate=t;fieldRefresh++}){Text("Create template")}}
        Text("Build reusable forms and writing structures. Fields can be removed from the active template without destroying their stored definitions.")
        if(selectedTemplate!=null){
            val t=selectedTemplate!!
            Row(verticalAlignment=Alignment.CenterVertically){Text(t.title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);TextButton(onClick={selectedTemplate=null}){Text("Done")}}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={db.templateVersion(t.id);addField=true}){Text("Add field")};TextButton(onClick={db.addTemplateSection(t.id,"Section ${db.templateSections(t.id).size+1}");fieldRefresh++}){Text("Add section")}}
            Text("Saved template versions: ${db.templateVersionCount(t.id)}",fontSize=12.sp)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){
                items(db.templateFields(t.id,true)){f->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(f.name,Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Text(f.fieldType,color=Plum,fontSize=11.sp)};Text("Section: ${f.section} · ${if(f.required)"Required" else "Optional"}",fontSize=11.sp);if(f.options.isNotBlank())Text("Options: ${f.options}",fontSize=11.sp);Row{if(f.id.isNotBlank()&&db.templateFields(t.id,false).none{it.id==f.id})TextButton(onClick={db.restoreTemplateField(f.id);fieldRefresh++}){Text("Restore") }else TextButton(onClick={db.removeTemplateField(f.id);fieldRefresh++}){Text("Remove (recoverable)")}}}}}
            }
        }else{
            LazyColumn{items(templates){t->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(t.title,fontWeight=FontWeight.SemiBold);Text(t.body.take(120).replace("\\n"," "),fontSize=12.sp);Text("${db.templateFields(t.id).size} custom fields",fontSize=11.sp,color=Plum);Row{TextButton(onClick={use(t)}){Text("Use")};TextButton(onClick={selectedTemplate=t;fieldRefresh++}){Text("Design fields")};TextButton(onClick={edit(t)}){Text("Edit content")}}}}}}
        }
    }
    if(addField)AlertDialog(onDismissRequest={addField=false},title={Text("Add template field")},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
        OutlinedTextField(fieldName,{fieldName=it},label={Text("Field name")},singleLine=true)
        Box{OutlinedButton(onClick={typeMenu=true},modifier=Modifier.fillMaxWidth()){Text("Type: $fieldType")};DropdownMenu(expanded=typeMenu,onDismissRequest={typeMenu=false}){types.forEach{DropdownMenuItem(text={Text(it)},onClick={fieldType=it;typeMenu=false})}}}
        OutlinedTextField(fieldSection,{fieldSection=it},label={Text("Section")},singleLine=true)
        OutlinedTextField(fieldHint,{fieldHint=it},label={Text("Help text / hint")})
        if(fieldType=="Dropdown"||fieldType=="Multi-select")OutlinedTextField(fieldOptions,{fieldOptions=it},label={Text("Options (comma separated)")})
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(required,{required=it});Text("Required field")}
    }},confirmButton={TextButton(onClick={if(fieldName.isNotBlank()){selectedTemplate?.let{db.addTemplateField(it.id,fieldName,fieldType,required,fieldSection,fieldOptions,fieldHint)};fieldName="";fieldHint="";fieldOptions="";required=false;addField=false;fieldRefresh++}}){Text("Add field")}},dismissButton={TextButton(onClick={addField=false}){Text("Cancel")}})
}
@Composable private fun EditorScreen(db:InkDatabase,record:InkRecord,fontSize:Float,onSaved:(InkRecord)->Unit,onDelete:(String)->Unit,onLinkScan:(InkRecord)->Int){
    var title by remember(record.id){mutableStateOf(record.title)}
    var html by remember(record.id){mutableStateOf(record.body)}
    var tags by remember(record.id){mutableStateOf(record.tags)}
    var aliases by remember(record.id){mutableStateOf(record.aliases)}
    var parent by remember(record.id){mutableStateOf(record.parentId)}
    var scope by remember(record.id){mutableStateOf(record.scope)}
    var folderId by remember(record.id){mutableStateOf(db.folderFor(record.id))}
    var folderMenu by remember{mutableStateOf(false)}
    var details by remember{mutableStateOf(false)}
    var history by remember{mutableStateOf(false)}
    var parentMenu by remember{mutableStateOf(false)}
    val latestRecordState=rememberUpdatedState(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope))
    DisposableEffect(record.id){onDispose{db.save(latestRecordState.value)}}
    var linkCount by remember{mutableIntStateOf(0)}
    var showFindReplace by remember{mutableStateOf(false)}
    var findText by remember{mutableStateOf("")}
    var replaceText by remember{mutableStateOf("")}
    var attachmentRefresh by remember{mutableIntStateOf(0)}
    val context=LocalContext.current
    val attachmentList=remember(record.id,attachmentRefresh){db.attachments(record.id)}
    val attachmentPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};db.addAttachment(record.id,it.toString(),context.contentResolver.getType(it).orEmpty(),it.lastPathSegment.orEmpty());attachmentRefresh++}}
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
            IconButton(onClick={showFindReplace=!showFindReplace}){Icon(Icons.Default.FindReplace,"Find and replace")}
            IconButton(onClick={history=!history}){Icon(Icons.Default.History,"History")}
            IconButton(onClick={onDelete(record.id)}){Icon(Icons.Default.Delete,"Delete")}
        }
        BasicTextField(title,{title=it},Modifier.fillMaxWidth().padding(vertical=8.dp),textStyle=MaterialTheme.typography.headlineMedium.copy(color=MaterialTheme.colorScheme.onBackground,fontWeight=FontWeight.Bold),decorationBox={inner->if(title.isEmpty())Text("Untitled",style=MaterialTheme.typography.headlineMedium,color=Color.Gray);inner()})
        Row(verticalAlignment=Alignment.CenterVertically){Text("Word count: ${android.text.Html.fromHtml(html,android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim().split(Regex("\\s+")).count{it.isNotBlank()}}",fontSize=11.sp,color=Plum);Spacer(Modifier.weight(1f));TextButton(onClick={linkCount=onLinkScan(record.copy(title=title,body=html,aliases=aliases))}){Text("Scan mentions")};if(linkCount>0)Text("$linkCount found",fontSize=11.sp,color=Plum)}
        if(showFindReplace)Column(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) { OutlinedTextField(findText,{findText=it},Modifier.fillMaxWidth(),label={Text("Find")},singleLine=true);OutlinedTextField(replaceText,{replaceText=it},Modifier.fillMaxWidth(),label={Text("Replace with")},singleLine=true);Button(onClick={val count=db.replaceInRecord(record.id,findText,replaceText);html=db.get(record.id)?.body?:html;Toast.makeText(context,"Replaced $count occurrence(s).",Toast.LENGTH_SHORT).show()},enabled=findText.isNotEmpty()){Text("Replace all in this document")} }
        HorizontalDivider()
        RichTextEditor(value=html,onValueChange={html=it},modifier=Modifier.weight(1f).fillMaxWidth(),fontSize=fontSize)
        if(details)Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
            OutlinedTextField(tags,{tags=it},Modifier.fillMaxWidth(),label={Text("Tags (comma separated)")})
            OutlinedTextField(aliases,{aliases=it},Modifier.fillMaxWidth(),label={Text("Aliases (comma separated)")})
            Box {
                OutlinedButton(onClick={parentMenu=true},modifier=Modifier.fillMaxWidth()){Text("Parent: "+(db.get(parent)?.title?: "None"))}
                DropdownMenu(expanded=parentMenu,onDismissRequest={parentMenu=false}){
                    DropdownMenuItem(text={Text("No parent")},onClick={parent="";parentMenu=false})
                    db.all().filter{it.id!=record.id&&it.kind in listOf("World","Series","Story","Book","Chapter")}.forEach{candidate->
                        DropdownMenuItem(text={Text(candidate.title+" · "+candidate.kind)},onClick={parent=candidate.id;parentMenu=false})
                    }
                }
            }
            Box { OutlinedButton(onClick={folderMenu=true},modifier=Modifier.fillMaxWidth()){Text("Folder: "+(db.folders().firstOrNull{it.first==folderId}?.second?: "Unfiled"))};DropdownMenu(expanded=folderMenu,onDismissRequest={folderMenu=false}){DropdownMenuItem(text={Text("Unfiled")},onClick={folderId="";db.moveToFolder(record.id,null);folderMenu=false});db.folders().forEach{f->DropdownMenuItem(text={Text(f.second)},onClick={folderId=f.first;db.moveToFolder(record.id,f.first);folderMenu=false})}} }
            Text("Sharing scope")
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("World","Series","Story").forEach{FilterChip(scope==it,{scope=it},label={Text(it)})}}
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={documentImport.launch(arrayOf("text/plain","text/markdown","text/html","application/xhtml+xml","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/epub+zip"))}){Text("Import")};TextButton(onClick={txtExport.launch(title.ifBlank{"Untitled"}+".txt")}){Text("TXT")};TextButton(onClick={mdExport.launch(title.ifBlank{"Untitled"}+".md")}){Text("Markdown")};TextButton(onClick={htmlExport.launch(title.ifBlank{"Untitled"}+".html")}){Text("HTML")};TextButton(onClick={pdfExport.launch(title.ifBlank{"Untitled"}+".pdf")}){Text("PDF")};TextButton(onClick={docxExport.launch(title.ifBlank{"Untitled"}+".docx")}){Text("DOCX")};TextButton(onClick={epubExport.launch(title.ifBlank{"Untitled"}+".epub")}){Text("EPUB")}}
            Text("Attachments",fontWeight=FontWeight.SemiBold)
            TextButton(onClick={attachmentPicker.launch(arrayOf("image/*","application/pdf","text/*","application/*"))}){Text("Add image or file")}
            attachmentList.forEach{attachment->Text("• ${attachment["displayName"]?.ifBlank{"Attached file"} ?: "Attached file"}",fontSize=12.sp)}
            Text("Mention suggestions",fontWeight=FontWeight.SemiBold)
            db.pendingLinkSuggestions(record.id).forEach{suggestion->Row(verticalAlignment=Alignment.CenterVertically){Text("“${suggestion.second}” → ${db.get(suggestion.first)?.title?: "Unknown"}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={db.resolveLinkSuggestion(record.id,suggestion.first,suggestion.third,true);linkCount++}){Text("Link")};TextButton(onClick={db.resolveLinkSuggestion(record.id,suggestion.first,suggestion.third,false);linkCount++}){Text("Dismiss")}}}
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

@Composable private fun SettingsDialog(db:InkDatabase,appearance:String,setAppearance:(String)->Unit,palette:String,setPalette:(String)->Unit,size:Float,setSize:(Float)->Unit,export:()->Unit,importBackup:()->Unit,cloudExport:(String)->Unit,cloudImport:(String)->Unit,close:()->Unit){
    val context=LocalContext.current
    var backupPassword by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=close,title={Text("Studio settings")},text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("Appearance");Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("system","light","dark").forEach{FilterChip(appearance==it,{setAppearance(it)},label={Text(it.replaceFirstChar(Char::uppercase))})}}
        Text("Color palette");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Plum","Ocean","Forest","Rose","Amber").forEach{FilterChip(palette==it,{setPalette(it)},label={Text(it)})}}
        Text("Editor size: ${size.toInt()}sp");Slider(size,setSize,valueRange=14f..30f)
        Text("Automatic mention links");var mode by remember{mutableStateOf(db.setting("autoLink","suggest"))};Row{listOf("off","suggest","auto").forEach{FilterChip(mode==it,{mode=it;db.setSetting("autoLink",it)},label={Text(it)})}}
        Text("Your writing stays on this phone first. Cloud backup is optional and encrypted on this device before it is sent to the storage provider you choose.")
        OutlinedTextField(backupPassword,{backupPassword=it},Modifier.fillMaxWidth(),label={Text("Cloud backup password")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true)
        Text("Use at least 10 characters. Keep this password safe: it cannot be recovered if lost.")
        TextButton(onClick=export){Text("Export local JSON backup")}
        TextButton(onClick=importBackup){Text("Import local JSON backup")}
        TextButton(onClick={if(backupPassword.length>=10){cloudExport(backupPassword)}else{Toast.makeText(context,"Use at least 10 characters.",Toast.LENGTH_SHORT).show()}}){Text("Save encrypted cloud backup")}
        TextButton(onClick={if(backupPassword.isNotBlank()){cloudImport(backupPassword)}else{Toast.makeText(context,"Enter your backup password.",Toast.LENGTH_SHORT).show()}}){Text("Restore encrypted cloud backup")}
        Text("Choose any connected Android storage provider in the system picker (for example, a cloud drive app). The provider handles its own sign-in.")
    }},confirmButton={TextButton(onClick=close){Text("Done")}})
}
