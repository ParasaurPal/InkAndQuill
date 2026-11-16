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
    val favorite:Boolean=false
)

class InkDatabase(context:Context):SQLiteOpenHelper(context.applicationContext,"inkandquill.db",null,17){
    private val appContext=context.applicationContext
    init {
        seed(writableDatabase)
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
        db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,title TEXT NOT NULL,body TEXT NOT NULL,kind TEXT NOT NULL,parentId TEXT,scope TEXT,tags TEXT,aliases TEXT,createdAt INTEGER,updatedAt INTEGER,favorite INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE links(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,position INTEGER DEFAULT 0,term TEXT,endPosition INTEGER,UNIQUE(sourceId,targetId,label,position))")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE revisions(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,title TEXT,body TEXT,createdAt INTEGER)")
        db.execSQL("CREATE TABLE template_fields(id TEXT PRIMARY KEY,templateId TEXT,name TEXT,fieldType TEXT,required INTEGER,position INTEGER,section TEXT DEFAULT 'General',options TEXT DEFAULT '',hint TEXT DEFAULT '',removed INTEGER DEFAULT 0,conditionField TEXT DEFAULT '',conditionValue TEXT DEFAULT '',formula TEXT DEFAULT '',repeatable INTEGER DEFAULT 0)")
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
        db.execSQL("CREATE TABLE record_relationships(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT NOT NULL,notes TEXT,createdAt INTEGER,UNIQUE(sourceId,targetId,label))")
        db.execSQL("CREATE TABLE entity_snapshots(id TEXT PRIMARY KEY,recordId TEXT NOT NULL,label TEXT NOT NULL,payload TEXT NOT NULL,createdAt INTEGER)")
        db.execSQL("CREATE TABLE timeline_events(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,title TEXT NOT NULL,startDate TEXT NOT NULL,endDate TEXT,description TEXT,recordId TEXT)")
        db.execSQL("CREATE TABLE planning_boards(id TEXT PRIMARY KEY,worldId TEXT,name TEXT NOT NULL,boardType TEXT NOT NULL,createdAt INTEGER,backgroundUri TEXT)")
        db.execSQL("CREATE TABLE board_items(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,recordId TEXT,title TEXT NOT NULL,content TEXT,x REAL DEFAULT 0,y REAL DEFAULT 0,color TEXT,imageUri TEXT)")
        db.execSQL("CREATE TABLE board_links(id TEXT PRIMARY KEY,boardId TEXT NOT NULL,fromItem TEXT NOT NULL,toItem TEXT NOT NULL,label TEXT,UNIQUE(boardId,fromItem,toItem))")
        db.execSQL("CREATE TABLE record_shares(recordId TEXT NOT NULL,projectId TEXT NOT NULL,createdAt INTEGER,PRIMARY KEY(recordId,projectId))")
        db.execSQL("CREATE TABLE tags(id TEXT PRIMARY KEY,name TEXT NOT NULL,tagType TEXT NOT NULL,color TEXT,UNIQUE(name,tagType))")
        db.execSQL("CREATE TABLE record_tags(recordId TEXT NOT NULL,tagId TEXT NOT NULL,PRIMARY KEY(recordId,tagId))")
        db.execSQL("CREATE TABLE tag_aliases(id TEXT PRIMARY KEY,tagId TEXT NOT NULL,alias TEXT NOT NULL,UNIQUE(tagId,alias))")
        db.execSQL("CREATE TABLE world_calendars(id TEXT PRIMARY KEY,worldId TEXT NOT NULL,name TEXT NOT NULL,months TEXT,weekdays TEXT,seasons TEXT,moons TEXT,notes TEXT,monthLengths TEXT,eras TEXT,leapRules TEXT)")
        db.execSQL("CREATE TABLE calendar_entries(id TEXT PRIMARY KEY,calendarId TEXT NOT NULL,year INTEGER,month INTEGER,day INTEGER,title TEXT NOT NULL,description TEXT,repeatYearly INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE link_history(id TEXT PRIMARY KEY,sourceId TEXT NOT NULL,targetId TEXT NOT NULL,label TEXT,position INTEGER,action TEXT,createdAt INTEGER,term TEXT,endPosition INTEGER)")
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
            "Reference Entry|reference|Subject,Description,Source,Connections,Notes",
            "Novel Project|novel|Premise,Genre,Audience,Themes,Synopsis,Main Cast,World,Outline,Revision Plan",
            "Standalone Story|standalone|Premise,POV,Setting,Characters,Conflict,Structure,Ending,Themes",
            "Series Bible|series|Series Premise,Core Themes,Volume List,Long Arc,Cast,Continuity Rules,Open Threads",
            "Book Plan|bookplan|Logline,Summary,POV,Cast,Chapter Outline,Subplots,Continuity,Revision Notes",
            "Short Story|shortstory|Hook,Character,Setting,Conflict,Turning Point,Climax,Resolution",
            "Flash Fiction|flash|Opening Image,Central Idea,Turn,Ending,Word Limit",
            "Screenplay|screenplay|Logline,Format,Characters,Sequence Outline,Scenes,Dialogue,Production Notes",
            "Stage Play|play|Premise,Cast,Acts,Scenes,Stage Directions,Dialogue,Themes",
            "Essay|essay|Question,Thesis,Arguments,Evidence,Counterargument,Conclusion,Sources",
            "Article or Blog Post|article|Headline,Audience,Angle,Outline,Key Points,Sources,Call to Action",
            "Research Paper|paper|Research Question,Thesis,Method,Findings,Discussion,Citations",
            "Journal Entry|journal|Date,Mood,Events,Reflections,Gratitude,Next Steps",
            "Daily Log|dailylog|Date,Highlights,Tasks,Notes,Reflection",
            "Dream Journal|dream|Date,Setting,People,Events,Emotions,Symbols,Interpretation",
            "Travel Journal|travel|Date,Place,Itinerary,Experiences,People,Memories,Expenses",
            "Meeting Notes|meeting|Date,Attendees,Agenda,Discussion,Decisions,Action Items",
            "Project Brief|project|Goal,Audience,Scope,Deliverables,Milestones,Risks,Resources",
            "World Flora and Fauna|ecology|Biomes,Plants,Animals,Food Webs,Adaptations,Threats",
            "Climate and Biomes|biome|Regions,Seasons,Weather,Climate Patterns,Resources,Effects",
            "Custom Calendar System|calendar-system|Year Length,Months,Weekdays,Seasons,Moons,Eras,Leap Rules",
            "Moon and Lunar Cycle|moon-cycle|Name,Orbit,Phases,Calendar Effects,Cultural Meaning",
            "Economy and Trade|economy|Currency,Resources,Industries,Trade Routes,Taxes,Class Effects",
            "Law and Government|government|Government Type,Leaders,Laws,Courts,Enforcement,Political Tensions",
            "Customs and Traditions|tradition|Name,Origin,Participants,Timing,Rituals,Meaning,Variations",
            "Save the Cat Outline|savecat|Opening Image,Theme Stated,Set-Up,Catalyst,Debate,Midpoint,All Is Lost,Finale",
            "Snowflake Method|snowflake|One Sentence,One Paragraph,Character Summaries,Expanded Synopsis,Scene List",
            "Story Circle|storycircle|You,Need,Go,Search,Find,Take,Return,Change",
            "Character Relationship Map|relationship-map|Nodes,Connections,Conflicts,Alliances,Changes",
            "Scene Breakdown|scene-breakdown|Scene Goal,Conflict,Disaster,Reaction,Dilemma,Decision",
            "Continuity Tracker|continuity|Fact,First Mention,Related Records,Dependencies,Potential Conflicts",
            "Source Citation|citation|Author,Title,Publisher,Publication Date,URL,Access Date,Notes",
            "Plant Profile|plant|Common Name,Scientific Name,Plant Type,Description,Native Range,Habitat,Climate,Soil,Seasonality,Uses,Edible Parts,Toxicity,Medicinal Properties,Magical Properties,Harvesting,Conservation,Image Gallery,References",
            "Animal or Fauna Profile|animal|Common Name,Scientific Name,Classification,Description,Range,Habitat,Diet,Predators,Behavior,Social Structure,Reproduction,Lifespan,Adaptations,Domestication,Conservation,Image Gallery,References",
            "Fungus Profile|fungus|Common Name,Scientific Name,Type,Appearance,Habitat,Seasonality,Ecological Role,Edibility,Toxicity,Uses,Reproduction,Harvesting,Image Gallery,References",
            "Mineral and Gemstone|mineral|Name,Classification,Composition,Hardness,Color,Formation,Locations,Physical Properties,Magical Properties,Industrial Uses,Rarity,Trade Value,Image Gallery,References",
            "Biome Profile|biome-profile|Name,Climate,Temperature Range,Precipitation,Seasons,Flora,Fauna,Geography,Resources,Hazards,Settlements,Ecological Changes,Image Gallery",
            "Weather and Climate Pattern|weather-pattern|Pattern,Season,Duration,Frequency,Temperature,Precipitation,Winds,Regional Effects,Hazards,Cultural Effects",
            "Celestial Body|celestial|Name,Type,Size,Mass,Orbit,Rotation,Atmosphere,Surface,Moons,Visibility,Mythology,Effects on World,Image Gallery",
            "Plane or Realm|realm|Name,Type,Description,Physical Laws,Environment,Inhabitants,Access Methods,Time Flow,Threats,Connections,Image Gallery",
            "Portal or Gateway|portal|Name,Origin,Destination,Activation,Requirements,Appearance,Stability,Limitations,Guardians,Known Locations",
            "Resource and Material|resource|Name,Type,Source,Locations,Extraction,Properties,Uses,Rarity,Renewability,Ownership,Trade Value,Environmental Impact",
            "Food and Cuisine|cuisine|Dish or Ingredient,Origin,Ingredients,Preparation,Flavor,Availability,Regional Variations,Cultural Meaning,Restrictions,Image Gallery",
            "Clothing and Fashion|clothing|Garment,Materials,Construction,Function,Climate,Social Meaning,Regional Styles,Status Markers,Variations,Image Gallery",
            "Custom or Tradition|custom-detail|Name,Origin,Occasion,Participants,Procedure,Materials,Symbolism,Regional Variations,Restrictions,Changes Over Time",
            "Holiday or Festival|festival|Name,Date or Season,Origin,Duration,Participants,Traditions,Food,Symbols,Events,Regional Variations,Historical Changes",
            "Ritual and Ceremony|ceremony|Name,Purpose,Tradition,Participants,Location,Timing,Preparation,Steps,Objects,Words or Chants,Expected Outcome,Risks,Variations",
            "Art and Craft Tradition|art-tradition|Medium,Materials,Techniques,Styles,Practitioners,Training,Uses,Symbolism,History,Regional Variations,Image Gallery",
            "Music and Instruments|music|Style,Instruments,Structure,Scales or Modes,Performance Context,Practitioners,History,Social Meaning,Examples",
            "Sport and Games|game|Name,Type,Equipment,Rules,Objective,Players,Scoring,Locations,Professional Scene,Cultural Importance,Variations",
            "Currency and Banking|currency|Currency Name,Symbol,Denominations,Materials,Issuing Authority,Exchange Rate,Backing,Counterfeiting,Institutions,Historical Changes",
            "Trade Route and Market|trade-route|Route,Origin,Destination,Distance,Travel Time,Goods,Carriers,Costs,Risks,Control,Seasonality,Political Importance",
            "Military and Warfare|military|Force Name,Command Structure,Branches,Recruitment,Training,Equipment,Doctrine,Strength,Weaknesses,Conflicts,Alliances,Logistics",
            "Law and Legal Code|law|Law or Code,Jurisdiction,Authority,Protected Rights,Prohibitions,Enforcement,Trial Process,Penalties,Exceptions,Historical Context",
            "Education and Knowledge|education|Institutions,Access,Teaching Methods,Curriculum,Literacy,Apprenticeships,Knowledge Keepers,Restrictions,History",
            "Medicine and Healing|medicine|Practice,Practitioners,Training,Tools,Remedies,Diagnosis,Treatments,Limitations,Ethics,Access,Regional Traditions",
            "Transportation and Travel|transport|Method,Vehicle or Animal,Capacity,Speed,Range,Infrastructure,Costs,Hazards,Availability,Regional Variations",
            "Architecture and Building Style|architecture|Style,Materials,Structural Methods,Climate Adaptation,Typical Layout,Decoration,Builders,Regional Variations,Historical Period,Image Gallery",
            "Measurement and Units|measurement|System Name,Length Units,Mass Units,Volume Units,Time Units,Temperature Units,Conversion Rules,Standards,Regional Variations",
            "Magic School or Tradition|magic-school|Name,Philosophy,Source,Practitioners,Training,Techniques,Rules,Costs,Limitations,Taboos,History,Related Traditions",
            "Magical Creature or Monster|magical-creature|Name,Classification,Appearance,Origin,Habitat,Behavior,Abilities,Weaknesses,Diet,Threat Level,Encounters,Lore,Image Gallery",
            "Dragon or Great Beast|dragon|Species,Appearance,Anatomy,Habitat,Intelligence,Abilities,Breath or Weapon,Weaknesses,Lifecycle,Social Structure,Treasures,History,Image Gallery",
            "Planting and Agriculture|agriculture|Crops,Growing Season,Soil,Climate,Methods,Tools,Irrigation,Yields,Pests,Storage,Trade,Food Security",
            "Population and Demographics|population|Region,Population,Population Density,Age Distribution,Languages,Migration,Birth Rate,Life Expectancy,Urbanization,Data Sources",
            "Political System and Succession|politics|System,Head of State,Legislature,Judiciary,Local Government,Succession,Parties or Factions,Checks and Balances,Current Tensions",
            "Historical Conflict or War|war|Name,Dates,Causes,Parties,Leaders,Major Battles,Strategies,Turning Points,Consequences,Treaties,Legacy,Primary Sources",
            "Myth, Legend, or Folktale|myth|Title,Tradition,Characters,Setting,Plot,Themes,Symbolism,Versions,Historical Basis,Cultural Function,Sources",
            "Prophecy and Omen|prophecy|Text or Sign,Origin,Interpreter,Conditions,Possible Meanings,Known Versions,Related Events,Who Believes It,Consequences",
            "Secret Society|secret-society|Purpose,Public Face,Membership,Initiation,Leadership,Symbols,Resources,Operations,Secrets,Allies,Enemies,Known Members",
            "Plant or Animal Species Taxonomy|taxonomy|Common Name,Scientific Name,Kingdom,Phylum,Class,Order,Family,Genus,Species,Subspecies,Related Species,References",
            "World Rules and Constants|world-rules|Rule,Scope,Exceptions,Origin,Observable Effects,Who Knows,Consequences,Related Systems",
            "Geographic Feature|geographic-feature|Name,Feature Type,Coordinates or Position,Formation,Dimensions,Climate,Flora and Fauna,Resources,Hazards,Settlements,History,Map Image",
            "Settlement District|district|Name,Location,Population,Architecture,Landmarks,Services,Economy,Authorities,Notable Residents,Conflicts,Map Image",
            "Character Lineage and Heritage|lineage|Person or Family,Ancestors,Descendants,House or Clan,Inheritance,Heritage,Adoptions,Disputed Claims,Secrets,Family Tree",
            "House, Clan, or Dynasty|dynasty|Name,Founder,Lineage,Motto,Symbols,Seat,Holdings,Allies,Rivals,Notable Members,Succession,History",
            "Magic Spell Catalogue|spell-catalogue|Spell Name,School,Effect,Range,Duration,Components,Requirements,Cost,Side Effects,Counterspell,Known Users",
            "Technology System|tech-system|Technology Level,Energy Sources,Materials,Manufacturing,Transportation,Communication,Medicine,Weapons,Limitations,Uneven Access",
            "Worldbuilding Glossary|glossary|Term,Definition,Pronunciation,Language,Category,First Use,Related Terms,Notes",
            "Worldbuilding Open Questions|open-questions|Question,Why It Matters,Current Assumptions,Options,Dependencies,Decision,Status,Related Records"
        )
        data.forEach{line->val p=line.split("|");val templateId="template_"+p[1];val category=when{p[1].contains("character")||p[1] in listOf("npc","faction","species","creature","location","culture","deity","religion")->"Characters & Worldbuilding";p[1].contains("magic")||p[1] in listOf("spell","artifact","technology","power")->"Magic & Systems";p[1].contains("plot")||p[1] in listOf("savecat","snowflake","storycircle","scene-breakdown","relationship-map")->"Plot & Structure";p[1].contains("calendar")||p[1].contains("moon")||p[1].contains("timeline")->"Timeline & Calendar";p[1].contains("map")||p[1].contains("floor")||p[1].contains("mood")||p[1].contains("board")->"Visual Planning";p[1].contains("reference")||p[1].contains("citation")||p[1].contains("research")->"Research & Reference";p[1] in listOf("novel","standalone","series","bookplan","shortstory","flash","screenplay","play","essay","article","paper","journal","dailylog","dream","travel","meeting","project")->"Writing";else->"Characters & Worldbuilding"};val v=ContentValues().apply{put("id",templateId);put("title",p[0]);put("body",p[2].split(",").joinToString("\n\n"){"@@{it}:\n"});put("kind","Template");put("tags",category)};db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_IGNORE);db.execSQL("UPDATE records SET tags=? WHERE id=? AND (tags IS NULL OR tags='')",arrayOf(category,templateId));p[2].split(",").forEachIndexed{index,field->val fieldName=field.trim();val fieldType=when{fieldName.contains("Date",true)||fieldName=="Seasonality"->"Date";fieldName in listOf("Population","Population Density","Age Distribution","Birth Rate","Life Expectancy","Distance","Travel Time","Capacity","Speed","Range","Duration","Temperature","Precipitation","Hardness","Mass","Size","Length Units","Mass Units","Volume Units","Time Units","Temperature Units","Yields","Year Length","Week Length","Days per Month","Lifespan","Scoring") ->"Number";fieldName.contains("Image",true)||fieldName.contains("Map Image")||fieldName=="Family Tree"->"Table";fieldName in listOf("Related Records","Related Systems","Related Terms","Related Species","People Involved","Notable Residents","Known Members","Allies","Enemies","Practitioners","Inhabitants","Followers","Members","Ancestors","Descendants","Owners","Leaders") ->"Relationship";fieldName in listOf("Type","Classification","Climate","Season","Government Type","School","Status","Threat Level","Rarity","Currency Name","System","Medium","Style","Occasion","Purpose","Jurisdiction","Kingdom","Phylum","Class","Order","Family","Genus","Species","Subspecies") ->"Dropdown";fieldName.endsWith("s")||fieldName in listOf("Abilities","Weaknesses","Ingredients","Materials","Symbols","Resources","Techniques","Traditions","Rules","Exceptions","Crops","Languages","Instruments","Branches","Denominations","Landmarks","Regions","Settlements","Moons","Seasons","Flora","Fauna","Predators","Adaptations","Side Effects","Components","Equipment","Major Battles","Turning Points","Known Versions") ->"Repeatable group";else->"Long text"};val fieldOptions=when(fieldName){"Type"->"Natural,Artificial,Magical,Social,Political,Religious,Other";"Classification"->"Mammal,Bird,Reptile,Amphibian,Fish,Insect,Arachnid,Fungus,Plant,Mineral,Other";"Climate"->"Tropical,Dry,Temperate,Continental,Polar,Alpine,Other";"Season"->"Spring,Summer,Autumn,Winter,Wet,Dry,Other";"Government Type"->"Monarchy,Republic,Democracy,Oligarchy,Theocracy,Dictatorship,Tribal Council,Anarchy,Other";"School"->"Elemental,Illusion,Divination,Enchantment,Conjuration,Abjuration,Necromancy,Transmutation,Other";"Status"->"Draft,Proposed,Established,Disputed,Deprecated,Other";"Threat Level"->"Negligible,Low,Moderate,High,Severe,Catastrophic";"Rarity"->"Common,Uncommon,Rare,Very Rare,Legendary,Unique";"Currency Name"->"Coin,Barter,Commodity,Paper,Digital,Other";"System"->"Metric,Imperial,Customary,Other";"Medium"->"Painting,Sculpture,Textile,Ceramic,Metalwork,Woodwork,Other";"Style"->"Traditional,Formal,Regional,Religious,Military,Everyday,Other";"Occasion"->"Birth,Coming of Age,Marriage,Death,Harvest,New Year,Solstice,Equinox,Other";"Purpose"->"Practical,Religious,Political,Social,Protective,Ceremonial,Other";"Jurisdiction"->"Local,Regional,National,International,Other";else->"Other"};val fv=ContentValues().apply{put("id","field_"+p[1]+"_"+index);put("templateId",templateId);put("name",fieldName);put("fieldType",fieldType);put("options",fieldOptions);put("required",0);put("position",index);put("section",when{fieldName.contains("Image",true)||fieldName.contains("Map",true)||fieldName.contains("Family Tree")->"Visual References";fieldName in listOf("History","Origin","Historical Context","Historical Changes","Historical Basis","Legacy")->"History & Lore";fieldName in listOf("Related Records","Related Systems","Related Terms","Related Species","References","Sources","Data Sources")->"Connections & Sources";else->"Details"})};db.insertWithOnConflict("template_fields",null,fv,SQLiteDatabase.CONFLICT_IGNORE);db.update("template_fields",ContentValues().apply{put("fieldType",fieldType);put("options",fieldOptions);put("section",when{fieldName.contains("Image",true)||fieldName.contains("Map",true)||fieldName.contains("Family Tree")->"Visual References";fieldName in listOf("History","Origin","Historical Context","Historical Changes","Historical Basis","Legacy")->"History & Lore";fieldName in listOf("Related Records","Related Systems","Related Terms","Related Species","References","Sources","Data Sources")->"Connections & Sources";else->"Details"})},"id=?",arrayOf("field_"+p[1]+"_"+index))}}
    }
    fun restoreStarterTemplates(){val db=writableDatabase;val ids=mutableListOf<String>();db.rawQuery("SELECT id FROM records WHERE kind='Template' AND id LIKE 'template_%'",null).use{while(it.moveToNext())ids.add(it.getString(0))};db.beginTransaction();try{ids.forEach{id->listOf("template_fields","template_sections","template_versions").forEach{table->db.delete(table,"templateId=?",arrayOf(id))};db.delete("records","id=?",arrayOf(id))};db.setTransactionSuccessful()}finally{db.endTransaction()};seed(db)}
    private fun android.database.Cursor.record()=InkRecord(getString(getColumnIndexOrThrow("id")),getString(getColumnIndexOrThrow("title")),getString(getColumnIndexOrThrow("body")),getString(getColumnIndexOrThrow("kind")),getString(getColumnIndexOrThrow("parentId"))?:"",getString(getColumnIndexOrThrow("scope"))?:"World",getString(getColumnIndexOrThrow("tags"))?:"",getString(getColumnIndexOrThrow("aliases"))?:"",getLong(getColumnIndexOrThrow("createdAt")),getLong(getColumnIndexOrThrow("updatedAt")),getInt(getColumnIndexOrThrow("favorite"))==1)
    fun descendants(parentId:String):List<InkRecord>{val allRecords=all();val result=mutableListOf<InkRecord>();val visited=mutableSetOf<String>();fun visit(id:String){allRecords.filter{it.parentId==id&&visited.add(it.id)}.forEach{record->result.add(record);visit(record.id)}};visit(parentId);return result}
    fun all(query:String="",kind:String?=null):List<InkRecord>{
        val sql=StringBuilder("SELECT * FROM records WHERE kind!='Template'");val args=mutableListOf<String>()
        if(!kind.isNullOrBlank()&&kind!="All"){sql.append(" AND kind=?");args.add(kind)}
        if(query.isNotBlank()){sql.append(" AND (title LIKE ? OR body LIKE ? OR tags LIKE ? OR aliases LIKE ? OR EXISTS(SELECT 1 FROM record_tags rt JOIN tags t ON t.id=rt.tagId WHERE rt.recordId=records.id AND t.name LIKE ?))");val q="%$query%";args.addAll(listOf(q,q,q,q,q))}
        sql.append(" ORDER BY favorite DESC,updatedAt DESC")
        return readableDatabase.rawQuery(sql.toString(),args.toTypedArray()).use{c->buildList{while(c.moveToNext())add(c.record())}}
    }
    fun get(id:String)=readableDatabase.rawQuery("SELECT * FROM records WHERE id=?",arrayOf(id)).use{if(it.moveToFirst())it.record()else null}
    fun templates()=readableDatabase.rawQuery("SELECT * FROM records WHERE kind='Template' ORDER BY title",null).use{c->buildList{while(c.moveToNext())add(c.record())}}
    fun save(r:InkRecord){
        if(r.parentId.isNotBlank()){
            require(r.parentId!=r.id){"A record cannot be its own parent."}
            if(get(r.parentId)!=null){
                require(descendants(r.id).none{it.id==r.parentId}){"This parent would create a circular project hierarchy."}
            }
        }
        val db=writableDatabase
        val old=get(r.id)
        if(old!=null && old.body!=r.body){val rev=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",old.id);put("title",old.title);put("body",old.body);put("createdAt",System.currentTimeMillis())};db.insert("revisions",null,rev)}
        val v=ContentValues().apply{put("id",r.id);put("title",r.title);put("body",r.body);put("kind",r.kind);put("parentId",r.parentId);put("scope",r.scope);put("tags",r.tags);put("aliases",r.aliases);put("createdAt",r.createdAt);put("updatedAt",System.currentTimeMillis());put("favorite",if(r.favorite)1 else 0)}
        db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun delete(id:String){
        val existing=get(id)
        val db=writableDatabase
        val mediaIds=mutableSetOf<String>()
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
            listOf("record_folders","record_tags","attachments","link_exclusions","entity_snapshots","revisions","record_shares","collection_records","open_tabs").forEach{table->db.delete(table,"recordId=?",arrayOf(id))}
            db.delete("timeline_events","recordId=?",arrayOf(id))
            removedBoardItemIds.forEach{itemId->db.delete("board_links","fromItem=? OR toItem=?",arrayOf(itemId,itemId))}
            db.delete("board_items","recordId=?",arrayOf(id))
            if(existing?.kind=="Template")listOf("template_fields","template_sections","template_versions").forEach{table->db.delete(table,"templateId=?",arrayOf(id))}
            if(existing?.kind=="World"){
                db.delete("timeline_events","worldId=?",arrayOf(id))
                worldBoardIds.forEach{boardId->
                    db.delete("board_links","boardId=?",arrayOf(boardId))
                    db.delete("board_items","boardId=?",arrayOf(boardId))
                }
                db.delete("planning_boards","worldId=?",arrayOf(id))
                db.delete("world_calendars","worldId=?",arrayOf(id))
            }
            db.delete("records","id=?",arrayOf(id))
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
    private val backupTables=listOf("links","settings","revisions","template_fields","folders","record_folders","collections","collection_records","attachments","link_exclusions","link_suggestions","open_tabs","template_sections","template_versions","record_relationships","entity_snapshots","timeline_events","planning_boards","board_items","board_links","record_shares","tags","record_tags","tag_aliases","world_calendars","calendar_entries","link_history")
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
        root.put("attachment_blobs",blobs)
        return root.toString(2)
    }
    private fun importTable(table:String,arr:JSONArray){
        val allowed=when(table){
            "links"->setOf("id","sourceId","targetId","label","position","term","endPosition");"settings"->setOf("key","value");"revisions"->setOf("id","recordId","title","body","createdAt");
            "template_fields"->setOf("id","templateId","name","fieldType","required","position","section","options","hint","removed","conditionField","conditionValue","formula","repeatable");"folders"->setOf("id","name","parentId","color","position");"record_folders"->setOf("recordId","folderId");
            "collections"->setOf("id","name","description","color","createdAt");"collection_records"->setOf("collectionId","recordId");"attachments"->setOf("id","recordId","uri","mimeType","displayName","size","createdAt");
            "link_exclusions"->setOf("id","recordId","term");"link_suggestions"->setOf("id","sourceId","targetId","term","position","status");"open_tabs"->setOf("position","recordId");
            "template_sections"->setOf("id","templateId","name","position");"template_versions"->setOf("id","templateId","snapshot","createdAt");"record_relationships"->setOf("id","sourceId","targetId","label","notes","createdAt");
            "entity_snapshots"->setOf("id","recordId","label","payload","createdAt");"timeline_events"->setOf("id","worldId","title","startDate","endDate","description","recordId");
            "planning_boards"->setOf("id","worldId","name","boardType","createdAt","backgroundUri");"board_items"->setOf("id","boardId","recordId","title","content","x","y","color","imageUri");"board_links"->setOf("id","boardId","fromItem","toItem","label");"record_shares"->setOf("recordId","projectId","createdAt");"tags"->setOf("id","name","tagType","color");"record_tags"->setOf("recordId","tagId");"tag_aliases"->setOf("id","tagId","alias");"world_calendars"->setOf("id","worldId","name","months","weekdays","seasons","moons","notes","monthLengths","eras","leapRules");"calendar_entries"->setOf("id","calendarId","year","month","day","title","description","repeatYearly");"link_history"->setOf("id","sourceId","targetId","label","position","action","createdAt","term","endPosition");else->emptySet()}
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
        } catch(error:Exception) {
            rollbackImportedMedia(mediaBackups,error)
            throw error
        }
    }

    private fun cleanupOrphanedImportedMedia() {
        val referenced=mutableSetOf<String>()
        listOf("attachments","planning_boards","board_items").forEach{table->
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
            save(InkRecord(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled"),o.optString("body"),o.optString("kind",o.optString("type","Document")),o.optString("parentId"),o.optString("scope","World"),o.optString("tags"),o.optString("aliases"),o.optLong("createdAt",System.currentTimeMillis()),favorite=o.optBoolean("favorite")))
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
    fun exportTemplateJson(id:String):String{val t=get(id)?:throw IllegalArgumentException("Template not found.");val fields=JSONArray();templateFields(id,true).forEach{f->fields.put(JSONObject().put("name",f.name).put("type",f.fieldType).put("required",f.required).put("position",f.position).put("section",f.section).put("options",f.options).put("hint",f.hint).put("conditionField",f.conditionField).put("conditionValue",f.conditionValue).put("formula",f.formula).put("repeatable",f.repeatable))};return JSONObject().put("format","inkandquill-template").put("version",1).put("title",t.title).put("body",t.body).put("category",t.tags).put("fields",fields).toString(2)}
    fun importTemplateJson(raw:String):InkRecord{val o=JSONObject(raw);require(o.optString("format")=="inkandquill-template"){"Not an Ink & Quill template file."};val t=InkRecord(title=o.optString("title","Imported template"),body=o.optString("body"),kind="Template",tags=o.optString("category","Other"));save(t);val fields=o.optJSONArray("fields")?:JSONArray();for(i in 0 until fields.length()){val f=fields.optJSONObject(i)?:continue;addTemplateField(t.id,f.optString("name","Field"),f.optString("type","Long text"),f.optBoolean("required"),f.optString("section","General"),f.optString("options"),f.optString("hint"),f.optString("conditionField"),f.optString("conditionValue"),f.optString("formula"),f.optBoolean("repeatable"))};return t}
    fun duplicateTemplate(id:String):InkRecord?{val source=get(id)?:return null;val copy=source.copy(id=UUID.randomUUID().toString(),title="Copy of "+source.title,createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis());save(copy);templateFields(id).forEach{f->addTemplateField(copy.id,f.name,f.fieldType,f.required,f.section,f.options,f.hint,f.conditionField,f.conditionValue,f.formula,f.repeatable)};return copy}
    fun templateFields(templateId:String,includeRemoved:Boolean=false):List<TemplateField>{
        val sql="SELECT id,templateId,name,fieldType,required,position,COALESCE(section,'General'),COALESCE(options,''),COALESCE(hint,''),COALESCE(conditionField,''),COALESCE(conditionValue,''),COALESCE(formula,''),COALESCE(repeatable,0) FROM template_fields WHERE templateId=? "+(if(includeRemoved) "" else "AND COALESCE(removed,0)=0 ")+" ORDER BY position"
        return readableDatabase.rawQuery(sql,arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(TemplateField(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1,c.getInt(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getInt(12)==1))}}
    }
    fun addTemplateField(templateId:String,name:String,type:String,required:Boolean,section:String="General",options:String="",hint:String="",conditionField:String="",conditionValue:String="",formula:String="",repeatable:Boolean=false){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",name.trim());put("fieldType",type);put("required",if(required)1 else 0);put("position",templateFields(templateId,true).size);put("section",section);put("options",options);put("hint",hint);put("removed",0);put("conditionField",conditionField);put("conditionValue",conditionValue);put("formula",formula);put("repeatable",if(repeatable)1 else 0)};writableDatabase.insert("template_fields",null,v)}
    fun moveTemplateField(templateId:String,id:String,delta:Int){val fields=templateFields(templateId,true);val index=fields.indexOfFirst{it.id==id};val target=index+delta;if(index<0||target !in fields.indices)return;val db=writableDatabase;db.beginTransaction();try{val a=ContentValues().apply{put("position",fields[target].position)};val b=ContentValues().apply{put("position",fields[index].position)};db.update("template_fields",a,"id=?",arrayOf(id));db.update("template_fields",b,"id=?",arrayOf(fields[target].id));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun updateTemplateField(id:String,name:String,type:String,required:Boolean,section:String,options:String,hint:String,conditionField:String="",conditionValue:String="",formula:String="",repeatable:Boolean=false){val v=ContentValues().apply{put("name",name);put("fieldType",type);put("required",if(required)1 else 0);put("section",section);put("options",options);put("hint",hint);put("conditionField",conditionField);put("conditionValue",conditionValue);put("formula",formula);put("repeatable",if(repeatable)1 else 0)};writableDatabase.update("template_fields",v,"id=?",arrayOf(id))}
    fun removeTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=1 WHERE id=?",arrayOf(id))}
    fun restoreTemplateField(id:String){writableDatabase.execSQL("UPDATE template_fields SET removed=0 WHERE id=?",arrayOf(id))}
    fun templateSections(templateId:String):List<String> = readableDatabase.rawQuery("SELECT name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(templateId)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun addTemplateSection(templateId:String,name:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("name",name);put("position",templateSections(templateId).size)};writableDatabase.insert("template_sections",null,v)}
    fun moveTemplateSection(templateId:String,name:String,delta:Int){val db=writableDatabase;val rows=mutableListOf<Pair<String,String>>();db.rawQuery("SELECT id,name FROM template_sections WHERE templateId=? ORDER BY position",arrayOf(templateId)).use{while(it.moveToNext())rows.add(it.getString(0) to it.getString(1))};val index=rows.indexOfFirst{it.second==name};val target=index+delta;if(index<0||target !in rows.indices)return;db.beginTransaction();try{val a=ContentValues().apply{put("position",target)};val b=ContentValues().apply{put("position",index)};db.update("template_sections",a,"id=?",arrayOf(rows[index].first));db.update("template_sections",b,"id=?",arrayOf(rows[target].first));db.setTransactionSuccessful()}finally{db.endTransaction()}}
    fun templateVersion(templateId:String){val snapshot=JSONArray();templateFields(templateId,true).forEach{f->snapshot.put(JSONObject().put("id",f.id).put("name",f.name).put("type",f.fieldType).put("required",f.required).put("section",f.section).put("options",f.options).put("hint",f.hint).put("conditionField",f.conditionField).put("conditionValue",f.conditionValue).put("formula",f.formula).put("repeatable",f.repeatable).put("position",f.position))};val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("templateId",templateId);put("snapshot",snapshot.toString());put("createdAt",System.currentTimeMillis())};writableDatabase.insert("template_versions",null,v)}
    fun templateVersionCount(templateId:String)=readableDatabase.rawQuery("SELECT COUNT(*) FROM template_versions WHERE templateId=?",arrayOf(templateId)).use{it.moveToFirst();it.getInt(0)}
    fun createWorldCalendar(worldId:String,name:String,months:String,weekdays:String,seasons:String,moons:String,notes:String="",monthLengths:String="",eras:String="",leapRules:String=""):String{val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("worldId",worldId);put("name",name);put("months",months);put("weekdays",weekdays);put("seasons",seasons);put("moons",moons);put("notes",notes);put("monthLengths",monthLengths);put("eras",eras);put("leapRules",leapRules)};writableDatabase.insert("world_calendars",null,v);return id}
    fun worldCalendars(worldId:String):List<Map<String,String>> = readableDatabase.rawQuery("SELECT id,name,COALESCE(months,''),COALESCE(weekdays,''),COALESCE(seasons,''),COALESCE(moons,''),COALESCE(notes,''),COALESCE(monthLengths,''),COALESCE(eras,''),COALESCE(leapRules,'') FROM world_calendars WHERE worldId=? ORDER BY name",arrayOf(worldId)).use{c->buildList{while(c.moveToNext())add(mapOf("id" to c.getString(0),"name" to c.getString(1),"months" to c.getString(2),"weekdays" to c.getString(3),"seasons" to c.getString(4),"moons" to c.getString(5),"notes" to c.getString(6),"monthLengths" to c.getString(7),"eras" to c.getString(8),"leapRules" to c.getString(9)))}}
    fun addCalendarEntry(calendarId:String,year:Int,month:Int,day:Int,title:String,description:String="",repeatYearly:Boolean=false){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("calendarId",calendarId);put("year",year);put("month",month);put("day",day);put("title",title);put("description",description);put("repeatYearly",if(repeatYearly)1 else 0)};writableDatabase.insert("calendar_entries",null,v)}
    fun calendarEntries(calendarId:String):List<CalendarEntry> = readableDatabase.rawQuery("SELECT id,year,month,day,title,COALESCE(description,''),repeatYearly FROM calendar_entries WHERE calendarId=? ORDER BY year,month,day",arrayOf(calendarId)).use{c->buildList{while(c.moveToNext())add(CalendarEntry(c.getString(0),c.getInt(1),c.getInt(2),c.getInt(3),c.getString(4),c.getString(5),c.getInt(6)==1))}}
    fun allTags():List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT id,name,tagType FROM tags ORDER BY tagType,name",null).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun createTag(name:String,type:String="General",color:String=""):String{val existing=readableDatabase.rawQuery("SELECT id FROM tags WHERE lower(name)=lower(?) AND tagType=?",arrayOf(name.trim(),type)).use{if(it.moveToFirst())it.getString(0)else null};if(existing!=null)return existing;val id=UUID.randomUUID().toString();val v=ContentValues().apply{put("id",id);put("name",name.trim());put("tagType",type);put("color",color)};writableDatabase.insert("tags",null,v);return id}
    fun assignTag(recordId:String,tagId:String){val v=ContentValues().apply{put("recordId",recordId);put("tagId",tagId)};writableDatabase.insertWithOnConflict("record_tags",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun removeTag(recordId:String,tagId:String){writableDatabase.delete("record_tags","recordId=? AND tagId=?",arrayOf(recordId,tagId))}
    fun tagsForRecord(recordId:String):Set<String> = readableDatabase.rawQuery("SELECT tagId FROM record_tags WHERE recordId=?",arrayOf(recordId)).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
    fun renameTag(id:String,name:String){val v=ContentValues().apply{put("name",name.trim())};writableDatabase.update("tags",v,"id=?",arrayOf(id))}
    fun addTagAlias(tagId:String,alias:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("tagId",tagId);put("alias",alias.trim())};writableDatabase.insertWithOnConflict("tag_aliases",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun shareWithProject(recordId:String,projectId:String){val v=ContentValues().apply{put("recordId",recordId);put("projectId",projectId);put("createdAt",System.currentTimeMillis())};writableDatabase.insertWithOnConflict("record_shares",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun unshareFromProject(recordId:String,projectId:String){writableDatabase.delete("record_shares","recordId=? AND projectId=?",arrayOf(recordId,projectId))}
    fun sharedProjects(recordId:String):Set<String> = readableDatabase.rawQuery("SELECT projectId FROM record_shares WHERE recordId=?",arrayOf(recordId)).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}}
    fun addRelationship(source:String,target:String,label:String,notes:String=""){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("sourceId",source);put("targetId",target);put("label",label);put("notes",notes);put("createdAt",System.currentTimeMillis())};writableDatabase.insertWithOnConflict("record_relationships",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
    fun relationships(id:String):List<Triple<String,String,String>> = readableDatabase.rawQuery("SELECT r.title,rr.label,rr.targetId FROM record_relationships rr JOIN records r ON r.id=rr.targetId WHERE rr.sourceId=? UNION ALL SELECT r.title,rr.label,rr.sourceId FROM record_relationships rr JOIN records r ON r.id=rr.sourceId WHERE rr.targetId=?",arrayOf(id,id)).use{c->buildList{while(c.moveToNext())add(Triple(c.getString(0),c.getString(1),c.getString(2)))}}
    fun saveEntitySnapshot(record:InkRecord,label:String){val payload=JSONObject().put("title",record.title).put("body",record.body).put("kind",record.kind).put("tags",record.tags).put("aliases",record.aliases).toString();val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("recordId",record.id);put("label",label);put("payload",payload);put("createdAt",System.currentTimeMillis())};writableDatabase.insert("entity_snapshots",null,v)}
    fun entitySnapshots(id:String):List<Pair<String,String>> = readableDatabase.rawQuery("SELECT label,payload FROM entity_snapshots WHERE recordId=? ORDER BY createdAt DESC",arrayOf(id)).use{c->buildList{while(c.moveToNext())add(c.getString(0) to c.getString(1))}}
    fun restoreEntitySnapshot(id:String,payload:String){val o=JSONObject(payload);get(id)?.let{save(it.copy(title=o.optString("title",it.title),body=o.optString("body",it.body),tags=o.optString("tags",it.tags),aliases=o.optString("aliases",it.aliases)))}}
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
    fun recordsForRevision(id:String)=revisions(id)
}
