package com.parasaurpal.inkandquill.data

class LocalLinkEngine(private val db:InkDatabase){
    data class Found(val record:InkRecord,val start:Int,val end:Int)
    fun findMentions(text:String,sourceId:String):List<Found>{
        val includeAliases=db.setting("matchAliases","true")=="true"
        val candidates=db.all().filter{it.id!=sourceId}.flatMap{r->(listOf(r.title)+(if(includeAliases)r.aliases.split(",").map(String::trim).filter(String::isNotBlank)else emptyList())).distinct().map{it to r}}
        val ambiguous=candidates.groupBy{it.first.lowercase()}.filterValues{values->values.map{it.second.id}.distinct().size>1}.keys
        val stopWords=setOf("home","main","world","story","book","chapter","scene","character","place","thing","people","time","day","night","name","other","general","notes","history")
        val unique=candidates.filter{(name,record)->name.length>=3&&name.lowercase() !in ambiguous&&name.lowercase() !in stopWords&&!db.isLinkExcluded(sourceId,name)}.distinctBy{it.first.lowercase()}
        val used=mutableListOf<IntRange>();val found=mutableListOf<Found>()
        unique.sortedByDescending{it.first.length}.forEach{(name,r)->val flags=if(db.setting("caseSensitive","false")=="true")emptySet() else setOf(RegexOption.IGNORE_CASE);val regex=Regex("(?<![\\p{L}\\p{N}_])"+Regex.escape(name)+"(?![\\p{L}\\p{N}_])",flags);regex.findAll(text).forEach{m->if(used.none{it.first<=m.range.last&&m.range.first<=it.last}){used.add(m.range);found.add(Found(r,m.range.first,m.range.last+1))}}}
        return found.sortedBy{it.start}
    }
    fun scan(source:InkRecord):List<Found>{val matches=findMentions(source.body,source.id);when(db.setting("autoLink","suggest")){"auto"->matches.forEach{db.addLink(source.id,it.record.id,"mention",it.start)};"suggest"->matches.forEach{db.addLinkSuggestion(source.id,it.record.id,it.record.title,it.start)}};return matches}
}
