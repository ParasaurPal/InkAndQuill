package com.parasaurpal.inkandquill.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TemplateField(val id:String,val templateId:String,val name:String,val fieldType:String,val required:Boolean,val position:Int,val section:String="",val options:String="",val hint:String="")

data class InkRecord(
    val id:String=UUID.randomUUID().toString(),
    val title:String="Untitled",
    val body:String="",
    val kind:String="Document",
    val parentId:String="",
    val scope:String="World",
    val tags:String="",
    val aliases:String="",
    val createdAt:Long=System.currentTimeMillis(),
    val updatedAt:Long=System.currentTimeMillis(),
    val favorite:Boolean=false
)

class InkDatabase(context:Context):SQLiteOpenHelper(context,"inkandquill.db",null,5){
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,title TEXT NOT NULL,body TEXT NOT NULL,kind TEXT NOT NULL,parentId TEXT,scope TEXT,tags TEXT,aliases TEXT,createdAt INTEGER,updatedAt INTEGER,favorite INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE links(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,position INTEGER DEFAULT 0,UNIQUE(sourceId,targetId,label))")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER,section TEXT DEFAULT 'General',options TEXT DEFAULT '',hint TEXT DEFAULT '',removed INTEGER DEFAULT 0)")
        db.execSQL("CREATE INDEX records_kind ON records(kind)")
        db.execSQL("CREATE INDEX records_parent ON records(parentId)")
        db.execSQL("CREATE INDEX links_target ON links(targetId)")
        db.execSQL("INSERT INTO settings VALUES('appearance','system'),('fontSize','18'),('autoLink','suggest'),('caseSensitive','false')")
        seed(db)
        db.execSQL("CREATE TABLE folders(id TEXT PRIMARY KEY,name TEXT NOT NULL,parentId TEXT,color TEXT,position INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE record_folders(recordId TEXT PRIMARY KEY,folderId TEXT NOT NULL)")
        db.execSQL("CREATE TABLE collections(id TEXT PRIMARY KEY,name TEXT NOT NULL,description TEXT,color TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE collection_records(collectionId TEXT NOT NULL,recordId TEXT NOT NULL,PRIMARY KEY(collectionId,recordId))")
        db.execSQL("CREATE TABLE attachments(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,uri TEXT NOT NULL,mimeType TEXT,displayName TEXT,size INTEGER,createdAt INTEGER)")
        db.execSQL("CREATE TABLE link_exclusions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,term TEXT NOT NULL,UNIQUE(recordId,term))")
        db.execSQL("CREATE TABLE link_suggestions(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,term TEXT NOT NULL,position INTEGER DEFAULT 0,status TEXT,UNIQUE(sourceId,targetId,position))")
        db.execSQL("CREATE TABLE open_tabs(position INTEGER PRIMARY KEY,recordId TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE template_sections(id TEXT PRIMARY KEY,templateId TEXT NOT NULL,name TEXT NOT NULL,position INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE template_versions(id TEXT PRIMARY KEY,templateId TEXT NOT NULL,snapshot TEXT NOT NULL,createdAt INTEGER)")
        db.execSQL("CREATE INDEX attachments_record ON attachments(recordId)")
        db.execSQL("CREATE INDEX collection_records_record ON collection_records(recordId)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        if(oldVersion<2){db.execSQL("CREATE TABLE IF NOT EXISTS revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)");db.execSQL("CREATE TABLE IF NOT EXISTS template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER)")}
        if(oldVersion<3) seed(db)
        if(oldVersion<4){
            db.execSQL("CREATE TABLE IF NOT EXISTS folders(id TEXT PRIMARY KEY,name TEXT NOT NULL,parentId TEXT,color TEXT,position INTEGER DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS record_folders(recordId TEXT PRIMARY KEY,folderId TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS collections(id TEXT PRIMARY KEY,name TEXT NOT NULL,description TEXT,color TEXT,createdAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS collection_records(collectionId TEXT NOT NULL,recordId TEXT NOT NULL,PRIMARY KEY(collectionId,recordId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS attachments(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,uri TEXT NOT NULL,mimeType TEXT,displayName TEXT,size INTEGER,createdAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS link_exclusions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,term TEXT NOT NULL,UNIQUE(recordId,term))")
            db.execSQL("CREATE TABLE IF NOT EXISTS link_suggestions(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,term TEXT NOT NULL,position INTEGER DEFAULT 0,status TEXT,UNIQUE(sourceId,targetId,position))")
            db.execSQL("CREATE TABLE IF NOT EXISTS open_tabs(position INTEGER PRIMARY KEY,recordId TEXT NOT NULL UNIQUE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS attachments_record ON attachments(recordId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS collection_records_record ON collection_records(recordId)")
        }
        if(oldVersion<5){
            runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN section TEXT DEFAULT 'General'")}
            runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN options TEXT DEFAULT ''")}
            runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN hint TEXT DEFAULT ''")}
            runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN removed INTEGER DEFAULT 0")}
            db.execSQL("CREATE TABLE IF NOT EXISTS template_sections(id TEXT PRIMARY KEY,templateId TEXT NOT NULL,name TEXT NOT NULL,position INTEGER DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS template_versions(id TEXT PRIMARY KEY,templateId TEXT NOT NULL,snapshot TEXT NOT NULL,createdAt INTEGER)")
        }
    }
    private fun seed(db:SQLiteDatabase){
        val data=listOf(
            "Character|character|Identity,Appearance,Personality,Backstory,Goals,Fears,Relationships,Abilities,Voice,Arc",
            "Location|location|Overview,Geography,Climate,History,Architecture,People,Culture,Secrets",
            "Culture|culture|Values,Customs,Language,Clothing,Food,Arts,Religion,Social Structure",
            "Faction|faction|Purpose,Leadership,Members,Allies,Enemies,Resources,History,Secrets",
            "Magic System|magic|Source,Rules,Costs,Limits,Practitioners,History,Exceptions",
            "Creature|creature|Description,Habitat,Behavior,Diet,Abilities,Weaknesses,Lore",
            "Deity|deity|Domains,Symbols,Myths,Worship,Followers,Relationships,Manifestations",
            "Timeline Event|event|Date,Era,Location,People Involved,Cause,Consequences,Sources",
            "Plot Outline|outline|Premise,Beginning,Inciting Incident,Turning Points,Climax,Resolution,Themes",
            "Scene|scene|Purpose,POV,Setting,Goal,Conflict,Outcome,Continuity Notes",
            "Research Note|research|Question,Summary,Source,Quotes,Relevance,Follow-up",
            "Poem|poem|Form,Theme,Imagery,Structure,Draft,Revision Notes",
            "NPC Profile|npc|Role,Appearance,Personality,Motivation,Knowledge,Relationships,Notes",
            "Character Arc|arc|Starting State,Want,Need,Flaw,Turning Points,Change,Ending",
            "Relationship|relationship|People Involved,Type,History,Dynamics,Conflict,Current State",
            "Family Tree|family|Members,Lineage,Branches,Inheritance,Secrets,Notes",
            "Inventory|inventory|Owner,Items,Origin,Properties,Condition,Notes",
            "World Overview|world|Premise,Themes,History,Rules,Regions,Peoples,Conflicts",
            "Continent or Region|region|Geography,Climate,Settlements,Resources,History,Politics",
            "Country or Kingdom|country|Government,Capital,Population,Economy,Military,Allies,Enemies",
            "City or Settlement|city|Districts,Landmarks,Government,Population,Trade,History",
            "Building or Interior|building|Purpose,Layout,Rooms,Materials,Occupants,Secrets",
            "Species Profile|species|Biology,Habitat,Culture,Language,Abilities,Life Cycle",
            "Society|society|Values,Class Structure,Law,Customs,Education,Economy",
            "Organization|organization|Mission,Structure,Leadership,Membership,Resources,Influence",
            "Spell or Ability|spell|Effect,Source,Requirements,Cost,Limitations,Countermeasures",
            "Artifact|artifact|Appearance,Origin,Abilities,Limitations,Ownership,History",
            "Technology or Invention|technology|Purpose,Components,Operation,Limitations,History",
            "Religion or Philosophy|belief|Beliefs,Practices,Texts,Symbols,Leadership,Schisms",
            "Language|language|Sounds,Grammar,Writing System,Vocabulary,History,Examples",
            "Three Act Outline|threeact|Act One,Act Two,Act Three,Turning Points,Climax",
            "Hero Journey|hero|Ordinary World,Call,Threshold,Trials,Ordeal,Reward,Return",
            "Subplot|subplot|Characters,Setup,Development,Intersection,Resolution",
            "Chapter Plan|chapter|Purpose,POV,Opening,Beats,Ending,Continuity",
            "Plot Twist|twist|Setup,Clues,Reveal,Impact,Aftermath",
            "Mystery and Clues|mystery|Question,Known Facts,Clues,Red Herrings,Revelation",
            "Calendar|calendar|Era,Months,Week Length,Seasons,Moons,Special Dates",
            "Historical Era|era|Dates,Major Events,Technology,Politics,Culture",
            "Map Reference|map|Area,Scale,Landmarks,Routes,Boundaries,Notes",
            "Floor Plan|floorplan|Levels,Rooms,Entrances,Exits,Hidden Areas",
            "Moodboard|moodboard|Visual Themes,Palette,Textures,References,Atmosphere",
            "Research Source|source|Citation,Author,Publication,URL,Summary,Trust Notes",
            "Reference Entry|reference|Subject,Description,Source,Connections,Notes"
        )
        data.forEach{line->val p=line.split("|");val v=ContentValues().apply{put("id","template_"+p[1]);put("title",p[0]);put("body",p[2].split(",").joinToString("\n\n"){"@@{it}:\n"});put("kind","Template")};db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    }
    private fun android.database.Cursor.record()=InkRecord(getString(getColumnIndexOrThrow("id")),getString(getColumnIndexOrThrow("title")),getString(getColumnIndexOrThrow("body")),getString(getColumnIndexOrThrow("kind")),getString(getColumnIndexOrThrow("parentId"))?:"",getString(getColumnIndexOrThrow("scope"))?:"World",getString(getColumnIndexOrThrow("tags"))?:"",getString(getColumnIndexOrThrow("aliases"))?:"",getLong(getColumnIndexOrThrow("createdAt")),getLong(getColumnIndexOrThrow("updatedAt")),getInt(getColumnIndexOrThrow("favorite"))==1)
    fun all(query:String="",kind:String?=null):List<InkRecord>{
        val sql=StringBuilder("SELECT * FROM records WHERE kind!='Template'");val args=mutableListOf<String>()
        if(!kind.isNullOrBlank()&&kind!="All"){sql.append(" AND kind=?");args.add(kind)}
        if(query.isNotBlank()){sql.append(" AND (title LIKE ? OR body LIKE ? OR tags LIKE ?)");val q="%$query%";args.addAll(listOf(q,q,q))}
        sql.append(" ORDER BY favorite DESC,updatedAt DESC")
        return readableDatabase.rawQuery(sql.toString(),args.toTypedArray()).use{c->buildList{while(c.moveToNext())add(c.record())}}
    }
    fun get(id:String)=readableDatabase.rawQuery("SELECT * FROM records WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.record()else null}
    fun templates()=readableDatabase.rawQuery("SELECT * FROM records WHERE kind='Template' ORDER BY title",null).use{c->buildList{while(c.moveToNext())add(c.record())}}
    fun save(r:InkRecord){
        val db=writableDatabase
        val old=get(r.id)
        if(old!=null && old.body!=r.body){val rev=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",old.id);put("title",old.title);put("body",old.body);put("createdAt",System.currentTimeMillis())};db.insert("revisions",null,rev)}
        val v=ContentValues().apply{put("id",r.id);put("title",r.title);put("body",r.body);put("kind",r.kind);put("parentId",r.parentId);put("scope",r.scope);put("tags",r.tags);put("aliases",r.aliases);put("createdAt",r.createdAt);put("updatedAt",System.currentTimeMillis());put("favorite",if(r.favorite)1 else 0)}
        db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun delete(id:String){writableDatabase.delete("records","id=?",arrayOf(id));writableDatabase.delete("links","sourceId=? OR targetId=?",arrayOf(id,id))}
    fun setting(key:String,fallback:String="")=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?",arrayOf(key)).use{if(it.moveToFirst())it.getString(0)else fallback}
    fun setSetting(key:String,value:String){val v=ContentValues().apply{put("key",key);put("value",value)};writableDatabase.insertWithOnConflict("settings",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
    fun addLink(source:String,target:String,label:String="mention",position:Int=0){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("position",position)};writableDatabase.insertWithOnConflict("links",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun backlinks(id:String)=readableDatabase.rawQuery("SELECT r.id,r.title,l.label,l.position FROM links l JOIN records r ON r.id=l.sourceId WHERE l.targetId=?",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(1),c.getString(2),c.getInt(3)))}}
    fun revisions(id:String)=readableDatabase.rawQuery("SELECT title,body,createdAt FROM revisions WHERE recordId=? ORDER BY createdAt DESC",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getLong(2)))}}
    fun exportJson():String{val arr=JSONArray();all().forEach{r->arr.put(JSONObject().put("id",r.id).put("title",r.title).put("body",r.body).put("kind",r.kind).put("parentId",r.parentId).put("scope",r.scope).put("tags",r.tags).put("aliases",r.aliases).put("createdAt",r.createdAt).put("favorite",r.favorite))};return JSONObject().put("format","inkandquill").put("version",2).put("records",arr).toString(2)}
    fun importJson(raw:String){val root=JSONObject(raw);val arr=root.optJSONArray("records")?:root.optJSONArray("items")?:JSONArray();for(i in 0 until arr.length()){val o=arr.getJSONObject(i);save(InkRecord(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled"),o.optString("body"),o.optString("kind",o.optString("type","Document")),o.optString("parentId"),o.optString("scope","World"),o.optString("tags"),o.optString("aliases"),o.optLong("createdAt",System.currentTimeMillis()),favorite=o.optBoolean("favorite")))}}

    fun createFolder(name:String,parentId:String="",color:String=""):String { val id=UUID.randomUUID().toString(); val v=ContentValues().apply{put("id",id);put("name",name.trim());put("parentId",parentId);put("color",color);put("position",0)};writableDatabase.insert("folders",null,v);return id }
    fun folders():List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,name,COALESCE(parentId,'') FROM folders ORDER BY position,name",null).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun moveToFolder(recordId:String,folderId:String?){if(folderId.isNullOrBlank())writableDatabase.delete("record_folders","recordId=?",arrayOf(recordId)) else {val v=ContentValues().apply{put("recordId",recordId);put("folderId",folderId)};writableDatabase.insertWithOnConflict("record_folders",null,v,SQLiteDatabase.CONFLICT_REPLACE)}}
    fun folderFor(recordId:String)=readableDatabase.rawQuery("SELECT folderId FROM record_folders WHERE recordId=?",arrayOf(recordId)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun createCollection(name:String,description:String=""):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("name",name.trim());put("description",description);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("collections",null,v);return id}
    fun addToCollection(collectionId:String,recordId:String){val v=ContentValues().apply{put("collectionId",collectionId);put("recordId",recordId)};writableDatabase.insertWithOnConflict("collection_records",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun collections():List<Pair<String,String>> = readableDatabase.rawQuery("SELECT id,name FROM collections ORDER BY name",null).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun addAttachment(recordId:String,uri:String,mimeType:String="",displayName:String="",size:Long=0L){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",recordId);put("uri",uri);put("mimeType",mimeType);put("displayName",displayName);put("size",size);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("attachments",null,v)}
    fun attachments(recordId:String):List<Map<String,String>> = readableDatabase.rawQuery("SELECT id,uri,mimeType,displayName FROM attachments WHERE recordId=?",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(mapOf("id" to c.getString(0),"uri" to c.getString(1),"mimeType" to (c.getString(2)?: ""),"displayName" to (c.getString(3)?: "")))}}
    fun removeAttachment(id:String){writableDatabase.delete("attachments","id=?",arrayOf(id))}
    fun excludeLinkTerm(recordId:String,term:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",recordId);put("term",term.trim())};writableDatabase.insertWithOnConflict("link_exclusions",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun isLinkExcluded(recordId:String,term:String)=readableDatabase.rawQuery("SELECT 1 FROM link_exclusions WHERE recordId=? AND lower(term)=lower(?)",arrayOf(recordId,term)).use{it.moveToFirst()}
    fun saveOpenTabs(ids:List<String>){writableDatabase.beginTransaction();try{writableDatabase.delete("open_tabs",null,null);ids.distinct().forEachIndexed{index,id->val v=ContentValues().apply{put("position",index);put("recordId",id)};writableDatabase.insert("open_tabs",null,v)};writableDatabase.setTransactionSuccessful()}finally{writableDatabase.endTransaction()}}
    fun openTabs():List<String> = readableDatabase.rawQuery("SELECT recordId FROM open_tabs ORDER BY position",null).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun replaceInRecord(id:String,find:String,replacement:String,matchCase:Boolean=false):Int{if(find.isEmpty())return 0;val r=get(id)?:return 0;val opts=if(matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE);val regex=Regex(Regex.escape(find),opts);val count=regex.findAll(r.body).count();if(count>0)save(r.copy(body=regex.replace(r.body){replacement}));return count}

    fun addLinkSuggestion(source:String,target:String,term:String,position:Int){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("term",term);put("position",position);put("status","pending")};writableDatabase.insertWithOnConflict("link_suggestions",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun pendingLinkSuggestions(source:String):List<Triple<String,String,Int>> = readableDatabase.rawQuery("SELECT targetId,term,position FROM link_suggestions WHERE sourceId=? AND status='pending' ORDER BY position",arrayOf(source)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getInt(2)))}}
    fun resolveLinkSuggestion(source:String,target:String,position:Int,approve:Boolean){writableDatabase.execSQL("UPDATE link_suggestions SET status=? WHERE sourceId=? AND targetId=? AND position=?",arrayOf(if(approve)"approved" else "dismissed",source,target,position));if(approve)addLink(source,target,"mention",position)}
    fun templateFields(templateId:String,includeRemoved:Boolean=false):List<TemplateField>{
        val sql="SELECT id,templateId,name,fieldType,required,position,COALESCE(section,'General'),COALESCE(options,''),COALESCE(hint,'') FROM template_fields WHERE templateId=? "+(if(includeRemoved) "" else "AND COALESCE(removed,0)=0 ")+" ORDER BY position"
        return readableDatabase.rawQuery(sql,arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(TemplateField(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1,c.getInt(5),c.getString(6),c.getString(7),c.getString(8)))}}
    }
    fun addTemplateField(templateId:String,name:String,type:String,required:Boolean,section:String="General",options:String="",hint:String=""){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",name.trim());put("fieldType",type);put("required",if(required)1 else 0);put("position",templateFields(templateId,true).size);put("section",section);put("options",options);put("hint",hint);put("removed",0)};writableDatabase.insert("template_fields",null,v)}
    fun updateTemplateField(id:String,name:String,type:String,required:Boolean,section:String,options:String,hint:String){val v=ContentValues().apply{put("name",name);put("fieldType",type);put("required",if(required)1 else 0);put("section",section);put("options",options);put("hint",hint)};writableDatabase.update("template_fields",v,"id=?",arrayOf(id))}
    fun removeTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=1 WHERE id=?",arrayOf(id))}
    fun restoreTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=0 WHERE id=?",arrayOf(id))}
    fun templateSections(templateId:String):List<String> = readableDatabase.rawQuery("SELECT name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun addTemplateSection(templateId:String,name:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",name);put("position",templateSections(templateId).size)};writableDatabase.insert("template_sections",null,v)}
    fun templateVersion(templateId:String){val snapshot=JSONArray();templateFields(templateId,true).forEach{f->snapshot.put(JSONObject().put("id",f.id).put("name",f.name).put("type",f.fieldType).put("required",f.required).put("section",f.section).put("options",f.options).put("hint",f.hint).put("position",f.position))};val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("snapshot",snapshot.toString());put("createdAt",System.currentTimeMillis())};writableDatabase.insert("template_versions",null,v)}
    fun templateVersionCount(templateId:String)=readableDatabase.rawQuery("SELECT COUNT(*) FROM template_versions WHERE templateId=?",arrayOf(templateId)).use{it.moveToFirst();it.getInt(0)}
    fun recordsForRevision(id:String)=revisions(id)
}
