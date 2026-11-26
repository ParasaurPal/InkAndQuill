package com.parasaurpal.inkandquill.ui

import android.content.ContentValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parasaurpal.inkandquill.data.NovelStudioStore
import org.json.JSONObject

private enum class StudioToolTab { BIBLE, TIMELINES, BOARDS, GOALS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelStudioToolsScreen(universeId:String,store:NovelStudioStore,onClose:()->Unit) {
    var tab by remember { mutableStateOf(StudioToolTab.BIBLE) }
    var refresh by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Character") }
    var selectedTimeline by remember { mutableStateOf("") }
    val db=store.readableDatabase
    val entities=remember(universeId,refresh,tab){db.rawQuery("SELECT id,entity_type,title,description,custom_label,fields_json FROM entities WHERE universe_id=? ORDER BY title",arrayOf(universeId)).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("type",c.getString(1)).put("title",c.getString(2)).put("description",c.getString(3)).put("label",c.getString(4)).put("fields",c.getString(5)))}}}
    val timelines=remember(universeId,refresh,tab){db.rawQuery("SELECT id,name,custom_description FROM timelines WHERE universe_id=? ORDER BY position,name",arrayOf(universeId)).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("name",c.getString(1)).put("description",c.getString(2)))}}}
    val boards=remember(universeId,refresh,tab){db.rawQuery("SELECT id,title,board_type FROM boards WHERE universe_id=? ORDER BY updated_at DESC",arrayOf(universeId)).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("title",c.getString(1)).put("type",c.getString(2)))}}}
    val goals=remember(universeId,refresh,tab){db.rawQuery("SELECT id,period,target_words,active_days_per_week,deadline_at FROM writing_goals WHERE universe_id=? ORDER BY created_at DESC",arrayOf(universeId)).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("period",c.getString(1)).put("target",c.getInt(2)).put("days",c.getInt(3)).put("deadline",c.getLong(4)))}}}
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text("Worldbuilding & planning",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            TextButton(onClick=onClose){Text("Back to writing")}
        }
        TabRow(selectedTabIndex=tab.ordinal) {
            Tab(tab==StudioToolTab.BIBLE,{tab=StudioToolTab.BIBLE},text={Text("Story Bible")})
            Tab(tab==StudioToolTab.TIMELINES,{tab=StudioToolTab.TIMELINES},text={Text("Timelines")})
            Tab(tab==StudioToolTab.BOARDS,{tab=StudioToolTab.BOARDS},text={Text("Boards")})
            Tab(tab==StudioToolTab.GOALS,{tab=StudioToolTab.GOALS},text={Text("Goals")})
        }
        when(tab) {
            StudioToolTab.BIBLE -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text(entities.size.toString()+" universe-wide entries",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick={type="Character";title="";detail="";showCreate=true}){Text("Add entry")}
                }
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
                    items(entities){e->Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(14.dp)){Text(e.optString("title"),fontWeight=FontWeight.SemiBold);Text(e.optString("type"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);if(e.optString("label").isNotBlank())Text(e.optString("label"),style=MaterialTheme.typography.labelSmall);Text(e.optString("description"))}}}
                    if(entities.isEmpty())item{Text("Create characters, locations, items, species, languages or custom records.")}
                }
            }
            StudioToolTab.TIMELINES -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(timelines.size.toString()+" timelines");Button(onClick={type="Timeline";title="";detail="";showCreate=true}){Text("New timeline")}}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
                    items(timelines){t->
                        val moments=remember(t.optString("id"),refresh){db.rawQuery("SELECT date_or_era_text,title,custom_text FROM moments WHERE timeline_id=? ORDER BY sort_key",arrayOf(t.optString("id"))).use{c->buildList{while(c.moveToNext())add(JSONObject().put("date",c.getString(0)).put("title",c.getString(1)).put("text",c.getString(2)))}}}
                        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(14.dp)){Text(t.optString("name"),fontWeight=FontWeight.SemiBold);Text(t.optString("description"));moments.forEach{m->Text(m.optString("date")+" · "+m.optString("title"),style=MaterialTheme.typography.bodyMedium);if(m.optString("text").isNotBlank())Text(m.optString("text"),style=MaterialTheme.typography.bodySmall)};TextButton(onClick={selectedTimeline=t.optString("id");type="Moment";title="";detail="";showCreate=true}){Text("Add moment")}}}
                    }
                }
            }
            StudioToolTab.BOARDS -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(boards.size.toString()+" visual boards");Button(onClick={type="Board";title="";detail="";showCreate=true}){Text("New board")}}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
                    items(boards){b->val notes=remember(b.optString("id"),refresh){db.rawQuery("SELECT payload_json FROM board_objects WHERE board_id=? ORDER BY z_index",arrayOf(b.optString("id"))).use{c->buildList{while(c.moveToNext())add(JSONObject(c.getString(0)).optString("text"))}}};Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(14.dp)){Text(b.optString("title"),fontWeight=FontWeight.SemiBold);Text(b.optString("type"),style=MaterialTheme.typography.labelSmall);notes.forEach{Text("• "+it)};TextButton(onClick={selectedTimeline=b.optString("id");type="Board note";title="";detail="";showCreate=true}){Text("Add text card")}}}}
                }
            }
            StudioToolTab.GOALS -> {
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(goals.size.toString()+" writing goals");Button(onClick={type="Goal";title="";detail="";showCreate=true}){Text("Set goal")}}
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
                    items(goals){g->Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.padding(14.dp)){Text(g.optString("period")+" goal",fontWeight=FontWeight.SemiBold);Text(g.optInt("target").toString()+" words · "+g.optInt("days").toString()+" active days per week")}}}
                }
            }
        }
    }
    if(showCreate) AlertDialog(
        onDismissRequest={showCreate=false},
        title={Text(when(type){"Timeline"->"New timeline";"Moment"->"Add timeline moment";"Board"->"New visual board";"Board note"->"Add board text";"Goal"->"Set writing goal";else->"New Story Bible entry"})},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(title,{title=it},label={Text(if(type=="Moment")"Moment title" else "Name")},singleLine=true)
            if(type=="Character"||type=="Location"||type=="Item"||type=="Species"||type=="Language"||type=="Custom") OutlinedTextField(type,{type=it},label={Text("Entity type")},singleLine=true)
            OutlinedTextField(detail,{detail=it},label={Text(if(type=="Moment")"Custom date / era and notes" else "Description / custom text")},minLines=2)
        }},
        confirmButton={TextButton(onClick={
            if(title.isNotBlank()) {
                when(type) {
                    "Timeline"->store.createTimeline(universeId,title,detail)
                    "Moment"->store.createMoment(selectedTimeline,detail.substringBefore("\n"),title,detail.substringAfter("\n",""),System.currentTimeMillis().toDouble())
                    "Board"->store.createBoard(universeId,title,detail.ifBlank{"PIN_BOARD"})
                    "Board note"->store.addBoardText(selectedTimeline,title,24f,24f,"")
                    "Goal"->{val target=detail.toIntOrNull()?:50000;val v=ContentValues().apply{put("id",java.util.UUID.randomUUID().toString());put("universe_id",universeId);put("period","total");put("target_words",target);put("active_days_per_week",5);put("created_at",System.currentTimeMillis())};store.writableDatabase.insertOrThrow("writing_goals",null,v)}
                    else->store.createEntity(universeId,type,title,detail)
                }
                refresh++;showCreate=false
            }
        }){Text("Save")}},
        dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}}
    )
}
