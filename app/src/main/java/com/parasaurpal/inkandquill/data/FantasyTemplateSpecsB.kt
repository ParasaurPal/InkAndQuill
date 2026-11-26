package com.parasaurpal.inkandquill.data

internal val fantasySpecsPartB: List<TemplateSpec> = listOf(
    // ── LANGUAGES & SCRIPTS ─────────────────────────────────────────────────
    TemplateSpec(
        slug = "language",
        title = "Constructed Language (Conlang)",
        category = "Languages & Scripts",
        defaultKind = "Language",
        description = "Full linguistic blueprint covering phonology, grammar, script, lexicon, idioms, and dialects.",
        sections = listOf("Overview & Speakers", "Phonology & Sound Aesthetic", "Morphology & Grammar", "Syntax & Pragmatics", "Writing System & Script", "Core Lexicon & Numbers", "Idioms, Honorifics & History"),
        fields = listOf(
            FieldSpec("Language Name", "Short text", "Overview & Speakers", required = true),
            FieldSpec("Endonym (Native Name)", "Short text", "Overview & Speakers"),
            FieldSpec("Language Family / Ancestry", "Short text", "Overview & Speakers"),
            FieldSpec("Primary Speakers & Regions", "Relationship", "Overview & Speakers"),
            FieldSpec("Consonant & Vowel Inventory", "Long text", "Phonology & Sound Aesthetic", hint = "Distinctive sounds, clicks, aspirates, forbidden clusters"),
            FieldSpec("Syllable Structure & Stress", "Short text", "Phonology & Sound Aesthetic", hint = "e.g., (C)V(C), penultimate stress"),
            FieldSpec("Tonal or Pitch System?", "Checkbox", "Phonology & Sound Aesthetic"),
            FieldSpec("Tone Contour Rules", "Long text", "Phonology & Sound Aesthetic", conditionField = "Tonal or Pitch System?", conditionValue = "true"),
            FieldSpec("Sound Aesthetic & Flow", "Long text", "Phonology & Sound Aesthetic"),
            FieldSpec("Word Order", "Dropdown", "Syntax & Pragmatics", options = "SVO,SOV,VSO,VOS,OSV,OVS,Free / Case-Marked"),
            FieldSpec("Morphology Type", "Dropdown", "Morphology & Grammar", options = "Agglutinative,Fusional,Isolating,Polysynthetic,Root-and-Pattern"),
            FieldSpec("Noun Classes, Cases & Gender", "Long text", "Morphology & Grammar"),
            FieldSpec("Verb Conjugation, Tense & Aspect", "Long text", "Morphology & Grammar"),
            FieldSpec("Pronouns & Honorific Registers", "Long text", "Syntax & Pragmatics"),
            FieldSpec("Linked Writing System", "Relationship", "Writing System & Script"),
            FieldSpec("Script Specimen Image", "Image", "Writing System & Script"),
            FieldSpec("Base Number System", "Dropdown", "Core Lexicon & Numbers", options = "Base 10,Base 12 (Duodecimal),Base 8 (Octal),Base 20 (Vigesimal),Base 6,Other"),
            FieldSpec("Core Vocabulary & Roots", "Table", "Core Lexicon & Numbers", hint = "Word | IPA / Pronunciation | Part of Speech | Meaning"),
            FieldSpec("Common Greetings, Oaths & Idioms", "Repeatable group", "Idioms, Honorifics & History", repeatable = true),
            FieldSpec("Dialects, Slang & Loanwords", "Long text", "Idioms, Honorifics & History")
        )
    ),
    TemplateSpec(
        slug = "script",
        title = "Writing System & Script",
        category = "Languages & Scripts",
        defaultKind = "Script",
        description = "Alphabet, syllabary, logography, knot-cords, or rune-carving systems.",
        sections = listOf("Script Identity & Medium", "Glyphs & Mechanics", "Calligraphy & Secrecy"),
        fields = listOf(
            FieldSpec("Script Name", "Short text", "Script Identity & Medium", required = true),
            FieldSpec("System Type", "Dropdown", "Script Identity & Medium", options = "Alphabet,Abjad,Abugida,Syllabary,Logographic,Runic,Tactile/Knot-Script,Magical Sigilry"),
            FieldSpec("Directionality", "Dropdown", "Script Identity & Medium", options = "Left-to-Right,Right-to-Left,Top-to-Bottom,Boustrophedon,Radial / Spiral"),
            FieldSpec("Primary Writing Medium & Tools", "Long text", "Script Identity & Medium", hint = "Vellum, clay tablets, carved bone, ink-brushes"),
            FieldSpec("Glyph Chart Image", "Image", "Glyphs & Mechanics"),
            FieldSpec("Glyph & Sound Table", "Table", "Glyphs & Mechanics", hint = "Glyph / Name | Sound | Origin / Pictograph"),
            FieldSpec("Numerals & Punctuation", "Long text", "Glyphs & Mechanics"),
            FieldSpec("Literacy Rate & Social Prestige", "Long text", "Calligraphy & Secrecy"),
            FieldSpec("Sacred Variants or Secret Ciphers", "Long text", "Calligraphy & Secrecy")
        )
    ),
    TemplateSpec(
        slug = "naming",
        title = "Naming Convention & Onomastics",
        category = "Languages & Scripts",
        defaultKind = "Language",
        description = "Keep character and place names culturally consistent with patterns, prefixes, and taboos.",
        sections = listOf("Culture & Language", "Personal Names", "Place & Clan Roots", "Taboos & Rites"),
        fields = listOf(
            FieldSpec("Convention Title", "Short text", "Culture & Language", required = true),
            FieldSpec("Culture / Species", "Relationship", "Culture & Language"),
            FieldSpec("Given Name Roots & Examples", "Repeatable group", "Personal Names", repeatable = true),
            FieldSpec("Family, Patronymic & Clan Rules", "Long text", "Personal Names"),
            FieldSpec("Earned Titles & Epithets", "Long text", "Personal Names"),
            FieldSpec("Toponym (Place-Name) Prefixes & Suffixes", "Table", "Place & Clan Roots", hint = "Element | Meaning | Example Place"),
            FieldSpec("Name Changes, True Names & Taboos", "Long text", "Taboos & Rites")
        )
    ),
    TemplateSpec(
        slug = "glossary",
        title = "Worldbuilding Lexicon & Glossary",
        category = "Languages & Scripts",
        defaultKind = "Language",
        description = "Living dictionary entry for in-world slang, titles, proverbs, and untranslatable terms.",
        sections = listOf("Term & Pronunciation", "Meaning & Etymology", "Usage"),
        fields = listOf(
            FieldSpec("Term / Phrase", "Short text", "Term & Pronunciation", required = true),
            FieldSpec("Pronunciation", "Short text", "Term & Pronunciation"),
            FieldSpec("Source Language", "Relationship", "Term & Pronunciation"),
            FieldSpec("Part of Speech / Category", "Dropdown", "Term & Pronunciation", options = "Noun,Verb,Adjective,Title/Honorific,Idiom/Proverb,Curse/Oath,Unit/Measure"),
            FieldSpec("Literal & Idiomatic Definition", "Long text", "Meaning & Etymology"),
            FieldSpec("Etymology & Cultural Context", "Long text", "Meaning & Etymology"),
            FieldSpec("Example Sentences", "Repeatable group", "Usage", repeatable = true)
        )
    ),

    // ── CULTURES & SOCIETIES ────────────────────────────────────────────────
    TemplateSpec(
        slug = "culture",
        title = "Culture & Society",
        category = "Cultures & Societies",
        defaultKind = "Culture",
        description = "Comprehensive anthropological portrait: ethos, kinship, etiquette, food, fashion, arts, and justice.",
        sections = listOf("Core Ethos & Worldview", "Social Structure & Kinship", "Customs, Etiquette & Taboos", "Material Culture & Arts", "Law, Faith & Language"),
        fields = listOf(
            FieldSpec("Culture Name", "Short text", "Core Ethos & Worldview", required = true),
            FieldSpec("Primary Homeland / Region", "Relationship", "Core Ethos & Worldview"),
            FieldSpec("Cultural Motto or Defining Virtue", "Long text", "Core Ethos & Worldview"),
            FieldSpec("Attitude Toward Outsiders & Magic", "Long text", "Core Ethos & Worldview"),
            FieldSpec("Class & Caste Stratification", "Long text", "Social Structure & Kinship"),
            FieldSpec("Family, Marriage & Inheritance", "Long text", "Social Structure & Kinship"),
            FieldSpec("Coming-of-Age & Funerary Rites", "Long text", "Social Structure & Kinship"),
            FieldSpec("Hospitality Rules & Everyday Etiquette", "Long text", "Customs, Etiquette & Taboos"),
            FieldSpec("Strict Cultural Taboos", "Repeatable group", "Customs, Etiquette & Taboos", repeatable = true),
            FieldSpec("Staple Cuisine & Dining Customs", "Long text", "Material Culture & Arts"),
            FieldSpec("Clothing, Hair & Adornment", "Long text", "Material Culture & Arts"),
            FieldSpec("Architecture, Music & Visual Arts", "Long text", "Material Culture & Arts"),
            FieldSpec("Visual Moodboard", "Gallery", "Material Culture & Arts"),
            FieldSpec("Dominant Languages", "Relationship", "Law, Faith & Language"),
            FieldSpec("Dominant Faiths & Philosophies", "Relationship", "Law, Faith & Language"),
            FieldSpec("Conflict Resolution & Justice", "Long text", "Law, Faith & Language")
        )
    ),
    TemplateSpec(
        slug = "faction",
        title = "Faction, Guild & Knightly Order",
        category = "Cultures & Societies",
        defaultKind = "Faction",
        description = "Track guilds, monastic orders, merchant syndicates, rebel cells, and knightly brotherhoods.",
        sections = listOf("Identity & Symbols", "Mandate & Ideology", "Hierarchy & Initiation", "Assets & Influence", "Alliances, Rivals & Secrets"),
        fields = listOf(
            FieldSpec("Faction Name", "Short text", "Identity & Symbols", required = true),
            FieldSpec("Faction Type", "Dropdown", "Identity & Symbols", options = "Merchant Guild,Knightly Order,Mage Circle,Assassins/Thieves,Noble Coalition,Rebel Movement,Monastic Order,Mercenary Company,Scholarly Academy"),
            FieldSpec("Emblem / Banner", "Image", "Identity & Symbols"),
            FieldSpec("Faction Color", "Color", "Identity & Symbols", options = "#D9B66F"),
            FieldSpec("Public Mandate", "Long text", "Mandate & Ideology"),
            FieldSpec("True Agenda", "Long text", "Mandate & Ideology"),
            FieldSpec("Current Leader", "Relationship", "Hierarchy & Initiation"),
            FieldSpec("Ranks & Chain of Command", "Table", "Hierarchy & Initiation", hint = "Rank | Responsibilities | Regalia"),
            FieldSpec("Recruitment & Initiation Trials", "Long text", "Hierarchy & Initiation"),
            FieldSpec("Headquarters & Safehouses", "Repeatable group", "Assets & Influence", repeatable = true),
            FieldSpec("Treasury, Troops & Leverage", "Long text", "Assets & Influence"),
            FieldSpec("Allied Groups", "Repeatable group", "Alliances, Rivals & Secrets", repeatable = true),
            FieldSpec("Enemies & Blood Rivals", "Repeatable group", "Alliances, Rivals & Secrets", repeatable = true),
            FieldSpec("Internal Fissures & Secrets", "Long text", "Alliances, Rivals & Secrets")
        )
    ),
    TemplateSpec(
        slug = "festival",
        title = "Holiday, Festival & Seasonal Rite",
        category = "Cultures & Societies",
        defaultKind = "Festival",
        description = "Calendar celebrations, harvest feasts, solstice vigils, and commemorative rites.",
        sections = listOf("Timing & Origin", "Rituals & Festivities", "Food, Symbols & Variations"),
        fields = listOf(
            FieldSpec("Festival Name", "Short text", "Timing & Origin", required = true),
            FieldSpec("Culture / Faith", "Relationship", "Timing & Origin"),
            FieldSpec("Date, Moon, or Season", "Short text", "Timing & Origin"),
            FieldSpec("Duration", "Short text", "Timing & Origin"),
            FieldSpec("Mythic or Historical Origin", "Long text", "Timing & Origin"),
            FieldSpec("Core Rituals, Parades & Games", "Long text", "Rituals & Festivities"),
            FieldSpec("Who Participates & Who Is Barred", "Long text", "Rituals & Festivities"),
            FieldSpec("Ceremonial Foods, Colors & Masks", "Long text", "Food, Symbols & Variations"),
            FieldSpec("Regional Variations & Modern Shifts", "Long text", "Food, Symbols & Variations")
        )
    ),
    TemplateSpec(
        slug = "cuisine",
        title = "Food, Feast & Tavern Cuisine",
        category = "Cultures & Societies",
        defaultKind = "Culture",
        description = "Iconic dishes, spice trades, street food, noble banquets, and fermented brews.",
        sections = listOf("Dish & Origin", "Ingredients & Preparation", "Cultural Meaning"),
        fields = listOf(
            FieldSpec("Dish or Drink Name", "Short text", "Dish & Origin", required = true),
            FieldSpec("Origin Culture / Region", "Relationship", "Dish & Origin"),
            FieldSpec("Dish Image", "Image", "Dish & Origin"),
            FieldSpec("Ingredients & Fantasy Spices", "Repeatable group", "Ingredients & Preparation", repeatable = true),
            FieldSpec("Preparation & Cooking Method", "Long text", "Ingredients & Preparation"),
            FieldSpec("Taste, Aroma & Presentation", "Long text", "Ingredients & Preparation"),
            FieldSpec("Social Class, Occasion & Restrictions", "Long text", "Cultural Meaning")
        )
    ),
    TemplateSpec(
        slug = "clothing",
        title = "Clothing, Regalia & Fashion",
        category = "Cultures & Societies",
        defaultKind = "Culture",
        description = "Everyday garb, court attire, weather-proof cloaks, and rank insignias.",
        sections = listOf("Garment Profile", "Materials & Status", "Regional & Ritual Variations"),
        fields = listOf(
            FieldSpec("Style / Garment Name", "Short text", "Garment Profile", required = true),
            FieldSpec("Culture / Station", "Relationship", "Garment Profile"),
            FieldSpec("Reference Sketch", "Image", "Garment Profile"),
            FieldSpec("Fabrics, Dyes & Climate Fit", "Long text", "Materials & Status"),
            FieldSpec("Colors, Pins & Status Markers", "Long text", "Materials & Status"),
            FieldSpec("Modesty Norms & Ceremonial Versions", "Long text", "Regional & Ritual Variations")
        )
    ),
    TemplateSpec(
        slug = "economy",
        title = "Economy, Currency & Guild Trade",
        category = "Cultures & Societies",
        defaultKind = "Economy",
        description = "Design coinage systems, exchange rates, trade routes, tariffs, and black markets.",
        sections = listOf("Currency & Standards", "Production & Trade Routes", "Institutions & Black Market"),
        fields = listOf(
            FieldSpec("Economic System Name", "Short text", "Currency & Standards", required = true),
            FieldSpec("Governing Nation / Region", "Relationship", "Currency & Standards"),
            FieldSpec("Denominations & Exchange Table", "Table", "Currency & Standards", hint = "Coin / Unit | Material | Value | Colloquial Nickname"),
            FieldSpec("What Backs the Currency", "Long text", "Currency & Standards"),
            FieldSpec("Major Exports & Imports", "Repeatable group", "Production & Trade Routes", repeatable = true),
            FieldSpec("Key Trade Routes & Chokepoints", "Long text", "Production & Trade Routes"),
            FieldSpec("Banks, Tax Collectors & Guild Monopolies", "Long text", "Institutions & Black Market"),
            FieldSpec("Counterfeiting & Smuggling Economy", "Long text", "Institutions & Black Market")
        )
    ),
    TemplateSpec(
        slug = "government",
        title = "Government, Law & Justice",
        category = "Cultures & Societies",
        defaultKind = "Government",
        description = "Offices of state, courts, trials by magic or combat, codified laws, and corruption.",
        sections = listOf("State Architecture", "Law & Rights", "Courts & Punishments"),
        fields = listOf(
            FieldSpec("Polity / Legal System Name", "Short text", "State Architecture", required = true),
            FieldSpec("Government Model", "Dropdown", "State Architecture", options = "Monarchy,Feudal Council,Republic,Theocracy,Magocracy,Oligarchy,Tribal Assembly,Military Junta,Anarchic Commune"),
            FieldSpec("Key Offices & Succession", "Table", "State Architecture", hint = "Office | Current Holder | Authority"),
            FieldSpec("Protected Rights & Social Privileges", "Long text", "Law & Rights"),
            FieldSpec("Major Prohibitions & Capital Crimes", "Repeatable group", "Law & Rights", repeatable = true),
            FieldSpec("Trial Procedure & Magic Evidence", "Long text", "Courts & Punishments"),
            FieldSpec("Penalties, Prisons & Bribery", "Long text", "Courts & Punishments")
        )
    ),
    TemplateSpec(
        slug = "military",
        title = "Military Force & Order of Battle",
        category = "Cultures & Societies",
        defaultKind = "Military",
        description = "Armies, navies, battle-mages, siege trains, recruitment, and supply lines.",
        sections = listOf("Command & Doctrine", "Units & Capabilities", "Recruitment & Logistics", "Campaign Record"),
        fields = listOf(
            FieldSpec("Force / Legion Name", "Short text", "Command & Doctrine", required = true),
            FieldSpec("Allegiance", "Relationship", "Command & Doctrine"),
            FieldSpec("Banner / Insignia", "Image", "Command & Doctrine"),
            FieldSpec("Strategic Doctrine", "Long text", "Command & Doctrine"),
            FieldSpec("Total Active Strength", "Number", "Units & Capabilities"),
            FieldSpec("Branches & Special Corps", "Table", "Units & Capabilities", hint = "Unit / Corps | Role | Equipment / Beasts / Mages"),
            FieldSpec("Conscription, Training & Morale", "Long text", "Recruitment & Logistics"),
            FieldSpec("Supply Lines, Mounts & Siege Gear", "Long text", "Recruitment & Logistics"),
            FieldSpec("Strengths, Weaknesses & Famous Campaigns", "Long text", "Campaign Record")
        )
    ),
    TemplateSpec(
        slug = "game",
        title = "Sports, Duels & Pastimes",
        category = "Cultures & Societies",
        defaultKind = "Culture",
        description = "Tavern dice, knightly tournaments, aerial races, and blood-sports.",
        sections = listOf("Overview & Venue", "Rules & Scoring", "Prestige & Culture"),
        fields = listOf(
            FieldSpec("Game or Sport Name", "Short text", "Overview & Venue", required = true),
            FieldSpec("Type", "Dropdown", "Overview & Venue", options = "Board/Tile Game,Dice/Card Game,Combat Tournament,Race,Team Ball/Field Sport,Magical Duel"),
            FieldSpec("Origin Culture", "Relationship", "Overview & Venue"),
            FieldSpec("Equipment & Arena", "Long text", "Overview & Venue"),
            FieldSpec("Core Rules & Win Condition", "Long text", "Rules & Scoring"),
            FieldSpec("Cheating, Stakes & Social Prestige", "Long text", "Prestige & Culture")
        )
    ),
    TemplateSpec(
        slug = "medicine",
        title = "Medicine, Healing & Chirurgery",
        category = "Cultures & Societies",
        defaultKind = "Culture",
        description = "How healers diagnose illness, blend mundane surgery with alchemy or magic, and navigate taboos.",
        sections = listOf("Medical Philosophy", "Practitioners & Tools", "Treatments & Limits"),
        fields = listOf(
            FieldSpec("Tradition / Practice Name", "Short text", "Medical Philosophy", required = true),
            FieldSpec("Theory of Disease & Anatomy", "Long text", "Medical Philosophy"),
            FieldSpec("Healer Orders & Training", "Long text", "Practitioners & Tools"),
            FieldSpec("Surgical Tools, Poultices & Charms", "Repeatable group", "Practitioners & Tools", repeatable = true),
            FieldSpec("What Can & Cannot Be Healed", "Long text", "Treatments & Limits"),
            FieldSpec("Access, Cost & Taboos (e.g. Necromancy)", "Long text", "Treatments & Limits")
        )
    ),

    // ── MAGIC & SYSTEMS ─────────────────────────────────────────────────────
    TemplateSpec(
        slug = "magic",
        title = "Magic System",
        category = "Magic & Systems",
        defaultKind = "Magic System",
        description = "Define metaphysical laws, casting mechanics, true costs, hard limits, and societal fallout.",
        sections = listOf("Core Source & Metaphysics", "Mechanics & Activation", "Costs, Drain & Catalysts", "Hard Limits & Paradoxes", "Training & Schools", "Society, Law & History"),
        fields = listOf(
            FieldSpec("System Name", "Short text", "Core Source & Metaphysics", required = true),
            FieldSpec("System Style", "Dropdown", "Core Source & Metaphysics", options = "Hard Rule-Bound,Soft Mythic,Hybrid,Pact/Patron-Based,Alchemical/Material,Innate Bloodline,Linguistic/Runic"),
            FieldSpec("Power Source", "Long text", "Core Source & Metaphysics", hint = "Starlight, life-force, ancestral memory, planar bleed, vows"),
            FieldSpec("Visual & Sensory Signature", "Long text", "Core Source & Metaphysics"),
            FieldSpec("Sigil / Diagram", "Image", "Core Source & Metaphysics"),
            FieldSpec("How Magic Is Invoked", "Long text", "Mechanics & Activation"),
            FieldSpec("Focuses, Catalysts & Components", "Repeatable group", "Mechanics & Activation", repeatable = true),
            FieldSpec("Immediate Cost & Exhaustion", "Long text", "Costs, Drain & Catalysts"),
            FieldSpec("Long-Term Corruption or Consequences", "Long text", "Costs, Drain & Catalysts"),
            FieldSpec("What Magic Cannot Do (Hard Limits)", "Repeatable group", "Hard Limits & Paradoxes", repeatable = true),
            FieldSpec("Known Paradoxes, Loopholes & Exceptions", "Long text", "Hard Limits & Paradoxes"),
            FieldSpec("Schools, Disciplines & Ranks", "Table", "Training & Schools", hint = "Discipline | Focus | Typical Rank/Title"),
            FieldSpec("Notable Practitioners", "Relationship", "Training & Schools"),
            FieldSpec("Legal Regulation & Public Fear/Reverence", "Long text", "Society, Law & History"),
            FieldSpec("Origin Myth & Historical Cataclysms", "Long text", "Society, Law & History")
        )
    ),
    TemplateSpec(
        slug = "spell",
        title = "Spell, Ritual & Incantation",
        category = "Magic & Systems",
        defaultKind = "Spell",
        description = "Catalog individual spells, long-form circle rituals, battle-chants, and counterspells.",
        sections = listOf("Classification", "Incantation & Components", "Casting & Effects", "Risks & Countermeasures"),
        fields = listOf(
            FieldSpec("Spell / Ritual Name", "Short text", "Classification", required = true),
            FieldSpec("Parent Magic System", "Relationship", "Classification"),
            FieldSpec("School / Aspect", "Dropdown", "Classification", options = "Elemental,Illusion,Divination,Enchantment,Conjuration,Abjuration,Necromancy,Transmutation,Chronomancy,Blood/Spirit,Other"),
            FieldSpec("Complexity / Tier", "Rating", "Classification"),
            FieldSpec("Verbal Phrase / Chant", "Long text", "Incantation & Components"),
            FieldSpec("Gestures, Sigils & Material Cost", "Long text", "Incantation & Components"),
            FieldSpec("Casting Time, Range & Duration", "Short text", "Casting & Effects"),
            FieldSpec("Exact Effect & Scaling", "Long text", "Casting & Effects"),
            FieldSpec("Backlash on Failure", "Long text", "Risks & Countermeasures"),
            FieldSpec("Wards, Counterspells & Banned Status", "Long text", "Risks & Countermeasures")
        )
    ),
    TemplateSpec(
        slug = "enchantment",
        title = "Enchantment, Rune & Alchemy Craft",
        category = "Magic & Systems",
        defaultKind = "Magic System",
        description = "Document crafting disciplines like runeforging, golemancy, sigil-weaving, and artificing.",
        sections = listOf("Craft Overview", "Materials & Process", "Failure Modes & Guilds"),
        fields = listOf(
            FieldSpec("Craft Name", "Short text", "Craft Overview", required = true),
            FieldSpec("Parent Magic System", "Relationship", "Craft Overview"),
            FieldSpec("Base Substrates & Catalysts", "Repeatable group", "Materials & Process", repeatable = true),
            FieldSpec("Step-by-Step Creation Process", "Long text", "Materials & Process"),
            FieldSpec("Durability, Decay & Recharging", "Long text", "Materials & Process"),
            FieldSpec("Catastrophic Mishaps & Guild Secrets", "Long text", "Failure Modes & Guilds")
        )
    ),
    TemplateSpec(
        slug = "technology",
        title = "Technology & Magitech System",
        category = "Magic & Systems",
        defaultKind = "Technology",
        description = "Clockwork engines, rune-powered rails, alchemy guns, and uneven technological access.",
        sections = listOf("Principles & Power", "Applications", "Access, Hazards & History"),
        fields = listOf(
            FieldSpec("Technology / System Name", "Short text", "Principles & Power", required = true),
            FieldSpec("Energy / Fuel Source", "Long text", "Principles & Power"),
            FieldSpec("Schematic / Diagram", "Image", "Principles & Power"),
            FieldSpec("Civilian & Industrial Uses", "Long text", "Applications"),
            FieldSpec("Military & Transport Uses", "Long text", "Applications"),
            FieldSpec("Who Controls It & Uneven Access", "Long text", "Access, Hazards & History"),
            FieldSpec("Limitations, Pollution & Breakdowns", "Long text", "Access, Hazards & History")
        )
    ),
    TemplateSpec(
        slug = "affliction",
        title = "Curse, Hex, Plague & Affliction",
        category = "Magic & Systems",
        defaultKind = "Affliction",
        description = "Track magical blights, ancestral curses, werewolf transformations, and arcane plagues.",
        sections = listOf("Origin & Vector", "Symptoms & Stages", "Containment & Cure"),
        fields = listOf(
            FieldSpec("Affliction Name", "Short text", "Origin & Vector", required = true),
            FieldSpec("Type", "Dropdown", "Origin & Vector", options = "Bloodline Curse,Contagious Plague,Planar Blight,Hex/Jinx,Parasitic Symbiosis,Soul Rot"),
            FieldSpec("Origin & Transmission Vector", "Long text", "Origin & Vector"),
            FieldSpec("Severity", "Rating", "Origin & Vector"),
            FieldSpec("Progression Stages", "Table", "Symptoms & Stages", hint = "Stage | Timeframe | Symptoms | Behavioral Shift"),
            FieldSpec("Quarantine, Wards & Mitigation", "Long text", "Containment & Cure"),
            FieldSpec("True Cure or Breaking Condition", "Long text", "Containment & Cure")
        )
    ),
    TemplateSpec(
        slug = "world_rules",
        title = "World Rules & Metaphysical Laws",
        category = "Magic & Systems",
        defaultKind = "Magic System",
        description = "Record absolute truths of your setting—gravity, death, souls, resurrection, or fate.",
        sections = listOf("Law Definition", "Observable Impact", "Exceptions & Lore"),
        fields = listOf(
            FieldSpec("Rule / Constant Name", "Short text", "Law Definition", required = true),
            FieldSpec("Scope", "Dropdown", "Law Definition", options = "Cosmic/Universal,Planar,Regional,Species-Specific,Historical Era"),
            FieldSpec("Exact Statement of Law", "Long text", "Law Definition"),
            FieldSpec("Observable Effects on Daily Life", "Long text", "Observable Impact"),
            FieldSpec("Known Exceptions or Myths", "Long text", "Exceptions & Lore"),
            FieldSpec("Related Systems", "Relationship", "Exceptions & Lore")
        )
    ),

    // ── BELIEF & MYTH ───────────────────────────────────────────────────────
    TemplateSpec(
        slug = "belief",
        title = "Religion & Belief System",
        category = "Belief & Myth",
        defaultKind = "Religion",
        description = "Complete theology sheet: cosmology, pantheon, clergy, sacraments, sacred texts, afterlife, and schisms.",
        sections = listOf("Cosmology & Core Tenets", "Sacred Figures & Pantheon", "Clergy & Hierarchy", "Rituals, Sacraments & Taboos", "Sacred Texts & Symbols", "Afterlife & Heresies"),
        fields = listOf(
            FieldSpec("Faith / Philosophy Name", "Short text", "Cosmology & Core Tenets", required = true),
            FieldSpec("Structure", "Dropdown", "Cosmology & Core Tenets", options = "Monotheistic,Polytheistic Pantheon,Dualistic,Animist/Spirit-Veneration,Ancestor Worship,Non-Theistic Philosophy,Mystery Cult"),
            FieldSpec("Creation Myth & Worldview", "Long text", "Cosmology & Core Tenets"),
            FieldSpec("Core Virtues & Commandments", "Repeatable group", "Cosmology & Core Tenets", repeatable = true),
            FieldSpec("Central Deities / Spirits", "Repeatable group", "Sacred Figures & Pantheon", repeatable = true),
            FieldSpec("Patron / High Deity", "Relationship", "Sacred Figures & Pantheon"),
            FieldSpec("Clergy Ranks & Governance", "Table", "Clergy & Hierarchy", hint = "Title | Duties | Vestments"),
            FieldSpec("Sects, Monastic Orders & Templars", "Repeatable group", "Clergy & Hierarchy", repeatable = true),
            FieldSpec("Key Sacraments, Prayers & Rites", "Long text", "Rituals, Sacraments & Taboos"),
            FieldSpec("Dietary, Speech & Moral Taboos", "Repeatable group", "Rituals, Sacraments & Taboos", repeatable = true),
            FieldSpec("Holy Texts & Liturgical Language", "Long text", "Sacred Texts & Symbols"),
            FieldSpec("Sacred Symbol", "Image", "Sacred Texts & Symbols"),
            FieldSpec("View of Death & the Afterlife", "Long text", "Afterlife & Heresies"),
            FieldSpec("Heresies, Schisms & Political Reach", "Long text", "Afterlife & Heresies")
        )
    ),
    TemplateSpec(
        slug = "deity",
        title = "Deity, Spirit, Demon & Patron",
        category = "Belief & Myth",
        defaultKind = "Deity",
        description = "Domains, true forms, favored mortals, shrines, offerings, and mythic rivalries.",
        sections = listOf("Domains & Titles", "Form & Symbols", "Dogma & Worship", "Myths & Relationships"),
        fields = listOf(
            FieldSpec("Name & Epithets", "Short text", "Domains & Titles", required = true),
            FieldSpec("Pantheon / Faith", "Relationship", "Domains & Titles"),
            FieldSpec("Entity Tier", "Dropdown", "Domains & Titles", options = "Primordial Creator,Greater Deity,Lesser Deity,Demi-God/Saint,Arch-Spirit,Demon/Fiend Lord,Eldritch Patron"),
            FieldSpec("Primary Domains", "Repeatable group", "Domains & Titles", repeatable = true),
            FieldSpec("True Form, Avatars & Voice", "Long text", "Form & Symbols"),
            FieldSpec("Sacred Animal, Plant & Symbol", "Long text", "Form & Symbols"),
            FieldSpec("Iconography / Sigil", "Image", "Form & Symbols"),
            FieldSpec("Tenets & Favored Offerings", "Long text", "Dogma & Worship"),
            FieldSpec("What Angers Them (Taboos)", "Long text", "Dogma & Worship"),
            FieldSpec("Miracles, Boons & Warlock Pacts", "Long text", "Dogma & Worship"),
            FieldSpec("Divine Allies, Kin & Rivals", "Table", "Myths & Relationships", hint = "Entity | Bond/Rivalry | Myth"),
            FieldSpec("Defining Myths & Manifestations", "Long text", "Myths & Relationships")
        )
    ),
    TemplateSpec(
        slug = "myth",
        title = "Myth, Legend & Creation Story",
        category = "Belief & Myth",
        defaultKind = "Myth",
        description = "Capture folk tales, heroic ballads, creation epics, and how different cultures tell them.",
        sections = listOf("Story & Tradition", "Narrative & Symbolism", "Variations & Truth"),
        fields = listOf(
            FieldSpec("Myth / Legend Title", "Short text", "Story & Tradition", required = true),
            FieldSpec("Told By (Culture / Faith)", "Relationship", "Story & Tradition"),
            FieldSpec("Core Narrative Summary", "Long text", "Narrative & Symbolism"),
            FieldSpec("Key Mythic Figures", "Repeatable group", "Narrative & Symbolism", repeatable = true),
            FieldSpec("Moral Lesson & Cultural Function", "Long text", "Narrative & Symbolism"),
            FieldSpec("Competing Regional Versions", "Long text", "Variations & Truth"),
            FieldSpec("Actual Historical Kernel", "Long text", "Variations & Truth")
        )
    ),
    TemplateSpec(
        slug = "prophecy",
        title = "Prophecy, Omen & Divination",
        category = "Belief & Myth",
        defaultKind = "Prophecy",
        description = "Verse prophecies, celestial omens, rival interpretations, and subtle fulfillment traps.",
        sections = listOf("The Prophecy / Sign", "Context & Divination", "Interpretations & Reality"),
        fields = listOf(
            FieldSpec("Prophecy Title", "Short text", "The Prophecy / Sign", required = true),
            FieldSpec("Exact Text or Omen", "Rich text", "The Prophecy / Sign"),
            FieldSpec("Origin, Seer & Divination Method", "Long text", "Context & Divination"),
            FieldSpec("Who Believes or Fears It", "Long text", "Context & Divination"),
            FieldSpec("Competing Interpretations", "Table", "Interpretations & Reality", hint = "Faction/Scholar | Interpretation | Motivation"),
            FieldSpec("True Fulfillment Conditions & Ironies", "Long text", "Interpretations & Reality")
        )
    ),
    TemplateSpec(
        slug = "secret_society",
        title = "Secret Society, Cult & Conspiracy",
        category = "Belief & Myth",
        defaultKind = "Faction",
        description = "Hidden cabals, heretical cults, spy rings, and shadow courts.",
        sections = listOf("Cover & True Purpose", "Initiation & Ciphers", "Cells, Patrons & Rivals"),
        fields = listOf(
            FieldSpec("Society / Cult Name", "Short text", "Cover & True Purpose", required = true),
            FieldSpec("Public Front / Cover Identity", "Long text", "Cover & True Purpose"),
            FieldSpec("Secret Doctrine & Endgame", "Long text", "Cover & True Purpose"),
            FieldSpec("Initiation Rituals & Oaths", "Long text", "Initiation & Ciphers"),
            FieldSpec("Recognition Signs, Passphrases & Ciphers", "Long text", "Initiation & Ciphers"),
            FieldSpec("Inner Circle & Patrons", "Relationship", "Cells, Patrons & Rivals"),
            FieldSpec("Known Cells & Counter-Hunters", "Long text", "Cells, Patrons & Rivals")
        )
    ),

    // ── TIME & HISTORY ──────────────────────────────────────────────────────
    TemplateSpec(
        slug = "celestial",
        title = "Celestial Body, Moon & Constellation",
        category = "Time & History",
        defaultKind = "Celestial",
        description = "Suns, twin moons, wandering comets, constellations, and their tidal/magical influence.",
        sections = listOf("Astronomical Profile", "Cycles & Eclipses", "Arcane & Cultural Meaning"),
        fields = listOf(
            FieldSpec("Body / Constellation Name", "Short text", "Astronomical Profile", required = true),
            FieldSpec("Type", "Dropdown", "Astronomical Profile", options = "Sun/Star,Moon,Planet,Comet,Constellation,Ring/Nebula,Planar Beacon"),
            FieldSpec("Color & Visual Appearance", "Color", "Astronomical Profile", options = "#E6D5F7"),
            FieldSpec("Visual Sketch", "Image", "Astronomical Profile"),
            FieldSpec("Orbital Period & Phases", "Long text", "Cycles & Eclipses"),
            FieldSpec("Eclipses & Rare Conjunctions", "Long text", "Cycles & Eclipses"),
            FieldSpec("Effects on Tides, Magic & Monsters", "Long text", "Arcane & Cultural Meaning"),
            FieldSpec("Astrology, Navigation & Myths", "Long text", "Arcane & Cultural Meaning")
        )
    ),
    TemplateSpec(
        slug = "calendar_system",
        title = "Calendar & Timekeeping System",
        category = "Time & History",
        defaultKind = "Calendar",
        description = "Design months, week lengths, intercalary festivals, lunar ties, and era reckoning.",
        sections = listOf("Calendar Structure", "Months & Seasons", "Eras & Leap Rules"),
        fields = listOf(
            FieldSpec("Calendar Name", "Short text", "Calendar Structure", required = true),
            FieldSpec("Culture / Region", "Relationship", "Calendar Structure"),
            FieldSpec("Days in Year", "Number", "Calendar Structure"),
            FieldSpec("Days in Week & Weekday Names", "Short text", "Calendar Structure"),
            FieldSpec("Months Table", "Table", "Months & Seasons", hint = "Month | Days | Season | Associated Meaning"),
            FieldSpec("Intercalary / Out-of-Time Days", "Long text", "Months & Seasons"),
            FieldSpec("Moon Cycle Alignment", "Long text", "Months & Seasons"),
            FieldSpec("Current Era & Year Zero Event", "Long text", "Eras & Leap Rules"),
            FieldSpec("Leap-Year & Drift Corrections", "Long text", "Eras & Leap Rules")
        )
    ),
    TemplateSpec(
        slug = "war",
        title = "Historical War, Crisis & Era",
        category = "Time & History",
        defaultKind = "Timeline Event",
        description = "Chronicle wars, succession crises, cataclysms, treaties, and generational aftermath.",
        sections = listOf("Chronology & Causes", "Belligerents & Turning Points", "Aftermath & Legacy"),
        fields = listOf(
            FieldSpec("Conflict / Era Name", "Short text", "Chronology & Causes", required = true),
            FieldSpec("Start & End Dates / Era", "Short text", "Chronology & Causes"),
            FieldSpec("Root Causes & Spark", "Long text", "Chronology & Causes"),
            FieldSpec("Belligerent Powers & Commanders", "Table", "Belligerents & Turning Points", hint = "Side | Factions | Key Commanders"),
            FieldSpec("Major Battles & Turning Points", "Repeatable group", "Belligerents & Turning Points", repeatable = true),
            FieldSpec("Magic, Monsters, or Weapons Used", "Long text", "Belligerents & Turning Points"),
            FieldSpec("Treaties, Border Shifts & Legacy", "Long text", "Aftermath & Legacy")
        )
    ),
    TemplateSpec(
        slug = "event",
        title = "Timeline Event & Historical Milestone",
        category = "Time & History",
        defaultKind = "Timeline Event",
        description = "Pinpoint specific historical turning points, births, treaties, or disasters.",
        sections = listOf("Date & Setting", "Event Details", "Consequences"),
        fields = listOf(
            FieldSpec("Event Title", "Short text", "Date & Setting", required = true),
            FieldSpec("Date / Era", "Date", "Date & Setting"),
            FieldSpec("Location", "Relationship", "Date & Setting"),
            FieldSpec("Key Figures Involved", "Repeatable group", "Event Details", repeatable = true),
            FieldSpec("Trigger & What Happened", "Long text", "Event Details"),
            FieldSpec("Immediate & Long-Term Consequences", "Long text", "Consequences"),
            FieldSpec("Primary Sources & Competing Accounts", "Long text", "Consequences")
        )
    ),
    TemplateSpec(
        slug = "continuity",
        title = "Continuity & Lore Tracker",
        category = "Time & History",
        defaultKind = "Research Note",
        description = "Keep canon tight across books: track established facts, first mentions, and constraints.",
        sections = listOf("Canon Fact", "Dependencies & Verification"),
        fields = listOf(
            FieldSpec("Canon Fact Title", "Short text", "Canon Fact", required = true),
            FieldSpec("Canonical Rule / Fact", "Long text", "Canon Fact"),
            FieldSpec("First Mentioned In", "Relationship", "Dependencies & Verification"),
            FieldSpec("Dependent Scenes or Characters", "Repeatable group", "Dependencies & Verification", repeatable = true),
            FieldSpec("Verification Status", "Dropdown", "Dependencies & Verification", options = "Canon Locked,Draft Assumption,Needs Revision,Retconned")
        )
    ),
    TemplateSpec(
        slug = "quest",
        title = "Quest, Mystery & Adventure Arc",
        category = "Plot & Writing",
        defaultKind = "Quest",
        description = "Structure quests, investigations, or heist arcs with hooks, clues, twists, and outcomes.",
        sections = listOf("Hook & Stakes", "Leads, Clues & Red Herrings", "Encounters & Outcomes"),
        fields = listOf(
            FieldSpec("Quest / Arc Title", "Short text", "Hook & Stakes", required = true),
            FieldSpec("Patron or Inciting Hook", "Long text", "Hook & Stakes"),
            FieldSpec("Key Locations & Factions", "Relationship", "Hook & Stakes"),
            FieldSpec("Stakes & Ticking Clock", "Long text", "Hook & Stakes"),
            FieldSpec("Clue Trail & Red Herrings", "Table", "Leads, Clues & Red Herrings", hint = "Clue | Where Found | True Meaning"),
            FieldSpec("Major Twists & Obstacles", "Repeatable group", "Encounters & Outcomes", repeatable = true),
            FieldSpec("Possible Resolutions & Fallout", "Long text", "Encounters & Outcomes")
        )
    )
)

