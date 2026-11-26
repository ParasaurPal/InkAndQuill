package com.parasaurpal.inkandquill.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Normalized offline-first manuscript/workbuilding store.
 *
 * Kept separate from the legacy InkDatabase so existing user libraries are never
 * rewritten by this feature. Every mutation is committed locally in SQLite before
 * the caller can enqueue any optional synchronization work.
 */
data class StudioNode(
    val id: String,
    val universeId: String,
    val parentId: String?,
    val kind: String,
    val title: String,
    val body: String,
    val position: Long,
    val status: String = "",
    val customStatus: String = "",
    val metadata: String = "{}",
    val updatedAt: Long = 0L
)

data class StudioEntity(
    val id: String,
    val universeId: String,
    val type: String,
    val title: String,
    val description: String,
    val parentId: String?,
    val fieldsJson: String,
    val customLabel: String,
    val updatedAt: Long
)

class NovelStudioStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "novel_studio.db", null, 1) {

    companion object {
        private val CONTENT_TABLES = listOf(
            "universes", "books", "chapters", "scenes", "documents",
            "annotations", "entities", "entity_links", "tags", "entity_tags",
            "attachments", "entity_categories", "category_fields", "embedded_tables",
            "table_columns", "table_rows", "table_cells", "timelines", "moments",
            "events", "event_entity_links", "boards", "board_objects", "board_connectors",
            "relationship_edges", "sprint_sessions", "writing_goals", "daily_logs",
            "prompt_entries", "version_snapshots"
        )
        private fun id() = UUID.randomUUID().toString()
        private fun now() = System.currentTimeMillis()
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val ddl = listOf(
            """CREATE TABLE universes(id TEXT PRIMARY KEY,title TEXT NOT NULL,author TEXT NOT NULL DEFAULT '',description TEXT NOT NULL DEFAULT '',settings_json TEXT NOT NULL DEFAULT '{}',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE books(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,title TEXT NOT NULL,custom_label TEXT NOT NULL DEFAULT '',position INTEGER NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE chapters(id TEXT PRIMARY KEY,book_id TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,title TEXT NOT NULL,position INTEGER NOT NULL,status_key TEXT NOT NULL DEFAULT 'NOT_STARTED',custom_status_text TEXT NOT NULL DEFAULT '',manual_status_override INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE scenes(id TEXT PRIMARY KEY,chapter_id TEXT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,title TEXT NOT NULL,position INTEGER NOT NULL,summary TEXT NOT NULL DEFAULT '',goal TEXT NOT NULL DEFAULT '',conflict TEXT NOT NULL DEFAULT '',pov_character_id TEXT,sensory_details TEXT NOT NULL DEFAULT '',hook TEXT NOT NULL DEFAULT '',custom_fields_json TEXT NOT NULL DEFAULT '{}',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE documents(id TEXT PRIMARY KEY,scene_id TEXT NOT NULL UNIQUE REFERENCES scenes(id) ON DELETE CASCADE,format_version INTEGER NOT NULL DEFAULT 1,content_json TEXT NOT NULL DEFAULT '{\"blocks\":[]}',plain_text TEXT NOT NULL DEFAULT '',word_count INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE annotations(id TEXT PRIMARY KEY,document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,start_offset INTEGER NOT NULL,end_offset INTEGER NOT NULL,kind TEXT NOT NULL,custom_label TEXT NOT NULL DEFAULT '',body_text TEXT NOT NULL DEFAULT '',color TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)""",
            """CREATE TABLE entities(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,entity_type TEXT NOT NULL,title TEXT NOT NULL,custom_label TEXT NOT NULL DEFAULT '',description TEXT NOT NULL DEFAULT '',parent_entity_id TEXT REFERENCES entities(id) ON DELETE SET NULL,fields_json TEXT NOT NULL DEFAULT '{}',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE entity_links(id TEXT PRIMARY KEY,source_type TEXT NOT NULL,source_id TEXT NOT NULL,target_type TEXT NOT NULL,target_id TEXT NOT NULL,relation_label TEXT NOT NULL DEFAULT '',custom_text TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,UNIQUE(source_type,source_id,target_type,target_id,relation_label))""",
            """CREATE TABLE tags(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,text TEXT NOT NULL,color TEXT NOT NULL DEFAULT '',UNIQUE(universe_id,text))""",
            """CREATE TABLE entity_tags(entity_id TEXT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,tag_id TEXT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,PRIMARY KEY(entity_id,tag_id))""",
            """CREATE TABLE attachments(id TEXT PRIMARY KEY,owner_type TEXT NOT NULL,owner_id TEXT NOT NULL,storage_key TEXT NOT NULL,mime_type TEXT NOT NULL DEFAULT '',file_name TEXT NOT NULL DEFAULT '',checksum TEXT NOT NULL DEFAULT '',size_bytes INTEGER NOT NULL DEFAULT 0,caption TEXT NOT NULL DEFAULT '',position INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)""",
            """CREATE TABLE entity_categories(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,name TEXT NOT NULL,icon_key TEXT NOT NULL DEFAULT '',color TEXT NOT NULL DEFAULT '',schema_version INTEGER NOT NULL DEFAULT 1)""",
            """CREATE TABLE category_fields(id TEXT PRIMARY KEY,category_id TEXT NOT NULL REFERENCES entity_categories(id) ON DELETE CASCADE,field_key TEXT NOT NULL,label TEXT NOT NULL,field_type TEXT NOT NULL,position INTEGER NOT NULL,config_json TEXT NOT NULL DEFAULT '{}',required INTEGER NOT NULL DEFAULT 0)""",
            """CREATE TABLE embedded_tables(id TEXT PRIMARY KEY,owner_entity_id TEXT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,title TEXT NOT NULL,position INTEGER NOT NULL DEFAULT 0)""",
            """CREATE TABLE table_columns(id TEXT PRIMARY KEY,table_id TEXT NOT NULL REFERENCES embedded_tables(id) ON DELETE CASCADE,column_key TEXT NOT NULL,label TEXT NOT NULL,column_type TEXT NOT NULL,position INTEGER NOT NULL,config_json TEXT NOT NULL DEFAULT '{}')""",
            """CREATE TABLE table_rows(id TEXT PRIMARY KEY,table_id TEXT NOT NULL REFERENCES embedded_tables(id) ON DELETE CASCADE,position INTEGER NOT NULL)""",
            """CREATE TABLE table_cells(row_id TEXT NOT NULL REFERENCES table_rows(id) ON DELETE CASCADE,column_id TEXT NOT NULL REFERENCES table_columns(id) ON DELETE CASCADE,value_json TEXT NOT NULL DEFAULT 'null',PRIMARY KEY(row_id,column_id))""",
            """CREATE TABLE timelines(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,name TEXT NOT NULL,custom_description TEXT NOT NULL DEFAULT '',position INTEGER NOT NULL DEFAULT 0)""",
            """CREATE TABLE moments(id TEXT PRIMARY KEY,timeline_id TEXT NOT NULL REFERENCES timelines(id) ON DELETE CASCADE,date_or_era_text TEXT NOT NULL DEFAULT '',sort_key REAL NOT NULL,title TEXT NOT NULL,custom_text TEXT NOT NULL DEFAULT '')""",
            """CREATE TABLE events(id TEXT PRIMARY KEY,timeline_id TEXT NOT NULL REFERENCES timelines(id) ON DELETE CASCADE,moment_id TEXT REFERENCES moments(id) ON DELETE SET NULL,description TEXT NOT NULL DEFAULT '',notes TEXT NOT NULL DEFAULT '',custom_fields_json TEXT NOT NULL DEFAULT '{}')""",
            """CREATE TABLE event_entity_links(event_id TEXT NOT NULL REFERENCES events(id) ON DELETE CASCADE,entity_id TEXT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,role_label TEXT NOT NULL DEFAULT '',PRIMARY KEY(event_id,entity_id,role_label))""",
            """CREATE TABLE boards(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,title TEXT NOT NULL,board_type TEXT NOT NULL,viewport_json TEXT NOT NULL DEFAULT '{}',background_json TEXT NOT NULL DEFAULT '{}',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)""",
            """CREATE TABLE board_objects(id TEXT PRIMARY KEY,board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,object_type TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,width REAL NOT NULL DEFAULT 240,height REAL NOT NULL DEFAULT 160,rotation REAL NOT NULL DEFAULT 0,z_index INTEGER NOT NULL DEFAULT 0,payload_json TEXT NOT NULL DEFAULT '{}')""",
            """CREATE TABLE board_connectors(id TEXT PRIMARY KEY,board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,source_object_id TEXT NOT NULL REFERENCES board_objects(id) ON DELETE CASCADE,target_object_id TEXT NOT NULL REFERENCES board_objects(id) ON DELETE CASCADE,direction TEXT NOT NULL DEFAULT 'FORWARD',label_text TEXT NOT NULL DEFAULT '',style_json TEXT NOT NULL DEFAULT '{}')""",
            """CREATE TABLE relationship_edges(id TEXT PRIMARY KEY,board_id TEXT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,source_entity_id TEXT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,target_entity_id TEXT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,edge_family TEXT NOT NULL,edge_type TEXT NOT NULL,custom_label TEXT NOT NULL DEFAULT '',source_note TEXT NOT NULL DEFAULT '',target_note TEXT NOT NULL DEFAULT '',color TEXT NOT NULL DEFAULT '',line_style TEXT NOT NULL DEFAULT '')""",
            """CREATE TABLE sprint_sessions(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,started_at INTEGER NOT NULL,ended_at INTEGER,target_words INTEGER NOT NULL DEFAULT 0,word_delta INTEGER NOT NULL DEFAULT 0,notes TEXT NOT NULL DEFAULT '')""",
            """CREATE TABLE writing_goals(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,period TEXT NOT NULL,target_words INTEGER NOT NULL,active_days_per_week INTEGER NOT NULL DEFAULT 5,deadline_at INTEGER,created_at INTEGER NOT NULL)""",
            """CREATE TABLE daily_logs(id TEXT PRIMARY KEY,universe_id TEXT NOT NULL REFERENCES universes(id) ON DELETE CASCADE,day_key TEXT NOT NULL,word_delta INTEGER NOT NULL DEFAULT 0,character_count INTEGER NOT NULL DEFAULT 0,paragraph_count INTEGER NOT NULL DEFAULT 0,session_notes TEXT NOT NULL DEFAULT '',custom_text TEXT NOT NULL DEFAULT '',UNIQUE(universe_id,day_key))""",
            """CREATE TABLE prompt_entries(id TEXT PRIMARY KEY,universe_id TEXT REFERENCES universes(id) ON DELETE CASCADE,prompt_text TEXT NOT NULL,source TEXT NOT NULL DEFAULT 'user',created_at INTEGER NOT NULL)""",
            """CREATE TABLE version_snapshots(id TEXT PRIMARY KEY,owner_type TEXT NOT NULL,owner_id TEXT NOT NULL,label TEXT NOT NULL DEFAULT '',content_json TEXT NOT NULL,created_at INTEGER NOT NULL,automatic INTEGER NOT NULL DEFAULT 0)""",
            "CREATE INDEX books_universe_order ON books(universe_id,position)",
            "CREATE INDEX chapters_book_order ON chapters(book_id,position)",
            "CREATE INDEX scenes_chapter_order ON scenes(chapter_id,position)",
            "CREATE INDEX entities_universe_type ON entities(universe_id,entity_type)",
            "CREATE INDEX entity_links_target ON entity_links(target_type,target_id)",
            "CREATE INDEX moments_timeline_order ON moments(timeline_id,sort_key)",
            "CREATE INDEX board_objects_order ON board_objects(board_id,z_index)"
        )
        db.beginTransaction()
        try {
            ddl.forEach(db::execSQL)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema changes are additive and must never destroy user-authored work.
        if (oldVersion < 1) onCreate(db)
    }

    fun createUniverse(title: String, author: String = "", description: String = ""): String {
        require(title.isNotBlank()) { "Universe title cannot be blank." }
        val key = id(); val time = now()
        val values = ContentValues().apply {
            put("id", key); put("title", title); put("author", author)
            put("description", description); put("created_at", time); put("updated_at", time)
        }
        writableDatabase.insertOrThrow("universes", null, values)
        return key
    }

    fun createBook(universeId: String, title: String, customLabel: String = ""): String =
        createOrderedChild("books", "universe_id", universeId, title, customLabel)

    fun createChapter(bookId: String, title: String, customStatus: String = ""): String {
        val key=id(); val time=now()
        val position=nextPosition("chapters","book_id",bookId)
        val v=ContentValues().apply { put("id",key);put("book_id",bookId);put("title",title);put("position",position);put("custom_status_text",customStatus);put("created_at",time);put("updated_at",time) }
        writableDatabase.insertOrThrow("chapters",null,v); return key
    }

    fun createScene(chapterId: String, title: String): String {
        val key=id(); val time=now(); val db=writableDatabase
        db.beginTransaction()
        try {
            val position=nextPosition("scenes","chapter_id",chapterId)
            val scene=ContentValues().apply { put("id",key);put("chapter_id",chapterId);put("title",title);put("position",position);put("created_at",time);put("updated_at",time) }
            db.insertOrThrow("scenes",null,scene)
            val doc=ContentValues().apply { put("id",id());put("scene_id",key);put("content_json","{\"blocks\":[]}");put("updated_at",time) }
            db.insertOrThrow("documents",null,doc)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        updateChapterStatus(chapterId)
        return key
    }

    private fun createOrderedChild(table:String,parentColumn:String,parentId:String,title:String,label:String):String {
        require(table=="books")
        val key=id();val time=now();val pos=nextPosition(table,parentColumn,parentId)
        val v=ContentValues().apply{put("id",key);put(parentColumn,parentId);put("title",title);put("custom_label",label);put("position",pos);put("created_at",time);put("updated_at",time)}
        writableDatabase.insertOrThrow(table,null,v);return key
    }

    private fun nextPosition(table:String,parentColumn:String,parentId:String):Long {
        require(table in setOf("books","chapters","scenes"))
        return writableDatabase.rawQuery("SELECT COALESCE(MAX(position),0)+1024 FROM $table WHERE $parentColumn=?",arrayOf(parentId)).use{if(it.moveToFirst())it.getLong(0)else 1024L}
    }

    fun updateChapterStatus(chapterId:String) {
        val db=writableDatabase
        val hasWords=db.rawQuery("SELECT COUNT(*) FROM scenes s JOIN documents d ON d.scene_id=s.id WHERE s.chapter_id=? AND LENGTH(TRIM(d.plain_text))>0",arrayOf(chapterId)).use{it.moveToFirst()&&it.getInt(0)>0}
        val hasPlan=db.rawQuery("SELECT COUNT(*) FROM scenes WHERE chapter_id=? AND (summary<>'' OR goal<>'' OR conflict<>'')",arrayOf(chapterId)).use{it.moveToFirst()&&it.getInt(0)>0}
        val manual=db.rawQuery("SELECT manual_status_override FROM chapters WHERE id=?",arrayOf(chapterId)).use{it.moveToFirst()&&it.getInt(0)==1}
        if(!manual){val status=if(hasWords)"WRITING" else if(hasPlan)"OUTLINING" else "NOT_STARTED";db.execSQL("UPDATE chapters SET status_key=?,updated_at=? WHERE id=?",arrayOf(status,now(),chapterId))}
    }

    fun setChapterStatus(chapterId:String,statusKey:String,customText:String,manual:Boolean=true) {
        require(statusKey in setOf("NOT_STARTED","OUTLINING","WRITING","FINISHED"))
        val v=ContentValues().apply{put("status_key",statusKey);put("custom_status_text",customText);put("manual_status_override",if(manual)1 else 0);put("updated_at",now())}
        writableDatabase.update("chapters",v,"id=?",arrayOf(chapterId))
    }

    fun saveScene(sceneId:String,title:String,metadata:JSONObject,documentJson:JSONObject,plainText:String) {
        val db=writableDatabase;val time=now()
        db.beginTransaction()
        try {
            val fields=ContentValues().apply{put("title",title);put("summary",metadata.optString("summary"));put("goal",metadata.optString("goal"));put("conflict",metadata.optString("conflict"));put("pov_character_id",metadata.optString("pov_character_id"));put("sensory_details",metadata.optString("sensory_details"));put("hook",metadata.optString("hook"));put("custom_fields_json",metadata.optJSONObject("custom_fields")?.toString()?: "{}");put("updated_at",time)}
            db.update("scenes",fields,"id=?",arrayOf(sceneId))
            val docId=db.rawQuery("SELECT id FROM documents WHERE scene_id=?",arrayOf(sceneId)).use{if(it.moveToFirst())it.getString(0)else null}
            if(docId==null){val v=ContentValues().apply{put("id",id());put("scene_id",sceneId);put("content_json",documentJson.toString());put("plain_text",plainText);put("word_count",wordCount(plainText));put("updated_at",time)};db.insertOrThrow("documents",null,v)}
            else {val v=ContentValues().apply{put("content_json",documentJson.toString());put("plain_text",plainText);put("word_count",wordCount(plainText));put("updated_at",time)};db.update("documents",v,"id=?",arrayOf(docId))}
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
        val chapterId=db.rawQuery("SELECT chapter_id FROM scenes WHERE id=?",arrayOf(sceneId)).use{if(it.moveToFirst())it.getString(0)else null}
        chapterId?.let(::updateChapterStatus)
    }

    fun createEntity(universeId:String,type:String,title:String,description:String="",parentId:String?=null,fields:JSONObject=JSONObject(),customLabel:String=""):String {
        require(type.isNotBlank()&&title.isNotBlank())
        val key=id();val time=now()
        val v=ContentValues().apply{put("id",key);put("universe_id",universeId);put("entity_type",type);put("title",title);put("description",description);put("parent_entity_id",parentId);put("fields_json",fields.toString());put("custom_label",customLabel);put("created_at",time);put("updated_at",time)}
        writableDatabase.insertOrThrow("entities",null,v);return key
    }

    fun link(sourceType:String,sourceId:String,targetType:String,targetId:String,label:String="",customText:String=""):String {
        val key=id();val v=ContentValues().apply{put("id",key);put("source_type",sourceType);put("source_id",sourceId);put("target_type",targetType);put("target_id",targetId);put("relation_label",label);put("custom_text",customText);put("created_at",now())}
        writableDatabase.insertWithOnConflict("entity_links",null,v,SQLiteDatabase.CONFLICT_IGNORE)
        return key
    }

    fun backlinks(type:String,id:String):List<JSONObject> =
        writableDatabase.rawQuery("SELECT source_type,source_id,relation_label,custom_text FROM entity_links WHERE target_type=? AND target_id=? UNION ALL SELECT target_type,target_id,relation_label,custom_text FROM entity_links WHERE source_type=? AND source_id=?",arrayOf(type,id,type,id)).use{c->buildList{while(c.moveToNext())add(JSONObject().put("type",c.getString(0)).put("id",c.getString(1)).put("label",c.getString(2)).put("text",c.getString(3)))}}

    fun createTag(universeId:String,text:String,color:String=""):String {
        require(text.isNotBlank());val key=id()
        val v=ContentValues().apply{put("id",key);put("universe_id",universeId);put("text",text);put("color",color)}
        writableDatabase.insertWithOnConflict("tags",null,v,SQLiteDatabase.CONFLICT_IGNORE)
        return writableDatabase.rawQuery("SELECT id FROM tags WHERE universe_id=? AND text=?",arrayOf(universeId,text)).use{it.moveToFirst();it.getString(0)}
    }

    fun saveVersion(ownerType:String,ownerId:String,label:String,content:JSONObject,automatic:Boolean=false):String {
        val key=id();val v=ContentValues().apply{put("id",key);put("owner_type",ownerType);put("owner_id",ownerId);put("label",label);put("content_json",content.toString());put("created_at",now());put("automatic",if(automatic)1 else 0)}
        writableDatabase.insertOrThrow("version_snapshots",null,v);return key
    }

    fun orderedNodes(parentId:String,kind:String):List<StudioNode> {
        val (table,parentColumn)=when(kind){"Book"->"books" to "universe_id";"Chapter"->"chapters" to "book_id";"Scene"->"scenes" to "chapter_id";else->throw IllegalArgumentException("Unknown manuscript node type")}
        val sql=when(kind) {
            "Book"->"SELECT id,universe_id,NULL, 'Book',title,'' ,position,'','', '{}' ,updated_at FROM books WHERE universe_id=? ORDER BY position"
            "Chapter"->"SELECT id,'',book_id,'Chapter',title,'',position,status_key,custom_status_text,'{}',updated_at FROM chapters WHERE book_id=? ORDER BY position"
            else->"SELECT id,'',chapter_id,'Scene',title,'',position,'','',json_object('summary',summary,'goal',goal,'conflict',conflict,'hook',hook),updated_at FROM scenes WHERE chapter_id=? ORDER BY position"
        }
        return readableDatabase.rawQuery(sql,arrayOf(parentId)).use{c->buildList{while(c.moveToNext())add(StudioNode(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),c.getString(7),c.getString(8),c.getString(9),c.getLong(10)))}}
    }

    fun universes():List<JSONObject> = readableDatabase.rawQuery("SELECT id,title,author,description,updated_at FROM universes ORDER BY updated_at DESC",null).use{c->buildList{while(c.moveToNext())add(JSONObject().put("id",c.getString(0)).put("title",c.getString(1)).put("author",c.getString(2)).put("description",c.getString(3)).put("updated_at",c.getLong(4)))}}

    fun createTimeline(universeId:String,name:String,description:String=""):String {
        val key=id();val v=ContentValues().apply{put("id",key);put("universe_id",universeId);put("name",name);put("custom_description",description)}
        writableDatabase.insertOrThrow("timelines",null,v);return key
    }

    fun createMoment(timelineId:String,dateOrEra:String,title:String,customText:String="",sortKey:Double=now().toDouble()):String {
        val key=id();val v=ContentValues().apply{put("id",key);put("timeline_id",timelineId);put("date_or_era_text",dateOrEra);put("title",title);put("custom_text",customText);put("sort_key",sortKey)}
        writableDatabase.insertOrThrow("moments",null,v);return key
    }

    fun createBoard(universeId:String,title:String,type:String):String {
        val key=id();val time=now();val v=ContentValues().apply{put("id",key);put("universe_id",universeId);put("title",title);put("board_type",type);put("created_at",time);put("updated_at",time)}
        writableDatabase.insertOrThrow("boards",null,v);return key
    }

    fun addBoardText(boardId:String,text:String,x:Float=0f,y:Float=0f,color:String=""):String {
        val key=id();val payload=JSONObject().put("text",text).put("color",color)
        val v=ContentValues().apply{put("id",key);put("board_id",boardId);put("object_type","TEXT");put("x",x);put("y",y);put("payload_json",payload.toString())}
        writableDatabase.insertOrThrow("board_objects",null,v);return key
    }

    fun exportJson():String {
        val root=JSONObject().put("format","fortelling").put("schema_version",1).put("exported_at",now())
        CONTENT_TABLES.forEach { table ->
            val rows=JSONArray()
            readableDatabase.rawQuery("SELECT * FROM $table",null).use{c->while(c.moveToNext()){
                val row=JSONObject()
                for(i in 0 until c.columnCount) {
                    when(c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL->row.put(c.getColumnName(i),JSONObject.NULL)
                        android.database.Cursor.FIELD_TYPE_INTEGER->row.put(c.getColumnName(i),c.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT->row.put(c.getColumnName(i),c.getDouble(i))
                        else->row.put(c.getColumnName(i),c.getString(i))
                    }
                }
                rows.put(row)
            }}
            root.put(table,rows)
        }
        return root.toString(2)
    }

    fun importJson(raw:String) {
        val root=JSONObject(raw)
        require(root.optString("format")=="fortelling") { "Not a Fortelling project archive." }
        require(root.optInt("schema_version",0) in 1..1) { "Unsupported Fortelling archive version." }
        val db=writableDatabase
        db.beginTransaction()
        try {
            CONTENT_TABLES.forEach { table ->
                val rows=root.optJSONArray(table)?:return@forEach
                val columns=columnsFor(table)
                for(i in 0 until rows.length()) {
                    val row=rows.getJSONObject(i);val v=ContentValues()
                    row.keys().forEach { key -> if(key in columns) {
                        when(val value=row.opt(key)) {
                            null,JSONObject.NULL->v.putNull(key)
                            is Boolean->v.put(key,if(value)1 else 0)
                            is Int->v.put(key,value)
                            is Long->v.put(key,value)
                            is Double->v.put(key,value)
                            else->v.put(key,value.toString())
                        }
                    }}
                    if(v.size()>0)db.insertOrThrow(table,null,v)
                }
            }
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }

    private fun columnsFor(table:String):Set<String> = when(table) {
        "universes"->setOf("id","title","author","description","settings_json","created_at","updated_at")
        "books"->setOf("id","universe_id","title","custom_label","position","created_at","updated_at")
        "chapters"->setOf("id","book_id","title","position","status_key","custom_status_text","manual_status_override","created_at","updated_at")
        "scenes"->setOf("id","chapter_id","title","position","summary","goal","conflict","pov_character_id","sensory_details","hook","custom_fields_json","created_at","updated_at")
        "documents"->setOf("id","scene_id","format_version","content_json","plain_text","word_count","updated_at")
        else->{
            val cols=readableDatabase.rawQuery("PRAGMA table_info($table)",null).use{c->buildSet{while(c.moveToNext())add(c.getString(1))}}
            cols
        }
    }

    private fun wordCount(text:String)=text.trim().split(Regex("\\s+")).count{it.isNotBlank()}
}
