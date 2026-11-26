package com.parasaurpal.inkandquill.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RecordLink(val recordId:String,val title:String,val label:String,val position:Int,val term:String="")
data class CalendarEntry(val id:String,val year:Int,val month:Int,val day:Int,val title:String,val description:String,val repeatYearly:Boolean)

// Map pin: x/y are normalized 0..1 coordinates relative to the map image, so pins survive background image changes.
data class MapPin(val id:String,val boardId:String,val x:Float,val y:Float,val label:String,val color:String,val recordId:String="",val linkedBoardId:String="")

data class TemplateField(val id:String,val templateId:String,val name:String,val fieldType:String,val required:Boolean,val position:Int,val section:String="",val options:String="",val hint:String="",val conditionField:String="",val conditionValue:String="",val formula:String="",val repeatable:Boolean=false)

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
    val favorite:Boolean=false,
    val coverUri:String=""
)

data class IndexedSearchRow(
    val docId: String,
    val docType: String,
    val kind: String,
    val title: String,
    val aliases: String,
    val tags: String,
    val templateTitle: String,
    val sheetContent: String,
    val proseContent: String
)

class InkDatabase(context:Context):SQLiteOpenHelper(context.applicationContext,"inkandquill.db",null,21){
    private val appContext=context.applicationContext
    init {
        seed(writableDatabase, overwriteUnmodified = false)
        ensureSearchIndexPopulated(writableDatabase)
        cleanupAbandonedMediaStages()
    }

