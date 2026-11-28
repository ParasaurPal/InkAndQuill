package com.parasaurpal.inkandquill

import android.net.Uri
import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
// Cover artwork is intentionally rendered as first-class library content.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.parasaurpal.inkandquill.data.*
import com.parasaurpal.inkandquill.ui.RichTextEditor
import com.parasaurpal.inkandquill.ui.MapScreen
import com.parasaurpal.inkandquill.ui.OutlineScreen
import com.parasaurpal.inkandquill.ui.WorldModulesScreen
import com.parasaurpal.inkandquill.ui.WorldModuleListScreen
import com.parasaurpal.inkandquill.ui.WorldEncyclopediaScreen
import com.parasaurpal.inkandquill.ui.SheetEditorTab
import com.parasaurpal.inkandquill.ui.BlueprintDesignerScreen
import com.parasaurpal.inkandquill.ui.TemplatePickerDialog
import com.parasaurpal.inkandquill.ui.GlobalSearchDialog
import com.parasaurpal.inkandquill.ui.GoogleDriveBackupDialog
import com.parasaurpal.inkandquill.ui.TemplateLibraryDialog
import com.parasaurpal.inkandquill.ui.CreateTemplateDialog
import com.parasaurpal.inkandquill.ui.SaveAsTemplateDialog
import com.parasaurpal.inkandquill.ui.ReadingModeScreen
import com.parasaurpal.inkandquill.ui.LiveWordCountBar
import com.parasaurpal.inkandquill.ui.WordCountDashboardDialog
import com.parasaurpal.inkandquill.ui.MediaManagerDialog
import com.parasaurpal.inkandquill.ui.NovelStudioScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private val Plum=Color(0xFFC9B6E4)
private val Night=Color(0xFF17141E)
private val Paper=Color(0xFFF2EAF8)

class MainActivity:ComponentActivity(){
    // Keep one database helper for the Activity lifetime. Constructing it inside
    // the Compose content lambda recreated SQLiteOpenHelper during recomposition.
    private val database:InkDatabase by lazy(LazyThreadSafetyMode.NONE){InkDatabase(applicationContext)}

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent{InkAndQuillApp(database,intent?.getBooleanExtra("open_novel_studio",false)==true)}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InkAndQuillApp(db:InkDatabase,startNovelStudio:Boolean=false){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val autoSaveManager=remember(context){TemplateAutoSaveManager(context)}
    val autoSaveStatus by autoSaveManager.status.collectAsState()
    var unlocked by remember{mutableStateOf(!AppLock.isEnabled(context))}
    DisposableEffect(context){
        val observer=LifecycleEventObserver{_,event->
            if(event==Lifecycle.Event.ON_STOP){
                scope.launch{autoSaveManager.saveSnapshot(db,"Background flush")}
                if(AppLock.isEnabled(context))unlocked=false
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose{ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)}
    }
    var screen by remember{mutableStateOf("Home")}
    var novelStudio by remember{mutableStateOf(startNovelStudio)}
    var selected by remember{mutableStateOf<InkRecord?>(null)}
    var outlineFor by remember{mutableStateOf<InkRecord?>(null)}
    var outlineAboveEditor by remember{mutableStateOf(false)}
    var outlineReturn by remember{mutableStateOf<InkRecord?>(null)}
    var activeBlueprint by remember{mutableStateOf<InkRecord?>(null)}
    var showCreateSheetPicker by remember{mutableStateOf(false)}
    var showGlobalSearch by remember{mutableStateOf(false)}
    var showGoogleDriveBackup by remember{mutableStateOf(false)}
    var showTemplateLibrary by remember{mutableStateOf(false)}
    var showCreateCustomTemplate by remember{mutableStateOf(false)}
    var showGlobalMediaManager by remember{mutableStateOf(false)}
    var readingRecord by remember{mutableStateOf<InkRecord?>(null)}
    var pendingFindTerm by remember{mutableStateOf("")}
    var pendingFindOccurrence by remember{mutableIntStateOf(0)}
    var openTabs by remember{mutableStateOf(db.openTabs().filter{db.get(it)!=null})}
    LaunchedEffect(selected?.id){selected?.let{openTabs=(openTabs.filterNot{id->id==it.id}+it.id).takeLast(8);db.saveOpenTabs(openTabs)}}
    var refresh by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){
        autoSaveManager.loadInitialStatus()
        if(autoSaveManager.recoverPendingDraftIfNewer(db)){
            refresh++
            Toast.makeText(context,"Recovered unsaved template draft.",Toast.LENGTH_SHORT).show()
        }
        while(isActive){
            delay(TemplateAutoSaveManager.AUTO_SAVE_INTERVAL_MS)
            autoSaveManager.saveSnapshot(db,"Scheduled 30s auto-save")
        }
    }
    var query by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("All")}
    var create by remember{mutableStateOf(false)}
    var settings by remember{mutableStateOf(false)}
    var cloudPassword by remember{mutableStateOf("")}
    var fontSize by remember{mutableFloatStateOf(db.setting("fontSize","18").toFloatOrNull()?:18f)}
    var appearance by remember{mutableStateOf(db.setting("appearance","dark"))}
    var palette by remember{mutableStateOf(db.setting("palette","Plum"))}
    var customAccent by remember{mutableStateOf(db.setting("customAccent","#9B83B5"))}
    val accent=when(palette){"Custom"->runCatching{Color(android.graphics.Color.parseColor(customAccent))}.getOrDefault(Plum);"Ocean"->Color(0xFF8DB9D8);"Forest"->Color(0xFF9DBD9A);"Rose"->Color(0xFFE0A8B8);"Amber"->Color(0xFFD9B66F);else->Plum}
    val dark=when(appearance){"light"->false;"dark"->true;else->isSystemInDarkTheme()}
    val navDefault="Home,Library,Worlds,Templates";val visibleTabs=db.setting("navTabs",navDefault).split(",").toSet();val navTabs=(listOf("Home")+db.setting("navOrder",navDefault).split(",").filter{it!="Home"}).distinct().filter{it=="Home"||it in visibleTabs}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?->uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(db.exportJson().toByteArray())}}}
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{r->db.importJson(r.readText())};refresh++}}}
    val cloudExportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri:Uri?->uri?.let{
        val result=runCatching{EncryptedCloudBackup.write(context.contentResolver,it,db.exportJson(),cloudPassword)}
        cloudPassword=""
        Toast.makeText(context,result.fold({"Encrypted cloud backup saved."},{it.message?: "Backup failed."}),Toast.LENGTH_LONG).show()
    }}
    val cloudSyncLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri:Uri?->uri?.let{val result=runCatching{context.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);CloudSyncManager.configure(context,it,cloudPassword)};cloudPassword="";Toast.makeText(context,result.fold({"Encrypted cloud sync configured."},{it.message?: "Could not enable scheduled backup."}),Toast.LENGTH_LONG).show()}}
    val cloudImportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{
        val result=runCatching{db.importJson(EncryptedCloudBackup.read(context.contentResolver,it,cloudPassword));refresh++}
        cloudPassword=""
        Toast.makeText(context,result.fold({"Encrypted backup restored."},{it.message?: "Restore failed."}),Toast.LENGTH_LONG).show()
    }}
    val exportBlueprintLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?->
        val tpl=activeBlueprint
        if(uri!=null&&tpl!=null){
            runCatching{
                context.contentResolver.openOutputStream(uri)?.use{out->out.write(db.exportTemplateJson(tpl.id).toByteArray())}
                val share=Intent(Intent.ACTION_SEND).apply{type="application/json";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}
                context.startActivity(Intent.createChooser(share,"Share template"))
            }
        }
    }
    BackHandler(enabled = novelStudio) { novelStudio = false }
    BackHandler(enabled = readingRecord != null) { readingRecord = null }
    val closeEditor:()->Unit={selected=null;refresh++}
    val closeOutline:()->Unit={if(outlineReturn!=null){outlineFor=outlineReturn;outlineReturn=null;outlineAboveEditor=false}else{outlineFor=null;outlineAboveEditor=false};refresh++}
    BackHandler(enabled = selected != null && activeBlueprint == null && readingRecord == null && !outlineAboveEditor) { closeEditor() }
    BackHandler(enabled = outlineFor != null && (selected == null || outlineAboveEditor)) { closeOutline() }
    MaterialTheme(
        colorScheme=if(dark) darkColorScheme(
            primary=accent,onPrimary=Color(0xFF211A2A),secondary=Color(0xFFB7A1D2),
            background=Color(0xFF121018),surface=Color(0xFF211D2A),surfaceVariant=Color(0xFF302A3A),
            onBackground=Color(0xFFF4EFF8),onSurface=Color(0xFFF4EFF8),onSurfaceVariant=Color(0xFFB7ADBF),
            outline=Color(0xFF51465D),outlineVariant=Color(0xFF3B3346),surfaceTint=accent
        ) else lightColorScheme(
            primary=accent,onPrimary=Color.White,secondary=Color(0xFF76618F),
            background=Color(0xFFF5F1F8),surface=Color(0xFFFFFCFF),surfaceVariant=Color(0xFFECE5F2),
            onBackground=Color(0xFF211B29),onSurface=Color(0xFF211B29),onSurfaceVariant=Color(0xFF655B6D),
            outline=Color(0xFFB9AEC2),outlineVariant=Color(0xFFD9D0E0)
        ),
        shapes=androidx.compose.material3.Shapes(
            extraSmall=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large=androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge=androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
        )
    ){
        if(!unlocked){LockScreen(onUnlock={pin->if(AppLock.verify(context,pin))unlocked=true})}else{
        Scaffold(
            containerColor=MaterialTheme.colorScheme.background,
            topBar={if(activeBlueprint==null&&readingRecord==null&&!novelStudio)TopAppBar(title={Text(if(selected!=null)selected!!.title.ifBlank{"Writing"} else "Ink & Quill",fontWeight=FontWeight.SemiBold)},navigationIcon={if(outlineFor!=null&&(selected==null||outlineAboveEditor))IconButton(onClick=closeOutline){Icon(Icons.Default.ArrowBack,"Back")}else if(selected!=null)IconButton(onClick=closeEditor){Icon(Icons.Default.ArrowBack,"Back")}},actions={IconButton(onClick={novelStudio=true},modifier=Modifier.size(48.dp)){Icon(Icons.Default.AutoStories,"Novel Studio")};IconButton(onClick={showGlobalSearch=true},modifier=Modifier.size(48.dp).testTag("global_search_button")){Icon(Icons.Default.Search,"Global search")};if(selected==null){IconButton(onClick={showGoogleDriveBackup=true},modifier=Modifier.size(48.dp).testTag("google_drive_top_button")){Icon(Icons.Default.CloudUpload,"Back up to Google Drive")};IconButton(onClick={settings=true},modifier=Modifier.size(48.dp)){Icon(Icons.Default.Settings,"Settings")}}})},
            bottomBar={if(selected==null&&activeBlueprint==null&&readingRecord==null&&!novelStudio&&outlineFor==null)NavigationBar{navTabs.forEach{tab->NavigationBarItem(selected=screen==tab,onClick={if(tab=="Settings")settings=true else screen=tab},icon={Icon(when(tab){"Home"->Icons.Default.Home;"Library"->Icons.Default.MenuBook;"Worlds"->Icons.Default.Public;"Templates"->Icons.Default.Dashboard;else->Icons.Default.Settings},tab)},label={Text(tab)})}}}
        ){padding->
            Column(Modifier.fillMaxSize().padding(padding)){
                if(novelStudio){NovelStudioScreen(onClose={novelStudio=false})} else {
                if(activeBlueprint==null&&readingRecord==null&&openTabs.isNotEmpty())Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){openTabs.mapNotNull{db.get(it)}.forEach{tab->FilterChip(selected?.id==tab.id,{selected=tab;outlineFor=null;outlineAboveEditor=false;outlineReturn=null},label={Text(tab.title.ifBlank{"Untitled"},maxLines=1)});TextButton(onClick={openTabs=openTabs.filterNot{id->id==tab.id};db.saveOpenTabs(openTabs);if(selected?.id==tab.id){selected=null;outlineFor=null;outlineAboveEditor=false;outlineReturn=null}}){Text("×")}}}
                Box(Modifier.weight(1f).fillMaxWidth()){
                    when(screen){
                        "Home"->HomeScreen(db,refresh,{selected=it},{create=true},{readingRecord=it})
                        "Library"->LibraryScreen(db,query,{query=it},filter,{filter=it},refresh,{readingRecord=it}){selected=it}
                        "Worlds"->WorldScreen(db,refresh,{selected=it},onRead={readingRecord=it},onOpenMediaManager={showGlobalMediaManager=true},backEnabled=selected==null&&outlineFor==null&&readingRecord==null&&activeBlueprint==null)
                        "Templates"->TemplateScreen(
                            db = db,
                            refresh = refresh,
                            autoSaveStatus = autoSaveStatus,
                            onAutoSaveNow = {
                                scope.launch {
                                    val ok = autoSaveManager.saveSnapshot(db, "Manual auto-save")
                                    Toast.makeText(context, if (ok) "Template data saved to DataStore." else "Auto-save failed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenTemplateLibrary = { showTemplateLibrary = true },
                            onOpenMediaManager = { showGlobalMediaManager = true },
                            edit = { selected = it },
                            designBlueprint = { activeBlueprint = it },
                            use = { created -> selected = created; refresh++ }
                        )
                        else->HomeScreen(db,refresh,{selected=it},{create=true},{readingRecord=it})
                    }
                    val editorLayer:@Composable ()->Unit={
                        if(selected!=null){
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
                                EditorScreen(
                        db = db,
                        record = selected!!,
                        fontSize = fontSize,
                        onSaved = { record -> db.save(record); selected = db.get(record.id); refresh++ },
                        onDelete = { id ->
                            autoSaveManager.clearEntryDraft(id)
                            db.delete(id)
                            openTabs = openTabs.filterNot { it == id }
                            db.saveOpenTabs(openTabs)
                            selected = null
                            refresh++
                        },
                        onLinkScan = { record -> val hits = LocalLinkEngine(db).scan(record); refresh++; hits.size },
                        jumpTerm = pendingFindTerm,
                        jumpOccurrence = pendingFindOccurrence,
                        onJumpHandled = { pendingFindTerm = ""; pendingFindOccurrence = 0 },
                        onOpen = { record, term, occurrence -> selected = record; pendingFindTerm = term; pendingFindOccurrence = occurrence },
                        onOutline = { record -> if(outlineFor!=null&&!outlineAboveEditor)outlineReturn=outlineFor; outlineFor = record; outlineAboveEditor = true; refresh++ },
                        onOpenBlueprint = { tplId -> db.get(tplId)?.let { activeBlueprint = it } },
                        onReadMode = { target -> readingRecord = target },
                        onTemplateCreated = { tpl -> activeBlueprint = tpl; refresh++ },
                        onUpdateEntryDraft = { draft -> autoSaveManager.updateEntryDraft(draft) },
                        onClearEntryDraft = { recordId -> autoSaveManager.clearEntryDraft(recordId) }
                    )
                            }
                        }
                    }
                    val outlineLayer:@Composable ()->Unit={
                        if(outlineFor!=null){
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
                                OutlineScreen(db=db,root=outlineFor!!,refresh=refresh,onOpen={record->selected=record;outlineAboveEditor=false;refresh++},onBack=closeOutline)
                            }
                        }
                    }
                    if(outlineAboveEditor){editorLayer();outlineLayer()}else{outlineLayer();editorLayer()}
                    if(readingRecord!=null){
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
                            ReadingModeScreen(
                            db = db,
                            record = readingRecord!!,
                            onEditRecord = { target ->
                                readingRecord = null
                                selected = target
                            },
                            onOpenRecord = { target ->
                                readingRecord = target
                            },
                            onClose = {
                                readingRecord = null
                                refresh++
                            }
                        )
                        }
                    }
                    if(activeBlueprint!=null){
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){}){
                            BlueprintDesignerScreen(
                            db = db,
                            template = activeBlueprint!!,
                            onTemplateUpdated = { tpl ->
                                activeBlueprint = tpl
                                autoSaveManager.updateBlueprintDraft(
                                    ActiveBlueprintDraft(
                                        templateId = tpl.id,
                                        title = tpl.title,
                                        description = tpl.body,
                                        category = tpl.tags,
                                        defaultKind = tpl.scope
                                    )
                                )
                                refresh++
                            },
                            onExportTemplate = { tpl -> activeBlueprint = tpl; exportBlueprintLauncher.launch("${tpl.title}.inktemplate.json") },
                            onDuplicateTemplate = { tpl ->
                                val dup = db.duplicateTemplate(tpl.id)
                                if (dup != null) {
                                    activeBlueprint = dup
                                    refresh++
                                    Toast.makeText(context, "Duplicated as ${dup.title}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onUseTemplate = { tpl ->
                                autoSaveManager.clearBlueprintDraft(tpl.id)
                                val created = db.createRecordFromTemplate(tpl.id)
                                activeBlueprint = null
                                selected = created
                                refresh++
                            },
                            onClose = {
                                autoSaveManager.clearBlueprintDraft(activeBlueprint?.id)
                                activeBlueprint = null
                                refresh++
                            }
                        )
                        }
                    }
                }
                }
            }
        }
        if(create)CreateDialog(
            onDismiss={create=false},
            onChoose={kind->val record=InkRecord(title="Untitled $kind",kind=kind);db.save(record);selected=record;create=false;refresh++},
            onFromTemplate={create=false;showCreateSheetPicker=true},
            onCreateCustomTemplate={create=false;showCreateCustomTemplate=true}
        )
        if(showCreateCustomTemplate)CreateTemplateDialog(
            db = db,
            onDismiss = { showCreateCustomTemplate = false },
            onCreatedForDesign = { tpl ->
                showCreateCustomTemplate = false
                activeBlueprint = tpl
                refresh++
            },
            onCreatedAndUse = { created ->
                showCreateCustomTemplate = false
                selected = created
                refresh++
            }
        )
        if(showCreateSheetPicker)TemplatePickerDialog(
            db = db,
            title = "Create Entry from Fantasy Sheet",
            onDismiss = { showCreateSheetPicker = false },
            onSelect = { tpl ->
                val created = db.createRecordFromTemplate(tpl.id)
                showCreateSheetPicker = false
                selected = created
                refresh++
            }
        )
        if(showGlobalSearch)GlobalSearchDialog(
            db = db,
            onDismiss = { showGlobalSearch = false },
            onOpenEntry = { record ->
                selected = record
                showGlobalSearch = false
            },
            onOpenTemplateBlueprint = { tpl ->
                activeBlueprint = tpl
                showGlobalSearch = false
            },
            onUseTemplate = { tpl ->
                val created = db.createRecordFromTemplate(tpl.id)
                selected = created
                showGlobalSearch = false
                refresh++
            }
        )
        if(showGoogleDriveBackup)GoogleDriveBackupDialog(
            db = db,
            onRestoredFromDrive = { refresh++ },
            onFallbackSafBackup = { export.launch("ink-and-quill-backup.json") },
            onDismiss = { showGoogleDriveBackup = false }
        )
        if(showTemplateLibrary)TemplateLibraryDialog(
            db = db,
            onDismiss = { showTemplateLibrary = false },
            onCreateEntryFromLibrary = { created ->
                showTemplateLibrary = false
                selected = created
                refresh++
            },
            onImportedBlueprint = { tpl ->
                showTemplateLibrary = false
                activeBlueprint = tpl
                refresh++
            }
        )
        if(showGlobalMediaManager)MediaManagerDialog(
            db = db,
            onOpenRecord = { rec ->
                showGlobalMediaManager = false
                selected = rec
            },
            onMediaChanged = { refresh++ },
            onDismiss = { showGlobalMediaManager = false }
        )
        if(settings)SettingsDialog(
            db,appearance,{appearance=it;db.setSetting("appearance",it)},
            palette,{palette=it;db.setSetting("palette",it)},
            customAccent,{customAccent=it;db.setSetting("customAccent",it)},
            fontSize,{fontSize=it;db.setSetting("fontSize",it.toInt().toString())},
            {export.launch("ink-and-quill-backup.json")},
            {importLauncher.launch(arrayOf("application/json","text/*"))},
            {password->cloudPassword=password;cloudExportLauncher.launch("InkAndQuill-encrypted-backup.inkq")},
            {password->cloudPassword=password;cloudImportLauncher.launch(arrayOf("application/octet-stream","application/*","*/*"))},
            {password->cloudPassword=password;cloudSyncLauncher.launch("InkAndQuill-scheduled-backup.inkq")},
            CloudSyncManager.isEnabled(context),
            {CloudSyncManager.disable(context)},
            AppLock.isEnabled(context),
            {pin->AppLock.setPin(context,pin)},
            {AppLock.disable(context)},
            CloudSyncManager.hasConflict(context),
            CloudSyncManager.lastSyncStatus(context),
            CloudSyncManager.lastSyncAt(context),
            CloudSyncManager.syncHistory(context),
            {keepPhone->CloudSyncManager.resolveConflict(context,keepPhone)},
            {db.all().forEach{LocalLinkEngine(db).scan(it)};refresh++},
            autoSaveStatus,
            {
                scope.launch {
                    val ok = autoSaveManager.saveSnapshot(db, "Manual auto-save")
                    Toast.makeText(context, if (ok) "Template data saved to DataStore." else "Auto-save failed.", Toast.LENGTH_SHORT).show()
                }
            },
            {
                scope.launch {
                    val result = autoSaveManager.restoreFromAutoSave(db)
                    refresh++
                    Toast.makeText(
                        context,
                        "Restored ${result.templatesRestored} templates & ${result.sheetsRestored} sheets.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            {
                settings = false
                showGoogleDriveBackup = true
            }
        ){settings=false}
        }
    }
}

