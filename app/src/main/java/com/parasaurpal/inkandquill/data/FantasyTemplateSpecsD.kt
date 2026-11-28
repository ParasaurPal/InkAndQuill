package com.parasaurpal.inkandquill.data

private fun parityFields(section: String, vararg names: String): List<FieldSpec> =
    names.mapIndexed { index, name ->
        val type = when {
            name.contains("Image", true) || name.contains("Portrait", true) || name.contains("Map", true) || name.contains("Diagram", true) || name.contains("Cover", true) -> "Image"
            name.contains("Gallery", true) -> "Gallery"
            name.contains("Related", true) || name.contains("Members", true) || name.contains("People", true) || name.contains("Character", true) || name.contains("Sources", true) || name.contains("Locations", true) || name.contains("Events", true) || name.contains("Parent", true) || name.contains("Child", true) || name.contains("Linked", true) -> "Relationship"
            name.contains("Table", true) || name.contains("Entries", true) || name.contains("Stages", true) || name.contains("Milestones", true) || name.contains("Branches", true) || name.contains("Examples", true) || name.contains("Rules", true) -> "Table"
            else -> "Long text"
        }
        FieldSpec(name, type, section, required = index == 0 && name.contains("Name", true))
    }

private fun paritySpec(slug: String, title: String, category: String, kind: String, description: String,
    sections: List<Pair<String, List<FieldSpec>>>) =
    TemplateSpec(slug, title, category, kind, description, sections.map { it.first }, sections.flatMap { it.second })