    private fun cleanupAbandonedMediaStages() {
        val dir=java.io.File(appContext.filesDir,"attachments")
        if(!dir.isDirectory)return
        dir.listFiles()?.filter{it.isFile&&it.name.startsWith(".")&&".stage." in it.name}?.forEach{staged->
            runCatching{if(staged.exists()&&!staged.delete())android.util.Log.w("InkDatabase","Could not remove abandoned media staging file: ${staged.name}")}
        }
    }
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,title TEXT NOT NULL,body TEXT NOT NULL,kind TEXT NOT NULL,parentId TEXT,scope TEXT,tags TEXT,aliases TEXT,createdAt INTEGER,updatedAt INTEGER,favorite INTEGER DEFAULT 0,coverUri TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE links(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,position INTEGER DEFAULT 0,term TEXT,endPosition INTEGER,UNIQUE(sourceId,targetId,label,position))")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER,section TEXT DEFAULT 'General',options TEXT DEFAULT '',hint TEXT DEFAULT '',removed INTEGER DEFAULT 0,conditionField TEXT DEFAULT '',conditionValue TEXT DEFAULT '',formula TEXT DEFAULT '',repeatable INTEGER DEFAULT 0)")
        db.execSQL("CREATE INDEX records_kind ON records(kind)")
        db.execSQL("CREATE INDEX records_parent ON records(parentId)")
        db.execSQL("CREATE INDEX links_target ON links(targetId)")
        db.execSQL("INSERT INTO settings VALUES('appearance','system'),('fontSize','18'),('autoLink','suggest'),('caseSensitive','false')")
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
        db.execSQL("CREATE TABLE record_sheets(recordId TEXT PRIMARY KEY,templateId TEXT NOT NULL,updatedAt INTEGER)")
        db.execSQL("CREATE TABLE record_sheet_values(recordId TEXT NOT NULL,fieldId TEXT NOT NULL,value TEXT NOT NULL,updatedAt INTEGER,PRIMARY KEY(recordId,fieldId))")
        db.execSQL("CREATE TABLE record_relationships(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,notes TEXT,createdAt INTEGER,UNIQUE(sourceId,targetId,label))")
        db.execSQL("CREATE TABLE entity_snapshots(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,label TEXT NOT NULL,payload TEXT NOT NULL,createdAt INTEGER)")
        db.execSQL("CREATE TABLE timeline_events(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,title TEXT NOT NULL,startDate TEXT NOT NULL,endDate TEXT,description TEXT,recordId TEXT)")
        db.execSQL("CREATE TABLE planning_boards(id TEXT PRIMARY KEY,worldId TEXT,name TEXT NOT NULL,boardType TEXT NOT NULL,createdAt INTEGER,backgroundUri TEXT)")
        db.execSQL("CREATE TABLE board_items(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,recordId TEXT,title TEXT NOT NULL,content TEXT,x REAL DEFAULT 0,y REAL DEFAULT 0,color TEXT,imageUri TEXT)")
        db.execSQL("CREATE TABLE board_links(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,fromItem TEXT NOT NULL,toItem TEXT NOT NULL,label TEXT,UNIQUE(boardId,fromItem,toItem))")
        db.execSQL("CREATE TABLE map_pins(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,label TEXT,color TEXT,recordId TEXT,linkedBoardId TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE record_shares(recordId TEXT NOT NULL,projectId TEXT NOT NULL,createdAt INTEGER,PRIMARY KEY(recordId,projectId))")
        db.execSQL("CREATE TABLE tags(id TEXT PRIMARY KEY,name TEXT NOT NULL,tagType TEXT NOT NULL,color TEXT,UNIQUE(name,tagType))")
        db.execSQL("CREATE TABLE record_tags(recordId TEXT NOT NULL,tagId TEXT NOT NULL,PRIMARY KEY(recordId,tagId))")
        db.execSQL("CREATE TABLE tag_aliases(id TEXT PRIMARY KEY,tagId TEXT NOT NULL,alias TEXT NOT NULL,UNIQUE(tagId,alias))")
        db.execSQL("CREATE TABLE world_calendars(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,name TEXT NOT NULL,months TEXT,weekdays TEXT,seasons TEXT,moons TEXT,notes TEXT,monthLengths TEXT,eras TEXT,leapRules TEXT)")
        db.execSQL("CREATE TABLE calendar_entries(id TEXT PRIMARY KEY,calendarId TEXT NOT NULL,year INTEGER,month INTEGER,day INTEGER,title TEXT NOT NULL,description TEXT,repeatYearly INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE link_history(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT,position INTEGER,action TEXT,createdAt INTEGER,term TEXT,endPosition INTEGER)")
        db.execSQL("CREATE INDEX attachments_record ON attachments(recordId)")
        db.execSQL("CREATE INDEX collection_records_record ON collection_records(recordId)")
        db.execSQL("CREATE INDEX record_sheet_values_record ON record_sheet_values(recordId)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts4(docId,docType,kind,title,aliases,tags,templateTitle,sheetContent,proseContent)")
        seed(db, overwriteUnmodified = true)
        rebuildSearchIndex(db)
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        if(oldVersion<19){db.execSQL("CREATE TABLE IF NOT EXISTS map_pins(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,label TEXT,color TEXT,recordId TEXT,linkedBoardId TEXT,createdAt INTEGER)")}
        if(oldVersion<18){runCatching{db.execSQL("ALTER TABLE records ADD COLUMN coverUri TEXT DEFAULT ''")}}
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
        if(oldVersion<7){db.execSQL("CREATE TABLE IF NOT EXISTS record_shares(recordId TEXT NOT NULL,projectId TEXT NOT NULL,createdAt INTEGER,PRIMARY KEY(recordId,projectId))")}
        if(oldVersion<17){runCatching{db.execSQL("ALTER TABLE links ADD COLUMN term TEXT")};runCatching{db.execSQL("ALTER TABLE links ADD COLUMN endPosition INTEGER")};db.execSQL("ALTER TABLE links RENAME TO links_old");db.execSQL("CREATE TABLE links(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,position INTEGER DEFAULT 0,term TEXT,endPosition INTEGER,UNIQUE(sourceId,targetId,label,position))");db.execSQL("INSERT OR IGNORE INTO links(id,sourceId,targetId,label,position,term,endPosition) SELECT id,sourceId,targetId,label,position,term,endPosition FROM links_old");db.execSQL("DROP TABLE links_old")}
        if(oldVersion<16){db.execSQL("CREATE TABLE IF NOT EXISTS link_history(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT,position INTEGER,action TEXT,createdAt INTEGER,term TEXT,endPosition INTEGER)");runCatching{db.execSQL("ALTER TABLE link_history ADD COLUMN term TEXT")};runCatching{db.execSQL("ALTER TABLE link_history ADD COLUMN endPosition INTEGER")}}
        if(oldVersion<15){runCatching{db.execSQL("ALTER TABLE links ADD COLUMN term TEXT")};runCatching{db.execSQL("ALTER TABLE links ADD COLUMN endPosition INTEGER")}}
        if(oldVersion<14){runCatching{db.execSQL("ALTER TABLE board_items ADD COLUMN imageUri TEXT")}}
        if(oldVersion<13){runCatching{db.execSQL("ALTER TABLE world_calendars ADD COLUMN monthLengths TEXT")};runCatching{db.execSQL("ALTER TABLE world_calendars ADD COLUMN eras TEXT")};runCatching{db.execSQL("ALTER TABLE world_calendars ADD COLUMN leapRules TEXT")}}
        if(oldVersion<12){db.execSQL("CREATE TABLE IF NOT EXISTS link_history(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT,position INTEGER,action TEXT,createdAt INTEGER,term TEXT,endPosition INTEGER)")}
        if(oldVersion<11){runCatching{db.execSQL("ALTER TABLE planning_boards ADD COLUMN backgroundUri TEXT")}}
        if(oldVersion<10){runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN conditionField TEXT DEFAULT ''")};runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN conditionValue TEXT DEFAULT ''")};runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN formula TEXT DEFAULT ''")};runCatching{db.execSQL("ALTER TABLE template_fields ADD COLUMN repeatable INTEGER DEFAULT 0")}}
        if(oldVersion<9){db.execSQL("CREATE TABLE IF NOT EXISTS world_calendars(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,name TEXT NOT NULL,months TEXT,weekdays TEXT,seasons TEXT,moons TEXT,notes TEXT)");db.execSQL("CREATE TABLE IF NOT EXISTS calendar_entries(id TEXT PRIMARY KEY,calendarId TEXT NOT NULL,year INTEGER,month INTEGER,day INTEGER,title TEXT NOT NULL,description TEXT,repeatYearly INTEGER DEFAULT 0)")}
        if(oldVersion<8){db.execSQL("CREATE TABLE IF NOT EXISTS tags(id TEXT PRIMARY KEY,name TEXT NOT NULL,tagType TEXT NOT NULL,color TEXT,UNIQUE(name,tagType))");db.execSQL("CREATE TABLE IF NOT EXISTS record_tags(recordId TEXT NOT NULL,tagId TEXT NOT NULL,PRIMARY KEY(recordId,tagId))");db.execSQL("CREATE TABLE IF NOT EXISTS tag_aliases(id TEXT PRIMARY KEY,tagId TEXT NOT NULL,alias TEXT NOT NULL,UNIQUE(tagId,alias))")}
        if(oldVersion<6){
            db.execSQL("CREATE TABLE IF NOT EXISTS record_relationships(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,notes TEXT,createdAt INTEGER,UNIQUE(sourceId,targetId,label))")
            db.execSQL("CREATE TABLE IF NOT EXISTS entity_snapshots(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,label TEXT NOT NULL,payload TEXT NOT NULL,createdAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS timeline_events(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,title TEXT NOT NULL,startDate TEXT NOT NULL,endDate TEXT,description TEXT,recordId TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS planning_boards(id TEXT PRIMARY KEY,worldId TEXT,name TEXT NOT NULL,boardType TEXT NOT NULL,createdAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS board_items(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,recordId TEXT,title TEXT NOT NULL,content TEXT,x REAL DEFAULT 0,y REAL DEFAULT 0,color TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS board_links(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,fromItem TEXT NOT NULL,toItem TEXT NOT NULL,label TEXT,UNIQUE(boardId,fromItem,toItem))")
        }
        if(oldVersion<20){
            db.execSQL("CREATE TABLE IF NOT EXISTS record_sheets(recordId TEXT PRIMARY KEY,templateId TEXT NOT NULL,updatedAt INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS record_sheet_values(recordId TEXT NOT NULL,fieldId TEXT NOT NULL,value TEXT NOT NULL,updatedAt INTEGER,PRIMARY KEY(recordId,fieldId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS record_sheet_values_record ON record_sheet_values(recordId)")
            seed(db, overwriteUnmodified = true)
        }
        if(oldVersion<21){
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS search_fts USING fts4(docId,docType,kind,title,aliases,tags,templateTitle,sheetContent,proseContent)")
            rebuildSearchIndex(db)
        }
    }
    private fun seed(db:SQLiteDatabase, overwriteUnmodified:Boolean=false){
        FantasyTemplateCatalog.allSpecs.forEach{spec->
            val templateId="template_"+spec.slug
            val exists=db.rawQuery("SELECT 1 FROM records WHERE id=?",arrayOf(templateId)).use{it.moveToFirst()}
            val editedByUser=db.rawQuery("SELECT 1 FROM template_versions WHERE templateId=? LIMIT 1",arrayOf(templateId)).use{it.moveToFirst()}
            val hasFields=db.rawQuery("SELECT 1 FROM template_fields WHERE templateId=? LIMIT 1",arrayOf(templateId)).use{it.moveToFirst()}
            val hasSections=db.rawQuery("SELECT 1 FROM template_sections WHERE templateId=? LIMIT 1",arrayOf(templateId)).use{it.moveToFirst()}
            val shouldWriteFields=!exists||!hasFields||(overwriteUnmodified&&!editedByUser)
            if(!exists){
                val v=ContentValues().apply{
                    put("id",templateId)
                    put("title",spec.title)
                    put("body",spec.description)
                    put("kind","Template")
                    put("scope",spec.defaultKind)
                    put("tags",spec.category)
                    put("createdAt",System.currentTimeMillis())
                    put("updatedAt",System.currentTimeMillis())
                }
                db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_IGNORE)
            } else if(overwriteUnmodified&&!editedByUser){
                db.update("records",ContentValues().apply{
                    put("title",spec.title)
                    put("body",spec.description)
                    put("scope",spec.defaultKind)
                    put("tags",spec.category)
                },"id=?",arrayOf(templateId))
            }
            if(shouldWriteFields){
                if(overwriteUnmodified&&!editedByUser){
                    db.delete("template_fields","templateId=?",arrayOf(templateId))
                    db.delete("template_sections","templateId=?",arrayOf(templateId))
                }
                spec.sections.forEachIndexed{sIndex,sectionName->
                    val sv=ContentValues().apply{
                        put("id","section_${spec.slug}_$sIndex")
                        put("templateId",templateId)
                        put("name",sectionName)
                        put("position",sIndex)
                    }
                    db.insertWithOnConflict("template_sections",null,sv,SQLiteDatabase.CONFLICT_REPLACE)
                }
                spec.fields.forEachIndexed{index,field->
                    val fv=ContentValues().apply{
                        put("id","field_${spec.slug}_$index")
                        put("templateId",templateId)
                        put("name",field.name)
                        put("fieldType",field.type)
                        put("options",field.options)
                        put("hint",field.hint)
                        put("required",if(field.required)1 else 0)
                        put("position",index)
                        put("section",field.section)
                        put("removed",0)
                        put("conditionField",field.conditionField)
                        put("conditionValue",field.conditionValue)
                        put("formula",field.formula)
                        put("repeatable",if(field.repeatable)1 else 0)
                    }
                    db.insertWithOnConflict("template_fields",null,fv,SQLiteDatabase.CONFLICT_REPLACE)
                }
            } else if(!hasSections){
                spec.sections.forEachIndexed{sIndex,sectionName->
                    val sv=ContentValues().apply{
                        put("id","section_${spec.slug}_$sIndex")
                        put("templateId",templateId)
                        put("name",sectionName)
                        put("position",sIndex)
                    }
                    db.insertWithOnConflict("template_sections",null,sv,SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        }
    }
    fun restoreStarterTemplates(){
        val db=writableDatabase
        val ids=mutableListOf<String>()
        db.rawQuery("SELECT id FROM records WHERE kind='Template' AND id LIKE 'template_%'",null).use{while(it.moveToNext())ids.add(it.getString(0))}
        db.beginTransaction()
        try{
            ids.forEach{id->
                listOf("template_fields","template_sections","template_versions").forEach{table->db.delete(table,"templateId=?",arrayOf(id))}
                db.delete("records","id=?",arrayOf(id))
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        seed(db, overwriteUnmodified = true)
        rebuildSearchIndex(db)
    }
    private fun android.database.Cursor.record()=InkRecord(getString(getColumnIndexOrThrow("id")),getString(getColumnIndexOrThrow("title")),getString(getColumnIndexOrThrow("body")),getString(getColumnIndexOrThrow("kind")),getString(getColumnIndexOrThrow("parentId"))?:"",getString(getColumnIndexOrThrow("scope"))?:"World",getString(getColumnIndexOrThrow("tags"))?:"",getString(getColumnIndexOrThrow("aliases"))?:"",getLong(getColumnIndexOrThrow("createdAt")),getLong(getColumnIndexOrThrow("updatedAt")),getInt(getColumnIndexOrThrow("favorite"))==1,getString(getColumnIndex("coverUri").takeIf{it>=0}?:-1)?.let{it}?:"")
    fun descendants(parentId:String):List<InkRecord>{val allRecords=all();val result=mutableListOf<InkRecord>();val visited=mutableSetOf<String>();fun visit(id:String){allRecords.filter{it.parentId==id&&visited.add(it.id)}.forEach{record->result.add(record);visit(record.id)}};visit(parentId);return result}
    fun all(query:String="",kind:String?=null):List<InkRecord>{
        val sql=StringBuilder("SELECT * FROM records WHERE kind NOT IN ('Template','Archived Template')");val args=mutableListOf<String>()
        if(!kind.isNullOrBlank()&&kind!="All"){sql.append(" AND kind=?");args.add(kind)}
        if(query.isNotBlank()){sql.append(" AND (title LIKE ? OR body LIKE ? OR tags LIKE ? OR aliases LIKE ? OR EXISTS(SELECT 1 FROM record_tags rt JOIN tags t ON t.id=rt.tagId WHERE rt.recordId=records.id AND t.name LIKE ?) OR EXISTS(SELECT 1 FROM record_sheet_values rsv WHERE rsv.recordId=records.id AND rsv.value LIKE ?))");val q="%$query%";args.addAll(listOf(q,q,q,q,q,q))}
        sql.append(" ORDER BY favorite DESC,updatedAt DESC")
        return readableDatabase.rawQuery(sql.toString(),args.toTypedArray()).use{c->buildList{while(c.moveToNext())add(c.record())}}
    }
    fun get(id:String)=readableDatabase.rawQuery("SELECT * FROM records WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.record()else null}
    fun templates()=readableDatabase.rawQuery("SELECT * FROM records WHERE kind IN ('Template','Archived Template') ORDER BY title",null).use{c->buildList{while(c.moveToNext())add(c.record())}}
    fun restoreCustomType(id:String){writableDatabase.execSQL("UPDATE records SET kind='Template',updatedAt=? WHERE id=? AND kind='Archived Template' AND parentId<>''",arrayOf(System.currentTimeMillis(),id));indexRecord(writableDatabase,id)}
    fun save(r:InkRecord){
        val db=writableDatabase
        val old=get(r.id)
        if(old!=null && old.body!=r.body){val rev=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",old.id);put("title",old.title);put("body",old.body);put("createdAt",System.currentTimeMillis())};db.insert("revisions",null,rev)}
        val v=ContentValues().apply{put("id",r.id);put("title",r.title);put("body",r.body);put("kind",r.kind);put("parentId",r.parentId);put("scope",r.scope);put("tags",r.tags);put("aliases",r.aliases);put("createdAt",r.createdAt);put("updatedAt",System.currentTimeMillis());put("favorite",if(r.favorite)1 else 0);put("coverUri",r.coverUri)}
        db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_REPLACE)
        indexRecord(db,r.id)
    }
    fun delete(id:String){
        val existing=get(id)
        val db=writableDatabase
        if(existing?.kind=="Template"&&existing.parentId.isNotBlank()){
            db.execSQL("UPDATE records SET kind='Archived Template',updatedAt=? WHERE id=?",arrayOf(System.currentTimeMillis(),id))
            indexRecord(db,id)
            return
        }
        val mediaIds=mutableSetOf<String>()
        if(existing?.coverUri?.contains(".fileprovider") == true)mediaIds.add(id)
        db.rawQuery("SELECT id FROM attachments WHERE recordId=?",arrayOf(id)).use{while(it.moveToNext())mediaIds.add(it.getString(0))}
        val removedBoardItemIds=mutableListOf<String>()
        db.rawQuery("SELECT id FROM board_items WHERE recordId=?",arrayOf(id)).use{while(it.moveToNext()){val itemId=it.getString(0);mediaIds.add(itemId);removedBoardItemIds.add(itemId)}}
        val worldBoardIds=mutableListOf<String>()
        if(existing?.kind=="World"){
            db.rawQuery("SELECT id FROM planning_boards WHERE worldId=?",arrayOf(id)).use{while(it.moveToNext()){val boardId=it.getString(0);worldBoardIds.add(boardId);mediaIds.add(boardId)}}
            db.rawQuery("SELECT bi.id FROM board_items bi JOIN planning_boards pb ON pb.id=bi.boardId WHERE pb.worldId=?",arrayOf(id)).use{while(it.moveToNext())mediaIds.add(it.getString(0))}
        }
        db.beginTransaction()
        try{
            db.execSQL("UPDATE records SET parentId='' WHERE parentId=?",arrayOf(id))
            listOf("links" to "sourceId=? OR targetId=?","link_suggestions" to "sourceId=? OR targetId=?","record_relationships" to "sourceId=? OR targetId=?","link_history" to "sourceId=? OR targetId=?").forEach{(table,where)->db.delete(table,where,arrayOf(id,id))}
            listOf("record_folders","attachments","link_exclusions","entity_snapshots","revisions","record_shares","collection_records","open_tabs","record_sheets","record_sheet_values").forEach{table->db.delete(table,"recordId=?",arrayOf(id))}
            db.delete("timeline_events","recordId=?",arrayOf(id))
            db.execSQL("UPDATE map_pins SET recordId='' WHERE recordId=?",arrayOf(id))
            removedBoardItemIds.forEach{itemId->db.delete("board_links","fromItem=? OR toItem=?",arrayOf(itemId,itemId))}
            db.delete("board_items","recordId=?",arrayOf(id))
            if(existing?.kind=="Template")listOf("template_fields","template_sections","template_versions").forEach{table->db.delete(table,"templateId=?",arrayOf(id))}
            if(existing?.kind=="World"){
                db.delete("timeline_events","worldId=?",arrayOf(id))
                worldBoardIds.forEach{boardId->
                    db.delete("board_links","boardId=?",arrayOf(boardId))
                    db.delete("board_items","boardId=?",arrayOf(boardId))
                    db.delete("map_pins","boardId=?",arrayOf(boardId))
                }
                db.delete("planning_boards","worldId=?",arrayOf(id))
                db.delete("world_calendars","worldId=?",arrayOf(id))
            }
            db.delete("records","id=?",arrayOf(id))
            db.delete("search_fts","docId=?",arrayOf(id))
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        mediaIds.forEach(::deleteImportedMediaBlob)
    }
    fun setting(key:String,fallback:String="")=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?",arrayOf(key)).use{if(it.moveToFirst())it.getString(0)else fallback}
    fun setSetting(key:String,value:String){val v=ContentValues().apply{put("key",key);put("value",value)};writableDatabase.insertWithOnConflict("settings",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
    fun addLink(source:String,target:String,label:String="mention",position:Int=0,term:String="",endPosition:Int=position){val db=writableDatabase;val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("position",position);put("term",term);put("endPosition",endPosition)};val inserted=db.insertWithOnConflict("links",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(inserted!=-1L){val h=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("position",position);put("action","added");put("createdAt",System.currentTimeMillis());put("term",term);put("endPosition",endPosition)};db.insert("link_history",null,h)}}
    fun incomingLinks(id:String):List<RecordLink> = readableDatabase.rawQuery("SELECT r.id,r.title,l.label,l.position,COALESCE(l.term,'') FROM links l JOIN records r ON r.id=l.sourceId WHERE l.targetId=? ORDER BY r.title",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(RecordLink(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4)))}}
    fun outgoingLinks(id:String):List<RecordLink> = readableDatabase.rawQuery("SELECT r.id,r.title,l.label,l.position,COALESCE(l.term,'') FROM links l JOIN records r ON r.id=l.targetId WHERE l.sourceId=? ORDER BY r.title",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(RecordLink(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4)))}}
    fun linksFrom(id:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT r.title,l.label,l.targetId FROM links l JOIN records r ON r.id=l.targetId WHERE l.sourceId=?",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun removeLink(source:String,target:String,label:String="mention",position:Int?=null){val db=writableDatabase;val link=db.rawQuery("SELECT position,COALESCE(term,''),COALESCE(endPosition,position) FROM links WHERE sourceId=? AND targetId=? AND label=?"+(if(position==null)"" else " AND position=?")+" ORDER BY position LIMIT 1",if(position==null)arrayOf(source,target,label)else arrayOf(source,target,label,position.toString())).use{if(it.moveToFirst())Triple(it.getInt(0),it.getString(1),it.getInt(2))else null};if(link!=null){val h=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("position",link.first);put("action","removed");put("createdAt",System.currentTimeMillis());put("term",link.second);put("endPosition",link.third)};db.insert("link_history",null,h);db.delete("links","sourceId=? AND targetId=? AND label=? AND position=?",arrayOf(source,target,label,link.first.toString()))}}
    fun undoLastLinkChange():Boolean{val db=writableDatabase;val last= db.rawQuery("SELECT id,sourceId,targetId,COALESCE(label,'mention'),COALESCE(position,0),action,COALESCE(term,''),COALESCE(endPosition,position) FROM link_history ORDER BY createdAt DESC LIMIT 1",null).use{if(it.moveToFirst())listOf(it.getString(0),it.getString(1),it.getString(2),it.getString(3),it.getInt(4).toString(),it.getString(5),it.getString(6),it.getInt(7).toString())else null}?:return false;db.beginTransaction();try{if(last[5]=="added")db.delete("links","sourceId=? AND targetId=? AND label=?",arrayOf(last[1],last[2],last[3]))else{val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",last[1]);put("targetId",last[2]);put("label",last[3]);put("position",last[4].toInt());put("term",last[6]);put("endPosition",last[7].toInt())};db.insertWithOnConflict("links",null,v,SQLiteDatabase.CONFLICT_IGNORE)};db.delete("link_history","id=?",arrayOf(last[0]));db.setTransactionSuccessful()}finally{db.endTransaction()};return true}
    fun backlinks(id:String)=readableDatabase.rawQuery("SELECT r.id,r.title,l.label,l.position FROM links l JOIN records r ON r.id=l.sourceId WHERE l.targetId=?",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(1),c.getString(2),c.getInt(3)))}}
    fun revisions(id:String)=readableDatabase.rawQuery("SELECT title,body,createdAt FROM revisions WHERE recordId=? ORDER BY createdAt DESC",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getLong(2)))}}
    private val backupTables=listOf("links","settings","revisions","template_fields","folders","record_folders","collections","collection_records","attachments","link_exclusions","link_suggestions","open_tabs","template_sections","template_versions","record_sheets","record_sheet_values","record_relationships","entity_snapshots","timeline_events","planning_boards","board_items","board_links","map_pins","record_shares","tags","record_tags","tag_aliases","world_calendars","calendar_entries","link_history")
    private fun exportTable(table:String):JSONArray{val arr=JSONArray();readableDatabase.rawQuery("SELECT * FROM $table",null).use{c->while(c.moveToNext()){val o=JSONObject();for(i in 0 until c.columnCount){val name=c.getColumnName(i);when(c.getType(i)){android.database.Cursor.FIELD_TYPE_NULL->o.put(name,JSONObject.NULL);android.database.Cursor.FIELD_TYPE_INTEGER->o.put(name,c.getLong(i));android.database.Cursor.FIELD_TYPE_FLOAT->o.put(name,c.getDouble(i));android.database.Cursor.FIELD_TYPE_BLOB->o.put(name,android.util.Base64.encodeToString(c.getBlob(i),android.util.Base64.NO_WRAP));else->o.put(name,c.getString(i))}};arr.put(o)}};return arr}
    fun exportJson():String {
        val root = JSONObject().put("format","inkandquill").put("version",4)
            .put("records",exportTable("records"))
        backupTables.forEach { root.put(it,exportTable(it)) }
        val blobs = JSONArray()
        fun addBlob(id:String,kind:String,mimeType:String,displayName:String,uri:String) {
            val bytes = appContext.contentResolver.openInputStream(android.net.Uri.parse(uri))
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot read $displayName. Reconnect its original storage provider and try the backup again.")
            if (bytes.isEmpty()) throw IllegalStateException("$displayName is empty or unavailable; backup was stopped to prevent missing media.")
            blobs.put(JSONObject().put("id",id).put("kind",kind).put("mimeType",mimeType)
                .put("displayName",displayName)
                .put("data",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)))
        }
        readableDatabase.rawQuery("SELECT id,uri,mimeType,displayName FROM attachments",null).use { cursor ->
            while(cursor.moveToNext()) addBlob(cursor.getString(0),"attachment",cursor.getString(2) ?: "application/octet-stream",cursor.getString(3) ?: "Attachment",cursor.getString(1))
        }
        readableDatabase.rawQuery("SELECT id,backgroundUri FROM planning_boards WHERE backgroundUri IS NOT NULL AND backgroundUri<>''",null).use { cursor ->
            while(cursor.moveToNext()) addBlob(cursor.getString(0),"board","image/*","Board background",cursor.getString(1))
        }
        readableDatabase.rawQuery("SELECT id,imageUri FROM board_items WHERE imageUri IS NOT NULL AND imageUri<>''",null).use { cursor ->
            while(cursor.moveToNext()) addBlob(cursor.getString(0),"board_item","image/*","Board card image",cursor.getString(1))
        }
        readableDatabase.rawQuery("SELECT id,coverUri FROM records WHERE coverUri IS NOT NULL AND coverUri<>''",null).use { cursor ->
            while(cursor.moveToNext()) addBlob(cursor.getString(0),"cover","image/*","Novel cover",cursor.getString(1))
        }
        root.put("attachment_blobs",blobs)
        return root.toString(2)
    }
    private fun importTable(table:String,arr:JSONArray){
        val allowed=when(table){
            "links"->setOf("id","sourceId","targetId","label","position","term","endPosition");"settings"->setOf("key","value");"revisions"->setOf("id","recordId","title","body","createdAt");
            "template_fields"->setOf("id","templateId","name","fieldType","required","position","section","options","hint","removed","conditionField","conditionValue","formula","repeatable");"folders"->setOf("id","name","parentId","color","position");"record_folders"->setOf("recordId","folderId");
            "collections"->setOf("id","name","description","color","createdAt");"collection_records"->setOf("collectionId","recordId");"attachments"->setOf("id","recordId","uri","mimeType","displayName","size","createdAt");
            "link_exclusions"->setOf("id","recordId","term");"link_suggestions"->setOf("id","sourceId","targetId","term","position","status");"open_tabs"->setOf("position","recordId");
            "template_sections"->setOf("id","templateId","name","position");"template_versions"->setOf("id","templateId","snapshot","createdAt");"record_sheets"->setOf("recordId","templateId","updatedAt");"record_sheet_values"->setOf("recordId","fieldId","value","updatedAt");"record_relationships"->setOf("id","sourceId","targetId","label","notes","createdAt");
            "entity_snapshots"->setOf("id","recordId","label","payload","createdAt");"timeline_events"->setOf("id","worldId","title","startDate","endDate","description","recordId");
            "planning_boards"->setOf("id","worldId","name","boardType","createdAt","backgroundUri");"board_items"->setOf("id","boardId","recordId","title","content","x","y","color","imageUri");"board_links"->setOf("id","boardId","fromItem","toItem","label");"map_pins"->setOf("id","boardId","x","y","label","color","recordId","linkedBoardId","createdAt");"record_shares"->setOf("recordId","projectId","createdAt");"tags"->setOf("id","name","tagType","color");"record_tags"->setOf("recordId","tagId");"tag_aliases"->setOf("id","tagId","alias");"world_calendars"->setOf("id","worldId","name","months","weekdays","seasons","moons","notes","monthLengths","eras","leapRules");"calendar_entries"->setOf("id","calendarId","year","month","day","title","description","repeatYearly");"link_history"->setOf("id","sourceId","targetId","label","position","action","createdAt","term","endPosition");else->emptySet()}
        for(i in 0 until arr.length()){val o=arr.optJSONObject(i)?:continue;val v=ContentValues();o.keys().forEach{key->if(key in allowed){val value=o.opt(key);when(value){null,JSONObject.NULL->v.putNull(key);is Boolean->v.put(key,if(value)1 else 0);is Number->v.put(key,value.toString());else->v.put(key,value.toString())}}};if(v.size()>0)writableDatabase.insertWithOnConflict(table,null,v,SQLiteDatabase.CONFLICT_REPLACE)}
    }
    fun replaceWithJson(raw:String){
        val parsed=JSONObject(raw)
        require(parsed.optJSONArray("records")!=null||parsed.optJSONArray("items")!=null){"Backup does not contain a records array."}
        val db=writableDatabase
        val mediaBackups=linkedMapOf<java.io.File,java.io.File?>()
        try {
            db.beginTransaction()
            try {
                backupTables.forEach{db.delete(it,null,null)}
                db.delete("records",null,null)
                importJsonInternal(raw,mediaBackups)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            mediaBackups.values.filterNotNull().forEach{it.delete()}
            cleanupOrphanedImportedMedia()
            rebuildSearchIndex(db)
        } catch(error:Exception) {
            rollbackImportedMedia(mediaBackups,error)
            throw error
        }
    }

    fun importJson(raw:String) {
        JSONObject(raw)
        val db=writableDatabase
        val mediaBackups=linkedMapOf<java.io.File,java.io.File?>()
        try {
            db.beginTransaction()
            try {
                importJsonInternal(raw,mediaBackups)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            mediaBackups.values.filterNotNull().forEach{it.delete()}
            rebuildSearchIndex(db)
        } catch(error:Exception) {
            rollbackImportedMedia(mediaBackups,error)
            throw error
        }
    }

    private fun cleanupOrphanedImportedMedia() {
        val referenced=mutableSetOf<String>()
        listOf("attachments","planning_boards","board_items","records").forEach{table->
            readableDatabase.rawQuery("SELECT id FROM $table",null).use{cursor->while(cursor.moveToNext())referenced.add(cursor.getString(0))}
        }
        val dir=java.io.File(appContext.filesDir,"attachments")
        if(!dir.isDirectory)return
        dir.listFiles()?.filter{file->file.isFile&&file.name.endsWith(".bin")&&file.name.removeSuffix(".bin") !in referenced}?.forEach{file->
            runCatching{if(file.exists()&&!file.delete())android.util.Log.w("InkDatabase","Could not remove obsolete imported media: ${file.name}")}
        }
    }

    private fun rollbackImportedMedia(backups:Map<java.io.File,java.io.File?>,cause:Exception) {
        backups.forEach{(destination,previous)->
            try {
                if(destination.exists()&&!destination.delete())throw java.io.IOException("Could not remove incomplete imported media at ${destination.name}.")
                if(previous!=null&&previous.exists()&&!previous.renameTo(destination))
                    throw java.io.IOException("Could not restore the previous media file at ${destination.name}.")
            } catch(rollbackError:Exception) {
                cause.addSuppressed(rollbackError)
            }
        }
    }

    private fun importJsonInternal(raw:String,mediaBackups:MutableMap<java.io.File,java.io.File?>){
        val root=JSONObject(raw)
        val arr=root.optJSONArray("records")?:root.optJSONArray("items")?:JSONArray()
        for(i in 0 until arr.length()){
            val o=arr.getJSONObject(i)
            save(InkRecord(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled"),o.optString("body"),o.optString("kind",o.optString("type","Document")),o.optString("parentId"),o.optString("scope","World"),o.optString("tags"),o.optString("aliases"),o.optLong("createdAt",System.currentTimeMillis()),favorite=o.optBoolean("favorite"),coverUri=o.optString("coverUri")))
        }
        backupTables.forEach{table->root.optJSONArray(table)?.let{importTable(table,it)}}
        val blobs=root.optJSONArray("attachment_blobs")?:JSONArray()
        val seenMediaIds=mutableSetOf<String>()
        val dir=java.io.File(appContext.filesDir,"attachments").apply{if(!exists()&&!mkdirs())throw java.io.IOException("Could not create the attachment storage directory.")}
        for(i in 0 until blobs.length()){
            val o=blobs.optJSONObject(i)?:throw IllegalArgumentException("Backup contains an invalid media entry at index $i.")
            val id=o.optString("id")
            val safeName=id.filter{it.isLetterOrDigit()||it=='-'||it=='_'}
            require(safeName.isNotBlank()&&safeName==id){"Backup contains an invalid media identifier."}
            require(seenMediaIds.add(id)){"Backup contains duplicate media identifier '$id'."}
            val kind=o.optString("kind","attachment")
            val bytes=try{android.util.Base64.decode(o.getString("data"),android.util.Base64.DEFAULT)}catch(e:Exception){throw IllegalArgumentException("Media data for '$id' is invalid or incomplete.",e)}
            require(bytes.isNotEmpty()){"Media data for '$id' is empty."}
            val file=java.io.File(dir,"$safeName.bin")
            if(!mediaBackups.containsKey(file)){
                val previous=if(file.exists())java.io.File(dir,".$safeName.previous.${UUID.randomUUID()}")else null
                mediaBackups[file]=previous
                if(previous!=null&&!file.renameTo(previous))throw java.io.IOException("Could not safely stage existing media '$id'.")
            }
            val staged=java.io.File(dir,".$safeName.stage.${UUID.randomUUID()}")
            try {
                staged.writeBytes(bytes)
                if(!staged.renameTo(file))throw java.io.IOException("Could not install restored media '$id'.")
            } finally { if(staged.exists())staged.delete() }
            val uri=androidx.core.content.FileProvider.getUriForFile(appContext,appContext.packageName+".fileprovider",file)
            val values=ContentValues()
            val updated=when(kind){
                "board"->{values.put("backgroundUri",uri.toString());writableDatabase.update("planning_boards",values,"id=?",arrayOf(id))}
                "board_item"->{values.put("imageUri",uri.toString());writableDatabase.update("board_items",values,"id=?",arrayOf(id))}
                "attachment"->{values.put("uri",uri.toString());writableDatabase.update("attachments",values,"id=?",arrayOf(id))}
                "cover"->{values.put("coverUri",uri.toString());writableDatabase.update("records",values,"id=?",arrayOf(id))}
                else->throw IllegalArgumentException("Backup contains an unsupported media type '$kind'.")
            }
            require(updated==1){"Media '$id' has no matching database record; the backup may be inconsistent."}
        }
    }

    fun createFolder(name:String,parentId:String="",color:String=""):String { val id=UUID.randomUUID().toString(); val v=ContentValues().apply{put("id",id);put("name",name.trim());put("parentId",parentId);put("color",color);put("position",0)};writableDatabase.insert("folders",null,v);return id }
    fun folders():List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,name,COALESCE(parentId,'') FROM folders ORDER BY position,name",null).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun folderTreeIds(id:String):Set<String>{val result=mutableSetOf<String>();fun visit(parent:String){if(!result.add(parent))return;folders().filter{it.third==parent}.forEach{visit(it.first)}};visit(id);return result}
    fun moveToFolder(recordId:String,folderId:String?){if(folderId.isNullOrBlank())writableDatabase.delete("record_folders","recordId=?",arrayOf(recordId)) else {val v=ContentValues().apply{put("recordId",recordId);put("folderId",folderId)};writableDatabase.insertWithOnConflict("record_folders",null,v,SQLiteDatabase.CONFLICT_REPLACE)}}
    fun folderFor(recordId:String)=readableDatabase.rawQuery("SELECT folderId FROM record_folders WHERE recordId=?",arrayOf(recordId)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun renameFolder(id:String,name:String){val v=ContentValues().apply{put("name",name.trim())};writableDatabase.update("folders",v,"id=?",arrayOf(id))}
    fun deleteFolder(id:String){val db=writableDatabase;db.beginTransaction();try{db.delete("record_folders","folderId=?",arrayOf(id));db.execSQL("UPDATE folders SET parentId='' WHERE parentId=?",arrayOf(id));db.delete("folders","id=?",arrayOf(id));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun renameCollection(id:String,name:String){val v=ContentValues().apply{put("name",name.trim())};writableDatabase.update("collections",v,"id=?",arrayOf(id))}
    fun deleteCollection(id:String){val db=writableDatabase;db.beginTransaction();try{db.delete("collection_records","collectionId=?",arrayOf(id));db.delete("collections","id=?",arrayOf(id));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun createCollection(name:String,description:String=""):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("name",name.trim());put("description",description);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("collections",null,v);return id}
    fun addToCollection(collectionId:String,recordId:String){val v=ContentValues().apply{put("collectionId",collectionId);put("recordId",recordId)};writableDatabase.insertWithOnConflict("collection_records",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun collectionRecords(collectionId:String):List<InkRecord> = readableDatabase.rawQuery("SELECT r.* FROM records r JOIN collection_records cr ON cr.recordId=r.id WHERE cr.collectionId=? ORDER BY r.updatedAt DESC",arrayOf(collectionId)).use{c->buildList{while(c.moveToNext())add(c.record())}}
    fun removeFromCollection(collectionId:String,recordId:String){writableDatabase.delete("collection_records","collectionId=? AND recordId=?",arrayOf(collectionId,recordId))}
    fun recordCollections(recordId:String):List<Pair<String,String>> = readableDatabase.rawQuery("SELECT c.id,c.name FROM collections c JOIN collection_records cr ON cr.collectionId=c.id WHERE cr.recordId=?",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun collections():List<Pair<String,String>> = readableDatabase.rawQuery("SELECT id,name FROM collections ORDER BY name",null).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun addAttachment(recordId:String,uri:String,mimeType:String="",displayName:String="",size:Long=0L){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",recordId);put("uri",uri);put("mimeType",mimeType);put("displayName",displayName);put("size",size);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("attachments",null,v)}
    fun attachments(recordId:String):List<Map<String,String>> = readableDatabase.rawQuery("SELECT id,uri,mimeType,displayName FROM attachments WHERE recordId=?",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(mapOf("id" to c.getString(0),"uri" to c.getString(1),"mimeType" to (c.getString(2)?: ""),"displayName" to (c.getString(3)?: "")))}}
    fun removeAttachment(id:String){
        writableDatabase.delete("attachments","id=?",arrayOf(id))
        deleteImportedMediaBlob(id)
    }
    private fun deleteImportedMediaBlob(id:String) {
        val safeName=id.filter{it.isLetterOrDigit()||it=='-'||it=='_'}
        if(safeName.isBlank()||safeName!=id)return
        val dir=java.io.File(appContext.filesDir,"attachments")
        val file=java.io.File(dir,"$safeName.bin")
        if(file.isFile&&!file.delete())android.util.Log.w("InkDatabase","Could not remove imported media: ${file.name}")
    }
    fun excludeLinkTerm(recordId:String,term:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",recordId);put("term",term.trim())};writableDatabase.insertWithOnConflict("link_exclusions",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun isLinkExcluded(recordId:String,term:String)=readableDatabase.rawQuery("SELECT 1 FROM link_exclusions WHERE recordId=? AND lower(term)=lower(?)",arrayOf(recordId,term)).use{it.moveToFirst()}
    fun saveOpenTabs(ids:List<String>){writableDatabase.beginTransaction();try{writableDatabase.delete("open_tabs",null,null);ids.distinct().forEachIndexed{index,id->val v=ContentValues().apply{put("position",index);put("recordId",id)};writableDatabase.insert("open_tabs",null,v)};writableDatabase.setTransactionSuccessful()}finally{writableDatabase.endTransaction()}}
    fun openTabs():List<String> = readableDatabase.rawQuery("SELECT recordId FROM open_tabs ORDER BY position",null).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun replaceInRecord(id:String,find:String,replacement:String,matchCase:Boolean=false):Int{if(find.isEmpty())return 0;val r=get(id)?:return 0;val opts=if(matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE);val regex=Regex(Regex.escape(find),opts);val count=regex.findAll(r.body).count();if(count>0)save(r.copy(body=regex.replace(r.body){replacement}));return count}

    fun addLinkSuggestion(source:String,target:String,term:String,position:Int){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("term",term);put("position",position);put("status","pending")};writableDatabase.insertWithOnConflict("link_suggestions",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun pendingLinkSuggestions(source:String):List<Triple<String,String,Int>> = readableDatabase.rawQuery("SELECT targetId,term,position FROM link_suggestions WHERE sourceId=? AND status='pending' ORDER BY position",arrayOf(source)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getInt(2)))}}
    fun resolveLinkSuggestion(source:String,target:String,position:Int,approve:Boolean){val term=pendingLinkSuggestions(source).firstOrNull{it.first==target&&it.third==position}?.second.orEmpty();writableDatabase.execSQL("UPDATE link_suggestions SET status=? WHERE sourceId=? AND targetId=? AND position=?",arrayOf(if(approve)"approved" else "dismissed",source,target,position));if(approve)addLink(source,target,"mention",position,term,position+term.length)}
    fun exportTemplateJson(id:String):String{
        val t=get(id)?:throw IllegalArgumentException("Template not found.")
        val fields=JSONArray()
        templateFields(id,true).forEach{f->fields.put(JSONObject().put("name",f.name).put("type",f.fieldType).put("required",f.required).put("position",f.position).put("section",f.section).put("options",f.options).put("hint",f.hint).put("conditionField",f.conditionField).put("conditionValue",f.conditionValue).put("formula",f.formula).put("repeatable",f.repeatable))}
        val sections=JSONArray()
        templateSections(id).forEach{sections.put(it)}
        return JSONObject().put("format","inkandquill-template").put("version",2).put("title",t.title).put("body",t.body).put("category",t.tags).put("defaultKind",t.scope).put("sections",sections).put("fields",fields).toString(2)
    }
    fun importTemplateJson(raw:String):InkRecord{
        val o=JSONObject(raw)
        require(o.optString("format")=="inkandquill-template"){"Not an Ink & Quill template file."}
        val t=InkRecord(title=o.optString("title","Imported template"),body=o.optString("body"),kind="Template",scope=o.optString("defaultKind","Document"),tags=o.optString("category","Other"))
        save(t)
        val sections=o.optJSONArray("sections")?:JSONArray()
        for(i in 0 until sections.length()){
            val sec=sections.optString(i)
            if(sec.isNotBlank())addTemplateSection(t.id,sec)
        }
        val fields=o.optJSONArray("fields")?:JSONArray()
        for(i in 0 until fields.length()){
            val f=fields.optJSONObject(i)?:continue
            val sec=f.optString("section","General")
            if(sec.isNotBlank()&&sec !in templateSections(t.id))addTemplateSection(t.id,sec)
            addTemplateField(t.id,f.optString("name","Field"),f.optString("type","Long text"),f.optBoolean("required"),sec,f.optString("options"),f.optString("hint"),f.optString("conditionField"),f.optString("conditionValue"),f.optString("formula"),f.optBoolean("repeatable"))
        }
        return t
    }
    fun duplicateTemplate(id:String):InkRecord?{
        val source=get(id)?:return null
        val copy=source.copy(id=UUID.randomUUID().toString(),title="Copy of "+source.title,createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis())
        save(copy)
        templateSections(id).forEach{sec->addTemplateSection(copy.id,sec)}
        templateFields(id).forEach{f->addTemplateField(copy.id,f.name,f.fieldType,f.required,f.section,f.options,f.hint,f.conditionField,f.conditionValue,f.formula,f.repeatable)}
        return copy
    }
    fun templateFields(templateId:String,includeRemoved:Boolean=false):List<TemplateField>{
        val sql="SELECT id,templateId,name,fieldType,required,position,COALESCE(section,'General'),COALESCE(options,''),COALESCE(hint,''),COALESCE(conditionField,''),COALESCE(conditionValue,''),COALESCE(formula,''),COALESCE(repeatable,0) FROM template_fields WHERE templateId=? "+(if(includeRemoved) "" else "AND COALESCE(removed,0)=0 ")+" ORDER BY position"
        return readableDatabase.rawQuery(sql,arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(TemplateField(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1,c.getInt(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getInt(12)==1))}}
    }
    fun addTemplateField(templateId:String,name:String,type:String,required:Boolean,section:String="General",options:String="",hint:String="",conditionField:String="",conditionValue:String="",formula:String="",repeatable:Boolean=false){
        val cleanSection=section.trim().ifBlank{"General"}
        if(cleanSection !in templateSections(templateId))addTemplateSection(templateId,cleanSection)
        val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",name.trim());put("fieldType",type);put("required",if(required)1 else 0);put("position",templateFields(templateId,true).size);put("section",cleanSection);put("options",options);put("hint",hint);put("removed",0);put("conditionField",conditionField);put("conditionValue",conditionValue);put("formula",formula);put("repeatable",if(repeatable)1 else 0)}
        writableDatabase.insert("template_fields",null,v)
        indexRecord(writableDatabase,templateId)
    }
    fun moveTemplateField(templateId:String,id:String,delta:Int){val fields=templateFields(templateId,true);val index=fields.indexOfFirst{it.id==id};val target=index+delta;if(index<0||target !in fields.indices)return;val db=writableDatabase;db.beginTransaction();try{val a=ContentValues().apply{put("position",fields[target].position)};val b=ContentValues().apply{put("position",fields[index].position)};db.update("template_fields",a,"id=?",arrayOf(id));db.update("template_fields",b,"id=?",arrayOf(fields[target].id));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun updateTemplateField(id:String,name:String,type:String,required:Boolean,section:String,options:String,hint:String,conditionField:String="",conditionValue:String="",formula:String="",repeatable:Boolean=false){val v=ContentValues().apply{put("name",name);put("fieldType",type);put("required",if(required)1 else 0);put("section",section.trim().ifBlank{"General"});put("options",options);put("hint",hint);put("conditionField",conditionField);put("conditionValue",conditionValue);put("formula",formula);put("repeatable",if(repeatable)1 else 0)};writableDatabase.update("template_fields",v,"id=?",arrayOf(id));writableDatabase.rawQuery("SELECT templateId FROM template_fields WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())indexRecord(writableDatabase,it.getString(0))}}
    fun removeTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=1 WHERE id=?",arrayOf(id));writableDatabase.rawQuery("SELECT templateId FROM template_fields WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())indexRecord(writableDatabase,it.getString(0))}}
    fun restoreTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=0 WHERE id=?",arrayOf(id));writableDatabase.rawQuery("SELECT templateId FROM template_fields WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())indexRecord(writableDatabase,it.getString(0))}}
    fun templateSections(templateId:String):List<String> = readableDatabase.rawQuery("SELECT name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun addTemplateSection(templateId:String,name:String){
        val clean=name.trim()
        if(clean.isBlank()||clean in templateSections(templateId))return
        val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",clean);put("position",templateSections(templateId).size)}
        writableDatabase.insert("template_sections",null,v)
        indexRecord(writableDatabase,templateId)
    }
    fun renameTemplateSection(templateId:String,oldName:String,newName:String){
        val clean=newName.trim()
        if(clean.isBlank()||clean==oldName)return
        val db=writableDatabase
        db.beginTransaction()
        try{
            db.update("template_sections",ContentValues().apply{put("name",clean)},"templateId=? AND name=?",arrayOf(templateId,oldName))
            db.update("template_fields",ContentValues().apply{put("section",clean)},"templateId=? AND section=?",arrayOf(templateId,oldName))
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        indexRecord(db,templateId)
    }
    fun deleteTemplateSection(templateId:String,name:String){
        val remaining=templateSections(templateId).filter{it!=name}
        val fallback=remaining.firstOrNull()?:"General"
        val db=writableDatabase
        db.beginTransaction()
        try{
            if(remaining.isEmpty())addTemplateSection(templateId,fallback)
            db.update("template_fields",ContentValues().apply{put("section",fallback)},"templateId=? AND section=?",arrayOf(templateId,name))
            db.delete("template_sections","templateId=? AND name=?",arrayOf(templateId,name))
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        indexRecord(db,templateId)
    }
    fun moveTemplateSection(templateId:String,name:String,delta:Int){val db=writableDatabase;val rows=mutableListOf<Pair<String,String>>();db.rawQuery("SELECT id,name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(templateId)).use{while(it.moveToNext())rows.add(it.getString(0) to it.getString(1))};val index=rows.indexOfFirst{it.second==name};val target=index+delta;if(index<0||target !in rows.indices)return;db.beginTransaction();try{val a=ContentValues().apply{put("position",target)};val b=ContentValues().apply{put("position",index)};db.update("template_sections",a,"id=?",arrayOf(rows[index].first));db.update("template_sections",b,"id=?",arrayOf(rows[target].first));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun templateVersion(templateId:String){val snapshot=JSONArray();templateFields(templateId,true).forEach{f->snapshot.put(JSONObject().put("id",f.id).put("name",f.name).put("type",f.fieldType).put("required",f.required).put("section",f.section).put("options",f.options).put("hint",f.hint).put("conditionField",f.conditionField).put("conditionValue",f.conditionValue).put("formula",f.formula).put("repeatable",f.repeatable).put("position",f.position))};val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("snapshot",snapshot.toString());put("createdAt",System.currentTimeMillis())};writableDatabase.insert("template_versions",null,v)}
    fun templateVersionCount(templateId:String)=readableDatabase.rawQuery("SELECT COUNT(*) FROM template_versions WHERE templateId=?",arrayOf(templateId)).use{it.moveToFirst();it.getInt(0)}
    fun templateVersions(templateId:String):List<Pair<String,Long>> = readableDatabase.rawQuery("SELECT id,createdAt FROM template_versions WHERE templateId=? ORDER BY createdAt DESC",arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getLong(1))}}
    fun restoreTemplateVersion(templateId:String,versionId:String){
        val raw=readableDatabase.rawQuery("SELECT snapshot FROM template_versions WHERE id=? AND templateId=?",arrayOf(versionId,templateId)).use{if(it.moveToFirst())it.getString(0)else null}?:return
        val arr=JSONArray(raw)
        val db=writableDatabase
        db.beginTransaction()
        try{
            db.delete("template_fields","templateId=?",arrayOf(templateId))
            for(i in 0 until arr.length()){
                val f=arr.optJSONObject(i)?:continue
                val sec=f.optString("section","General")
                if(sec.isNotBlank()&&sec !in templateSections(templateId))addTemplateSection(templateId,sec)
                val fv=ContentValues().apply{
                    put("id",f.optString("id",UUID.randomUUID().toString()))
                    put("templateId",templateId)
                    put("name",f.optString("name","Field"))
                    put("fieldType",f.optString("type","Long text"))
                    put("required",if(f.optBoolean("required"))1 else 0)
                    put("position",f.optInt("position",i))
                    put("section",sec)
                    put("options",f.optString("options"))
                    put("hint",f.optString("hint"))
                    put("removed",0)
                    put("conditionField",f.optString("conditionField"))
                    put("conditionValue",f.optString("conditionValue"))
                    put("formula",f.optString("formula"))
                    put("repeatable",if(f.optBoolean("repeatable"))1 else 0)
                }
                db.insertWithOnConflict("template_fields",null,fv,SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        indexRecord(db,templateId)
    }

    // ── Record Sheets (Live Structured Entries) ────────────────────────────
    fun attachSheet(recordId:String,templateId:String){
        val v=ContentValues().apply{put("recordId",recordId);put("templateId",templateId);put("updatedAt",System.currentTimeMillis())}
        writableDatabase.insertWithOnConflict("record_sheets",null,v,SQLiteDatabase.CONFLICT_REPLACE)
        indexRecord(writableDatabase,recordId)
    }
    fun detachSheet(recordId:String){
        writableDatabase.delete("record_sheets","recordId=?",arrayOf(recordId))
        indexRecord(writableDatabase,recordId)
    }
    fun sheetTemplateId(recordId:String):String = readableDatabase.rawQuery("SELECT templateId FROM record_sheets WHERE recordId=?",arrayOf(recordId)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun sheetValues(recordId:String):Map<String,String> = readableDatabase.rawQuery("SELECT fieldId,value FROM record_sheet_values WHERE recordId=?",arrayOf(recordId)).use{c->buildMap{while(c.moveToNext())put(c.getString(0),c.getString(1))}}
    fun saveSheetValue(recordId:String,fieldId:String,value:String){
        val v=ContentValues().apply{put("recordId",recordId);put("fieldId",fieldId);put("value",value);put("updatedAt",System.currentTimeMillis())}
        writableDatabase.insertWithOnConflict("record_sheet_values",null,v,SQLiteDatabase.CONFLICT_REPLACE)
        indexRecord(writableDatabase,recordId)
    }
    fun saveSheetValues(recordId:String,values:Map<String,String>){
        val db=writableDatabase
        val now=System.currentTimeMillis()
        db.beginTransaction()
        try{
            values.forEach{(fieldId,value)->
                if(value.isEmpty()){
                    db.delete("record_sheet_values","recordId=? AND fieldId=?",arrayOf(recordId,fieldId))
                }else{
                    val v=ContentValues().apply{put("recordId",recordId);put("fieldId",fieldId);put("value",value);put("updatedAt",now)}
                    db.insertWithOnConflict("record_sheet_values",null,v,SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        indexRecord(db,recordId)
    }
    fun createRecordFromTemplate(templateId:String,parentId:String="",scope:String="World",initialTitle:String=""):InkRecord{
        val template=get(templateId)?:throw IllegalArgumentException("Template not found.")
        val targetKind=when{
            template.id=="template_world_overview"->"World"
            template.scope.isNotBlank()&&template.scope!="World"->template.scope
            template.parentId.isNotBlank()->template.title
            else->"Document"
        }
        val resolvedTitle=initialTitle.ifBlank{"Untitled $targetKind"}
        val resolvedParent=if(targetKind=="World") "" else parentId.ifBlank{template.parentId}
        val record=InkRecord(id=UUID.randomUUID().toString(),title=resolvedTitle,body="",kind=targetKind,parentId=resolvedParent,scope=scope)
        save(record)
        attachSheet(record.id,templateId)
        return record
    }

    fun createCustomTemplate(
        title: String,
        description: String,
        category: String,
        defaultKind: String,
        sections: List<String>,
        fields: List<FieldSpec>,
        parentId: String = "",
        baseTemplateId: String? = null
    ): InkRecord {
        val cleanTitle = title.trim().ifBlank { "Custom Template" }
        val cleanCategory = category.trim().ifBlank { "Other" }
        val cleanKind = defaultKind.trim().ifBlank { "Document" }
        val template = InkRecord(
            id = "custom_tpl_${UUID.randomUUID()}",
            title = cleanTitle,
            body = description.trim(),
            kind = "Template",
            parentId = parentId,
            scope = cleanKind,
            tags = cleanCategory
        )
        save(template)
        if (!baseTemplateId.isNullOrBlank()) {
            templateSections(baseTemplateId).forEach { addTemplateSection(template.id, it) }
            templateFields(baseTemplateId).forEach { f ->
                addTemplateField(
                    templateId = template.id,
                    name = f.name,
                    type = f.fieldType,
                    required = f.required,
                    section = f.section,
                    options = f.options,
                    hint = f.hint,
                    conditionField = f.conditionField,
                    conditionValue = f.conditionValue,
                    formula = f.formula,
                    repeatable = f.repeatable
                )
            }
        }
        val effectiveSections = sections.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            .ifEmpty { if (baseTemplateId.isNullOrBlank()) listOf("Overview") else emptyList() }
        effectiveSections.forEach { addTemplateSection(template.id, it) }
        fields.forEach { f ->
            if (f.name.isNotBlank()) {
                addTemplateField(
                    templateId = template.id,
                    name = f.name,
                    type = f.type,
                    required = f.required,
                    section = f.section.ifBlank { effectiveSections.firstOrNull() ?: "Overview" },
                    options = f.options,
                    hint = f.hint,
                    conditionField = f.conditionField,
                    conditionValue = f.conditionValue,
                    formula = f.formula,
                    repeatable = f.repeatable
                )
            }
        }
        return get(template.id) ?: template
    }

    fun saveRecordAsTemplate(
        record: InkRecord,
        templateTitle: String,
        templateCategory: String,
        includeProseFields: Boolean = true
    ): InkRecord {
        val attachedTplId = sheetTemplateId(record.id)
        if (attachedTplId.isNotBlank()) {
            return createCustomTemplate(
                title = templateTitle,
                description = "Created from ${record.title.ifBlank { record.kind }}",
                category = templateCategory,
                defaultKind = record.kind.ifBlank { "Document" },
                sections = emptyList(),
                fields = emptyList(),
                baseTemplateId = attachedTplId
            )
        }
        val headingRegex = Regex("<h[1-4][^>]*>(.*?)</h[1-4]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val extractedSections = headingRegex.findAll(record.body)
            .map { DocumentExport.plain(it.groupValues[1]).trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
            .ifEmpty { listOf("Overview", "Details") }
        val starterFields = if (includeProseFields) {
            extractedSections.map { sec ->
                FieldSpec(
                    name = "$sec Notes",
                    type = "Rich text",
                    section = sec,
                    hint = "Structured notes for $sec"
                )
            }
        } else {
            listOf(FieldSpec(name = "Summary", type = "Long text", section = extractedSections.first()))
        }
        return createCustomTemplate(
            title = templateTitle,
            description = "Custom template created from ${record.title.ifBlank { record.kind }}",
            category = templateCategory,
            defaultKind = record.kind.ifBlank { "Document" },
            sections = extractedSections,
            fields = starterFields
        )
    }

    fun deleteCustomTemplate(id: String): Boolean {
        if (get(id) == null || id.startsWith("template_")) return false
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf("template_fields", "template_sections", "template_versions").forEach { table ->
                db.delete(table, "templateId=?", arrayOf(id))
            }
            db.delete("record_sheets", "templateId=?", arrayOf(id))
            db.delete("records", "id=?", arrayOf(id))
            db.delete("search_fts", "docId=?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return true
    }

    fun bookChapters(bookId: String): List<InkRecord> {
        return readableDatabase.rawQuery(
            "SELECT * FROM records WHERE parentId=? AND kind IN ('Chapter','Scene') ORDER BY createdAt ASC, title ASC",
            arrayOf(bookId)
        ).use { c -> buildList { while (c.moveToNext()) add(c.record()) } }
    }

    fun chapterScenes(chapterId: String): List<InkRecord> {
        return readableDatabase.rawQuery(
            "SELECT * FROM records WHERE parentId=? AND kind='Scene' ORDER BY createdAt ASC, title ASC",
            arrayOf(chapterId)
        ).use { c -> buildList { while (c.moveToNext()) add(c.record()) } }
    }

    fun siblingChapters(chapter: InkRecord): List<InkRecord> {
        if (chapter.parentId.isBlank()) return listOf(chapter)
        val siblings = readableDatabase.rawQuery(
            "SELECT * FROM records WHERE parentId=? AND kind=? ORDER BY createdAt ASC, title ASC",
            arrayOf(chapter.parentId, chapter.kind)
        ).use { c -> buildList { while (c.moveToNext()) add(c.record()) } }
        return if (siblings.any { it.id == chapter.id }) siblings else listOf(chapter)
    }

    // ── Global FTS4 Search Index ───────────────────────────────────────────
    private fun ensureSearchIndexPopulated(db:SQLiteDatabase){
        val ftsCount=db.rawQuery("SELECT COUNT(*) FROM search_fts",null).use{if(it.moveToFirst())it.getInt(0)else 0}
        val recCount=db.rawQuery("SELECT COUNT(*) FROM records",null).use{if(it.moveToFirst())it.getInt(0)else 0}
        if(recCount>0&&ftsCount==0) rebuildSearchIndex(db)
    }
    fun rebuildSearchIndex(db:SQLiteDatabase=writableDatabase){
        val ids=mutableListOf<String>()
        db.rawQuery("SELECT id FROM records",null).use{c->while(c.moveToNext())ids.add(c.getString(0))}
        db.beginTransaction()
        try{
            db.delete("search_fts",null,null)
            ids.forEach{id->indexRecord(db,id)}
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    private fun indexRecord(db:SQLiteDatabase,recordId:String){
        db.delete("search_fts","docId=?",arrayOf(recordId))
        val rec=db.rawQuery("SELECT * FROM records WHERE id=?",arrayOf(recordId)).use{if(it.moveToFirst())it.record()else null} ?: return
        val isTemplate=rec.kind=="Template"||rec.kind=="Archived Template"
        val docType=if(isTemplate)"template" else "entry"
        val sharedTagNames=db.rawQuery("SELECT t.name FROM record_tags rt JOIN tags t ON t.id=rt.tagId WHERE rt.recordId=?",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
        val tagsText=(listOf(rec.tags).filter{it.isNotBlank()}+sharedTagNames).distinct().joinToString(" ")
        var templateTitle=""
        var sheetContent=""
        if(isTemplate){
            templateTitle=rec.title
            val sections=db.rawQuery("SELECT name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
            val fields=db.rawQuery("SELECT name,COALESCE(options,''),COALESCE(hint,'') FROM template_fields WHERE templateId=? AND COALESCE(removed,0)=0 ORDER BY position",arrayOf(recordId)).use{c->buildList{while(c.moveToNext())add(listOf(c.getString(0),c.getString(1),c.getString(2)).filter{it.isNotBlank()}.joinToString(" "))}}
            sheetContent=(sections+fields).joinToString("\n")
        }else{
            val attachedTplId=db.rawQuery("SELECT templateId FROM record_sheets WHERE recordId=?",arrayOf(recordId)).use{if(it.moveToFirst())it.getString(0)else ""}
            if(attachedTplId.isNotBlank()){
                templateTitle=db.rawQuery("SELECT title FROM records WHERE id=?",arrayOf(attachedTplId)).use{if(it.moveToFirst())it.getString(0)else ""}
                val valueMap=db.rawQuery("SELECT fieldId,value FROM record_sheet_values WHERE recordId=?",arrayOf(recordId)).use{c->buildMap{while(c.moveToNext())put(c.getString(0),c.getString(1))}}
                val fields=db.rawQuery("SELECT id,templateId,name,fieldType,required,position,COALESCE(section,'General'),COALESCE(options,''),COALESCE(hint,''),COALESCE(conditionField,''),COALESCE(conditionValue,''),COALESCE(formula,''),COALESCE(repeatable,0) FROM template_fields WHERE templateId=? AND COALESCE(removed,0)=0 ORDER BY position",arrayOf(attachedTplId)).use{c->buildList{while(c.moveToNext())add(TemplateField(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1,c.getInt(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getInt(12)==1))}}
                sheetContent=fields.mapNotNull{f->
                    val raw=valueMap[f.id].orEmpty()
                    if(raw.isBlank()&&f.fieldType!="Calculated")null
                    else if(f.fieldType=="Image"||f.fieldType=="Gallery")null
                    else{
                        val plain=SheetEngine.formatFieldPlainValue(f,fields,valueMap){targetId->
                            db.rawQuery("SELECT * FROM records WHERE id=?",arrayOf(targetId)).use{if(it.moveToFirst())it.record()else null}
                        }
                        if(plain.isBlank())null else "${f.name}: $plain"
                    }
                }.joinToString("\n")
            }
        }
        val plainProse=DocumentExport.plain(rec.body).replace(Regex("data:[^\\s\"']+"),"").trim()
        val kindCol=if(isTemplate)"${rec.scope} Template".trim() else rec.kind
        val cv=ContentValues().apply{
            put("docId",rec.id)
            put("docType",docType)
            put("kind",kindCol)
            put("title",rec.title)
            put("aliases",rec.aliases)
            put("tags",tagsText)
            put("templateTitle",templateTitle)
            put("sheetContent",sheetContent)
            put("proseContent",plainProse)
        }
        db.insert("search_fts",null,cv)
    }
    fun indexedSearchRows(ftsQuery:String,limit:Int=200):List<IndexedSearchRow>{
        if(ftsQuery.isBlank())return emptyList()
        return runCatching{
            readableDatabase.rawQuery(
                "SELECT docId,docType,kind,title,aliases,tags,templateTitle,sheetContent,proseContent FROM search_fts WHERE search_fts MATCH ? LIMIT ?",
                arrayOf(ftsQuery,limit.toString())
            ).use{c->
                buildList{
                    while(c.moveToNext()){
                        add(IndexedSearchRow(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8)))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
    fun allIndexedRows(limit:Int=200):List<IndexedSearchRow>{
        return readableDatabase.rawQuery(
            "SELECT docId,docType,kind,title,aliases,tags,templateTitle,sheetContent,proseContent FROM search_fts LIMIT ?",
            arrayOf(limit.toString())
        ).use{c->
            buildList{
                while(c.moveToNext()){
                    add(IndexedSearchRow(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8)))
                }
            }
        }
    }
    fun createWorldCalendar(worldId:String,name:String,months:String,weekdays:String,seasons:String,moons:String,notes:String="",monthLengths:String="",eras:String="",leapRules:String=""):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("worldId",worldId);put("name",name);put("months",months);put("weekdays",weekdays);put("seasons",seasons);put("moons",moons);put("notes",notes);put("monthLengths",monthLengths);put("eras",eras);put("leapRules",leapRules)};writableDatabase.insert("world_calendars",null,v);return id}
    fun worldCalendars(worldId:String):List<Map<String,String>> = readableDatabase.rawQuery("SELECT id,name,COALESCE(months,''),COALESCE(weekdays,''),COALESCE(seasons,''),COALESCE(moons,''),COALESCE(notes,''),COALESCE(monthLengths,''),COALESCE(eras,''),COALESCE(leapRules,'') FROM world_calendars WHERE worldId=? ORDER BY name",arrayOf(worldId)).use{c->buildList{while(c.moveToNext())add(mapOf("id" to c.getString(0),"name" to c.getString(1),"months" to c.getString(2),"weekdays" to c.getString(3),"seasons" to c.getString(4),"moons" to c.getString(5),"notes" to c.getString(6),"monthLengths" to c.getString(7),"eras" to c.getString(8),"leapRules" to c.getString(9)))}}
    fun addCalendarEntry(calendarId:String,year:Int,month:Int,day:Int,title:String,description:String="",repeatYearly:Boolean=false){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("calendarId",calendarId);put("year",year);put("month",month);put("day",day);put("title",title);put("description",description);put("repeatYearly",if(repeatYearly)1 else 0)};writableDatabase.insert("calendar_entries",null,v)}
    fun calendarEntries(calendarId:String):List<CalendarEntry> = readableDatabase.rawQuery("SELECT id,year,month,day,title,COALESCE(description,''),repeatYearly FROM calendar_entries WHERE calendarId=? ORDER BY year,month,day",arrayOf(calendarId)).use{c->buildList{while(c.moveToNext())add(CalendarEntry(c.getString(0),c.getInt(1),c.getInt(2),c.getInt(3),c.getString(4),c.getString(5),c.getInt(6)==1))}}
    fun allTags():List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,name,tagType FROM tags ORDER BY tagType,name",null).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun createTag(name:String,type:String="General",color:String=""):String{val existing=readableDatabase.rawQuery("SELECT id FROM tags WHERE lower(name)=lower(?) AND tagType=?",arrayOf(name.trim(),type)).use{if(it.moveToFirst())it.getString(0)else null};if(existing!=null)return existing;val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("name",name.trim());put("tagType",type);put("color",color)};writableDatabase.insert("tags",null,v);return id}
    fun assignTag(recordId:String,tagId:String){val v=ContentValues().apply{put("recordId",recordId);put("tagId",tagId)};writableDatabase.insertWithOnConflict("record_tags",null,v,SQLiteDatabase.CONFLICT_IGNORE);indexRecord(writableDatabase,recordId)}
    fun removeTag(recordId:String,tagId:String){writableDatabase.delete("record_tags","recordId=? AND tagId=?",arrayOf(recordId,tagId));indexRecord(writableDatabase,recordId)}
    fun tagsForRecord(recordId:String):Set<String> = readableDatabase.rawQuery("SELECT tagId FROM record_tags WHERE recordId=?",arrayOf(recordId)).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
    fun renameTag(id:String,name:String){val v=ContentValues().apply{put("name",name.trim())};writableDatabase.update("tags",v,"id=?",arrayOf(id));rebuildSearchIndex()}
    fun addTagAlias(tagId:String,alias:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("tagId",tagId);put("alias",alias.trim())};writableDatabase.insertWithOnConflict("tag_aliases",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun shareWithProject(recordId:String,projectId:String){val v=ContentValues().apply{put("recordId",recordId);put("projectId",projectId);put("createdAt",System.currentTimeMillis())};writableDatabase.insertWithOnConflict("record_shares",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun unshareFromProject(recordId:String,projectId:String){writableDatabase.delete("record_shares","recordId=? AND projectId=?",arrayOf(recordId,projectId))}
    fun sharedProjects(recordId:String):Set<String> = readableDatabase.rawQuery("SELECT projectId FROM record_shares WHERE recordId=?",arrayOf(recordId)).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
    fun addRelationship(source:String,target:String,label:String,notes:String=""){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("notes",notes);put("createdAt",System.currentTimeMillis())};writableDatabase.insertWithOnConflict("record_relationships",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
    fun relationships(id:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT r.title,rr.label,rr.targetId FROM record_relationships rr JOIN records r ON r.id=rr.targetId WHERE rr.sourceId=? UNION ALL SELECT r.title,rr.label,rr.sourceId FROM record_relationships rr JOIN records r ON r.id=rr.sourceId WHERE rr.targetId=?",arrayOf(id,id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun saveEntitySnapshot(record:InkRecord,label:String){
        val sheetObj=JSONObject()
        sheetValues(record.id).forEach{(k,v)->sheetObj.put(k,v)}
        val payload=JSONObject().put("title",record.title).put("body",record.body).put("kind",record.kind).put("tags",record.tags).put("aliases",record.aliases).put("templateId",sheetTemplateId(record.id)).put("sheetValues",sheetObj).toString()
        val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",record.id);put("label",label);put("payload",payload);put("createdAt",System.currentTimeMillis())}
        writableDatabase.insert("entity_snapshots",null,v)
    }
    fun entitySnapshots(id:String):List<Pair<String,String>> = readableDatabase.rawQuery("SELECT label,payload FROM entity_snapshots WHERE recordId=? ORDER BY createdAt DESC",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun restoreEntitySnapshot(id:String,payload:String){
        val o=JSONObject(payload)
        get(id)?.let{save(it.copy(title=o.optString("title",it.title),body=o.optString("body",it.body),tags=o.optString("tags",it.tags),aliases=o.optString("aliases",it.aliases)))}
        val tplId=o.optString("templateId","")
        if(tplId.isNotBlank())attachSheet(id,tplId)
        val sv=o.optJSONObject("sheetValues")
        if(sv!=null){
            val restored=buildMap{sv.keys().forEach{k->put(k,sv.optString(k,""))}}
            saveSheetValues(id,restored)
        }
    }
    fun addTimelineEvent(worldId:String,title:String,start:String,end:String="",description:String="",recordId:String=""){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("worldId",worldId);put("title",title);put("startDate",start);put("endDate",end);put("description",description);put("recordId",recordId)};writableDatabase.insert("timeline_events",null,v)}
    fun timeline(worldId:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT title,startDate,COALESCE(description,'') FROM timeline_events WHERE worldId=? ORDER BY startDate",arrayOf(worldId)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun createBoard(worldId:String,name:String,type:String):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("worldId",worldId);put("name",name);put("boardType",type);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("planning_boards",null,v);return id}
    fun setBoardBackground(boardId:String,uri:String){val v=ContentValues().apply{put("backgroundUri",uri)};writableDatabase.update("planning_boards",v,"id=?",arrayOf(boardId))}
    fun boardBackground(boardId:String):String = readableDatabase.rawQuery("SELECT COALESCE(backgroundUri,'') FROM planning_boards WHERE id=?",arrayOf(boardId)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun boards(worldId:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,name,boardType FROM planning_boards WHERE worldId=? ORDER BY createdAt DESC",arrayOf(worldId)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun addBoardItem(boardId:String,title:String,content:String="",recordId:String="",x:Float=0f,y:Float=0f,color:String="#D9C7EE",imageUri:String=""):String{val count=boardItems(boardId).size;val px=if(x==0f&&y==0f)(count%2)*170f else x;val py=if(x==0f&&y==0f)(count/2)*130f else y;val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("boardId",boardId);put("title",title);put("content",content);put("recordId",recordId);put("x",px);put("y",py);put("color",color);put("imageUri",imageUri)};writableDatabase.insert("board_items",null,v);return id}
    fun boardItemImage(id:String):String=readableDatabase.rawQuery("SELECT COALESCE(imageUri,'') FROM board_items WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun boardItemColor(id:String):String=readableDatabase.rawQuery("SELECT COALESCE(color,'#D9C7EE') FROM board_items WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else "#D9C7EE"}
    fun boardItemRecord(id:String):String=readableDatabase.rawQuery("SELECT COALESCE(recordId,'') FROM board_items WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun boardItemPosition(id:String):Pair<Float,Float> = readableDatabase.rawQuery("SELECT COALESCE(x,0),COALESCE(y,0) FROM board_items WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getFloat(0) to it.getFloat(1) else 0f to 0f}
    fun autoArrangeBoard(boardId:String,layout:String){val items=boardItems(boardId);if(items.isEmpty())return;val positions=when(layout){ "Relationship Graph"->{val radius=maxOf(140.0,items.size*24.0);items.mapIndexed{i,item->item.first to ((radius+radius*kotlin.math.cos(2*Math.PI*i/items.size)).toFloat() to (radius+radius*kotlin.math.sin(2*Math.PI*i/items.size)).toFloat())} }; "Family Tree"->{val groups=items.groupBy{item->val recordId=boardItemRecord(item.first);var depth=0;var parent=dbParent(recordId);val seen=mutableSetOf<String>();while(parent.isNotBlank()&&seen.add(parent)&&depth<12){depth++;parent=dbParent(parent)};depth}.toSortedMap();groups.flatMap{(depth,nodes)->nodes.mapIndexed{i,item->item.first to (i*190f to depth*150f)}}}; "Flowchart"->items.mapIndexed{i,item->item.first to ((i%3)*220f to (i/3)*150f)}; "Timeline"->items.mapIndexed{i,item->item.first to (40f to i*140f)};else->items.mapIndexed{i,item->item.first to ((i%3)*190f to (i/3)*140f)} };positions.forEach{(id,pos)->moveBoardItem(id,pos.first,pos.second)} }
    private fun dbParent(id:String):String=if(id.isBlank())"" else readableDatabase.rawQuery("SELECT COALESCE(parentId,'') FROM records WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun boardType(id:String):String=readableDatabase.rawQuery("SELECT boardType FROM planning_boards WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else "Corkboard"}
    fun moveBoardItem(id:String,x:Float,y:Float){val v=ContentValues().apply{put("x",x.coerceAtLeast(0f));put("y",y.coerceAtLeast(0f))};writableDatabase.update("board_items",v,"id=?",arrayOf(id))}
    fun boardItems(boardId:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,title,content FROM board_items WHERE boardId=? ORDER BY rowid",arrayOf(boardId)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun boardLinkLabel(boardId:String,from:String,to:String):String=readableDatabase.rawQuery("SELECT COALESCE(label,'') FROM board_links WHERE boardId=? AND fromItem=? AND toItem=?",arrayOf(boardId,from,to)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun boardLinks(boardId:String):List<Pair<String,String>> = readableDatabase.rawQuery("SELECT fromItem,toItem FROM board_links WHERE boardId=?",arrayOf(boardId)).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun linkBoardItems(boardId:String,from:String,to:String,label:String=""){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("boardId",boardId);put("fromItem",from);put("toItem",to);put("label",label)};writableDatabase.insertWithOnConflict("board_links",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun addMapPin(boardId:String,x:Float,y:Float,label:String,color:String="#D64545",recordId:String="",linkedBoardId:String=""):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("boardId",boardId);put("x",x.coerceIn(0f,1f));put("y",y.coerceIn(0f,1f));put("label",label);put("color",color);put("recordId",recordId);put("linkedBoardId",linkedBoardId);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("map_pins",null,v);return id}
    fun mapPins(boardId:String):List<MapPin> = readableDatabase.rawQuery("SELECT id,boardId,COALESCE(x,0),COALESCE(y,0),COALESCE(label,''),COALESCE(color,'#D64545'),COALESCE(recordId,''),COALESCE(linkedBoardId,'') FROM map_pins WHERE boardId=? ORDER BY createdAt",arrayOf(boardId)).use{c->buildList{while(c.moveToNext())add(MapPin(c.getString(0),c.getString(1),c.getFloat(2),c.getFloat(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)))}}
    fun updateMapPin(id:String,label:String,color:String,recordId:String,linkedBoardId:String){val v=ContentValues().apply{put("label",label);put("color",color);put("recordId",recordId);put("linkedBoardId",linkedBoardId)};writableDatabase.update("map_pins",v,"id=?",arrayOf(id))}
    fun moveMapPin(id:String,x:Float,y:Float){val v=ContentValues().apply{put("x",x.coerceIn(0f,1f));put("y",y.coerceIn(0f,1f))};writableDatabase.update("map_pins",v,"id=?",arrayOf(id))}
    fun deleteMapPin(id:String){writableDatabase.delete("map_pins","id=?",arrayOf(id))}
    fun boardName(id:String):String=readableDatabase.rawQuery("SELECT name FROM planning_boards WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.getString(0)else ""}
    fun recordsForRevision(id:String)=revisions(id)
}