@Composable private fun HomeScreen(db:InkDatabase,refresh:Int,open:(InkRecord)->Unit,create:()->Unit,onRead:(InkRecord)->Unit={}){
    val recent=remember(refresh){db.all().take(8)}
    val latest=recent.firstOrNull()
    val homeDefault="Novels & Stories,Poetry,Journal,Documents,Notes,Creative Projects,Worldbuilding"
    val visibleSections=db.setting("homeSections",homeDefault).split(",").toSet()
    val sections=db.setting("homeOrder",homeDefault).split(",").filter{it in visibleSections}
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal=16.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp),
        contentPadding=PaddingValues(top=18.dp,bottom=28.dp)
    ){
        item{
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("INK & QUILL",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp,color=Plum)
                Text("Your writing space.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
                Text("A quiet place to turn ideas into finished work.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.68f))
            }
        }
        item{
            Card(onClick=create,modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF33283F))){
                Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("Start writing",fontSize=19.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFFFFF8FF))
                        Text("Open a fresh page for your next idea.",fontSize=12.sp,color=Color(0xFFD6C9E0))
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("NEW DOCUMENT",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Color(0xFFE7D5FF))
                            Icon(Icons.Default.ArrowForward,null,tint=Color(0xFFE7D5FF),modifier=Modifier.padding(start=6.dp).size(16.dp))
                        }
                    }
                    Box(Modifier.size(52.dp).background(Color(0xFF554365),androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){
                        Icon(Icons.Default.EditNote,null,tint=Color(0xFFF0DFFF),modifier=Modifier.size(29.dp))
                    }
                }
            }
        }
        if(latest!=null)item{
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("PICK UP WHERE YOU LEFT OFF",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp,color=Plum,modifier=Modifier.weight(1f))
                    TextButton(onClick={open(latest)}){Text("Continue")}
                }
                Card(onClick={open(latest)},modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Icon(Icons.Default.MenuBook,null,tint=Plum,modifier=Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(latest.kind.uppercase(),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,color=Plum)
                            Spacer(Modifier.weight(1f))
                            val words=WordCountEngine.analyzeDocument(latest.body,emptyList(),emptyMap()).totalWords
                            Text("${WordCountEngine.formatCompact(words)} words",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(latest.title.ifBlank{"Untitled"},fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                        val preview=android.text.Html.fromHtml(latest.body,android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim().replace(Regex("\\s+")," ").take(120)
                        Text(preview.ifBlank{"Your page is ready for the next sentence."},fontSize=12.sp,lineHeight=17.sp,maxLines=2,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.68f))
                        Text("CONTINUE WRITING  →",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,color=Plum)
                    }
                }
            }
        }
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("Recent work",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("Your latest drafts and projects",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f))
                }
                if(recent.isNotEmpty())Text("${recent.size} ITEMS",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Plum)
            }
        }
        if(recent.isEmpty())item{
            Card(shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Icon(Icons.Default.AutoStories,null,tint=Plum,modifier=Modifier.size(30.dp))
                    Text("Your first page is waiting",fontWeight=FontWeight.SemiBold)
                    Text("Create a story, poem, journal entry, or note to begin.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f),textAlign=TextAlign.Center)
                    FilledTonalButton(onClick=create){Text("Create your first draft")}
                }
            }
        }else items(recent.take(5)){RecordCard(db,it,open,onRead)}
        item{
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Your studio",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text("Planning and organizing spaces",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f))
                    }
                    Text("${sections.size} SPACES",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Plum)
                }
                sections.chunked(2).forEach{pair->
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        pair.forEach{label->
                            Card(onClick=create,modifier=Modifier.weight(1f).heightIn(min=76.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
                                    val symbol=when(label){
                                        "Novels & Stories"->Icons.Default.AutoStories
                                        "Poetry"->Icons.Default.FormatQuote
                                        "Journal"->Icons.Default.Book
                                        "Documents"->Icons.Default.Description
                                        "Notes"->Icons.Default.StickyNote2
                                        "Creative Projects"->Icons.Default.Brush
                                        "Worldbuilding"->Icons.Default.Public
                                        else->Icons.Default.Folder
                                    }
                                    Box(Modifier.size(34.dp).background(Plum.copy(alpha=.16f),androidx.compose.foundation.shape.RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(symbol,null,tint=Plum,modifier=Modifier.size(19.dp))}
                                    Text(label,Modifier.weight(1f).padding(start=9.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
                                    Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurface.copy(alpha=.45f),modifier=Modifier.size(17.dp))
                                }
                            }
                        }
                        if(pair.size==1)Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
@Composable private fun RecordCard(db:InkDatabase,record:InkRecord,open:(InkRecord)->Unit,onRead:((InkRecord)->Unit)?=null){
    val context=LocalContext.current
    val cover=remember(record.coverUri){if(record.coverUri.isNotBlank())runCatching{context.contentResolver.openInputStream(Uri.parse(record.coverUri))?.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()else null}
    val sharedTags=db.allTags().filter{it.first in db.tagsForRecord(record.id)}.map{it.second}
    val sheetTplId=remember(record.id,record.updatedAt){db.sheetTemplateId(record.id)}
    val cardWordCount=remember(record.id,record.body,sheetTplId,record.updatedAt){
        val fields=if(sheetTplId.isNotBlank())db.templateFields(sheetTplId) else emptyList()
        val vals=if(sheetTplId.isNotBlank())db.sheetValues(record.id) else emptyMap()
        WordCountEngine.analyzeDocument(record.body,fields,vals).totalWords
    }
    val previewText=remember(record.id,record.body,sheetTplId,record.updatedAt){
        if(record.body.isNotBlank()){
            android.text.Html.fromHtml(record.body,android.text.Html.FROM_HTML_MODE_COMPACT).toString().take(150).replace('
',' ')
        }else if(sheetTplId.isNotBlank()){
            val fields=db.templateFields(sheetTplId)
            val secs=db.templateSections(sheetTplId)
            val vals=db.sheetValues(record.id)
            SheetEngine.compileToPlainText(fields,secs,vals){db.get(it)}
                .lines().filter{!it.startsWith("## ")&&it.isNotBlank()}.take(3).joinToString(" · ").take(150)
        }else ""
    }
    Card(onClick={open(record)},modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.Top){
            Box(Modifier.width(if(cover!=null)62.dp else 42.dp).height(if(cover!=null)86.dp else 42.dp).background(Plum.copy(alpha=.15f),androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){
                if(cover!=null)Image(cover,"Novel cover",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else Icon(when(record.kind){
                    "World","Realm","Celestial"->Icons.Default.Public
                    "Poem"->Icons.Default.FormatQuote
                    "Journal"->Icons.Default.Book
                    "Note","Research Note"->Icons.Default.StickyNote2
                    "Character","NPC","Dynasty","Lineage"->Icons.Default.Person
                    "Flora","Herb","Fungus","Biome"->Icons.Default.Spa
                    "Fauna","Monster","Species","Dragon"->Icons.Default.Pets
                    "Location","Region","Kingdom","Settlement","Dungeon","Stronghold"->Icons.Default.Place
                    "Item","Artifact","Weapon","Potion","Material","Vessel"->Icons.Default.AutoAwesome
                    "Language","Script"->Icons.Default.Translate
                    "Culture","Faction","Festival","Economy","Government","Military"->Icons.Default.Groups
                    "Magic System","Spell","Technology","Affliction"->Icons.Default.Bolt
                    "Religion","Deity","Myth","Prophecy"->Icons.Default.TempleHindu
                    "Calendar","Timeline Event"->Icons.Default.CalendarMonth
                    else->Icons.Default.Description
                },null,tint=Plum,modifier=Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(record.title.ifBlank{"Untitled"},Modifier.weight(1f),fontWeight=FontWeight.SemiBold,fontSize=15.sp,maxLines=1)
                    if(cardWordCount>0){
                        Text("${WordCountEngine.formatCompact(cardWordCount)} w · ",fontSize=9.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text((if(sheetTplId.isNotBlank())"SHEET · " else "")+record.kind.uppercase(),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp,color=Plum)
                }
                if(previewText.isNotBlank())Text(previewText,maxLines=2,fontSize=12.sp,lineHeight=17.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.66f))
                val tags=listOf(record.tags).filter{it.isNotBlank()}+sharedTags
                if(tags.isNotEmpty())Text(tags.joinToString(" · "),maxLines=1,fontSize=10.sp,color=Plum.copy(alpha=.9f))
            }
            if(onRead!=null && record.kind in listOf("Book","Chapter","Novel","Story","Scene")){
                IconButton(
                    onClick={onRead(record)},
                    modifier=Modifier.size(48.dp).testTag("card_read_${record.id}")
                ){
                    Icon(Icons.Default.AutoStories, contentDescription="Read ${record.kind.lowercase()}", tint=Plum)
                }
            }
            Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurface.copy(alpha=.4f),modifier=Modifier.padding(start=5.dp).size(18.dp))
        }
    }
}

@Composable private fun LibraryVisualRecordCard(record:InkRecord,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit){
    val context=LocalContext.current
    val count=remember(record.id,record.body,record.updatedAt){WordCountEngine.analyzeDocument(record.body,emptyList(),emptyMap()).totalWords}
    val preview=remember(record.id,record.body,record.updatedAt){android.text.Html.fromHtml(record.body,android.text.Html.FROM_HTML_MODE_COMPACT).toString().replace(Regex("\\s+")," ").trim().take(150)}
    Card(Modifier.fillMaxWidth().clickable{open(record)},shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            RecordCoverArt(record,Modifier.width(100.dp).height(140.dp),record.title+" cover")
            Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text(record.kind.uppercase(),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Plum)
                Text(record.title.ifBlank{"Untitled"},fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=2)
                if(preview.isNotBlank())Text(preview,fontSize=11.sp,lineHeight=15.sp,maxLines=3,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.68f))
                if(record.tags.isNotBlank())Text(record.tags,fontSize=10.sp,color=Plum,maxLines=1)
                Row(verticalAlignment=Alignment.CenterVertically){
                    if(count>0)Text(WordCountEngine.formatCompact(count)+" words",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f))else Spacer(Modifier.weight(1f))
                    if(record.kind in listOf("Book","Chapter","Novel","Story","Scene"))IconButton(onClick={onRead(record)}){Icon(Icons.Default.MenuBook,"Read "+record.title)}
                    IconButton(onClick={open(record)}){Icon(Icons.Default.Edit,"Edit "+record.title)}
                }
            }
        }
    }
}

@Composable private fun LibraryScreen(db:InkDatabase,query:String,setQuery:(String)->Unit,filter:String,setFilter:(String)->Unit,refresh:Int,onRead:(InkRecord)->Unit={},open:(InkRecord)->Unit){
    var folderFilter by remember{mutableStateOf("")}
    var folderMenu by remember{mutableStateOf(false)}
    var createFolder by remember{mutableStateOf(false)}
    var newFolderName by remember{mutableStateOf("")}
    var newFolderParent by remember{mutableStateOf("")}
    var folderParentMenu by remember{mutableStateOf(false)}
    var collectionFilter by remember{mutableStateOf("")}
    var collectionMenu by remember{mutableStateOf(false)}
    var createCollection by remember{mutableStateOf(false)}
    var manageLibrary by remember{mutableStateOf(false)}
    var renameDialog by remember{mutableStateOf(false)}
    var renameItemId by remember{mutableStateOf("")}
    var renameItemType by remember{mutableStateOf("folder")}
    var renameItemName by remember{mutableStateOf("")}
    var collectionName by remember{mutableStateOf("")}
    var favoritesOnly by remember{mutableStateOf(false)}
    var addCollectionRecord by remember{mutableStateOf("")}
    var addCollectionMenu by remember{mutableStateOf(false)}
    val folders=db.folders()
    val collections=db.collections()
    val records=remember(query,filter,refresh,folderFilter,collectionFilter,favoritesOnly){
        db.all(query,filter).filter{(folderFilter.isBlank()||db.folderFor(it.id) in db.folderTreeIds(folderFilter))&&(!favoritesOnly||it.favorite)&&(collectionFilter.isBlank()||db.collectionRecords(collectionFilter).any{r->r.id==it.id})}
    }
    val availableKinds=remember(refresh){(FantasyTemplateCatalog.starterRecordKinds+db.all().map{it.kind}).distinct()}
    val wordSnapshot=remember(refresh){WordCountEngine.buildSnapshot(db)}
    val series=records.filter{it.kind=="Series"}
    val seriesIds=series.map{it.id}.toSet()
    val seriesChildren=records.filter{it.parentId in seriesIds}.groupBy{it.parentId}
    val hiddenBySeries=remember(records,seriesIds){
        records.filter{record->
            var parent=record.parentId
            var found=false
            var depth=0
            while(parent.isNotBlank()&&depth<12&&!found){
                if(parent in seriesIds)found=true else parent=db.get(parent)?.parentId.orEmpty()
                depth++
            }
            found
        }.map{it.id}.toSet()
    }
    val standalone=records.filter{it.kind!="Series"&&it.id !in hiddenBySeries}
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp)){
        OutlinedTextField(query,setQuery,Modifier.fillMaxWidth(),label={Text("Search your library")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Box{
                OutlinedButton(onClick={folderMenu=true}){Text("Folder: "+(folders.firstOrNull{it.first==folderFilter}?.second?: "All"))}
                DropdownMenu(expanded=folderMenu,onDismissRequest={folderMenu=false}){
                    DropdownMenuItem(text={Text("All folders")},onClick={folderFilter="";folderMenu=false})
                    folders.forEach{f->DropdownMenuItem(text={Text(if(f.third.isBlank())f.second else "↳ "+f.second)},onClick={folderFilter=f.first;folderMenu=false})}
                }
            }
            TextButton(onClick={createFolder=true}){Text("+ Folder")}
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FilterChip(filter=="All",{setFilter("All")},label={Text("All")})
            availableKinds.forEach{kind->FilterChip(filter==kind,{setFilter(kind)},label={Text(kind)})}
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
            Box{
                OutlinedButton(onClick={collectionMenu=true}){Text("Collection: "+(collections.firstOrNull{it.first==collectionFilter}?.second?: "All"))}
                DropdownMenu(expanded=collectionMenu,onDismissRequest={collectionMenu=false}){
                    DropdownMenuItem(text={Text("All collections")},onClick={collectionFilter="";collectionMenu=false})
                    collections.forEach{col->DropdownMenuItem(text={Text(col.second)},onClick={collectionFilter=col.first;collectionMenu=false})}
                }
            }
            TextButton(onClick={createCollection=true}){Text("+")}
            TextButton(onClick={manageLibrary=true}){Text("Manage")}
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(favoritesOnly,{favoritesOnly=it});Text("★",fontSize=13.sp)}
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=22.dp)){
            if(series.isNotEmpty()){
                item{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("Series collections",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Text(series.size.toString(),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Plum)
                    }
                }
                items(series,key={it.id}){seriesRecord->
                    SeriesCollectionCard(db,seriesRecord,seriesChildren[seriesRecord.id].orEmpty().sortedBy{it.position},open,onRead,wordSnapshot)
                }
            }
            if(standalone.isNotEmpty()){
                item{
                    Text(if(series.isEmpty())"Your library" else "Other books & entries",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                }
                items(standalone,key={it.id}){record->
                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
                        LibraryVisualRecordCard(record,open,onRead)
                        if(collectionFilter.isNotBlank())TextButton(onClick={db.removeFromCollection(collectionFilter,record.id)}){Text("Remove from collection")}
                        else if(collections.isNotEmpty())Box{
                            TextButton(onClick={addCollectionRecord=record.id;addCollectionMenu=true}){Text("Add to collection")}
                            DropdownMenu(expanded=addCollectionMenu&&addCollectionRecord==record.id,onDismissRequest={addCollectionMenu=false}){
                                collections.forEach{col->DropdownMenuItem(text={Text(col.second)},onClick={db.addToCollection(col.first,record.id);addCollectionMenu=false})}
                            }
                        }
                    }
                }
            }
            if(series.isEmpty()&&standalone.isEmpty())item{
                Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Icon(Icons.Default.MenuBook,null,tint=Plum,modifier=Modifier.size(38.dp))
                    Text("Your library is waiting",fontWeight=FontWeight.SemiBold)
                    Text("Create books, stories, and series to build your visual bookshelf.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
                }
            }
        }
    }
    if(manageLibrary)AlertDialog(onDismissRequest={manageLibrary=false},title={Text("Manage folders and collections")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState())){Text("Folders",fontWeight=FontWeight.SemiBold);folders.forEach{f->Row(verticalAlignment=Alignment.CenterVertically){Text(f.second,Modifier.weight(1f));TextButton(onClick={renameItemId=f.first;renameItemType="folder";renameItemName=f.second;renameDialog=true}){Text("Rename")};TextButton(onClick={db.deleteFolder(f.first);if(folderFilter==f.first)folderFilter="";manageLibrary=false}){Text("Delete")}}};Text("Collections",fontWeight=FontWeight.SemiBold);collections.forEach{col->Row(verticalAlignment=Alignment.CenterVertically){Text(col.second,Modifier.weight(1f));TextButton(onClick={renameItemId=col.first;renameItemType="collection";renameItemName=col.second;renameDialog=true}){Text("Rename")};TextButton(onClick={db.deleteCollection(col.first);if(collectionFilter==col.first)collectionFilter="";manageLibrary=false}){Text("Delete")}}}}},confirmButton={TextButton(onClick={manageLibrary=false}){Text("Done")}})
    if(renameDialog)AlertDialog(onDismissRequest={renameDialog=false},title={Text("Rename")},text={OutlinedTextField(renameItemName,{renameItemName=it},label={Text("New name")},singleLine=true)},confirmButton={TextButton(onClick={if(renameItemName.isNotBlank()){if(renameItemType=="folder")db.renameFolder(renameItemId,renameItemName)else db.renameCollection(renameItemId,renameItemName);renameDialog=false}}){Text("Save")}},dismissButton={TextButton(onClick={renameDialog=false}){Text("Cancel")}})
    if(createCollection)AlertDialog(onDismissRequest={createCollection=false},title={Text("New collection")},text={OutlinedTextField(collectionName,{collectionName=it},label={Text("Collection name")})},confirmButton={TextButton(onClick={if(collectionName.isNotBlank()){db.createCollection(collectionName);collectionName="";createCollection=false}}){Text("Create")}},dismissButton={TextButton(onClick={createCollection=false}){Text("Cancel")}})
    if(createFolder)AlertDialog(onDismissRequest={createFolder=false},title={Text("New folder")},text={Column{OutlinedTextField(newFolderName,{newFolderName=it},label={Text("Folder name")},singleLine=true);Box{OutlinedButton(onClick={folderParentMenu=true}){Text("Parent: "+(folders.firstOrNull{it.first==newFolderParent}?.second?: "None"))};DropdownMenu(expanded=folderParentMenu,onDismissRequest={folderParentMenu=false}){DropdownMenuItem(text={Text("No parent")},onClick={newFolderParent="";folderParentMenu=false});folders.forEach{f->DropdownMenuItem(text={Text(f.second)},onClick={newFolderParent=f.first;folderParentMenu=false})}}}}},confirmButton={TextButton(onClick={if(newFolderName.isNotBlank()){db.createFolder(newFolderName,newFolderParent);newFolderName="";newFolderParent="";createFolder=false}}){Text("Create")}},dismissButton={TextButton(onClick={createFolder=false}){Text("Cancel")}})
}
@Composable private fun WorldScreen(db:InkDatabase,refresh:Int,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit={},onOpenMediaManager:()->Unit={},backEnabled:Boolean=true){
    var localRefresh by remember{mutableIntStateOf(0)}
    val worlds=remember(refresh,localRefresh){db.all().filter{it.kind=="World"}}
    var world by remember{mutableStateOf<InkRecord?>(null)}
    var createChild by remember{mutableStateOf(false)}
    var createParentId by remember{mutableStateOf("")}
    var mode by remember{mutableStateOf("Modules")}
    var moduleTitle by remember{mutableStateOf("")}
    var moduleKinds by remember{mutableStateOf<List<String>>(emptyList())}
    var modulePrimaryKind by remember{mutableStateOf("")}
    var showWorldMenu by remember{mutableStateOf(false)}
    var eventDialog by remember{mutableStateOf(false)}
    var eventTitle by remember{mutableStateOf("")}
    var eventDate by remember{mutableStateOf("")}
    var eventDescription by remember{mutableStateOf("")}
    var boardDialog by remember{mutableStateOf(false)}
    var boardName by remember{mutableStateOf("")}
    var boardType by remember{mutableStateOf("Corkboard")}
    var selectedBoard by remember{mutableStateOf("")}
    var boardItemDialog by remember{mutableStateOf(false)}
    var boardItemTitle by remember{mutableStateOf("")}
    var boardItemImageUri by remember{mutableStateOf("")}
    var boardItemColor by remember{mutableStateOf("#D9C7EE")}
    var boardItemRecordId by remember{mutableStateOf("")}
    var boardRecordMenu by remember{mutableStateOf(false)}
    var boardItemBody by remember{mutableStateOf("")}
    var calendarDialog by remember{mutableStateOf(false)}
    var calendarName by remember{mutableStateOf("New calendar")}
    var calendarMonths by remember{mutableStateOf("First,Second,Third")}
    var calendarMonthLengths by remember{mutableStateOf("30,30,30")}
    var calendarEras by remember{mutableStateOf("Age of Origins,Current Age")}
    var calendarLeapRules by remember{mutableStateOf("")}
    var calendarWeekdays by remember{mutableStateOf("Day 1,Day 2,Day 3,Day 4,Day 5,Day 6,Day 7")}
    var calendarSeasons by remember{mutableStateOf("Spring,Summer,Autumn,Winter")}
    var calendarMoons by remember{mutableStateOf("Moon 1")}
    var selectedCalendar by remember{mutableStateOf("")}
    var calendarEntryDialog by remember{mutableStateOf(false)}
    var calendarEntryTitle by remember{mutableStateOf("")}
    var calendarEntryYear by remember{mutableStateOf("1")}
    var calendarEntryMonth by remember{mutableStateOf("1")}
    var calendarEntryDay by remember{mutableStateOf("1")}
    var calendarEntryDescription by remember{mutableStateOf("")}
    var calendarRepeat by remember{mutableStateOf(false)}
    var calendarMonthIndex by remember{mutableIntStateOf(0)}
    var calendarYear by remember{mutableIntStateOf(1)}
    var showWorldWordStats by remember{mutableStateOf(false)}
    val wordSnapshot=remember(refresh,localRefresh){WordCountEngine.buildSnapshot(db)}
    var linkMode by remember{mutableStateOf(false)}
    var boardLinkLabel by remember{mutableStateOf("related to")}
    var linkFrom by remember{mutableStateOf("")}
    val context=LocalContext.current
    val boardItemImagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION);boardItemImageUri=it.toString()}}}
    val boardBackgroundPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION);db.setBoardBackground(selectedBoard,it.toString());localRefresh++}}}
    val boardTypes=listOf("Corkboard","Moodboard","Map","Flowchart","Relationship graph","Family tree","Floor plan","Reference board")
    var showWorldSheetPicker by remember{mutableStateOf(false)}
    var showWorldCustomTemplate by remember{mutableStateOf(false)}
    BackHandler(enabled = world != null && backEnabled) { if(moduleKinds.isNotEmpty())moduleKinds=emptyList() else if(mode!="Modules")mode="Modules" else {world=null;selectedBoard=""} }
    if(world==null){
        Column(Modifier.fillMaxSize().padding(16.dp)){ 
            Text("Worlds",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text("Worlds are the top-level home for each fictional universe. Expand a Series to see its books, chapters, and scenes.")
            Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={val created=InkRecord(title="Untitled World",kind="World");db.save(created);world=created;mode="Modules";moduleKinds=emptyList();localRefresh++}){Icon(Icons.Default.Add,null);Text(" Create a world")}
                FilledTonalButton(onClick=onOpenMediaManager,modifier=Modifier.testTag("worlds_media_manager_button")){
                    Icon(Icons.Default.PhotoCamera,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Media")
                }
            }
            LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
                items(worlds){ w -> WorldListCard(db,w,open,onRead,wordSnapshot){world=it;mode="Modules";moduleKinds=emptyList()} }
                if(worlds.isEmpty()) item { Text("No worlds yet. Create a World to begin your story universe.",Modifier.padding(12.dp)) }
            }
        }
    }else{
        val w=world!!
        LaunchedEffect(w.id){moduleKinds=emptyList()}
        val worldTotalWords=remember(w.id,wordSnapshot){wordSnapshot.subtreeWords(w.id)}
        Column(Modifier.fillMaxSize().padding(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={if(moduleKinds.isNotEmpty())moduleKinds=emptyList() else if(mode!="Modules")mode="Modules" else {world=null;selectedBoard=""}}){Icon(Icons.Default.ArrowBack,"Back")}
                Column(Modifier.weight(1f)){
                    Text(w.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("Worldbuilding",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f))
                }
                IconButton(onClick={moduleKinds=emptyList();mode="Resources"}){Icon(Icons.Default.Search,"Search resources")}
                Box{
                    IconButton(onClick={showWorldMenu=true}){Icon(Icons.Default.MoreVert,"World options")}
                    DropdownMenu(expanded=showWorldMenu,onDismissRequest={showWorldMenu=false}){
                        DropdownMenuItem(text={Text("Word counts")},onClick={showWorldMenu=false;showWorldWordStats=true},modifier=Modifier.testTag("world_word_counts_button"))
                        DropdownMenuItem(text={Text("Files & media")},onClick={showWorldMenu=false;onOpenMediaManager()})
                        DropdownMenuItem(text={Text("Edit world")},onClick={showWorldMenu=false;open(w)})
                    }
                }
            }
            Row(Modifier.fillMaxWidth()){
                Column(Modifier.weight(1f).clickable{moduleKinds=emptyList();mode="Modules"},horizontalAlignment=Alignment.CenterHorizontally){
                    Icon(Icons.Default.Extension,"Worldbuilding modules",Modifier.padding(vertical=8.dp),tint=if(mode!="Hierarchy")MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=.45f))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if(mode!="Hierarchy")MaterialTheme.colorScheme.primary else Color.Transparent))
                }
                Column(Modifier.weight(1f).clickable{moduleKinds=emptyList();mode="Hierarchy"},horizontalAlignment=Alignment.CenterHorizontally){
                    Icon(Icons.Default.MenuBook,"Manuscript hierarchy",Modifier.padding(vertical=8.dp),tint=if(mode=="Hierarchy")MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=.45f))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if(mode=="Hierarchy")MaterialTheme.colorScheme.primary else Color.Transparent))
                }
            }
            when(mode){
                "Modules"->{if(moduleKinds.isNotEmpty())WorldModuleListScreen(db,w,moduleTitle,moduleKinds,modulePrimaryKind,refresh+localRefresh,open){moduleKinds=emptyList()} else WorldModulesScreen(db,w,refresh+localRefresh,{title,kinds,primary->moduleTitle=title;moduleKinds=kinds;modulePrimaryKind=primary},{mode=it},onOpenMediaManager)}
                "Hierarchy"->{Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={createParentId=w.id;createChild=true}){Text("Add to world")};FilledTonalButton(onClick={showWorldSheetPicker=true},modifier=Modifier.testTag("world_new_from_sheet")){Icon(Icons.Default.DashboardCustomize,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("New from Sheet")}};val allWorldRecords=remember(w.id,localRefresh,refresh){db.all()};val direct=allWorldRecords.filter{it.parentId==w.id};val series=direct.filter{it.kind=="Series"};val stories=direct.filter{it.kind=="Story"||it.kind=="Novel"};val resources=direct.filter{it.kind!="Series"&&it.kind!="Story"};LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){if(series.isNotEmpty())item{Text("Series",fontWeight=FontWeight.SemiBold,color=Plum)};items(series){seriesRecord->var expanded by remember(w.id,seriesRecord.id){mutableStateOf(true)};val seriesWords=wordSnapshot.subtreeWords(seriesRecord.id);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(seriesRecord.title,fontWeight=FontWeight.SemiBold);Text("%,d words".format(seriesWords),fontSize=11.sp,color=Plum)};TextButton(onClick={open(seriesRecord)}){Text("Open")};IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,"Expand series")}};if(expanded)WorldTreeChildren(db,seriesRecord.id,1,open,onRead,allWorldRecords,wordSnapshot)}}};if(stories.isNotEmpty())item{Text("Standalone stories",fontWeight=FontWeight.SemiBold,color=Plum)};items(stories){story->var expanded by remember(w.id,story.id){mutableStateOf(true)};val storyWords=wordSnapshot.subtreeWords(story.id);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(story.title,fontWeight=FontWeight.SemiBold);Text("%,d words".format(storyWords),fontSize=11.sp,color=Plum)};TextButton(onClick={open(story)}){Text("Open")};IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,"Expand story")}};if(expanded)WorldTreeChildren(db,story.id,1,open,onRead,allWorldRecords,wordSnapshot)}}};if(resources.isNotEmpty())item{Text("World resources",fontWeight=FontWeight.SemiBold,color=Plum)};items(resources){resource->Column{RecordCard(db,resource,open,onRead);WorldTreeChildren(db,resource.id,1,open,onRead,allWorldRecords,wordSnapshot)}}}}
                "Encyclopedia"->{WorldEncyclopediaScreen(db,w,refresh+localRefresh,onEditArticle=open,onReadArticle=onRead,onBack={mode="Modules"})}
                "Resources"->{WorldResourcesScreen(db,w,refresh+localRefresh,open,onRead,onOpenMediaManager){parentId->createParentId=parentId;createChild=true}}
                "Timeline"->{val events=db.timeline(w.id);Row(verticalAlignment=Alignment.CenterVertically){Text("World chronology",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Button(onClick={eventDialog=true}){Text("Add event")}};LazyColumn{itemsIndexed(events){index,e->Row(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(24.dp)){Box(Modifier.size(12.dp).background(Plum,CircleShape));if(index<events.lastIndex)Box(Modifier.width(2.dp).height(70.dp).background(Plum.copy(alpha=.5f))) };Card(Modifier.weight(1f)){Column(Modifier.padding(12.dp)){Text(e.second,color=Plum,fontSize=12.sp);Text(e.first,fontWeight=FontWeight.SemiBold);Text(e.third,fontSize=12.sp)}}}}}}
                "Calendar"->{val calendars=remember(w.id,localRefresh){db.worldCalendars(w.id)};Row(verticalAlignment=Alignment.CenterVertically){Text("Custom world calendars",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Button(onClick={calendarDialog=true}){Text("New calendar")}};calendars.forEach{cal->Card(Modifier.fillMaxWidth().clickable{selectedCalendar=cal["id"].orEmpty();calendarMonthIndex=0}.padding(vertical=3.dp)){Column(Modifier.padding(10.dp)){Text(cal["name"].orEmpty(),fontWeight=FontWeight.SemiBold);Text("Months: "+cal["months"].orEmpty(),fontSize=11.sp);Text("Moons: "+cal["moons"].orEmpty(),fontSize=11.sp)}}};if(selectedCalendar.isNotBlank()){val cal=calendars.firstOrNull{it["id"]==selectedCalendar};val months=(cal?.get("months")?: "Month 1").split(",").map{it.trim()}.filter{it.isNotBlank()}.ifEmpty{listOf("Month 1")};val monthIndex=calendarMonthIndex.coerceIn(0,months.lastIndex);val lengths=(cal?.get("monthLengths")?: "").split(",").map{it.trim().toIntOrNull()?:30};val daysInMonth=lengths.getOrNull(monthIndex)?.coerceIn(1,100)?:30;val events=db.calendarEntries(selectedCalendar);Row(verticalAlignment=Alignment.CenterVertically){Text(cal?.get("name")?:"Calendar",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);TextButton(onClick={calendarEntryYear=calendarYear.toString();calendarEntryMonth=(monthIndex+1).toString();calendarEntryDay="1";calendarEntryDialog=true}){Text("Add date")}};Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){TextButton(onClick={if(monthIndex>0)calendarMonthIndex=monthIndex-1 else{calendarMonthIndex=months.lastIndex;calendarYear--}}){Text("‹")};Text("${months[monthIndex]} · Year $calendarYear",Modifier.weight(1f),textAlign=TextAlign.Center,fontWeight=FontWeight.SemiBold);TextButton(onClick={if(monthIndex<months.lastIndex)calendarMonthIndex=monthIndex+1 else{calendarMonthIndex=0;calendarYear++}}){Text("›")}};Row(Modifier.fillMaxWidth()){cal?.get("weekdays").orEmpty().split(",").map{it.trim()}.filter{it.isNotBlank()}.take(7).forEach{Text(it,Modifier.weight(1f),fontSize=9.sp,textAlign=TextAlign.Center)}};LazyVerticalGrid(columns=GridCells.Fixed(7),modifier=Modifier.height(300.dp)){items((1..daysInMonth).toList()){day->val dayEvents=events.filter{(it.year==calendarYear||it.repeatYearly)&&it.month==monthIndex+1&&it.day==day};Card(Modifier.padding(2.dp).clickable{calendarEntryYear=calendarYear.toString();calendarEntryMonth=(monthIndex+1).toString();calendarEntryDay=day.toString();calendarEntryDialog=true}){Column(Modifier.padding(5.dp)){Text(day.toString(),fontWeight=FontWeight.Bold);dayEvents.take(2).forEach{Text(it.title,maxLines=2,fontSize=9.sp,color=Plum)}}}}};Text("Seasons: "+cal?.get("seasons").orEmpty(),fontSize=11.sp);Text("Moons: "+cal?.get("moons").orEmpty(),fontSize=11.sp);Text("Eras: "+cal?.get("eras").orEmpty(),fontSize=11.sp);Text("Leap rules: "+cal?.get("leapRules").orEmpty(),fontSize=11.sp)}}
                "Boards"->{val boards=remember(w.id,localRefresh){db.boards(w.id)};Row(verticalAlignment=Alignment.CenterVertically){Text("Visual planning boards",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Button(onClick={boardDialog=true}){Text("New board")}};boards.forEach{b->Card(Modifier.fillMaxWidth().clickable{selectedBoard=b.first}.padding(vertical=3.dp)){Row(Modifier.padding(12.dp)){Text(b.second,Modifier.weight(1f));Text(b.third,color=Plum,fontSize=11.sp)}}};if(selectedBoard.isNotBlank()&&db.boardType(selectedBoard)=="Map")MapScreen(db,selectedBoard,w.id,{open(it)},{boardId->selectedBoard=boardId;localRefresh++},{selectedBoard=""})else if(selectedBoard.isNotBlank()){val boardItems=remember(selectedBoard,localRefresh){db.boardItems(selectedBoard)};Row(verticalAlignment=Alignment.CenterVertically){Text("Board items",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);TextButton(onClick={db.autoArrangeBoard(selectedBoard,db.boardType(selectedBoard));localRefresh++}){Text("Auto arrange")};TextButton(onClick={boardBackgroundPicker.launch(arrayOf("image/*"))}){Text("Background")};TextButton(onClick={boardItemDialog=true}){Text("Add card")};TextButton(onClick={linkMode=!linkMode;linkFrom=""}){Text(if(linkMode)"Cancel linking" else "Link cards")}};if(linkMode){Text(if(linkFrom.isBlank())"Choose the first card, then the card it connects to." else "Now choose the second card.",fontSize=12.sp,color=Plum);OutlinedTextField(boardLinkLabel,{boardLinkLabel=it},label={Text("Connection label")},singleLine=true)};Box(Modifier.fillMaxWidth().height(560.dp).verticalScroll(rememberScrollState())){val bgUri=db.boardBackground(selectedBoard);val bgBitmap=remember(bgUri){if(bgUri.isNotBlank())runCatching{context.contentResolver.openInputStream(Uri.parse(bgUri))?.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()else null};if(bgBitmap!=null)Image(bgBitmap,"Board background",Modifier.matchParentSize(),contentScale=ContentScale.Crop,alpha=0.32f);Canvas(Modifier.matchParentSize()){val scale=density;db.boardLinks(selectedBoard).forEach{edge->val a=db.boardItemPosition(edge.first);val b=db.boardItemPosition(edge.second);val start=Offset((a.first+77f)*scale,(a.second+50f)*scale);val end=Offset((b.first+77f)*scale,(b.second+50f)*scale);val edgeColor=Color(0xFF9B83B5);drawLine(edgeColor,start,end,strokeWidth=3f);val angle=kotlin.math.atan2((end.y-start.y).toDouble(),(end.x-start.x).toDouble());val left=Offset(end.x-14f*kotlin.math.cos(angle-0.55).toFloat(),end.y-14f*kotlin.math.sin(angle-0.55).toFloat());val right=Offset(end.x-14f*kotlin.math.cos(angle+0.55).toFloat(),end.y-14f*kotlin.math.sin(angle+0.55).toFloat());drawLine(edgeColor,end,left,strokeWidth=3f);drawLine(edgeColor,end,right,strokeWidth=3f)}};Column(Modifier.align(Alignment.TopEnd).padding(5.dp)){db.boardLinks(selectedBoard).forEach{edge->Text("${boardItems.firstOrNull{it.first==edge.first}?.second.orEmpty()} ${db.boardLinkLabel(selectedBoard,edge.first,edge.second)} ${boardItems.firstOrNull{it.first==edge.second}?.second.orEmpty()}",fontSize=9.sp,color=Color.White)}};boardItems.forEach{item->var pos by remember(item.first){mutableStateOf(db.boardItemPosition(item.first))};val density=LocalDensity.current;val cardImageUri=db.boardItemImage(item.first);val cardImage=remember(cardImageUri){if(cardImageUri.isNotBlank())runCatching{context.contentResolver.openInputStream(Uri.parse(cardImageUri))?.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}}.getOrNull()else null};val cardColor=remember(item.first){runCatching{Color(android.graphics.Color.parseColor(db.boardItemColor(item.first)))}.getOrDefault(Color(0xFFD9C7EE))};Card(Modifier.offset(x=pos.first.dp,y=pos.second.dp).width(155.dp).clickable{if(linkMode){if(linkFrom.isBlank())linkFrom=item.first else if(linkFrom!=item.first){db.linkBoardItems(selectedBoard,linkFrom,item.first,boardLinkLabel);linkFrom="";localRefresh++}}}.pointerInput(item.first){detectDragGestures(onDragEnd={db.moveBoardItem(item.first,pos.first,pos.second)}){change,drag->change.consume();with(density){pos=(pos.first+drag.x.toDp().value).coerceAtLeast(0f) to (pos.second+drag.y.toDp().value).coerceAtLeast(0f)}}},colors=CardDefaults.cardColors(containerColor=cardColor)){Column(Modifier.padding(10.dp)){if(cardImage!=null)Image(cardImage,"Board card image",Modifier.fillMaxWidth().height(90.dp),contentScale=ContentScale.Crop);Text(item.second,fontWeight=FontWeight.SemiBold);Text(item.third,fontSize=12.sp);val linkedRecord=db.boardItemRecord(item.first);if(linkedRecord.isNotBlank())TextButton(onClick={db.get(linkedRecord)?.let(open)}){Text("Open linked record",fontSize=10.sp)};if(linkFrom==item.first)Text("Selected",color=Plum,fontSize=10.sp)}}}}}}
                "Connections"->{val related=db.descendants(w.id);Row(verticalAlignment=Alignment.CenterVertically){Text("World connection explorer",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);Button(onClick={val graph=db.createBoard(w.id,w.title+" Connections","Relationship Graph");val nodes=related.associate{record->record.id to db.addBoardItem(graph,record.title,record.kind,recordId=record.id)};related.forEach{record->val from=nodes[record.id];if(from!=null){db.relationships(record.id).forEach{relation->nodes[relation.third]?.let{to->db.linkBoardItems(graph,from,to,relation.second)}};db.outgoingLinks(record.id).forEach{mention->nodes[mention.recordId]?.let{to->db.linkBoardItems(graph,from,to,"mentions: "+mention.term)}}}};selectedBoard=graph;mode="Boards";localRefresh++}){Text("Build graph")}};Text("This creates an image-backed, draggable relationship graph from the world’s linked records and automatic mention links.");related.forEach{record->val links=db.relationships(record.id);val mentions=db.outgoingLinks(record.id);if(links.isNotEmpty()||mentions.isNotEmpty()){Text(record.title,fontWeight=FontWeight.SemiBold);links.forEach{Text("• ${it.second}: ${it.first}",fontSize=12.sp)};mentions.forEach{Text("• Mentions ${it.title}: ${it.term}",fontSize=12.sp,color=Plum)}}}}
            }
        }
    }
    if(showWorldSheetPicker)TemplatePickerDialog(
        db = db,
        title = "Add Sheet to ${db.get(createParentId)?.title ?: world?.title ?: "World"}",
        onDismiss = { showWorldSheetPicker = false },
        onSelect = { tpl ->
            val w = world
            if (w != null) {
                val targetParent = createParentId.ifBlank{w.id}
                val created = db.createRecordFromTemplate(tpl.id, parentId = targetParent, scope = "World")
                showWorldSheetPicker = false
                localRefresh++
                open(created)
            }
        }
    )
    if(showWorldCustomTemplate && world!=null)CreateTemplateDialog(
        db = db,
        parentWorldId = createParentId.ifBlank{world!!.id},
        onDismiss = { showWorldCustomTemplate = false },
        onCreatedForDesign = { tpl ->
            val targetParent = createParentId.ifBlank{world!!.id}
            val created = db.createRecordFromTemplate(tpl.id, parentId = targetParent, scope = "World")
            showWorldCustomTemplate = false
            localRefresh++
            open(created)
        },
        onCreatedAndUse = { created ->
            showWorldCustomTemplate = false
            localRefresh++
            open(created)
        }
    )
    if(createChild)AlertDialog(onDismissRequest={createChild=false},title={Text("Add to ${db.get(createParentId)?.title?:world?.title?: "world"}")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState())){Button(onClick={createChild=false;showWorldSheetPicker=true},modifier=Modifier.fillMaxWidth().padding(bottom=6.dp)){Text("From Fantasy Template Sheet...")};FantasyTemplateCatalog.starterRecordKinds.filter{it!="All"&&it!="World"&&it !in setOf("Book","Chapter","Scene")}.forEach{kind->TextButton(onClick={world?.let{val r=InkRecord(title="Untitled $kind",kind=kind,parentId=createParentId.ifBlank{it.id},scope="World");db.save(r);open(r)};createChild=false}){Text(kind)}};HorizontalDivider();Text("Custom worldbuilding type",fontWeight=FontWeight.SemiBold);Text("Create a reusable template for a type unique to this project.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.7f));TextButton(onClick={createChild=false;showWorldCustomTemplate=true}){Text("+ Create custom template")};world?.let{w->val customTypes=db.templates().filter{it.parentId==createParentId.ifBlank{w.id}&&it.kind=="Template"};if(customTypes.isNotEmpty()){Text("Custom types: "+customTypes.joinToString(", "){it.title},fontSize=12.sp,color=Plum);Text("To create an entry with its custom fields, open Templates and choose Use Sheet on that type.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.7f))}}}},confirmButton={TextButton(onClick={createChild=false}){Text("Close")}})
    if(calendarDialog)AlertDialog(onDismissRequest={calendarDialog=false},title={Text("Create custom calendar")},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(calendarName,{calendarName=it},label={Text("Calendar name")});OutlinedTextField(calendarMonths,{calendarMonths=it},label={Text("Months, comma separated")});OutlinedTextField(calendarMonthLengths,{calendarMonthLengths=it},label={Text("Days per month, comma separated")});OutlinedTextField(calendarEras,{calendarEras=it},label={Text("Eras")});OutlinedTextField(calendarLeapRules,{calendarLeapRules=it},label={Text("Leap-year rules")});OutlinedTextField(calendarWeekdays,{calendarWeekdays=it},label={Text("Weekdays, comma separated")});OutlinedTextField(calendarSeasons,{calendarSeasons=it},label={Text("Seasons")});OutlinedTextField(calendarMoons,{calendarMoons=it},label={Text("Moons / lunar cycles")})}},confirmButton={TextButton(onClick={if(calendarName.isNotBlank()){world?.let{selectedCalendar=db.createWorldCalendar(it.id,calendarName,calendarMonths,calendarWeekdays,calendarSeasons,calendarMoons,monthLengths=calendarMonthLengths,eras=calendarEras,leapRules=calendarLeapRules)};calendarDialog=false;localRefresh++}}){Text("Create")}},dismissButton={TextButton(onClick={calendarDialog=false}){Text("Cancel")}})
    if(calendarEntryDialog)AlertDialog(onDismissRequest={calendarEntryDialog=false},title={Text("Calendar date")},text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)){OutlinedTextField(calendarEntryTitle,{calendarEntryTitle=it},label={Text("Event name")});OutlinedTextField(calendarEntryYear,{calendarEntryYear=it},label={Text("Year")});OutlinedTextField(calendarEntryMonth,{calendarEntryMonth=it},label={Text("Month number")});OutlinedTextField(calendarEntryDay,{calendarEntryDay=it},label={Text("Day")});OutlinedTextField(calendarEntryDescription,{calendarEntryDescription=it},label={Text("Notes")});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(calendarRepeat,{calendarRepeat=it});Text("Repeat every calendar year")}}},confirmButton={TextButton(onClick={val y=calendarEntryYear.toIntOrNull();val m=calendarEntryMonth.toIntOrNull();val d=calendarEntryDay.toIntOrNull();val selectedCal=world?.let{db.worldCalendars(it.id).firstOrNull{cal->cal["id"]==selectedCalendar}};val monthCount=maxOf(1,(selectedCal?.get("months")?: "").split(",").filter{it.isNotBlank()}.size);val lengths=(selectedCal?.get("monthLengths")?: "").split(",").map{it.trim().toIntOrNull()?:30};val maxDays=lengths.getOrNull((m?:1)-1)?:30;if(calendarEntryTitle.isNotBlank()&&y!=null&&m!=null&&d!=null&&m in 1..monthCount&&d in 1..maxDays){db.addCalendarEntry(selectedCalendar,y,m,d,calendarEntryTitle,calendarEntryDescription,calendarRepeat);calendarEntryTitle="";calendarEntryDescription="";calendarRepeat=false;calendarEntryDialog=false;localRefresh++}}){Text("Save date")}},dismissButton={TextButton(onClick={calendarEntryDialog=false}){Text("Cancel")}})
    if(eventDialog)AlertDialog(onDismissRequest={eventDialog=false},title={Text("Timeline event")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(eventTitle,{eventTitle=it},label={Text("Event title")});OutlinedTextField(eventDate,{eventDate=it},label={Text("Date or era (sortable text)")});OutlinedTextField(eventDescription,{eventDescription=it},label={Text("Description")})}},confirmButton={TextButton(onClick={if(eventTitle.isNotBlank()&&eventDate.isNotBlank()){world?.let{db.addTimelineEvent(it.id,eventTitle,eventDate,description=eventDescription)};eventTitle="";eventDate="";eventDescription="";eventDialog=false;localRefresh++}}){Text("Save event")}},dismissButton={TextButton(onClick={eventDialog=false}){Text("Cancel")}})
    if(boardDialog)AlertDialog(onDismissRequest={boardDialog=false},title={Text("Create planning board")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(boardName,{boardName=it},label={Text("Board name")});Text("Board type");Row(Modifier.horizontalScroll(rememberScrollState())){boardTypes.forEach{FilterChip(boardType==it,{boardType=it},label={Text(it)})}}}},confirmButton={TextButton(onClick={if(boardName.isNotBlank()){world?.let{selectedBoard=db.createBoard(it.id,boardName,boardType)};boardName="";boardDialog=false;localRefresh++}}){Text("Create")}},dismissButton={TextButton(onClick={boardDialog=false}){Text("Cancel")}})
    if(boardItemDialog)AlertDialog(onDismissRequest={boardItemDialog=false},title={Text("Add board card")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(boardItemTitle,{boardItemTitle=it},label={Text("Title")});OutlinedTextField(boardItemBody,{boardItemBody=it},label={Text("Notes")});OutlinedTextField(boardItemColor,{boardItemColor=it},label={Text("Card color · hex")},singleLine=true);Box{OutlinedButton(onClick={boardRecordMenu=true}){Text("Linked record: "+(db.get(boardItemRecordId)?.title?: "None"))};DropdownMenu(expanded=boardRecordMenu,onDismissRequest={boardRecordMenu=false}){DropdownMenuItem(text={Text("No linked record")},onClick={boardItemRecordId="";boardRecordMenu=false});db.all().filter{it.kind!="Template"}.forEach{r->DropdownMenuItem(text={Text(r.title)},onClick={boardItemRecordId=r.id;boardRecordMenu=false})}}};TextButton(onClick={boardItemImagePicker.launch(arrayOf("image/*"))}){Text(if(boardItemImageUri.isBlank())"Add image" else "Image selected")}}},confirmButton={TextButton(onClick={if(boardItemTitle.isNotBlank()){db.addBoardItem(selectedBoard,boardItemTitle,boardItemBody,recordId=boardItemRecordId,color=boardItemColor,imageUri=boardItemImageUri);boardItemTitle="";boardItemBody="";boardItemRecordId="";boardItemColor="#D9C7EE";boardItemImageUri="";boardItemDialog=false;localRefresh++}}){Text("Add")}},dismissButton={TextButton(onClick={boardItemDialog=false}){Text("Cancel")}})
    if(showWorldWordStats && world!=null){
        val w=world!!
        val worldReport=remember(w.id,wordSnapshot){WordCountEngine.buildReport(wordSnapshot,w,w.body)}
        WordCountDashboardDialog(
            db = db,
            report = worldReport,
            sessionStartWords = worldReport.currentStats.totalWords,
            onGoalUpdated = { localRefresh++ },
            onOpenRecord = { target ->
                showWorldWordStats = false
                open(target)
            },
            onDismiss = { showWorldWordStats = false }
        )
    }
}
@Composable fun WorldResourcesScreen(db:InkDatabase,world:InkRecord,refresh:Int,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit,onOpenMediaManager:()->Unit={},onCreate:(String)->Unit){
    var query by remember(world.id){mutableStateOf("")}
    var selectedKind by remember(world.id){mutableStateOf("All")}
    var selectedProjectId by remember(world.id){mutableStateOf(world.id)}
    val excludedKinds=setOf("World","Series","Story","Novel","Book","Chapter","Scene")
    val allRecords=remember(world.id,refresh){db.all()}
    val projects=allRecords.filter{it.parentId==world.id&&(it.kind=="Series"||it.kind=="Story")}.sortedBy{it.title.lowercase()}
    val projectLabel=projects.firstOrNull{it.id==selectedProjectId}?.title?:world.title
    val resources=remember(world.id,refresh,selectedProjectId,projects){
        val contained=if(selectedProjectId==world.id)db.descendants(world.id)else db.descendants(selectedProjectId)
        val explicitlyShared=if(selectedProjectId==world.id)emptyList()else allRecords.filter{selectedProjectId in db.sharedProjects(it.id)}
        (contained+explicitlyShared).distinctBy{it.id}.filter{it.kind !in excludedKinds}
            .sortedWith(compareBy<InkRecord>({it.kind},{it.title.lowercase()}))
    }
    val kinds=resources.map{it.kind}.distinct().sorted()
    val filtered=resources.filter{(selectedKind=="All"||it.kind==selectedKind)&&(query.isBlank()||it.title.contains(query,true)||it.kind.contains(query,true)||it.tags.contains(query,true)||it.aliases.contains(query,true))}
    Column(Modifier.fillMaxSize().padding(4.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Column(Modifier.weight(1f)){
                Text(if(selectedProjectId==world.id)"World resource library" else "$projectLabel resources",fontWeight=FontWeight.SemiBold)
                Text(resources.size.toString()+" linked resources in this project",fontSize=11.sp,color=Plum)
            }
            FilledTonalButton(onClick=onOpenMediaManager){Icon(Icons.Default.PhotoCamera,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("Media")}
            Button(onClick={onCreate(selectedProjectId)}){Icon(Icons.Default.Add,null);Text("Add")}
        }
        if(projects.isNotEmpty()){
            Text("Project view",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                FilterChip(selectedProjectId==world.id,{selectedProjectId=world.id;selectedKind="All"},label={Text("Whole world")})
                projects.forEach{project->FilterChip(selectedProjectId==project.id,{selectedProjectId=project.id;selectedKind="All"},label={Text(project.title,maxLines=1)})}
            }
        }
        OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Search resources")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)})
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            FilterChip(selectedKind=="All",{selectedKind="All"},label={Text("All")})
            kinds.forEach{kind->FilterChip(selectedKind==kind,{selectedKind=kind},label={Text(kind)})}
        }
        if(filtered.isEmpty())Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){
            Text(if(resources.isEmpty())"No resources in this project yet. Add characters, locations, cultures, factions, and other reference records, or share existing records with this project." else "No resources match this search.",color=MaterialTheme.colorScheme.onSurface.copy(alpha=.7f))
        }else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
            items(filtered,key={it.id}){resource->
                val shared=selectedProjectId!=world.id&&selectedProjectId in db.sharedProjects(resource.id)
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(resource.kind,fontSize=11.sp,color=Plum,fontWeight=FontWeight.SemiBold)
                        if(shared){Spacer(Modifier.width(6.dp));Text("Shared",fontSize=10.sp,color=MaterialTheme.colorScheme.tertiary)}
                        Spacer(Modifier.weight(1f))
                        val parentTitle=db.get(resource.parentId)?.title?:world.title
                        Text("In "+parentTitle,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f),maxLines=1)
                    }
                    RecordCard(db,resource,open,onRead)
                }
            }
        }
    }
}