internal val fantasySpecsPartD: List<TemplateSpec> = listOf(
    paritySpec("research-source", "Research Source & Bibliography", "Research & Reference", "Research Note",
        "A source record for books, articles, archives, interviews, websites and primary documents, with reliability and citation details.",
        listOf(
            "Source identity" to parityFields("Source identity", "Source Name", "Author / Creator", "Publication Title", "Source Type", "Cover / Source Image", "Publication Date", "Publisher / Institution", "Edition, Volume and Pages", "Stable URL / Identifier"),
            "Evaluation & evidence" to parityFields("Evaluation & evidence", "Central Subject and Research Question", "Summary and Key Findings", "Primary or Secondary Evidence", "Reliability and Perspective", "Bias, Limitations and Disputes", "Quoted Passages and Page References"),
            "Project use" to parityFields("Project use", "Relevant Characters, Places and Topics", "Related Research Notes", "Facts Adapted into the Setting", "Citation Format", "Access Date and Research Status")
        )),
    paritySpec("research-annotation", "Research Annotation & Field Note", "Research & Reference", "Research Note",
        "An annotated reading or field observation linked to its source, question, evidence and creative application.",
        listOf(
            "Observation" to parityFields("Observation", "Annotation Name", "Research Source", "Date Observed / Read", "Page, Timestamp or Location", "Image / Scan"),
            "Evidence & interpretation" to parityFields("Evidence & interpretation", "Exact Observation or Quotation", "Context and Method", "Interpretation", "Confidence and Uncertainty", "Questions to Verify"),
            "Application" to parityFields("Application", "Related Research Questions", "Related World Records", "Possible Story Applications", "Follow-up Tasks", "Tags and Collections")
        )),
    paritySpec("philosophy", "Philosophy & School of Thought", "Belief & Myth", "Philosophy",
        "A structured account of a worldview, ethical tradition or school of thought, including its arguments, thinkers and historical influence.",
        listOf(
            "Identity & foundations" to parityFields("Identity & foundations", "Philosophy Name", "Tradition / School", "Founders and Major Thinkers", "Historical Context", "Core Questions and First Principles", "Emblem / Portrait"),
            "Ideas & arguments" to parityFields("Ideas & arguments", "Metaphysics and View of Reality", "Epistemology and Theory of Knowledge", "Ethics and Moral Duties", "Political and Social Philosophy", "Key Arguments and Thought Experiments", "Texts and Canonical Works"),
            "Debate & influence" to parityFields("Debate & influence", "Internal Branches and Interpretations", "Critics and Counterarguments", "Contradictions and Unresolved Problems", "Historical Influence", "Related Religions and Cultures", "Modern Practice and Legacy")
        )),
    paritySpec("character-arc", "Character Arc", "Plot & Writing", "Character",
        "Track one character's internal and external transformation across scenes, chapters or an entire series.",
        listOf(
            "Arc premise" to parityFields("Arc premise", "Arc Name", "Central Character", "Arc Type", "Starting Worldview and Emotional State", "Desire, Need and Misbelief", "Triggering Event"),
            "Progression" to parityFields("Progression", "Resistance and First Choices", "Escalations and Turning Points", "Relationships that Challenge the Character", "Midpoint Revelation", "Setbacks and Cost", "Crisis and Climax"),
            "Resolution & continuity" to parityFields("Resolution & continuity", "Final Choice and Changed Belief", "Ending State", "Key Scenes and Milestones", "Parallel or Conflicting Arcs", "Unresolved Threads", "Arc Status")
        )),
    paritySpec("story-arc", "Story Arc & Plot Thread", "Plot & Writing", "Story",
        "Plan a major plotline, subplot or multi-book thread with beats, stakes, dependencies and resolution.",
        listOf(
            "Arc definition" to parityFields("Arc definition", "Story Arc Name", "Arc Type", "Premise and Dramatic Question", "POV Characters", "Central Conflict", "Stakes and Deadline"),
            "Plot structure" to parityFields("Plot structure", "Inciting Incident", "Major Beats and Milestones", "Reversals and Revelations", "Climax and Resolution", "Scenes and Chapters", "Timeline Events"),
            "Threads & outcomes" to parityFields("Threads & outcomes", "Related Character Arcs", "Related Subplots", "Foreshadowing and Payoffs", "Open Questions", "Continuity Dependencies", "Arc Status")
        )),
    paritySpec("relationship-web", "Relationship Web", "Characters & Lineage", "Document",
        "Map a network of people, groups and institutions with typed, directional and time-aware connections.",
        listOf(
            "Network identity" to parityFields("Network identity", "Network Name", "Central Person / Group", "Network Purpose", "Diagram / Web Image", "Scope and Time Period"),
            "Connections" to parityFields("Connections", "People and Organizations Included", "Connection Table", "Relationship Types and Labels", "Direction, Strength and Public Visibility", "Alliances, Rivalries and Dependencies"),
            "History & interpretation" to parityFields("History & interpretation", "Changes Over Time", "Secrets and Conflicting Accounts", "Key Events that Changed Connections", "Related Character Records", "Notes and Sources")
        )),
    paritySpec("family-tree", "Family Tree & Genealogy", "Characters & Lineage", "Document",
        "Record ancestry, descendants, marriages, adoption, succession and uncertain or disputed parentage.",
        listOf(
            "Household identity" to parityFields("Household identity", "Family / Dynasty Name", "House Sigil / Tree Diagram", "Founding Ancestor", "Home Region and Cultural Context", "Genealogical Scope"),
            "Lineage records" to parityFields("Lineage records", "Ancestor and Descendant Records", "Parentage and Guardianship", "Marriages, Partnerships and Adoption", "Siblings and Collateral Branches", "Generations and Dates"),
            "Inheritance & uncertainty" to parityFields("Inheritance & uncertainty", "Titles, Inheritance and Succession", "Legitimacy and Lineal Claims", "Unknown or Disputed Parentage", "Family Events and Turning Points", "Related Characters and Sources")
        )),
    paritySpec("language-dictionary", "Constructed Language Dictionary", "Languages & Scripts", "Language",
        "A searchable lexicon for an invented language with pronunciation, morphology, usage, semantic fields and etymology.",
        listOf(
            "Language context" to parityFields("Language context", "Dictionary Name", "Language", "Dialect / Register", "Writing System", "Pronunciation Key and Phonology"),
            "Lexicon" to parityFields("Lexicon", "Dictionary Entries Table", "Headword and Part of Speech", "Pronunciation and IPA", "Definition and Semantic Range", "Inflected and Derived Forms", "Example Sentences and Translation"),
            "Usage & history" to parityFields("Usage & history", "Etymology and Word Formation", "Synonyms, Antonyms and Related Words", "Idioms and Fixed Expressions", "Regional and Historical Variants", "Loanwords and Cultural Notes")
        )),
    paritySpec("script-system", "Writing System & Script", "Languages & Scripts", "Script",
        "Design an alphabet, syllabary, abugida, logographic or mixed writing system with glyphs and orthographic rules.",
        listOf(
            "Script identity" to parityFields("Script identity", "Script Name", "Associated Language", "Writing System Type", "Historical Origin and Users", "Script Specimen / Chart"),
            "Glyph inventory" to parityFields("Glyph inventory", "Glyph Inventory Table", "Character Names and Transliteration", "Sound / Meaning Values", "Glyph Variants and Ligatures", "Numerals and Punctuation"),
            "Orthography & use" to parityFields("Orthography & use", "Writing Direction and Layout", "Spelling and Phonotactic Rules", "Diacritics and Combining Marks", "Handwriting and Typography", "Historical Reforms and Regional Styles")
        )),
    paritySpec("map-atlas", "Map & Atlas Entry", "Locations & Realms", "Location",
        "An atlas record for a map image, its projection, scale, layers, annotations and linked geographic records.",
        listOf(
            "Map identity" to parityFields("Map identity", "Map Name", "Map Type and Geographic Scope", "Primary Map Image", "Parent Map / Atlas", "Creator and In-World Date"),
            "Cartographic details" to parityFields("Cartographic details", "Projection, Orientation and Scale", "Legend and Symbol Key", "Coordinate Grid and Measurements", "Layers, Zones and Boundaries", "Routes and Travel Distances"),
            "Annotations & links" to parityFields("Annotations & links", "Map Pins and Annotations", "Linked Locations and Regions", "Changes Across Historical Versions", "Accuracy, Unknown Areas and Bias", "Related Maps and Sources")
        )),
    paritySpec("system-diagram", "System & Process Diagram", "Magic & Systems", "Document",
        "Describe a rule-based system or process with components, inputs, outputs, dependencies, exceptions and a visual diagram.",
        listOf(
            "System identity" to parityFields("System identity", "System Name", "System Type", "Purpose and Scope", "System Diagram", "Version and Status"),
            "Components & rules" to parityFields("Components & rules", "Components and Responsibilities", "Inputs, Outputs and Resources", "Rules, Constraints and Invariants", "Dependencies and Feedback Loops", "States and Transitions"),
            "Operation & failure" to parityFields("Operation & failure", "Step-by-Step Process", "Exceptions and Edge Cases", "Failure Modes and Recovery", "Security, Costs and Risks", "Related Systems and Records")
        )),
    paritySpec("calendar-observance", "Holiday, Festival & Observance", "Time & History", "Calendar",
        "A recurring or one-time observance with calendar rules, traditions, rituals, participants and historical changes.",
        listOf(
            "Observance identity" to parityFields("Observance identity", "Observance Name", "Observance Type", "Associated Calendar", "Date, Season or Recurrence Rule", "Related Culture / Religion", "Festival Image"),
            "Traditions & activities" to parityFields("Traditions & activities", "Origin and Historical Meaning", "Stories and Symbolism", "Rituals, Foods and Customs", "Participants and Roles", "Locations and Processions", "Preparation and Prohibitions"),
            "Modern practice" to parityFields("Modern practice", "Regional Variations", "Public / Private Status", "Changes, Bans and Revivals", "Related Timeline Events", "Sources and Notes")
        ))
    paritySpec("culture-profile", "Culture & Society Profile", "Cultures & Societies", "Culture",
        "A detailed cultural dossier covering identity, values, institutions, daily life and change.",
        listOf(
            "Identity & worldview" to parityFields("Identity & worldview", "Culture Name", "Endonyms and Exonyms", "Region and Environment", "Cultural Emblem / Image", "Origin and Founding Narratives", "Core Values and Worldview"),
            "People & social life" to parityFields("People & social life", "Population and Communities", "Languages and Dialects", "Family and Kinship Structures", "Social Classes and Roles", "Hospitality and Etiquette", "Daily Life and Life Stages"),
            "Traditions & change" to parityFields("Traditions & change", "Food, Clothing and Architecture", "Arts, Music and Storytelling", "Rites, Festivals and Holidays", "Religion and Philosophy", "Internal Differences and Subcultures", "Historical Changes and External Influences")
        )),
    paritySpec("government-system", "Government & Political System", "Cultures & Societies", "Government",
        "A practical reference for political institutions, authority, law, administration and civic participation.",
        listOf(
            "Structure & legitimacy" to parityFields("Structure & legitimacy", "Government Name", "Government Type", "Constitution / Founding Charter", "Source of Legitimacy", "Head of State and Executive", "Territorial Scope"),
            "Institutions & law" to parityFields("Institutions & law", "Legislative Bodies and Process", "Courts and Legal Traditions", "Administrative Departments", "Local and Regional Government", "Rights, Duties and Citizenship", "Taxation and Public Finance"),
            "Power & accountability" to parityFields("Power & accountability", "Elections and Succession", "Political Parties and Factions", "Checks, Oversight and Accountability", "Corruption and Abuse Risks", "Foreign Relations and Treaties", "Reforms and Constitutional Crises")
        )),
    paritySpec("military-force", "Military & Armed Forces", "Cultures & Societies", "Military",
        "An organizational profile for armed forces, command, doctrine, logistics, equipment and campaigns.",
        listOf(
            "Identity & command" to parityFields("Identity & command", "Force Name", "Branch and Service Type", "Emblem / Uniform Image", "Founding and Historical Context", "Commander and Chain of Command", "Recruitment and Personnel"),
            "Doctrine & capability" to parityFields("Doctrine & capability", "Strategic Doctrine and Objectives", "Training and Discipline", "Units, Ranks and Specializations", "Weapons, Armor and Equipment", "Magic / Technology Integration", "Intelligence and Communications"),
            "Operations & support" to parityFields("Operations & support", "Logistics, Supply and Medical Support", "Bases, Fortifications and Fleet", "Allies and Rival Forces", "Campaigns and Battle History", "Rules of Engagement and Ethics", "Current Readiness and Weaknesses")
        )),
    paritySpec("economy-system", "Economy & Trade Network", "Cultures & Societies", "Economy",
        "A setting-level economic reference covering production, labor, exchange, finance and inequality.",
        listOf(
            "Economic identity" to parityFields("Economic identity", "Economy Name", "Economic Model", "Currency and Units of Value", "Geographic Scope", "Historical Development", "Key Economic Institutions"),
            "Production & exchange" to parityFields("Production & exchange", "Agriculture and Food Systems", "Industries, Crafts and Services", "Natural Resources and Extraction", "Trade Routes and Trading Partners", "Markets, Guilds and Merchant Groups", "Imports, Exports and Supply Chains"),
            "People & stability" to parityFields("People & stability", "Labor, Wages and Working Conditions", "Wealth Distribution and Social Mobility", "Taxation, Debt and Public Spending", "Banking, Credit and Investment", "Shortages, Inflation and Crises", "Informal Economy and Black Markets")
        )),
    paritySpec("cuisine-tradition", "Cuisine & Food Culture", "Cultures & Societies", "Culture",
        "A culinary reference for ingredients, techniques, meals, etiquette and regional food traditions.",
        listOf(
            "Food identity" to parityFields("Food identity", "Cuisine Name", "Associated Culture and Region", "Historical Influences", "Staple Ingredients", "Climate and Seasonal Availability", "Food Image Gallery"),
            "Methods & dishes" to parityFields("Methods & dishes", "Cooking Methods and Equipment", "Signature Dishes and Recipes", "Preservation and Fermentation", "Meals and Daily Eating Patterns", "Drinks, Sweets and Street Food", "Dietary Restrictions and Taboos"),
            "Social meaning" to parityFields("Social meaning", "Hospitality and Table Etiquette", "Feasts, Ceremonial Foods and Offerings", "Trade, Scarcity and Food Security", "Regional and Class Differences", "Related Festivals and Traditions", "Sources and Inspiration")
        )),
    paritySpec("clothing-tradition", "Clothing & Dress Tradition", "Cultures & Societies", "Culture",
        "A costume and material-culture record for garments, textiles, adornment, status and practical dress.",
        listOf(
            "Dress identity" to parityFields("Dress identity", "Tradition Name", "Associated Culture and Region", "Historical Period", "Reference Gallery", "Climate and Practical Needs", "Materials and Textile Sources"),
            "Garments & craft" to parityFields("Garments & craft", "Everyday Garments and Layers", "Formal, Ceremonial and Sacred Dress", "Footwear, Headwear and Accessories", "Textile Production and Dyeing", "Construction, Fasteners and Patterns", "Care, Repair and Reuse"),
            "Meaning & variation" to parityFields("Meaning & variation", "Status, Profession and Rank Signals", "Gender, Age and Life-Stage Conventions", "Mourning, Marriage and Rites of Passage", "Uniforms, Insignia and Regalia", "Regional and Historical Variations", "Cultural Rules and Controversies")
        )),
    paritySpec("biome-ecosystem", "Biome & Ecosystem", "Flora & Ecology", "Biome",
        "An ecological profile of a habitat system, its climate, communities, food webs and environmental pressures.",
        listOf(
            "Environment" to parityFields("Environment", "Biome Name", "Biome Type", "Map / Landscape Image", "Geographic Range", "Climate and Seasonal Patterns", "Terrain, Soil and Water"),
            "Ecological networks" to parityFields("Ecological networks", "Dominant Flora and Vegetation", "Characteristic Fauna", "Food Web and Trophic Levels", "Keystone and Indicator Species", "Pollination and Decomposition", "Migration and Seasonal Cycles"),
            "Change & stewardship" to parityFields("Change & stewardship", "Natural Disturbances and Succession", "Diseases, Invasive Species and Threats", "Human Use and Settlement Pressure", "Conservation and Management", "Regional Variants and Microhabitats", "Unknowns and Research Notes")
        )),
    paritySpec("dungeon-site", "Dungeon & Subterranean Site", "Locations & Realms", "Location",
        "A layered site dossier for ruins, caverns, vaults and dangerous underground complexes.",
        listOf(
            "Site identity" to parityFields("Site identity", "Site Name", "Site Type and Purpose", "Entrance Image / Site Map", "Location and Access Points", "Creator, Builders or Natural Origin", "Age and Historical Context"),
            "Layout & hazards" to parityFields("Layout & hazards", "Levels, Rooms and Connected Areas", "Passages, Vertical Shafts and Routes", "Traps, Locks and Security Systems", "Environmental Hazards and Conditions", "Defenders, Creatures and Occupants", "Secrets, Hidden Areas and Mechanisms"),
            "Exploration & lore" to parityFields("Exploration & lore", "Treasures, Relics and Resources", "Puzzles, Clues and Challenges", "Historical Events and Previous Expeditions", "Current Control and Factions", "Known Routes and Exit Conditions", "Related Maps, People and Sources")
        )),
    paritySpec("temple-site", "Temple, Shrine & Sacred Site", "Belief & Myth", "Location",
        "A sacred-place record connecting architecture, ritual use, religious authority and pilgrimage.",
        listOf(
            "Sacred identity" to parityFields("Sacred identity", "Site Name", "Sacred Tradition and Patron", "Site Type and Status", "Exterior / Interior Image", "Location and Founding Date", "Origin Story and Consecration"),
            "Architecture & service" to parityFields("Architecture & service", "Layout, Orientation and Sacred Geometry", "Altars, Relics and Sacred Objects", "Priesthood and Resident Community", "Ritual Schedule and Ceremonies", "Pilgrimage, Access and Restrictions", "Maintenance and Funding"),
            "Meaning & history" to parityFields("Meaning & history", "Doctrines and Symbolic Meaning", "Miracles, Omens and Reported Events", "Historical Conflicts and Changes", "Related Deities, Figures and Festivals", "Current Condition and Threats", "Sources and Oral Traditions")
        )),
    paritySpec("quest-brief", "Quest & Adventure Brief", "Plot & Writing", "Quest",
        "A playable or narrative quest plan with objectives, stakes, steps, branching outcomes and rewards.",
        listOf(
            "Quest premise" to parityFields("Quest premise", "Quest Name", "Quest Type", "Quest Giver and Key Participants", "Hook and Initial Situation", "Primary Objective", "Stakes, Deadline and Failure Cost"),
            "Progress & obstacles" to parityFields("Progress & obstacles", "Required Steps and Milestones", "Locations and Travel Route", "Clues, Leads and Required Knowledge", "Opposition, Hazards and Complications", "Choices and Branching Paths", "Resources, Allies and Requirements"),
            "Outcome & continuity" to parityFields("Outcome & continuity", "Possible Endings and Consequences", "Rewards, Costs and Discoveries", "Related Character and Story Arcs", "Timeline Events and Scene Links", "Open Threads and Follow-up Hooks", "Quest Status")
        ))
)