internal val writingAndLegacySpecs: List<TemplateSpec> = listOf(
    TemplateSpec("novel", "Novel Project", "Plot & Writing", "Novel", "Full novel planning bible covering premise, cast, world, and revision.", listOf("Premise & Cast", "Structure & Revision"), listOf(
        FieldSpec("Premise", "Long text", "Premise & Cast", required = true),
        FieldSpec("Genre & Audience", "Short text", "Premise & Cast"),
        FieldSpec("Themes", "Repeatable group", "Premise & Cast", repeatable = true),
        FieldSpec("Synopsis", "Long text", "Premise & Cast"),
        FieldSpec("Main Cast", "Relationship", "Premise & Cast"),
        FieldSpec("Primary World", "Relationship", "Premise & Cast"),
        FieldSpec("Outline", "Long text", "Structure & Revision"),
        FieldSpec("Revision Plan", "Long text", "Structure & Revision")
    )),
    TemplateSpec("series", "Series Bible", "Plot & Writing", "Series", "Multi-volume arc, recurring cast, and continuity rules.", listOf("Series Arc", "Volumes & Continuity"), listOf(
        FieldSpec("Series Premise", "Long text", "Series Arc", required = true),
        FieldSpec("Core Themes", "Repeatable group", "Series Arc", repeatable = true),
        FieldSpec("Volume List", "Table", "Volumes & Continuity", hint = "Book | Title | Core Arc | Status"),
        FieldSpec("Long Arc", "Long text", "Volumes & Continuity"),
        FieldSpec("Continuity Rules & Open Threads", "Long text", "Volumes & Continuity")
    )),
    TemplateSpec("standalone", "Standalone Story", "Plot & Writing", "Story", "Complete standalone fiction outline.", listOf("Story Core", "Conflict & Ending"), listOf(
        FieldSpec("Premise", "Long text", "Story Core", required = true),
        FieldSpec("POV & Tense", "Short text", "Story Core"),
        FieldSpec("Setting", "Relationship", "Story Core"),
        FieldSpec("Central Conflict", "Long text", "Conflict & Ending"),
        FieldSpec("Structure & Ending", "Long text", "Conflict & Ending")
    )),
    TemplateSpec("bookplan", "Book Plan", "Plot & Writing", "Book", "Chapter-by-chapter book blueprint.", listOf("Overview", "Chapters & Subplots"), listOf(
        FieldSpec("Logline", "Short text", "Overview", required = true),
        FieldSpec("Summary", "Long text", "Overview"),
        FieldSpec("Chapter Outline", "Table", "Chapters & Subplots", hint = "Chapter | POV | Goal | Turn"),
        FieldSpec("Subplots & Revision Notes", "Long text", "Chapters & Subplots")
    )),
    TemplateSpec("chapter", "Chapter Plan", "Plot & Writing", "Chapter", "Plan individual chapter beats and continuity.", listOf("Chapter Beats"), listOf(
        FieldSpec("Purpose", "Long text", "Chapter Beats", required = true),
        FieldSpec("POV Character", "Relationship", "Chapter Beats"),
        FieldSpec("Opening Hook", "Long text", "Chapter Beats"),
        FieldSpec("Scene Beats", "Repeatable group", "Chapter Beats", repeatable = true),
        FieldSpec("Chapter Ending & Continuity", "Long text", "Chapter Beats")
    )),
    TemplateSpec("scene", "Scene Sheet", "Plot & Writing", "Scene", "Track POV, goal, conflict, disaster, and outcome for a scene.", listOf("Scene Mechanics"), listOf(
        FieldSpec("POV & Setting", "Short text", "Scene Mechanics", required = true),
        FieldSpec("Scene Goal", "Long text", "Scene Mechanics"),
        FieldSpec("Conflict & Obstacles", "Long text", "Scene Mechanics"),
        FieldSpec("Outcome / Shift", "Long text", "Scene Mechanics"),
        FieldSpec("Continuity & World Details", "Long text", "Scene Mechanics")
    )),
    TemplateSpec("outline", "Plot Outline", "Plot & Writing", "Document", "Classic narrative outline.", listOf("Structure"), listOf(
        FieldSpec("Premise", "Long text", "Structure", required = true),
        FieldSpec("Inciting Incident", "Long text", "Structure"),
        FieldSpec("Turning Points", "Repeatable group", "Structure", repeatable = true),
        FieldSpec("Climax & Resolution", "Long text", "Structure")
    )),
    TemplateSpec("threeact", "Three-Act Outline", "Plot & Writing", "Document", "Act I, Act II, Act III beat sheet.", listOf("Three Acts"), listOf(
        FieldSpec("Act One (Setup & Break)", "Long text", "Three Acts"),
        FieldSpec("Act Two (Confrontation & Midpoint)", "Long text", "Three Acts"),
        FieldSpec("Act Three (Climax & Denouement)", "Long text", "Three Acts")
    )),
    TemplateSpec("hero", "Hero's Journey", "Plot & Writing", "Document", "Mythic monomyth stages from Ordinary World to Return.", listOf("Departure", "Initiation & Return"), listOf(
        FieldSpec("Ordinary World & Call", "Long text", "Departure"),
        FieldSpec("Crossing the Threshold", "Long text", "Departure"),
        FieldSpec("Trials, Allies & Enemies", "Long text", "Initiation & Return"),
        FieldSpec("Ordeal, Reward & Return", "Long text", "Initiation & Return")
    )),
    TemplateSpec("savecat", "Save the Cat Beat Sheet", "Plot & Writing", "Document", "15-beat commercial pacing sheet.", listOf("Act I", "Act II & III"), listOf(
        FieldSpec("Opening Image & Theme Stated", "Long text", "Act I"),
        FieldSpec("Catalyst & Debate", "Long text", "Act I"),
        FieldSpec("Midpoint & Bad Guys Close In", "Long text", "Act II & III"),
        FieldSpec("All Is Lost & Finale", "Long text", "Act II & III")
    )),
    TemplateSpec("poem", "Poem & Bardic Verse", "Plot & Writing", "Poem", "Craft in-world ballads, epic verse, or personal poetry.", listOf("Form & Craft", "Draft"), listOf(
        FieldSpec("Poetic Form / Meter", "Short text", "Form & Craft"),
        FieldSpec("Theme & Imagery", "Long text", "Form & Craft"),
        FieldSpec("Draft & Stanzas", "Rich text", "Draft")
    )),
    TemplateSpec("shortstory", "Short Story", "Plot & Writing", "Story", "Compact story sheet from hook to twist.", listOf("Story Sheet"), listOf(
        FieldSpec("Hook", "Short text", "Story Sheet", required = true),
        FieldSpec("Character & Setting", "Long text", "Story Sheet"),
        FieldSpec("Conflict & Climax", "Long text", "Story Sheet"),
        FieldSpec("Resolution", "Long text", "Story Sheet")
    )),
    TemplateSpec("journal", "Journal Entry", "Plot & Writing", "Journal", "Reflective daily writing or in-character diary entry.", listOf("Entry"), listOf(
        FieldSpec("Date", "Date", "Entry"),
        FieldSpec("Mood", "Short text", "Entry"),
        FieldSpec("Events & Reflections", "Long text", "Entry")
    )),
    TemplateSpec("research", "Research & Lore Source", "Research & Reference", "Research Note", "Track historical inspirations, references, quotes, and citations.", listOf("Source & Summary"), listOf(
        FieldSpec("Research Question", "Short text", "Source & Summary", required = true),
        FieldSpec("Source / Citation", "Long text", "Source & Summary"),
        FieldSpec("Key Findings & Quotes", "Long text", "Source & Summary"),
        FieldSpec("Relevance to World", "Long text", "Source & Summary")
    )),
    TemplateSpec("map", "Map Reference Sheet", "Locations & Realms", "Location", "Document map scale, key routes, borders, and legend notes.", listOf("Cartography"), listOf(
        FieldSpec("Area & Scale", "Short text", "Cartography", required = true),
        FieldSpec("Map Image", "Image", "Cartography"),
        FieldSpec("Landmarks, Borders & Routes", "Long text", "Cartography"),
        FieldSpec("Cartographer's Bias or Errors", "Long text", "Cartography")
    )),
    TemplateSpec("floorplan", "Floor Plan & Interior Sheet", "Locations & Realms", "Location", "Map levels, rooms, hidden passages, and guard rotations.", listOf("Layout"), listOf(
        FieldSpec("Building / Site", "Short text", "Layout", required = true),
        FieldSpec("Floor Plan Image", "Image", "Layout"),
        FieldSpec("Levels & Room Key", "Table", "Layout", hint = "Level | Room | Purpose | Secret"),
        FieldSpec("Entrances, Exits & Escape Routes", "Long text", "Layout")
    ))
)