@Composable private fun RecordCoverArt(record:InkRecord,modifier:Modifier=Modifier,description:String="Cover artwork"){
    val context=LocalContext.current
    val bitmap=remember(record.coverUri){
        if(record.coverUri.isNotBlank())runCatching{
            context.contentResolver.openInputStream(Uri.parse(record.coverUri))?.use{BitmapFactory.decodeStream(it)?.asImageBitmap()}
        }.getOrNull()else null
    }
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(Plum.copy(alpha=.16f)),contentAlignment=Alignment.Center){
        if(bitmap!=null)Image(bitmap,description,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            Icon(when(record.kind){
                "World","Realm","Celestial"->Icons.Default.Public
                "Series","Book","Novel","Story","Chapter"->Icons.Default.AutoStories
                "Poem"->Icons.Default.FormatQuote
                "Character","NPC"->Icons.Default.Person
                else->Icons.Default.MenuBook
            },null,tint=Plum,modifier=Modifier.size(25.dp))
            Text(record.kind.uppercase(),fontSize=8.sp,fontWeight=FontWeight.Bold,color=Plum,maxLines=1)
        }
    }
}

@Composable private fun SeriesCollectionCard(db:InkDatabase,series:InkRecord,books:List<InkRecord>,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit,wordSnapshot:WorldWordSnapshot){
    var expanded by remember(series.id){mutableStateOf(true)}
    val seriesWords=wordSnapshot.subtreeWords(series.id)
    Card(Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                RecordCoverArt(series,Modifier.width(76.dp).height(108.dp),series.title+" series cover")
                Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text("SERIES COLLECTION",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=Plum)
                    Text(series.title.ifBlank{"Untitled series"},fontSize=17.sp,fontWeight=FontWeight.Bold,maxLines=2)
                    Text(books.size.toString()+" "+(if(books.size==1)"book" else "books")+" · "+WordCountEngine.formatCompact(seriesWords)+" words",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick={open(series)},contentPadding=PaddingValues(horizontal=0.dp,vertical=0.dp)){
                        Text("Open series")
                        Icon(Icons.Default.ArrowForward,null,Modifier.padding(start=4.dp).size(15.dp))
                    }
                }
                IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,if(expanded)"Collapse books" else "Show books")}
            }
            if(expanded){
                if(books.isNotEmpty()){
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        books.forEachIndexed{index,book->
                            Column(Modifier.width(88.dp).clickable{open(book)},verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Box{
                                    RecordCoverArt(book,Modifier.fillMaxWidth().height(124.dp),book.title+" cover")
                                    Box(Modifier.align(Alignment.TopStart).padding(5.dp).background(Color.Black.copy(alpha=.72f),androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(horizontal=6.dp,vertical=3.dp)){
                                        Text((index+1).toString(),fontSize=9.sp,fontWeight=FontWeight.Bold,color=Color.White)
                                    }
                                }
                                Text(book.title.ifBlank{"Untitled"},fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=2,lineHeight=14.sp)
                                Text(book.kind,fontSize=9.sp,color=Plum,maxLines=1)
                                if(book.kind in listOf("Book","Novel","Story","Chapter"))TextButton(onClick={onRead(book)},contentPadding=PaddingValues(0.dp)){Text("Read",fontSize=10.sp)}
                            }
                        }
                    }
                }else Text("No books in this series yet.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={
                    val next=InkRecord(title="Untitled Book",kind="Book",parentId=series.id,scope=series.scope)
                    db.save(next)
                    open(next)
                },contentPadding=PaddingValues(horizontal=0.dp,vertical=0.dp)){
                    Icon(Icons.Default.Add,null,Modifier.size(16.dp))
                    Text(" Add a book")
                }
            }
        }
    }
}

