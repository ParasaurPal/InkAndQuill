package com.parasaurpal.inkandquill.data

data class FieldSpec(
    val name: String,
    val type: String = "Long text",
    val section: String = "Details",
    val required: Boolean = false,
    val options: String = "",
    val hint: String = "",
    val conditionField: String = "",
    val conditionValue: String = "",
    val formula: String = "",
    val repeatable: Boolean = false
)

data class TemplateSpec(
    val slug: String,
    val title: String,
    val category: String,
    val defaultKind: String,
    val description: String,
    val sections: List<String>,
    val fields: List<FieldSpec>
)

object FantasyTemplateCatalog {
    val categories = listOf(
        "All",
        "Characters & Lineage",
        "Flora & Ecology",
        "Fauna & Monsters",
        "Locations & Realms",
        "Items & Artifacts",
        "Languages & Scripts",
        "Cultures & Societies",
        "Magic & Systems",
        "Belief & Myth",
        "Time & History",
        "Plot & Writing",
        "Research & Reference",
        "Other"
    )

    val allSpecs: List<TemplateSpec> by lazy {
        fantasySpecsPartA + fantasySpecsPartB + fantasySpecsPartC + fantasySpecsPartD + writingAndLegacySpecs
    }

    val starterRecordKinds: List<String> = listOf(
        "All",
        "Document",
        "Novel",
        "Series",
        "Story",
        "Book",
        "Chapter",
        "Scene",
        "Poem",
        "Journal",
        "Note",
        "World",
        "Character",
        "NPC",
        "Dynasty",
        "Lineage",
        "Flora",
        "Herb",
        "Fungus",
        "Biome",
        "Fauna",
        "Monster",
        "Species",
        "Dragon",
        "Location",
        "Region",
        "Kingdom",
        "Settlement",
        "Dungeon",
        "Stronghold",
        "Realm",
        "Item",
        "Artifact",
        "Weapon",
        "Potion",
        "Material",
        "Vessel",
        "Language",
        "Script",
        "Culture",
        "Faction",
        "Festival",
        "Economy",
        "Government",
        "Military",
        "Magic System",
        "Spell",
        "Technology",
        "Affliction",
        "Religion",
        "Deity",
        "Myth",
        "Prophecy",
        "Celestial",
        "Calendar",
        "Timeline Event",
        "Quest",
        "Research Note"
    )
}
