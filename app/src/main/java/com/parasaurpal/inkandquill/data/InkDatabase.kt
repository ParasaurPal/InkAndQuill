package com.parasaurpal.inkandquill.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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

class InkDatabase(context:Context):SQLiteOpenHelper(context,"inkandquill.db",null,2){
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,title TEXT NOT NULL,body TEXT NOT NULL,kind TEXT NOT NULL,parentId TEXT,scope TEXT,tags TEXT,aliases TEXT,createdAt INTEGER,updatedAt INTEGER,favorite INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE links(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,position INTEGER DEFAULT 0,UNIQUE(sourceId,targetId,label))")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER)")
        db.execSQL("CREATE INDEX records_kind ON records(kind)")
        db.execSQL("CREATE INDEX records_parent ON records(parentId)")
        db.execSQL("CREATE INDEX links_target ON links(targetId)")
        db.execSQL("INSERT INTO settings VALUES('appearance','system'),('fontSize','18'),('autoLink','suggest'),('caseSensitive','false')")
        seed(db)
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        if(oldVersion<2){db.execSQL("CREATE TABLE IF NOT EXISTS revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)");db.execSQL("CREATE TABLE IF NOT EXISTS template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER)")}
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
            "Poem|poem|Form,Theme,Imagery,Structure,Draft,Revision Notes"
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
    fun recordsForRevision(id:String)=revisions(id)
}