@Composable private fun WorldListCard(db:InkDatabase,world:InkRecord,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit={},wordSnapshot:WorldWordSnapshot=remember(world.id){WordCountEngine.buildSnapshot(db)},onOpenWorld:(InkRecord)->Unit={}){
    var expandedStories by remember(world.id){mutableStateOf(true)}
    val allRecords=wordSnapshot.allRecords
    val direct=allRecords.filter{it.parentId==world.id}
    val series=direct.filter{it.kind=="Series"}
    val stories=direct.filter{it.kind=="Story"||it.kind=="Novel"}
    val worldTotalWords=wordSnapshot.subtreeWords(world.id)
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Card(Modifier.fillMaxWidth().clickable{onOpenWorld(world)},shape=androidx.compose.foundation.shape.RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                RecordCoverArt(world,Modifier.width(84.dp).height(112.dp),world.title+" world artwork")
                Column(Modifier.weight(1f).padding(start=13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text("FICTIONAL UNIVERSE",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp,color=Plum)
                    Text(world.title.ifBlank{"Untitled world"},fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=2)
                    Text(db.descendants(world.id).size.toString()+" connected entries",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(WordCountEngine.formatCompact(worldTotalWords)+" words",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Plum)
                    Text("Open world  →",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Plum)
                }
            }
        }
        if(series.isNotEmpty()){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Series",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text(series.size.toString()+" COLLECTIONS",fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,color=Plum)
            }
            series.forEach{seriesRecord->
                val books=allRecords.filter{it.parentId==seriesRecord.id}.sortedBy{it.position}
                SeriesCollectionCard(db,seriesRecord,books,open,onRead,wordSnapshot)
            }
        }
        if(stories.isNotEmpty()){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Standalone stories",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                IconButton(onClick={expandedStories=!expandedStories}){Icon(if(expandedStories)Icons.Default.ExpandLess else Icons.Default.ExpandMore,"Toggle standalone stories")}
            }
            if(expandedStories)stories.forEach{story->
                Card(Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
                    Row(Modifier.fillMaxWidth().clickable{open(story)}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        RecordCoverArt(story,Modifier.width(64.dp).height(88.dp),story.title+" cover")
                        Column(Modifier.weight(1f).padding(start=11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(story.title,fontWeight=FontWeight.SemiBold,maxLines=2)
                            Text(story.kind,fontSize=10.sp,color=Plum)
                            Text(WordCountEngine.formatCompact(wordSnapshot.subtreeWords(story.id))+" words",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick={onRead(story)}){Icon(Icons.Default.MenuBook,"Read "+story.title)}
                    }
                }
            }
        }
    }
}

