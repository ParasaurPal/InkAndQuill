package com.parasaurpal.inkandquill.ui

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parasaurpal.inkandquill.data.NovelStudioStore
import com.parasaurpal.inkandquill.WritingProgressWidget
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class StudioToolTab { BIBLE, TIMELINES, BOARDS, GOALS, PROMPTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelStudioToolsScreen(universeId:String,store:NovelStudioStore,onClose:()->Unit) {
    var tab by remember { mutableStateOf(StudioToolTab.BIBLE) }
    var refresh by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Character") }
    var search by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var linkTarget by remember { mutableStateOf("") }
    var linkLabel by remember { mutableStateOf("") }
    var selectedTimeline by remember { mutableStateOf("") }
    var selectedBoard by remember { mutableStateOf("") }
    var selectedPrompt by remember { mutableStateOf("") }
    var sprintWords by remember { mutableStateOf("0") }
    var sprintNotes by remember { mutableStateOf("") }
    val db=store.writableDatabase
    val context=LocalContext.current
    LaunchedEffect(refresh) { WritingProgressWidget.refresh(context) }

    val entities=remember(universeId,refresh,tab,search) {
        db.rawQuery("SELECT id,entity_type,title,description,custom_label,fields_json,parent_entity_id FROM entities WHERE universe_id=? AND (title LIKE ? OR description LIKE ? OR entity_type LIKE ?) ORDER BY title",
            arrayOf(universeId,"%"+search+"%","%"+search+"%","%"+search+"%")).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("type",c.getString(1)).put("title",c.getString(2)).put("description",c.getString(3)).put("label",c.getString(4)).put("fields",c.getString(5)).put("parent",c.getString(6))) }
        }
    }
    val timelines=remember(universeId,refresh,tab) {
        db.rawQuery("SELECT id,name,custom_description FROM timelines WHERE universe_id=? ORDER BY position,name",arrayOf(universeId)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("name",c.getString(1)).put("description",c.getString(2))) }
        }
    }
    val boards=remember(universeId,refresh,tab) {
        db.rawQuery("SELECT id,title,board_type FROM boards WHERE universe_id=? ORDER BY updated_at DESC",arrayOf(universeId)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("title",c.getString(1)).put("type",c.getString(2))) }
        }
    }
    val goals=remember(universeId,refresh,tab) {
        db.rawQuery("SELECT id,period,target_words,active_days_per_week FROM writing_goals WHERE universe_id=? ORDER BY created_at DESC",arrayOf(universeId)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("period",c.getString(1)).put("target",c.getInt(2)).put("days",c.getInt(3))) }
        }
    }
    val prompts=remember(universeId,refresh,tab) {
        db.rawQuery("SELECT id,prompt_text,source FROM prompt_entries WHERE universe_id=? OR universe_id IS NULL ORDER BY created_at DESC",arrayOf(universeId)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("text",c.getString(1)).put("source",c.getString(2))) }
        }
    }
    val cards=remember(selectedBoard,refresh) {
        if(selectedBoard.isBlank()) emptyList() else db.rawQuery("SELECT id,payload_json,x,y FROM board_objects WHERE board_id=? ORDER BY z_index",arrayOf(selectedBoard)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("payload",c.getString(1)).put("x",c.getDouble(2)).put("y",c.getDouble(3))) }
        }
    }
    val moments=remember(selectedTimeline,refresh) {
        if(selectedTimeline.isBlank()) emptyList() else db.rawQuery("SELECT id,date_or_era_text,title,custom_text FROM moments WHERE timeline_id=? ORDER BY sort_key",arrayOf(selectedTimeline)).use { c ->
            buildList { while(c.moveToNext()) add(JSONObject().put("id",c.getString(0)).put("date",c.getString(1)).put("title",c.getString(2)).put("text",c.getString(3))) }
        }
    }
    fun createDialog(kind:String) {
        type=kind;title="";detail="";selectedId="";parentId="";tagsText="";linkTarget="";linkLabel="";showCreate=true
    }
    fun editEntity(e:JSONObject) {
        type=e.optString("type");title=e.optString("title");detail=e.optString("description");selectedId=e.optString("id");parentId=e.optString("parent")
        tagsText="";linkTarget="";linkLabel="";showCreate=true
    }
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column { Text("Worldbuilding & planning",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Offline · stored in this universe",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick=onClose){Text("Back to writing")}
        }
        ScrollableTabRow(selectedTabIndex=tab.ordinal,edgePadding=0.dp) {
            Tab(tab==StudioToolTab.BIBLE,{tab=StudioToolTab.BIBLE},text={Text("Story Bible")})
            Tab(tab==StudioToolTab.TIMELINES,{tab=StudioToolTab.TIMELINES},text={Text("Timelines")})
            Tab(tab==StudioToolTab.BOARDS,{tab=StudioToolTab.BOARDS},text={Text("Boards")})
            Tab(tab==StudioToolTab.GOALS,{tab=StudioToolTab.GOALS},text={Text("Goals")})
            Tab(tab==StudioToolTab.PROMPTS,{tab=StudioToolTab.PROMPTS},text={Text("Prompts")})
        }
        when(tab) {
            StudioToolTab.BIBLE -> {
                OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth().padding(top=8.dp),singleLine=true,label={Text("Search Story Bible")})
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(entities.size.toString()+" entries",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick={createDialog("Character")}){Text("Add entry")}
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                    items(entities) { e ->
                        val entityTags=remember(e.optString("id"),refresh) {
                            db.rawQuery("SELECT t.text FROM tags t JOIN entity_tags et ON et.tag_id=t.id WHERE et.entity_id=? ORDER BY t.text",arrayOf(e.optString("id"))).use { c -> buildList { while(c.moveToNext())add(c.getString(0)) } }
                        }
                        val links=remember(e.optString("id"),refresh){store.backlinks("entity",e.optString("id"))}
                        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)){Text(e.optString("title"),fontWeight=FontWeight.SemiBold);Text(e.optString("type"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                                    TextButton(onClick={editEntity(e)}){Text("Edit")}
                                    TextButton(onClick={db.delete("entities","id=?",arrayOf(e.optString("id")));refresh++}){Text("Delete")}
                                }
                                if(e.optString("description").isNotBlank())Text(e.optString("description"))
                                if(entityTags.isNotEmpty())Text(entityTags.joinToString(" · "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                                if(links.isNotEmpty())Text(links.size.toString()+" linked records / backlinks",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                if(e.optString("parent").isNotBlank())Text("Nested under another record",style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if(entities.isEmpty())item{Text("Create characters, locations, items, species, languages, factions, cultures or custom records.")}
                }
            }
            StudioToolTab.TIMELINES -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(timelines.size.toString()+" timelines")
                    Button(onClick={createDialog("Timeline")}){Text("New timeline")}
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                    items(timelines) { t ->
                        val active=selectedTimeline==t.optString("id")
                        Surface(Modifier.fillMaxWidth().clickable{selectedTimeline=if(active)"" else t.optString("id")},shape=RoundedCornerShape(12.dp),color=if(active)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.optString("name"),fontWeight=FontWeight.SemiBold);Text(t.optString("description"),style=MaterialTheme.typography.bodySmall)};TextButton(onClick={selectedTimeline=t.optString("id");createDialog("Moment")}){Text("Add moment")};TextButton(onClick={db.delete("timelines","id=?",arrayOf(t.optString("id")));refresh++}){Text("Delete")}}
                                if(active)moments.forEach{m->Text(m.optString("date")+" · "+m.optString("title"));if(m.optString("text").isNotBlank())Text(m.optString("text"),style=MaterialTheme.typography.bodySmall)}
                            }
                        }
                    }
                }
            }
            StudioToolTab.BOARDS -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(boards.size.toString()+" boards");Button(onClick={createDialog("Board")}){Text("New board")}}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
                    items(boards){b->
                        val active=selectedBoard==b.optString("id")
                        Surface(Modifier.fillMaxWidth().clickable{selectedBoard=if(active)"" else b.optString("id")},shape=RoundedCornerShape(12.dp),color=if(active)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(b.optString("title"),fontWeight=FontWeight.SemiBold);Text(b.optString("type"),style=MaterialTheme.typography.labelSmall)};TextButton(onClick={selectedBoard=b.optString("id");createDialog("Board note")}){Text("Add card")};TextButton(onClick={db.delete("boards","id=?",arrayOf(b.optString("id")));selectedBoard="";refresh++}){Text("Delete")}}
                                if(active)cards.forEach{card->
                                    val payload=runCatching{JSONObject(card.optString("payload"))}.getOrDefault(JSONObject())
                                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(payload.optString("text"),Modifier.weight(1f));TextButton(onClick={selectedId=card.optString("id");title=payload.optString("text");detail=payload.optString("color");type="Edit board card";showCreate=true}){Text("Edit")};TextButton(onClick={db.delete("board_objects","id=?",arrayOf(card.optString("id")));refresh++}){Text("Remove")}}
                                }
                            }
                        }
                    }
                }
            }
            StudioToolTab.GOALS -> {
                val today=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
                val todayWords=remember(universeId,refresh){db.rawQuery("SELECT word_delta FROM daily_logs WHERE universe_id=? AND day_key=?",arrayOf(universeId,today)).use{c->if(c.moveToFirst())c.getInt(0)else 0}}
                val sprint=remember(universeId,refresh){db.rawQuery("SELECT id FROM sprint_sessions WHERE universe_id=? AND ended_at IS NULL ORDER BY started_at DESC LIMIT 1",arrayOf(universeId)).use{c->if(c.moveToFirst())c.getString(0)else ""}}
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(goals.size.toString()+" goals");Button(onClick={createDialog("Goal")}){Text("Set goal")}}
                Surface(Modifier.fillMaxWidth().padding(bottom=8.dp),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Today's log · "+todayWords+" words",fontWeight=FontWeight.SemiBold)
                        OutlinedTextField(sprintWords,{sprintWords=it},label={Text("Words written in sprint")},singleLine=true)
                        OutlinedTextField(sprintNotes,{sprintNotes=it},label={Text("Session notes")},minLines=2)
                        Button(onClick={
                            if(sprint.isBlank()){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("universe_id",universeId);put("started_at",System.currentTimeMillis());put("target_words",0)};db.insertOrThrow("sprint_sessions",null,v)}
                            else {val words=sprintWords.toIntOrNull()?:0;db.execSQL("UPDATE sprint_sessions SET ended_at=?,word_delta=?,notes=? WHERE id=?",arrayOf(System.currentTimeMillis(),words,sprintNotes,sprint));val existing=db.rawQuery("SELECT id FROM daily_logs WHERE universe_id=? AND day_key=?",arrayOf(universeId,today)).use{if(it.moveToFirst())it.getString(0)else null};if(existing==null){val log=ContentValues().apply{put("id",UUID.randomUUID().toString());put("universe_id",universeId);put("day_key",today);put("word_delta",words);put("session_notes",sprintNotes)};db.insertOrThrow("daily_logs",null,log)}else{db.execSQL("UPDATE daily_logs SET word_delta=word_delta+?,session_notes=CASE WHEN session_notes='' THEN ? ELSE session_notes||char(10)||? END WHERE id=?",arrayOf(words,sprintNotes,sprintNotes,existing))}}
                            refresh++
                        }){Text(if(sprint.isBlank())"Start writing sprint" else "Finish sprint and log words")}
                    }
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){items(goals){g->Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(g.optString("period")+" goal",fontWeight=FontWeight.SemiBold);Text(g.optInt("target").toString()+" words · "+g.optInt("days")+" active days/week")};TextButton(onClick={db.delete("writing_goals","id=?",arrayOf(g.optString("id")));refresh++}){Text("Delete")}}}}}
            }
            StudioToolTab.PROMPTS -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(prompts.size.toString()+" prompts");Button(onClick={createDialog("Prompt")}){Text("Add prompt")}}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){items(prompts){p->Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(14.dp)){Text(p.optString("text"));Row(verticalAlignment=Alignment.CenterVertically){Text(p.optString("source"),style=MaterialTheme.typography.labelSmall);Spacer(Modifier.weight(1f));TextButton(onClick={selectedPrompt=p.optString("id");title=p.optString("text");detail=p.optString("source");type="Edit prompt";showCreate=true}){Text("Edit")};TextButton(onClick={db.delete("prompt_entries","id=?",arrayOf(p.optString("id")));refresh++}){Text("Delete")}}}}}}
            }
        }
    }

    if(showCreate) AlertDialog(
        onDismissRequest={showCreate=false},
        title={Text(when(type){"Timeline"->"New timeline";"Moment"->"Add timeline moment";"Board"->"New board";"Board note","Edit board card"->"Board card";"Goal"->"Set writing goal";"Prompt","Edit prompt"->"Writing prompt";else->if(selectedId.isBlank())"New Story Bible entry" else "Edit Story Bible entry"})},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(title,{title=it},label={Text(if(type=="Moment")"Moment title" else "Name / text")},singleLine=true)
            if(type=="Character"||type=="Location"||type=="Item"||type=="Species"||type=="Language"||type=="Custom"||type=="Edit Story Bible entry")OutlinedTextField(type,{type=it},label={Text("Entity type (custom text allowed)")},singleLine=true)
            OutlinedTextField(detail,{detail=it},label={Text(when(type){"Moment"->"Date / era and notes";"Goal"->"Target word count";"Board"->"Board type";"Board note","Edit board card"->"Card color";"Prompt","Edit prompt"->"Prompt source";else->"Description / custom text"})},minLines=if(type=="Moment"||type=="Prompt"||type=="Edit prompt")2 else 1)
            if(type=="Character"||type=="Location"||type=="Item"||type=="Species"||type=="Language"||type=="Custom"||type=="Edit Story Bible entry"){
                OutlinedTextField(parentId,{parentId=it},label={Text("Parent entity ID (optional)")},singleLine=true)
                OutlinedTextField(tagsText,{tagsText=it},label={Text("Tags, comma separated")},singleLine=true)
                OutlinedTextField(linkTarget,{linkTarget=it},label={Text("Link to entity ID (optional)")},singleLine=true)
                OutlinedTextField(linkLabel,{linkLabel=it},label={Text("Relationship label")},singleLine=true)
            }
        }},
        confirmButton={TextButton(onClick={
            if(title.isNotBlank()){
                when(type){
                    "Timeline"->store.createTimeline(universeId,title,detail)
                    "Moment"->store.createMoment(selectedTimeline,detail.substringBefore("\n"),title,detail.substringAfter("\n",""),System.currentTimeMillis().toDouble())
                    "Board"->store.createBoard(universeId,title,detail.ifBlank{"CORKBOARD"})
                    "Board note"->store.addBoardText(selectedBoard,title,24f,24f,"")
                    "Edit board card"->{val v=ContentValues().apply{put("payload_json",JSONObject().put("text",title).put("color",detail).toString())};db.update("board_objects",v,"id=?",arrayOf(selectedId))}
                    "Goal"->{val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("universe_id",universeId);put("period","total");put("target_words",detail.toIntOrNull()?:50000);put("active_days_per_week",5);put("created_at",System.currentTimeMillis())};db.insertOrThrow("writing_goals",null,v)}
                    "Prompt"->{val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("universe_id",universeId);put("prompt_text",title);put("source",detail.ifBlank{"user"});put("created_at",System.currentTimeMillis())};db.insertOrThrow("prompt_entries",null,v)}
                    "Edit prompt"->{val v=ContentValues().apply{put("prompt_text",title);put("source",detail)};db.update("prompt_entries",v,"id=?",arrayOf(selectedPrompt))}
                    else->{
                        val id=if(selectedId.isBlank())store.createEntity(universeId,type,title,detail,parentId.ifBlank{null}) else selectedId
                        if(selectedId.isNotBlank()){val v=ContentValues().apply{put("entity_type",type);put("title",title);put("description",detail);put("parent_entity_id",parentId.ifBlank{null});put("updated_at",System.currentTimeMillis())};db.update("entities",v,"id=?",arrayOf(id))}
                        tagsText.split(",").map{it.trim()}.filter{it.isNotBlank()}.forEach{tag->val tagId=store.createTag(universeId,tag);val v=ContentValues().apply{put("entity_id",id);put("tag_id",tagId)};db.insertWithOnConflict("entity_tags",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
                        if(linkTarget.isNotBlank())store.link("entity",id,"entity",linkTarget,linkLabel)
                    }
                }
                refresh++;showCreate=false
            }
        }){Text("Save")}},
        dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}}
    )
}