@Composable private fun WorldTreeChildren(db:InkDatabase,parentId:String,depth:Int,open:(InkRecord)->Unit,onRead:(InkRecord)->Unit={},records:List<InkRecord> = db.all(),wordSnapshot:WorldWordSnapshot? = null){
    if(depth>8)return
    val parent=db.get(parentId)
    val nextKind=when(parent?.kind){"Series"->"Book";"Story"->"Chapter";"Novel"->"Chapter";"Book"->"Chapter";"Chapter"->"Scene";else->""}
    val children=records.filter{it.parentId==parentId}
    var expanded by remember(parentId){mutableStateOf(true)}
    Column(Modifier.padding(start=(depth*10).dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
        if(nextKind.isNotBlank())TextButton(onClick={val child=InkRecord(title="Untitled $nextKind",kind=nextKind,parentId=parentId,scope=parent?.scope?: "World");db.save(child);open(child)},contentPadding=PaddingValues(horizontal=8.dp,vertical=2.dp)){Icon(Icons.Default.Add,null,modifier=Modifier.size(16.dp));Text(" Add $nextKind")}
        if(children.isNotEmpty())Row(verticalAlignment=Alignment.CenterVertically){Text(children.size.toString()+" "+(if(children.size==1)"item" else "items"),Modifier.weight(1f),fontSize=11.sp,color=Plum);TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,modifier=Modifier.size(16.dp));Text(if(expanded)"Collapse" else "Expand",fontSize=11.sp)}}
        if(expanded)children.forEach{child->
            val subWords=wordSnapshot?.subtreeWords(child.id)?:0
            Column(Modifier.padding(start=8.dp)){
                if(subWords>0 && child.kind in setOf("Series","Book","Novel","Story","Chapter")){
                    Text("${child.kind} subtree: %,d words".format(subWords),fontSize=10.sp,color=Plum,modifier=Modifier.padding(start=4.dp,bottom=2.dp))
                }
                RecordCard(db,child,open,onRead)
                WorldTreeChildren(db,child.id,depth+1,open,onRead,records,wordSnapshot)
            }
        }
    }
}

@Composable private fun TemplateScreen(
    db: InkDatabase,
    refresh: Int,
    autoSaveStatus: AutoSaveStatus,
    onAutoSaveNow: () -> Unit,
    onOpenTemplateLibrary: () -> Unit,
    onOpenMediaManager: () -> Unit = {},
    edit: (InkRecord) -> Unit,
    designBlueprint: (InkRecord) -> Unit,
    use: (InkRecord) -> Unit
) {
    val context = LocalContext.current
    var templateQuery by remember { mutableStateOf("") }
    var templateCategory by remember { mutableStateOf("All") }
    var fieldRefresh by remember { mutableIntStateOf(0) }
    var restoreTemplatesDialog by remember { mutableStateOf(false) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<InkRecord?>(null) }

    val templates = remember(refresh, templateQuery, templateCategory, fieldRefresh) {
        db.templates().filter { t ->
            val catMatch = when (templateCategory) {
                "All" -> true
                "My Custom" -> !t.id.startsWith("template_")
                else -> t.tags == templateCategory
            }
            (templateQuery.isBlank() ||
                t.title.contains(templateQuery, true) ||
                t.body.contains(templateQuery, true) ||
                t.scope.contains(templateQuery, true)) &&
                catMatch
        }
    }

    val importTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@runCatching
                val imported = db.importTemplateJson(raw)
                fieldRefresh++
                designBlueprint(imported)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Template gallery", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { importTemplateLauncher.launch(arrayOf("application/json", "text/*")) }) { Text("Import") }
            TextButton(onClick = { restoreTemplatesDialog = true }) { Text("Restore") }
            Button(
                onClick = { showCreateTemplateDialog = true },
                modifier = Modifier.testTag("create_template_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Create")
            }
        }
        Text(
            "Browse ${templates.size} fantasy worldbuilding & writing sheets. Use a sheet directly to create a live structured entry, or edit any blueprint.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val savedLabel = when {
                autoSaveStatus.isSaving -> "DataStore auto-save (30s): Saving..."
                autoSaveStatus.lastSavedAt > 0L -> "DataStore auto-save (30s) · Saved ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(autoSaveStatus.lastSavedAt))}"
                else -> "DataStore auto-save: Active every 30s"
            }
            Text(
                text = savedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).testTag("autosave_status_text")
            )
            TextButton(
                onClick = onAutoSaveNow,
                modifier = Modifier.testTag("autosave_now_button")
            ) {
                Text("Save Now")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenTemplateLibrary,
                modifier = Modifier.weight(1f).testTag("open_template_library_button")
            ) {
                Icon(Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("JSON Library", maxLines = 1)
            }
            FilledTonalButton(
                onClick = onOpenMediaManager,
                modifier = Modifier.testTag("template_gallery_media_button")
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Media")
            }
        }
        OutlinedTextField(
            value = templateQuery,
            onValueChange = { templateQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("template_gallery_search"),
            label = { Text("Search fantasy sheets (character, flora, dragon, spell...)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (listOf("All", "My Custom") + FantasyTemplateCatalog.categories.filter { it != "All" }).forEach { category ->
                FilterChip(
                    selected = templateCategory == category,
                    onClick = { templateCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.testTag("template_gallery_cat_${category.lowercase().replace(" ", "_")}")
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(templates, key = { it.id }) { t ->
                val isUserCustom = !t.id.startsWith("template_")
                val fieldsCount = remember(t.id, fieldRefresh, refresh) { db.templateFields(t.id).size }
                val sectionsCount = remember(t.id, fieldRefresh, refresh) { db.templateSections(t.id).size }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                t.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isUserCustom) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "CUSTOM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                t.scope.ifBlank { "Document" }.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Plum
                            )
                        }
                        if (t.body.isNotBlank()) {
                            Text(
                                t.body.replace("\
", " "),
                                fontSize = 12.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            )
                        }
                        Text(
                            "${t.tags.ifBlank { "Other" }} · $sectionsCount sections · $fieldsCount fields",
                            fontSize = 11.sp,
                            color = Plum
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (t.kind == "Archived Template") {
                                FilledTonalButton(onClick = { db.restoreCustomType(t.id); fieldRefresh++ }) {
                                    Text("Restore Type")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val created = db.createRecordFromTemplate(t.id)
                                        use(created)
                                    },
                                    modifier = Modifier.testTag("use_template_${t.id}")
                                ) {
                                    Text("Use Sheet")
                                }
                            }
                            FilledTonalButton(
                                onClick = { designBlueprint(t) },
                                modifier = Modifier.testTag("edit_blueprint_${t.id}")
                            ) {
                                Text("Edit Blueprint")
                            }
                            TextButton(onClick = {
                                val dup = db.duplicateTemplate(t.id)
                                fieldRefresh++
                                if (dup != null) designBlueprint(dup)
                            }) { Text("Duplicate") }
                            TextButton(onClick = { edit(t) }) { Text("Notes") }
                            if (isUserCustom) {
                                TextButton(
                                    onClick = { deletingTemplate = t },
                                    modifier = Modifier.testTag("delete_template_${t.id}")
                                ) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateTemplateDialog) {
        CreateTemplateDialog(
            db = db,
            initialCategory = if (templateCategory in listOf("All", "My Custom")) "Other" else templateCategory,
            onDismiss = { showCreateTemplateDialog = false },
            onCreatedForDesign = { tpl ->
                showCreateTemplateDialog = false
                fieldRefresh++
                designBlueprint(tpl)
            },
            onCreatedAndUse = { created ->
                showCreateTemplateDialog = false
                fieldRefresh++
                use(created)
            }
        )
    }

    deletingTemplate?.let { tpl ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            title = { Text("Delete custom template?") },
            text = { Text("Delete \"${tpl.title}\"? Entries already created from it will remain in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteCustomTemplate(tpl.id)
                    deletingTemplate = null
                    fieldRefresh++
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTemplate = null }) { Text("Cancel") }
            }
        )
    }

    if (restoreTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { restoreTemplatesDialog = false },
            title = { Text("Restore starter fantasy templates?") },
            text = { Text("This restores all built-in starter fantasy templates and their original sections/fields. Your custom templates and entries created from templates are kept safe.") },
            confirmButton = {
                TextButton(onClick = {
                    db.restoreStarterTemplates()
                    restoreTemplatesDialog = false
                    fieldRefresh++
                }) { Text("Restore starters") }
            },
            dismissButton = {
                TextButton(onClick = { restoreTemplatesDialog = false }) { Text("Cancel") }
            }
        )
    }
}
@Composable private fun EditorScreen(
    db:InkDatabase,
    record:InkRecord,
    fontSize:Float,
    onSaved:(InkRecord)->Unit,
    onDelete:(String)->Unit,
    onLinkScan:(InkRecord)->Int,
    jumpTerm:String,
    jumpOccurrence:Int,
    onJumpHandled:()->Unit,
    onOpen:(InkRecord,String,Int)->Unit,
    onOpenBlueprint:(String)->Unit={},
    onReadMode:(InkRecord)->Unit={},
    onTemplateCreated:(InkRecord)->Unit={},
    onUpdateEntryDraft:(ActiveEntryDraft)->Unit={},
    onClearEntryDraft:(String)->Unit={},
    onOutline:(InkRecord)->Unit={}
){
    val appContext=LocalContext.current.applicationContext
    var title by remember(record.id){mutableStateOf(record.title)}
    var html by remember(record.id){mutableStateOf(record.body)}
    var coverUri by remember(record.id){mutableStateOf(record.coverUri)}
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{appContext.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)};coverUri=it.toString();onSaved(record.copy(coverUri=coverUri))}}
    var tags by remember(record.id){mutableStateOf(record.tags)}
    var aliases by remember(record.id){mutableStateOf(record.aliases)}
    var excludeTerm by remember{mutableStateOf("")}
    var newTagName by remember{mutableStateOf("")}
    var newTagType by remember{mutableStateOf("Category")}
    var tagTypeMenu by remember{mutableStateOf(false)}
    var tagRefresh by remember{mutableIntStateOf(0)}
    val sharedTags=remember(record.id,tagRefresh){db.allTags()}
    val assignedTagIds=remember(record.id,tagRefresh){db.tagsForRecord(record.id)}
    var snapshotLabel by remember{mutableStateOf("Alternate version")}
    var relationTarget by remember{mutableStateOf("")}
    var relationLabel by remember{mutableStateOf("related to")}
    var relationMenu by remember{mutableStateOf(false)}
    var parent by remember(record.id){mutableStateOf(record.parentId)}
    var scope by remember(record.id){mutableStateOf(record.scope)}
    var folderId by remember(record.id){mutableStateOf(db.folderFor(record.id))}
    var folderMenu by remember{mutableStateOf(false)}
    var details by remember{mutableStateOf(false)}
    var focusMode by remember{mutableStateOf(false)}
    var attachedTemplateId by remember(record.id){mutableStateOf(db.sheetTemplateId(record.id))}
    var sheetValues by remember(record.id){mutableStateOf(db.sheetValues(record.id))}
    var sheetRefresh by remember(record.id){mutableIntStateOf(0)}
    var editorMode by remember(record.id,attachedTemplateId){mutableStateOf(if(attachedTemplateId.isNotBlank())"Sheet" else "Prose")}
    var showSheetPicker by remember{mutableStateOf(false)}
    var showLibraryImport by remember{mutableStateOf(false)}
    var showSaveAsTemplate by remember{mutableStateOf(false)}
    var pageMode by remember{mutableStateOf(db.setting("pageMode","false")=="true")}
    var pageWidth by remember{mutableFloatStateOf(db.setting("pageWidth","760").toFloatOrNull()?:760f)}
    var pageMargin by remember{mutableFloatStateOf(db.setting("pageMargin","12").toFloatOrNull()?:12f)}
    var paragraphSpacing by remember{mutableFloatStateOf(db.setting("paragraphSpacing","12").toFloatOrNull()?:12f)}
    var editorBackground by remember{mutableStateOf(db.setting("editorBackground","transparent"))}
    var editorTextColor by remember{mutableStateOf(db.setting("editorTextColor","#eee8f5"))}
    var backgroundImageData by remember{mutableStateOf(db.setting("backgroundImageData",""))}
    var backgroundImageMime by remember{mutableStateOf(db.setting("backgroundImageMime","image/jpeg"))}
    val backgroundPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{val bytes=appContext.contentResolver.openInputStream(it)?.use{stream->stream.readBytes()}?:byteArrayOf();backgroundImageData=Base64.encodeToString(bytes,Base64.NO_WRAP);backgroundImageMime=appContext.contentResolver.getType(it)?: "image/jpeg";db.setSetting("backgroundImageData",backgroundImageData);db.setSetting("backgroundImageMime",backgroundImageMime)}}}
    var backgroundMenu by remember{mutableStateOf(false)}
    var textColorMenu by remember{mutableStateOf(false)}
    var fontFamily by remember{mutableStateOf(db.setting("fontFamily","sans-serif"))}
    var customFontData by remember{mutableStateOf(db.setting("customFontData",""))}
    var customFontMime by remember{mutableStateOf(db.setting("customFontMime","font/ttf"))}
    val fontPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{val bytes=appContext.contentResolver.openInputStream(it)?.use{stream->stream.readBytes()}?:byteArrayOf();customFontData=Base64.encodeToString(bytes,Base64.NO_WRAP);customFontMime=appContext.contentResolver.getType(it)?: "font/ttf";db.setSetting("customFontData",customFontData);db.setSetting("customFontMime",customFontMime);fontFamily="Custom";db.setSetting("fontFamily","Custom")}}}
    var lineSpacing by remember{mutableFloatStateOf(db.setting("lineSpacing","1.65").toFloatOrNull()?:1.65f)}
    var fontMenu by remember{mutableStateOf(false)}
    val toolbarOptions=listOf("undo","redo","bold","italic","underline","strikeThrough","subscript","superscript","inlineCode","formatBlock:H1","formatBlock:H2","formatBlock:H3","insertUnorderedList","insertOrderedList","checklist","hiliteColor","foreColor","justifyLeft","justifyCenter","justifyRight","justifyFull","indent","outdent","formatBlock:BLOCKQUOTE","insertHorizontalRule","removeFormat")
    var toolbarCommands by remember(record.id){
        val saved=db.setting("toolbarCommands",toolbarOptions.joinToString("|")).split("|").filter{it.isNotBlank()}.toSet()
        val migrated=if(db.setting("toolbarCommands_v3","")!="1"){
            val merged=saved+setOf("subscript","superscript","inlineCode","checklist","justifyFull")
            db.setSetting("toolbarCommands",merged.joinToString("|"))
            db.setSetting("toolbarCommands_v3","1")
            merged
        }else saved
        mutableStateOf(migrated)
    }
    var history by remember{mutableStateOf(false)}
    var parentMenu by remember{mutableStateOf(false)}
    val latestRecordState=rememberUpdatedState(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope,coverUri=coverUri))
    val latestSheetValues=rememberUpdatedState(sheetValues)
    LaunchedEffect(record.id,title,html,tags,aliases,parent,scope,coverUri,attachedTemplateId,sheetValues){
        onUpdateEntryDraft(
            ActiveEntryDraft(
                recordId = record.id,
                title = title,
                body = html,
                tags = tags,
                aliases = aliases,
                parentId = parent,
                scope = scope,
                coverUri = coverUri,
                templateId = attachedTemplateId,
                sheetValues = sheetValues
            )
        )
    }
    DisposableEffect(record.id){onDispose{db.save(latestRecordState.value);db.saveSheetValues(record.id,latestSheetValues.value);onClearEntryDraft(record.id)}}
    var linkCount by remember{mutableIntStateOf(0)}
    var showFindReplace by remember{mutableStateOf(false)}
    var findText by remember{mutableStateOf("")}
    var replaceText by remember{mutableStateOf("")}
    var attachmentRefresh by remember{mutableIntStateOf(0)}
    val attachmentList=remember(record.id,attachmentRefresh){db.attachments(record.id)}
    val mediaCount=remember(record.id,attachmentRefresh){db.mediaForRecord(record.id).size}
    var showMediaManager by remember{mutableStateOf(false)}
    var showWordDashboard by remember{mutableStateOf(false)}
    var wordGoalRefresh by remember{mutableIntStateOf(0)}
    val sheetFields=remember(attachedTemplateId,sheetRefresh){if(attachedTemplateId.isNotBlank())db.templateFields(attachedTemplateId) else emptyList()}
    val wordSnapshot=remember(record.id,parent,wordGoalRefresh){WordCountEngine.buildSnapshot(db)}
    val sessionStartWords=remember(record.id){WordCountEngine.analyzeDocument(record.body,sheetFields,sheetValues).totalWords}
    val liveWordReport=remember(record.id,html,sheetFields,sheetValues,parent,wordSnapshot){
        WordCountEngine.buildReport(
            snapshot = wordSnapshot,
            record = latestRecordState.value,
            liveHtml = html,
            liveFields = sheetFields,
            liveSheetValues = sheetValues,
            liveParentId = parent
        )
    }
    val attachmentPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{appContext.contentResolver.takePersistableUriPermission(it,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)};db.addAttachment(record.id,it.toString(),appContext.contentResolver.getType(it).orEmpty(),it.lastPathSegment.orEmpty());attachmentRefresh++}}

    fun compiledHtml():String = if(attachedTemplateId.isNotBlank()){
        val fields=db.templateFields(attachedTemplateId)
        val sections=db.templateSections(attachedTemplateId)
        SheetEngine.compileToHtml(fields,sections,sheetValues,html,{MediaManager.toInlineDataUri(appContext,it)}){db.get(it)}
    }else html

    fun compiledMarkdown():String = if(attachedTemplateId.isNotBlank()){
        val fields=db.templateFields(attachedTemplateId)
        val sections=db.templateSections(attachedTemplateId)
        SheetEngine.compileToMarkdown(title,fields,sections,sheetValues,html){db.get(it)}
    }else DocumentExport.markdown(html)

    fun compiledPlain():String = if(attachedTemplateId.isNotBlank()){
        val fields=db.templateFields(attachedTemplateId)
        val sections=db.templateSections(attachedTemplateId)
        val sheetPart=SheetEngine.compileToPlainText(fields,sections,sheetValues){db.get(it)}
        val prosePart=DocumentExport.plain(html).trim()
        listOf(sheetPart,prosePart).filter{it.isNotBlank()}.joinToString("

---

")
    }else DocumentExport.plain(html)

    val txtExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->uri?.let{appContext.contentResolver.openOutputStream(it)?.use{o->o.write(compiledPlain().toByteArray())}}}
    val mdExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")){uri->uri?.let{appContext.contentResolver.openOutputStream(it)?.use{o->o.write(compiledMarkdown().toByteArray())}}}
    val htmlExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")){uri->uri?.let{appContext.contentResolver.openOutputStream(it)?.use{o->o.write(DocumentExport.htmlDocument(title,compiledHtml()).toByteArray())}}}
    val pdfExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->uri?.let{runCatching{PdfExport.write(appContext.contentResolver,it,title,compiledHtml())}}}
    val docxExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")){uri->uri?.let{runCatching{PortableExport.docx(appContext.contentResolver,it,title,compiledHtml())}}}
    val epubExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")){uri->uri?.let{runCatching{PortableExport.epub(appContext.contentResolver,it,title,compiledHtml())}}}
    val documentImport=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{val mime=appContext.contentResolver.getType(it).orEmpty();html=when{mime=="application/pdf"||mime.contains("pdf")->DocumentExport.plainToHtml(PortableExport.extractPdf(appContext,it));mime.contains("wordprocessingml")->DocumentExport.plainToHtml(PortableExport.extractDocx(appContext.contentResolver,it));mime.contains("epub")->DocumentExport.plainToHtml(PortableExport.extractEpub(appContext.contentResolver,it));else->{val raw=appContext.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->reader.readText()}?:return@let;when{mime.contains("markdown")->DocumentExport.markdownToHtml(raw);mime.contains("html")->raw;else->DocumentExport.plainToHtml(raw)}}};editorMode="Prose"}}}
    LaunchedEffect(record.id,title,html,tags,aliases,parent,scope){delay(700);val updated=record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope);onSaved(updated);if(db.setting("autoLink","suggest")=="auto")LocalLinkEngine(db).scan(updated)}
    LaunchedEffect(record.id,attachedTemplateId,sheetValues){if(attachedTemplateId.isNotBlank()){delay(500);db.saveSheetValues(record.id,sheetValues)}}
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp)){
        if(!focusMode)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
            Text(record.kind,color=Plum,modifier=Modifier.weight(1f))
            if(record.kind in listOf("Book","Chapter","Novel","Story","Scene")){
                FilledTonalButton(
                    onClick={
                        val latest=latestRecordState.value
                        db.save(latest)
                        db.saveSheetValues(record.id,sheetValues)
                        onReadMode(db.get(record.id)?:latest)
                    },
                    contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp),
                    modifier=Modifier.padding(end=4.dp).defaultMinSize(minHeight=40.dp).testTag("open_reading_mode_button")
                ){
                    Icon(Icons.Default.AutoStories,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Read",fontSize=12.sp)
                }
            }
            if(record.kind=="Novel"||record.kind=="Book")TextButton(onClick={coverPicker.launch(arrayOf("image/*"))}){Text(if(coverUri.isBlank())"Add cover" else "Change cover")}
            if(record.kind in listOf("Novel","Book","Story","Series"))IconButton(onClick={onOutline(record)}){Icon(Icons.Default.FormatListBulleted,"Outline")}
            TextButton(onClick={db.saveSheetValues(record.id,sheetValues);onSaved(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope,coverUri=coverUri))}){Text("Save")}
            IconButton(onClick={onSaved(record.copy(title=title,body=html,tags=tags,aliases=aliases,parentId=parent,scope=scope,favorite=!record.favorite))}){Icon(if(record.favorite)Icons.Default.Star else Icons.Default.StarBorder,"Favorite")}
            IconButton(onClick={details=!details}){Icon(Icons.Default.Tune,"Details")}
            IconButton(onClick={focusMode=!focusMode}){Icon(Icons.Default.CenterFocusStrong,"Focus mode")}
            IconButton(onClick={showFindReplace=!showFindReplace}){Icon(Icons.Default.FindReplace,"Find and replace")}
            IconButton(onClick={history=!history}){Icon(Icons.Default.History,"History")}
            IconButton(onClick={onDelete(record.id)}){Icon(Icons.Default.Delete,"Delete")}
        }
        BasicTextField(title,{title=it},Modifier.fillMaxWidth().padding(vertical=8.dp),textStyle=MaterialTheme.typography.headlineMedium.copy(color=MaterialTheme.colorScheme.onBackground,fontWeight=FontWeight.Bold),decorationBox={inner->if(title.isEmpty())Text("Untitled",style=MaterialTheme.typography.headlineMedium,color=Color.Gray);inner()})
        if(!focusMode)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
            if(attachedTemplateId.isNotBlank()){
                SingleChoiceSegmentedButtonRow(modifier=Modifier.padding(end=8.dp)){
                    SegmentedButton(selected=editorMode=="Sheet",onClick={editorMode="Sheet"},shape=SegmentedButtonDefaults.itemShape(0,2),modifier=Modifier.testTag("mode_tab_sheet")){Text("Sheet")}
                    SegmentedButton(selected=editorMode=="Prose",onClick={editorMode="Prose"},shape=SegmentedButtonDefaults.itemShape(1,2),modifier=Modifier.testTag("mode_tab_prose")){Text("Prose")}
                }
            }else{
                TextButton(onClick={showSheetPicker=true},modifier=Modifier.testTag("attach_sheet_button")){
                    Icon(Icons.Default.DashboardCustomize,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Attach Sheet")
                }
                TextButton(onClick={showLibraryImport=true},modifier=Modifier.testTag("editor_template_library_button")){
                    Icon(Icons.Default.CollectionsBookmark,null,modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("JSON Library")
                }
            }
            TextButton(onClick={showMediaManager=true},modifier=Modifier.testTag("editor_media_manager_button")){
                Icon(Icons.Default.PhotoCamera,null,modifier=Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if(mediaCount>0)"Media ($mediaCount)" else "Media")
            }
            TextButton(onClick={showSaveAsTemplate=true},modifier=Modifier.testTag("save_as_template_button")){
                Icon(Icons.Default.BookmarkAdd,null,modifier=Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save Template")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick={db.saveSheetValues(record.id,sheetValues);linkCount=onLinkScan(record.copy(title=title,body=html,aliases=aliases))}){Text("Scan mentions")}
            if(linkCount>0)Text("$linkCount found",fontSize=11.sp,color=Plum)
        }
        LiveWordCountBar(
            report = liveWordReport,
            sessionStartWords = sessionStartWords,
            compactFocusMode = focusMode,
            onOpenDashboard = { showWordDashboard = true },
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if(showFindReplace)Column(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) { OutlinedTextField(findText,{findText=it},Modifier.fillMaxWidth(),label={Text("Find")},singleLine=true);OutlinedTextField(replaceText,{replaceText=it},Modifier.fillMaxWidth(),label={Text("Replace with")},singleLine=true);Button(onClick={val count=db.replaceInRecord(record.id,findText,replaceText);html=db.get(record.id)?.body?:html;Toast.makeText(appContext,"Replaced $count occurrence(s).",Toast.LENGTH_SHORT).show()},enabled=findText.isNotEmpty()){Text("Replace all in this document")} }
        HorizontalDivider()
        if(attachedTemplateId.isNotBlank()&&editorMode=="Sheet"){
            SheetEditorTab(
                db = db,
                record = record,
                templateId = attachedTemplateId,
                values = sheetValues,
                onValueChange = { fieldId, nextVal ->
                    sheetValues = sheetValues + (fieldId to nextVal)
                },
                onOpenRecord = { target -> onOpen(target, "", 0) },
                onSwitchSheet = { showSheetPicker = true },
                onEditBlueprint = { tplId ->
                    db.saveSheetValues(record.id, sheetValues)
                    onOpenBlueprint(tplId)
                },
                onCopyToProse = {
                    val fields = db.templateFields(attachedTemplateId)
                    val sections = db.templateSections(attachedTemplateId)
                    val snapshotHtml = SheetEngine.compileToHtml(fields, sections, sheetValues, "", { MediaManager.toInlineDataUri(appContext, it) }) { db.get(it) }
                    if (snapshotHtml.isNotBlank()) {
                        html = if (DocumentExport.plain(html).isBlank()) snapshotHtml else html + "<hr/>" + snapshotHtml
                        editorMode = "Prose"
                        Toast.makeText(appContext, "Sheet fields copied into Prose.", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportFromLibrary = {
                    db.saveSheetValues(record.id, sheetValues)
                    showLibraryImport = true
                },
                sheetRefresh = sheetRefresh,
                fontSize = fontSize,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }else{
            RichTextEditor(value=html,onValueChange={html=it},modifier=Modifier.weight(1f).fillMaxWidth(),fontSize=fontSize,fontFamily=fontFamily,customFontData=customFontData,customFontMime=customFontMime,lineSpacing=lineSpacing,pageMode=pageMode,enabledCommands=toolbarCommands,pageWidth=pageWidth,pageMargin=pageMargin,paragraphSpacing=paragraphSpacing,backgroundColor=editorBackground,backgroundImageData=backgroundImageData,backgroundImageMime=backgroundImageMime,textColor=editorTextColor,findTerm=jumpTerm,findOccurrence=jumpOccurrence,onFindHandled=onJumpHandled)
        }
        if(details&&!focusMode)Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text("Structured template sheet",fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Text(if(attachedTemplateId.isBlank())"No template sheet attached" else "Sheet: "+(db.get(attachedTemplateId)?.title?:"Custom"),Modifier.weight(1f),fontSize=12.sp,color=Plum)
                TextButton(onClick={showSheetPicker=true}){Text(if(attachedTemplateId.isBlank())"Attach" else "Switch")}
                if(attachedTemplateId.isNotBlank()){
                    TextButton(onClick={onOpenBlueprint(attachedTemplateId)}){Text("Blueprint")}
                    TextButton(onClick={db.detachSheet(record.id);attachedTemplateId="";editorMode="Prose"}){Text("Detach")}
                }
            }
            HorizontalDivider()
            Text("Toolbar buttons",fontWeight=FontWeight.SemiBold)
            toolbarOptions.forEach{command->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(command in toolbarCommands,{toolbarCommands=if(it)toolbarCommands+command else toolbarCommands-command;db.setSetting("toolbarCommands",toolbarCommands.joinToString("|"))});Text(command.substringAfter(":").replaceFirstChar(Char::uppercase),fontSize=12.sp)}}
            Text("Typography and page layout",fontWeight=FontWeight.SemiBold)
            TextButton(onClick={fontPicker.launch(arrayOf("font/ttf","font/otf","application/octet-stream"))}){Text("Import custom font")}
            Box{OutlinedButton(onClick={fontMenu=true}){Text("Font: $fontFamily")};DropdownMenu(expanded=fontMenu,onDismissRequest={fontMenu=false}){(listOf("sans-serif","serif","monospace","cursive")+(if(customFontData.isNotBlank())listOf("Custom")else emptyList())).forEach{DropdownMenuItem(text={Text(it)},onClick={fontFamily=it;db.setSetting("fontFamily",it);fontMenu=false})}}}
            Text("Line spacing: ${"%.2f".format(lineSpacing)}");Slider(lineSpacing,{lineSpacing=it;db.setSetting("lineSpacing",it.toString())},valueRange=1.1f..2.3f)
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pageMode,{pageMode=it;db.setSetting("pageMode",it.toString())});Text("Narrow page layout")}
            if(pageMode){Text("Page width: ${pageWidth.toInt()} dp");Slider(pageWidth,{pageWidth=it;db.setSetting("pageWidth",it.toInt().toString())},valueRange=420f..900f)}
            Text("Page margins: ${pageMargin.toInt()} dp");Slider(pageMargin,{pageMargin=it;db.setSetting("pageMargin",it.toInt().toString())},valueRange=4f..48f)
            Text("Paragraph spacing: ${paragraphSpacing.toInt()} px");Slider(paragraphSpacing,{paragraphSpacing=it;db.setSetting("paragraphSpacing",it.toInt().toString())},valueRange=0f..32f)
            TextButton(onClick={backgroundPicker.launch(arrayOf("image/*"))}){Text(if(backgroundImageData.isBlank())"Import editor background / texture" else "Change editor background / texture")}
            if(backgroundImageData.isNotBlank())TextButton(onClick={backgroundImageData="";db.setSetting("backgroundImageData","")}){Text("Remove background image")}
            Row{Box{TextButton(onClick={backgroundMenu=true}){Text("Editor background: $editorBackground")};DropdownMenu(expanded=backgroundMenu,onDismissRequest={backgroundMenu=false}){listOf("transparent","#17141E","#F5F0E8","#FFF8E8","#202020").forEach{DropdownMenuItem(text={Text(it)},onClick={editorBackground=it;db.setSetting("editorBackground",it);backgroundMenu=false})}}};Box{TextButton(onClick={textColorMenu=true}){Text("Text color")};DropdownMenu(expanded=textColorMenu,onDismissRequest={textColorMenu=false}){listOf("#eee8f5","#17141E","#FFFFFF","#2D2520","#D8D0E5").forEach{DropdownMenuItem(text={Text(it)},onClick={editorTextColor=it;db.setSetting("editorTextColor",it);textColorMenu=false})}}}}

        
            OutlinedTextField(tags,{tags=it},Modifier.fillMaxWidth(),label={Text("Legacy / freeform tags")})
            Text("Shared reusable taxonomy",fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(newTagName,{newTagName=it},Modifier.weight(1f),label={Text("New tag")},singleLine=true);Box{TextButton(onClick={tagTypeMenu=true}){Text(newTagType)};DropdownMenu(expanded=tagTypeMenu,onDismissRequest={tagTypeMenu=false}){listOf("Category","Property","Use","Element","Planet","Zodiac","Relationship","Custom").forEach{DropdownMenuItem(text={Text(it)},onClick={newTagType=it;tagTypeMenu=false})}}};TextButton(onClick={if(newTagName.isNotBlank()){val id=db.createTag(newTagName,newTagType);db.assignTag(record.id,id);newTagName="";tagRefresh++}}){Text("Add")}}
            sharedTags.groupBy{it.third}.forEach{(type,list)->Column{Text(type,fontSize=11.sp,color=Plum);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){list.forEach{tag->FilterChip(tag.first in assignedTagIds,{if(tag.first in assignedTagIds)db.removeTag(record.id,tag.first)else db.assignTag(record.id,tag.first);tagRefresh++},label={Text(tag.second)})}}}}
            OutlinedTextField(aliases,{aliases=it},Modifier.fillMaxWidth(),label={Text("Aliases (comma separated)")})
            Text("Exclude a phrase from automatic mention matching",fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(excludeTerm,{excludeTerm=it},Modifier.weight(1f),label={Text("Excluded phrase")},singleLine=true);TextButton(onClick={if(excludeTerm.isNotBlank()){db.excludeLinkTerm(record.id,excludeTerm);excludeTerm=""}}){Text("Exclude")}}
            Text("Alternate versions and snapshots",fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(snapshotLabel,{snapshotLabel=it},Modifier.weight(1f),label={Text("Version label")},singleLine=true);TextButton(onClick={db.saveSheetValues(record.id,sheetValues);db.saveEntitySnapshot(record.copy(title=title,body=html,tags=tags,aliases=aliases),snapshotLabel.ifBlank{"Snapshot"});snapshotLabel="Alternate version"}){Text("Save snapshot")}}
            db.entitySnapshots(record.id).forEach{snapshot->Row(verticalAlignment=Alignment.CenterVertically){Text(snapshot.first,Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={db.restoreEntitySnapshot(record.id,snapshot.second);val restored=db.get(record.id);if(restored!=null){title=restored.title;html=restored.body;tags=restored.tags;aliases=restored.aliases};attachedTemplateId=db.sheetTemplateId(record.id);sheetValues=db.sheetValues(record.id)}){Text("Restore")}}}
            Text("Explicit relationships",fontWeight=FontWeight.SemiBold)
            Box{OutlinedButton(onClick={relationMenu=true},modifier=Modifier.fillMaxWidth()){Text("Connect to: "+(db.get(relationTarget)?.title?: "Choose a record"))};DropdownMenu(expanded=relationMenu,onDismissRequest={relationMenu=false}){db.all().filter{it.id!=record.id}.forEach{candidate->DropdownMenuItem(text={Text(candidate.title+" · "+candidate.kind)},onClick={relationTarget=candidate.id;relationMenu=false})}}}
            OutlinedTextField(relationLabel,{relationLabel=it},Modifier.fillMaxWidth(),label={Text("Relationship label (e.g. sibling, located in, member of)")},singleLine=true)
            TextButton(onClick={if(relationTarget.isNotBlank()&&relationLabel.isNotBlank()){db.addRelationship(record.id,relationTarget,relationLabel);relationTarget=""}}){Text("Add relationship")}
            db.relationships(record.id).forEach{link->Text("• ${link.second}: ${link.first}",fontSize=12.sp)}

            val blockedParentIds=remember(record.id){db.descendants(record.id).map{it.id}.toSet()+record.id}
            val allowedParentKinds=when(record.kind){"World"->emptySet<String>();"Series","Story","Novel"->setOf("World");"Book"->setOf("Series");"Chapter"->setOf("Book","Story","Novel");"Scene"->setOf("Chapter");else->setOf("World","Series","Story")}
            val validParents=db.all().filter{it.id !in blockedParentIds&&it.kind in allowedParentKinds}
            Box {
                OutlinedButton(onClick={parentMenu=true},modifier=Modifier.fillMaxWidth()){Text("Parent: "+(db.get(parent)?.title?: "None"))}
                DropdownMenu(expanded=parentMenu,onDismissRequest={parentMenu=false}){
                    DropdownMenuItem(text={Text("No parent")},onClick={parent="";parentMenu=false})
                    validParents.forEach{candidate->DropdownMenuItem(text={Text(candidate.title+" · "+candidate.kind)},onClick={parent=candidate.id;parentMenu=false})}
                    if(validParents.isEmpty())DropdownMenuItem(text={Text("No compatible parent records")},enabled=false,onClick={})
                }
            }
            Box { OutlinedButton(onClick={folderMenu=true},modifier=Modifier.fillMaxWidth()){Text("Folder: "+(db.folders().firstOrNull{it.first==folderId}?.second?: "Unfiled"))};DropdownMenu(expanded=folderMenu,onDismissRequest={folderMenu=false}){DropdownMenuItem(text={Text("Unfiled")},onClick={folderId="";db.moveToFolder(record.id,null);folderMenu=false});db.folders().forEach{f->DropdownMenuItem(text={Text(f.second)},onClick={folderId=f.first;db.moveToFolder(record.id,f.first);folderMenu=false})}} }
            Text("Share selectively with projects",fontWeight=FontWeight.SemiBold)
            db.all().filter{it.kind=="Series"||it.kind=="Story"}.forEach{project->val checked=project.id in db.sharedProjects(record.id);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked,{if(it)db.shareWithProject(record.id,project.id)else db.unshareFromProject(record.id,project.id)});Text(project.title,fontSize=12.sp)}}
            Text("Sharing scope")
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("World","Series","Story").forEach{FilterChip(scope==it,{scope=it},label={Text(it)})}}
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={documentImport.launch(arrayOf("text/plain","text/markdown","text/html","application/xhtml+xml","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/epub+zip"))}){Text("Import")};TextButton(onClick={txtExport.launch(title.ifBlank{"Untitled"}+".txt")}){Text("TXT")};TextButton(onClick={mdExport.launch(title.ifBlank{"Untitled"}+".md")}){Text("Markdown")};TextButton(onClick={htmlExport.launch(title.ifBlank{"Untitled"}+".html")}){Text("HTML")};TextButton(onClick={pdfExport.launch(title.ifBlank{"Untitled"}+".pdf")}){Text("PDF")};TextButton(onClick={docxExport.launch(title.ifBlank{"Untitled"}+".docx")}){Text("DOCX")};TextButton(onClick={epubExport.launch(title.ifBlank{"Untitled"}+".epub")}){Text("EPUB")}}
            Text("Attachments & Camera Media",fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilledTonalButton(onClick={showMediaManager=true}){Text("Open Media Manager ($mediaCount)")}
                TextButton(onClick={attachmentPicker.launch(arrayOf("image/*","application/pdf","text/*","application/*"))}){Text("Add file")}
            }
            attachmentList.forEach{attachment->AttachmentPreview(attachment){id->db.removeAttachment(id);attachmentRefresh++}}
            Text("Mention suggestions",fontWeight=FontWeight.SemiBold)
            db.pendingLinkSuggestions(record.id).forEach{suggestion->Row(verticalAlignment=Alignment.CenterVertically){Text("“${suggestion.second}” → ${db.get(suggestion.first)?.title?: "Unknown"}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={db.resolveLinkSuggestion(record.id,suggestion.first,suggestion.third,true);linkCount++}){Text("Link")};TextButton(onClick={db.resolveLinkSuggestion(record.id,suggestion.first,suggestion.third,false);linkCount++}){Text("Dismiss")}}}
            Row(verticalAlignment=Alignment.CenterVertically){Text("Outgoing mention links",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);TextButton(onClick={if(db.undoLastLinkChange())linkCount++}){Text("Undo last change")}}
            db.outgoingLinks(record.id).forEach{link->Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={db.get(link.recordId)?.let{onOpen(it,"",0)}},modifier=Modifier.weight(1f)){Text("${link.title} · ${link.label} · ${link.term}")};TextButton(onClick={db.removeLink(record.id,link.recordId,link.label,link.position)}){Text("Unlink")}}}
            Text("Backlinks",fontWeight=FontWeight.SemiBold)
            db.incomingLinks(record.id).forEach{link->TextButton(onClick={db.get(link.recordId)?.let{source->val prefix=android.text.Html.fromHtml(source.body.take(link.position.coerceIn(0,source.body.length)),android.text.Html.FROM_HTML_MODE_LEGACY).toString();val options=if(db.setting("caseSensitive","false")=="true")emptySet()else setOf(RegexOption.IGNORE_CASE);val occurrence=if(link.term.isBlank())0 else Regex(Regex.escape(link.term),options).findAll(prefix).count();onOpen(source,link.term,occurrence)}}){Text("• ${link.title} — ${link.label} · “${link.term}” (position ${link.position})",fontSize=12.sp)}}
        }
        if(history)Column{Text("Recent versions",fontWeight=FontWeight.SemiBold);db.revisions(record.id).take(5).forEach{rev->Row(verticalAlignment=Alignment.CenterVertically){Text("• ${rev.first} · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(rev.third))}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={title=rev.first;html=rev.second}){Text("Restore")}}}}
        Text("Offline · stored on this device",Modifier.padding(vertical=5.dp),fontSize=11.sp,color=Plum)
    }
    if(showSheetPicker)TemplatePickerDialog(
        db = db,
        title = if(attachedTemplateId.isBlank())"Attach Fantasy Sheet" else "Switch Fantasy Sheet",
        onDismiss = { showSheetPicker = false },
        onSelect = { tpl ->
            db.saveSheetValues(record.id, sheetValues)
            db.attachSheet(record.id, tpl.id)
            attachedTemplateId = tpl.id
            editorMode = "Sheet"
            showSheetPicker = false
        }
    )
    if(showLibraryImport)TemplateLibraryDialog(
        db = db,
        targetRecord = latestRecordState.value,
        onDismiss = { showLibraryImport = false },
        onImportedIntoSheet = { result ->
            attachedTemplateId = result.templateId
            sheetValues = db.sheetValues(record.id)
            sheetRefresh++
            editorMode = "Sheet"
            showLibraryImport = false
            onSaved(latestRecordState.value)
        }
    )
    if(showSaveAsTemplate)SaveAsTemplateDialog(
        record = latestRecordState.value,
        hasAttachedSheet = attachedTemplateId.isNotBlank(),
        onDismiss = { showSaveAsTemplate = false },
        onConfirm = { tplTitle, tplCat, includeProse ->
            db.save(latestRecordState.value)
            db.saveSheetValues(record.id, sheetValues)
            val createdTpl = db.saveRecordAsTemplate(latestRecordState.value, tplTitle, tplCat, includeProse)
            showSaveAsTemplate = false
            Toast.makeText(appContext, "Saved template \"${createdTpl.title}\"", Toast.LENGTH_SHORT).show()
            onTemplateCreated(createdTpl)
        }
    )
    if(showWordDashboard)WordCountDashboardDialog(
        db = db,
        report = liveWordReport,
        sessionStartWords = sessionStartWords,
        onGoalUpdated = { wordGoalRefresh++ },
        onOpenRecord = { target ->
            showWordDashboard = false
            onOpen(target, "", 0)
        },
        onDismiss = { showWordDashboard = false }
    )
    if(showMediaManager)MediaManagerDialog(
        db = db,
        initialRecord = latestRecordState.value,
        initialTemplateId = attachedTemplateId,
        onAssignToSheetField = { fieldId, uri ->
            val field = sheetFields.firstOrNull { it.id == fieldId }
            if (field?.fieldType == "Gallery") {
                val cur = sheetValues[fieldId].orEmpty().split("||").filter { it.isNotBlank() }
                sheetValues = sheetValues + (fieldId to (cur + uri).distinct().joinToString("||"))
            } else {
                sheetValues = sheetValues + (fieldId to uri)
            }
            db.saveSheetValues(record.id, sheetValues)
            sheetRefresh++
        },
        onInsertIntoProse = { asset ->
            val dataUri = MediaManager.toInlineDataUri(appContext, asset.uri)
            if (!dataUri.isNullOrBlank()) {
                val captionHtml = if (asset.caption.isNotBlank()) "<figcaption>${android.text.TextUtils.htmlEncode(asset.caption)}</figcaption>" else ""
                val fig = """<figure style="margin:12px 0;text-align:center;"><img src="$dataUri" style="max-width:100%;height:auto;border-radius:10px;" />$captionHtml</figure><p></p>"""
                html += fig
                editorMode = "Prose"
            }
        },
        onSetAsCover = { asset ->
            coverUri = asset.uri
            onSaved(latestRecordState.value.copy(coverUri = asset.uri))
        },
        onOpenRecord = { target ->
            showMediaManager = false
            onOpen(target, "", 0)
        },
        onMediaChanged = { attachmentRefresh++ },
        onDismiss = { showMediaManager = false }
    )
}

@Composable private fun AttachmentPreview(attachment:Map<String,String>,remove:(String)->Unit){
    val context=LocalContext.current
    val uri=attachment["uri"].orEmpty()
    val mime=attachment["mimeType"].orEmpty()
    val bitmap=remember(uri){if(mime.startsWith("image/"))MediaManager.decodeImageBitmap(context,uri,maxDim=640)else null}
    Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.padding(8.dp)){Text(attachment["displayName"]?.ifBlank{"Attached file"}?: "Attached file",fontWeight=FontWeight.Medium);if(bitmap!=null)Image(bitmap=bitmap,contentDescription=attachment["displayName"],modifier=Modifier.fillMaxWidth().heightIn(max=180.dp),contentScale=ContentScale.Fit);Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}}){Text("Open")};TextButton(onClick={remove(attachment["id"].orEmpty())}){Text("Remove")}}}}
}

@Composable private fun LockScreen(onUnlock:(String)->Unit){var pin by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Lock,null,tint=Plum,modifier=Modifier.size(48.dp));Text("Ink & Quill is locked",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(pin,{pin=it},label={Text("PIN or passphrase")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true);Button(onClick={onUnlock(pin)},enabled=pin.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Unlock")}}}

@Composable private fun CreateDialog(onDismiss:()->Unit,onChoose:(String)->Unit,onFromTemplate:()->Unit,onCreateCustomTemplate:()->Unit={}){
    val kinds=FantasyTemplateCatalog.starterRecordKinds.filter{it!="All"}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Create something")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())){Button(onClick=onFromTemplate,modifier=Modifier.fillMaxWidth().padding(bottom=6.dp).testTag("create_from_template_button")){Icon(Icons.Default.DashboardCustomize,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("From Fantasy Template Sheet...")};FilledTonalButton(onClick=onCreateCustomTemplate,modifier=Modifier.fillMaxWidth().padding(bottom=8.dp).testTag("create_custom_template_from_dialog")){Icon(Icons.Default.AddCircleOutline,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Create Custom Template...")};HorizontalDivider(Modifier.padding(bottom=6.dp));kinds.forEach{TextButton(onClick={onChoose(it)}){Text(it)}}}},confirmButton={TextButton(onClick=onDismiss){Text("Close")}})
}

@Composable private fun SettingsDialog(db:InkDatabase,appearance:String,setAppearance:(String)->Unit,palette:String,setPalette:(String)->Unit,customAccent:String,setCustomAccent:(String)->Unit,size:Float,setSize:(Float)->Unit,export:()->Unit,importBackup:()->Unit,cloudExport:(String)->Unit,cloudImport:(String)->Unit,cloudSync:(String)->Unit,cloudSyncEnabled:Boolean,disableCloudSync:()->Unit,lockEnabled:Boolean,setPin:(String)->Unit,disableLock:()->Unit,cloudConflict:Boolean,cloudSyncStatus:String,cloudSyncAt:Long,cloudSyncHistory:List<Pair<Long,String>>,resolveCloudConflict:(Boolean)->Unit,retroScan:()->Unit,autoSaveStatus:AutoSaveStatus,onAutoSaveNow:()->Unit,onRestoreAutoSave:()->Unit,onOpenGoogleDriveBackup:()->Unit,close:()->Unit){
    val context=LocalContext.current
    var backupPassword by remember{mutableStateOf("")}
    var showCloudHistory by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=close,title={Text("Studio settings")},text={Column(Modifier.heightIn(max=600.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
        var lockActive by remember{mutableStateOf(lockEnabled)};Text("Local app lock");var pinEntry by remember{mutableStateOf("")};if(lockActive)TextButton(onClick={disableLock();lockActive=false}){Text("Disable PIN lock")}else{OutlinedTextField(pinEntry,{pinEntry=it},label={Text("Set PIN or passphrase (6+ characters)")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation());TextButton(onClick={if(pinEntry.length>=6){setPin(pinEntry);lockActive=true;pinEntry=""}else Toast.makeText(context,"Use at least 6 characters.",Toast.LENGTH_SHORT).show()}){Text("Enable app lock")}}
        Text("Appearance");Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("system","light","dark").forEach{FilterChip(appearance==it,{setAppearance(it)},label={Text(it.replaceFirstChar(Char::uppercase))})}}
        var navigationTabs by remember{mutableStateOf(db.setting("navTabs","Home,Library,Worlds,Templates").split(",").toSet())};var navigationOrder by remember{mutableStateOf(db.setting("navOrder","Home,Library,Worlds,Templates").split(","))};Text("Navigation tabs");listOf("Home","Library","Worlds","Templates").forEach{tab->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(tab in navigationTabs,{navigationTabs=if(it)navigationTabs+tab else navigationTabs-tab;db.setSetting("navTabs",navigationTabs.joinToString(","))});Text(tab,fontSize=12.sp)}};Text("Navigation order");navigationOrder.forEachIndexed{index,tab->Row(verticalAlignment=Alignment.CenterVertically){Text(tab,Modifier.weight(1f));TextButton(enabled=index>0,onClick={navigationOrder=navigationOrder.toMutableList().also{list->val item=list.removeAt(index);list.add(index-1,item)};db.setSetting("navOrder",navigationOrder.joinToString(","))}){Text("↑")};TextButton(enabled=index<navigationOrder.lastIndex,onClick={navigationOrder=navigationOrder.toMutableList().also{list->val item=list.removeAt(index);list.add(index+1,item)};db.setSetting("navOrder",navigationOrder.joinToString(","))}){Text("↓")}}}
        var homeSections by remember{mutableStateOf(db.setting("homeSections","Novels & Stories,Poetry,Journal,Documents,Notes,Creative Projects,Worldbuilding").split(",").toSet())};var homeOrder by remember{mutableStateOf(db.setting("homeOrder","Novels & Stories,Poetry,Journal,Documents,Notes,Creative Projects,Worldbuilding").split(","))};Text("Home sections");listOf("Novels & Stories","Poetry","Journal","Documents","Notes","Creative Projects","Worldbuilding").forEach{section->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(section in homeSections,{homeSections=if(it)homeSections+section else homeSections-section;db.setSetting("homeSections",homeSections.joinToString(","))});Text(section,fontSize=12.sp)}};Text("Home section order");homeOrder.forEachIndexed{index,section->Row(verticalAlignment=Alignment.CenterVertically){Text(section,Modifier.weight(1f));TextButton(enabled=index>0,onClick={homeOrder=homeOrder.toMutableList().also{list->val item=list.removeAt(index);list.add(index-1,item)};db.setSetting("homeOrder",homeOrder.joinToString(","))}){Text("↑")};TextButton(enabled=index<homeOrder.lastIndex,onClick={homeOrder=homeOrder.toMutableList().also{list->val item=list.removeAt(index);list.add(index+1,item)};db.setSetting("homeOrder",homeOrder.joinToString(","))}){Text("↓")}}}
        Text("Color palette");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Plum","Ocean","Forest","Rose","Amber","Custom").forEach{FilterChip(palette==it,{setPalette(it)},label={Text(it)})}}
        if(palette=="Custom"){OutlinedTextField(customAccent,setCustomAccent,label={Text("Custom accent hex")},singleLine=true);TextButton(onClick={if(runCatching{android.graphics.Color.parseColor(customAccent)}.isSuccess)setPalette("Custom")}){Text("Apply custom accent")}}
        Text("Editor size: ${size.toInt()}sp");Slider(size,setSize,valueRange=14f..30f)
        Text("Template & sheet auto-save (30s DataStore)",fontWeight=FontWeight.SemiBold)
        Text(
            if(autoSaveStatus.lastSavedAt>0L)"Last auto-saved: ${DateFormat.getDateTimeInstance().format(Date(autoSaveStatus.lastSavedAt))} · ${autoSaveStatus.templateCount} templates, ${autoSaveStatus.sheetCount} sheets" else autoSaveStatus.lastMessage,
            fontSize=12.sp,
            color=MaterialTheme.colorScheme.onSurface.copy(alpha=.75f)
        )
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick=onAutoSaveNow){Text("Auto-save now")}
            TextButton(onClick=onRestoreAutoSave){Text("Restore auto-save")}
        }
        TextButton(onClick=retroScan){Text("Scan all existing documents for mentions")}
        Text("Automatic mention links");var mode by remember{mutableStateOf(db.setting("autoLink","suggest"))};Row{listOf("off","suggest","auto").forEach{FilterChip(mode==it,{mode=it;db.setSetting("autoLink",it)},label={Text(it)})}}
        var aliasMatching by remember{mutableStateOf(db.setting("matchAliases","true")=="true")};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(aliasMatching,{aliasMatching=it;db.setSetting("matchAliases",it.toString())});Text("Match aliases")}
        var caseSensitive by remember{mutableStateOf(db.setting("caseSensitive","false")=="true")};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(caseSensitive,{caseSensitive=it;db.setSetting("caseSensitive",it.toString())});Text("Case-sensitive matching")}
        Text("Google Drive backup",fontWeight=FontWeight.SemiBold)
        val driveState=remember{GoogleDriveBackupManager.uiState(context)}
        Text(
            if(driveState.connectedEmail.isNotBlank())"Connected: ${driveState.connectedEmail} · ${driveState.lastStatus}" else driveState.lastStatus,
            fontSize=12.sp,
            color=MaterialTheme.colorScheme.onSurface.copy(alpha=.75f)
        )
        FilledTonalButton(onClick=onOpenGoogleDriveBackup,modifier=Modifier.fillMaxWidth().testTag("open_google_drive_backup_button")){
            Icon(Icons.Default.CloudUpload,null,modifier=Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back up to Google Drive")
        }
        Text("Your writing stays on this phone first. Cloud backup is optional and encrypted on this device before it is sent to the storage provider you choose.")
        OutlinedTextField(backupPassword,{backupPassword=it},Modifier.fillMaxWidth(),label={Text("Cloud backup password")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true)
        Text("Use at least 10 characters. Keep this password safe: it cannot be recovered if lost.")
        TextButton(onClick=export){Text("Export local JSON backup")}
        TextButton(onClick=importBackup){Text("Import local JSON backup")}
        TextButton(onClick={if(backupPassword.length>=10){cloudExport(backupPassword)}else{Toast.makeText(context,"Use at least 10 characters.",Toast.LENGTH_SHORT).show()}}){Text("Save encrypted cloud backup")}
        TextButton(onClick={if(backupPassword.isNotBlank()){cloudImport(backupPassword)}else{Toast.makeText(context,"Enter your backup password.",Toast.LENGTH_SHORT).show()}}){Text("Restore encrypted cloud backup")}
        Text("Cloud status: $cloudSyncStatus",fontSize=12.sp)
        if(cloudSyncAt>0)Text("Last checked: "+java.text.DateFormat.getDateTimeInstance().format(java.util.Date(cloudSyncAt)),fontSize=11.sp)
        TextButton(onClick={showCloudHistory=!showCloudHistory}){Text(if(showCloudHistory)"Hide sync history" else "View sync history ("+cloudSyncHistory.size+")")}
        if(showCloudHistory){
            if(cloudSyncHistory.isEmpty())Text("No sync activity recorded yet.",fontSize=12.sp)
            cloudSyncHistory.take(12).forEach{entry->
                Column(Modifier.fillMaxWidth().padding(start=8.dp,top=2.dp,bottom=2.dp)){
                    Text(entry.second,fontSize=12.sp,fontWeight=FontWeight.Medium)
                    Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.first)),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f))
                }
            }
        }
        if(cloudSyncEnabled)TextButton(onClick={CloudSyncManager.syncNow(context)}){Text("Sync now")}
        if(cloudSyncEnabled)TextButton(onClick=disableCloudSync){Text("Disable cloud sync")}else TextButton(onClick={if(backupPassword.length>=10)cloudSync(backupPassword)else Toast.makeText(context,"Use at least 10 characters.",Toast.LENGTH_SHORT).show()}){Text("Enable encrypted cloud sync (12-hour schedule)")}
        if(cloudConflict){Text("A cloud conflict was detected: both the phone and cloud copy changed since the last shared version. Choose which copy should replace the other.",color=Color(0xFFE0A45C));TextButton(onClick={resolveCloudConflict(true)}){Text("Keep this phone version")};TextButton(onClick={resolveCloudConflict(false)}){Text("Use the cloud version")}}
        Text("Encrypted sync runs when Android reports a network connection. Changes are exchanged through your selected provider; simultaneous edits trigger a keep-phone or use-cloud choice. Offline work is queued and retried.")
        Text("Choose any connected Android storage provider in the system picker (for example, a cloud drive app). The provider handles its own sign-in.")
    }},confirmButton={TextButton(onClick=close){Text("Done")}})
}
