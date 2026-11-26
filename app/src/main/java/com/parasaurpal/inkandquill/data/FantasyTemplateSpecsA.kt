package com.parasaurpal.inkandquill.data

internal val fantasySpecsPartA: List<TemplateSpec> = listOf(
    // ── CHARACTERS & LINEAGE ────────────────────────────────────────────────
    TemplateSpec(
        slug = "character",
        title = "Character Sheet",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Deep multi-section dossier for main cast, heroes, and major world figures.",
        sections = listOf("Identity & Titles", "Appearance & Aura", "Personality & Psychology", "Origins & Lineage", "Magic, Skills & Gear", "Bonds & Allegiances", "Story Arc"),
        fields = listOf(
            FieldSpec("Full Name", "Short text", "Identity & Titles", required = true, hint = "True or legal name"),
            FieldSpec("Titles & Epithets", "Short text", "Identity & Titles", hint = "Honorifics, sobriquets, or nicknames"),
            FieldSpec("Species / Ancestry", "Relationship", "Identity & Titles", hint = "Link to a Species or Ancestry sheet"),
            FieldSpec("Culture / Homeland", "Relationship", "Identity & Titles"),
            FieldSpec("Role in Story", "Dropdown", "Identity & Titles", options = "Protagonist,Deuteragonist,Antagonist,Mentor,Rival,Ally,Foil,Wildcard"),
            FieldSpec("Age / Lifespan Stage", "Short text", "Identity & Titles"),
            FieldSpec("Pronouns & Voice", "Short text", "Identity & Titles", hint = "Cadence, accent, speech quirks"),
            FieldSpec("Portrait", "Image", "Appearance & Aura"),
            FieldSpec("Signature Color", "Color", "Appearance & Aura", options = "#C9B6E4"),
            FieldSpec("Build, Height & Bearing", "Long text", "Appearance & Aura"),
            FieldSpec("Distinguishing Marks", "Long text", "Appearance & Aura", hint = "Scars, birthmarks, runes, eyes"),
            FieldSpec("Attire & Silhouettes", "Long text", "Appearance & Aura"),
            FieldSpec("Gallery & Visual Refs", "Gallery", "Appearance & Aura"),
            FieldSpec("Core Temperament", "Long text", "Personality & Psychology"),
            FieldSpec("Virtues & Strengths", "Repeatable group", "Personality & Psychology", repeatable = true),
            FieldSpec("Flaws & Blind Spots", "Repeatable group", "Personality & Psychology", repeatable = true),
            FieldSpec("Deepest Desire", "Long text", "Personality & Psychology"),
            FieldSpec("Greatest Fear", "Long text", "Personality & Psychology"),
            FieldSpec("Moral Code & Taboos", "Long text", "Personality & Psychology"),
            FieldSpec("Birthplace & Childhood", "Long text", "Origins & Lineage"),
            FieldSpec("Family & Bloodline", "Relationship", "Origins & Lineage"),
            FieldSpec("Formative Trauma or Turning Point", "Long text", "Origins & Lineage"),
            FieldSpec("Secret History", "Long text", "Origins & Lineage"),
            FieldSpec("Wields Magic?", "Checkbox", "Magic, Skills & Gear"),
            FieldSpec("Magic Tradition / System", "Relationship", "Magic, Skills & Gear", conditionField = "Wields Magic?", conditionValue = "true"),
            FieldSpec("Innate Gift / Affinity", "Long text", "Magic, Skills & Gear", conditionField = "Wields Magic?", conditionValue = "true"),
            FieldSpec("Notable Skills & Proficiencies", "Table", "Magic, Skills & Gear", hint = "Skill | Mastery | Notes"),
            FieldSpec("Signature Items & Relics", "Repeatable group", "Magic, Skills & Gear", repeatable = true),
            FieldSpec("Faction / Guild Allegiance", "Relationship", "Bonds & Allegiances"),
            FieldSpec("Key Allies & Confidants", "Repeatable group", "Bonds & Allegiances", repeatable = true),
            FieldSpec("Rivals & Enemies", "Repeatable group", "Bonds & Allegiances", repeatable = true),
            FieldSpec("Starting State", "Long text", "Story Arc"),
            FieldSpec("Inciting Lie & Truth Needed", "Long text", "Story Arc"),
            FieldSpec("Arc Trajectory & Ending", "Long text", "Story Arc"),
            FieldSpec("Trait Count", "Calculated", "Story Arc", formula = "count(Core Temperament)")
        )
    ),
    TemplateSpec(
        slug = "protagonist",
        title = "Hero & Protagonist Dossier",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Focused protagonist sheet balancing internal want/need tension with heroic trials.",
        sections = listOf("Identity", "Inner Conflict", "Capabilities & Cost", "Cast Dynamics", "Milestone Tracker"),
        fields = listOf(
            FieldSpec("Name & Alias", "Short text", "Identity", required = true),
            FieldSpec("Portrait", "Image", "Identity"),
            FieldSpec("Archetype", "Dropdown", "Identity", options = "Reluctant Hero,Chosen Vessel,Fallen Noble,Scholar-Mage,Outcast Rogue,Oathbound Knight,Trickster,Other"),
            FieldSpec("Surface Want", "Long text", "Inner Conflict", hint = "What they actively pursue at story start"),
            FieldSpec("Hidden Need", "Long text", "Inner Conflict", hint = "What actually heals or completes them"),
            FieldSpec("Fatal Flaw", "Long text", "Inner Conflict"),
            FieldSpec("Ghost / Wound", "Long text", "Inner Conflict"),
            FieldSpec("Signature Power or Skill", "Long text", "Capabilities & Cost"),
            FieldSpec("Price Paid to Use It", "Long text", "Capabilities & Cost"),
            FieldSpec("Foil / Mirror Character", "Relationship", "Cast Dynamics"),
            FieldSpec("Mentor or Patron", "Relationship", "Cast Dynamics"),
            FieldSpec("Arc Milestones", "Table", "Milestone Tracker", hint = "Act | Event | Internal Shift")
        )
    ),
    TemplateSpec(
        slug = "antagonist",
        title = "Villain & Antagonist Dossier",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Architect compelling villains with coherent ideology, methods, lieutenants, and fatal blind spots.",
        sections = listOf("Identity & Threat", "Ideology & Creed", "Grand Scheme", "Forces & Assets", "Vulnerabilities"),
        fields = listOf(
            FieldSpec("Villain Name & Title", "Short text", "Identity & Threat", required = true),
            FieldSpec("Threat Tier", "Rating", "Identity & Threat"),
            FieldSpec("Portrait", "Image", "Identity & Threat"),
            FieldSpec("Antagonist Type", "Dropdown", "Identity & Threat", options = "Ideologue,Tyrant,Monster,Dark Mirror,Mastermind,Corrupted Hero,Cosmic Force,Betrayer"),
            FieldSpec("Why They Believe They Are Right", "Long text", "Ideology & Creed"),
            FieldSpec("Virtue Twisted Into Vice", "Long text", "Ideology & Creed"),
            FieldSpec("Primary Objective", "Long text", "Grand Scheme"),
            FieldSpec("Contingency Plans", "Repeatable group", "Grand Scheme", repeatable = true),
            FieldSpec("Lieutenants & Agents", "Repeatable group", "Forces & Assets", repeatable = true),
            FieldSpec("Stronghold / Base", "Relationship", "Forces & Assets"),
            FieldSpec("Resources & Leverage", "Long text", "Forces & Assets"),
            FieldSpec("Hubris / Fatal Blind Spot", "Long text", "Vulnerabilities"),
            FieldSpec("What Breaks Their Plan", "Long text", "Vulnerabilities")
        )
    ),
    TemplateSpec(
        slug = "companion",
        title = "Companion, Familiar & Bonded Beast",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Track animal companions, spirit familiars, bonded mounts, or loyal sidekicks.",
        sections = listOf("Bond & Origin", "Appearance & Nature", "Abilities & Quirks", "Care & Loyalty"),
        fields = listOf(
            FieldSpec("Name", "Short text", "Bond & Origin", required = true),
            FieldSpec("Species / Type", "Short text", "Bond & Origin"),
            FieldSpec("Bonded To", "Relationship", "Bond & Origin"),
            FieldSpec("Nature of Bond", "Dropdown", "Bond & Origin", options = "Telepathic,Blood Pact,Lifelong Companion,Summoned Familiar,Earned Trust,Enchanted Collar"),
            FieldSpec("Appearance", "Long text", "Appearance & Nature"),
            FieldSpec("Image", "Image", "Appearance & Nature"),
            FieldSpec("Personality & Habits", "Long text", "Appearance & Nature"),
            FieldSpec("Special Abilities", "Repeatable group", "Abilities & Quirks", repeatable = true),
            FieldSpec("Communication Method", "Short text", "Abilities & Quirks"),
            FieldSpec("Diet & Upkeep", "Long text", "Care & Loyalty"),
            FieldSpec("Breaking Point / Trigger", "Long text", "Care & Loyalty")
        )
    ),
    TemplateSpec(
        slug = "npc",
        title = "NPC & Minor Figure",
        category = "Characters & Lineage",
        defaultKind = "NPC",
        description = "Fast, memorable sheet for recurring tavern keepers, scholars, captains, and informants.",
        sections = listOf("Role & Impression", "Motivation & Secrets", "Connections"),
        fields = listOf(
            FieldSpec("Name", "Short text", "Role & Impression", required = true),
            FieldSpec("Occupation / Station", "Short text", "Role & Impression"),
            FieldSpec("Primary Location", "Relationship", "Role & Impression"),
            FieldSpec("Three-Second Impression", "Long text", "Role & Impression", hint = "Voice, posture, distinct detail"),
            FieldSpec("Immediate Goal", "Long text", "Motivation & Secrets"),
            FieldSpec("What They Know", "Long text", "Motivation & Secrets"),
            FieldSpec("Hidden Secret or Leverage", "Long text", "Motivation & Secrets"),
            FieldSpec("Faction / Connections", "Relationship", "Connections"),
            FieldSpec("Rumors About Them", "Repeatable group", "Connections", repeatable = true)
        )
    ),
    TemplateSpec(
        slug = "dynasty",
        title = "Noble House, Clan & Dynasty",
        category = "Characters & Lineage",
        defaultKind = "Dynasty",
        description = "Heraldry, lands, bloodline traits, succession laws, and ancient feuds.",
        sections = listOf("Heraldry & Seat", "Bloodline & Traits", "Power & Holdings", "Succession & Politics"),
        fields = listOf(
            FieldSpec("House / Clan Name", "Short text", "Heraldry & Seat", required = true),
            FieldSpec("Words / Motto", "Short text", "Heraldry & Seat"),
            FieldSpec("Crest / Sigil", "Image", "Heraldry & Seat"),
            FieldSpec("Banner Color", "Color", "Heraldry & Seat", options = "#8C2D38"),
            FieldSpec("Ancestral Seat", "Relationship", "Heraldry & Seat"),
            FieldSpec("Founder & Mythic Origin", "Long text", "Bloodline & Traits"),
            FieldSpec("Hereditary Traits or Gifts", "Long text", "Bloodline & Traits"),
            FieldSpec("Notable Members", "Table", "Bloodline & Traits", hint = "Name | Title | Status"),
            FieldSpec("Vassals, Lands & Wealth", "Long text", "Power & Holdings"),
            FieldSpec("Heirlooms & Ancestral Relics", "Repeatable group", "Power & Holdings", repeatable = true),
            FieldSpec("Succession Rule", "Dropdown", "Succession & Politics", options = "Primogeniture,Elective Council,Matriarchal,Trial by Combat,Magical Inheritance,Seniority,Other"),
            FieldSpec("Allied Houses", "Repeatable group", "Succession & Politics", repeatable = true),
            FieldSpec("Blood Feuds & Rivals", "Repeatable group", "Succession & Politics", repeatable = true),
            FieldSpec("House Secrets", "Long text", "Succession & Politics")
        )
    ),
    TemplateSpec(
        slug = "lineage",
        title = "Character Lineage & Bloodline",
        category = "Characters & Lineage",
        defaultKind = "Lineage",
        description = "Trace ancestry, inherited magic, cadet branches, and disputed claims across generations.",
        sections = listOf("Root & Heritage", "Inherited Gifts", "Genealogy & Claims"),
        fields = listOf(
            FieldSpec("Lineage Name", "Short text", "Root & Heritage", required = true),
            FieldSpec("Subject / Heir", "Relationship", "Root & Heritage"),
            FieldSpec("Ancestral Origin", "Long text", "Root & Heritage"),
            FieldSpec("Bloodline Magic / Curse", "Long text", "Inherited Gifts"),
            FieldSpec("Physical Hallmarks", "Long text", "Inherited Gifts"),
            FieldSpec("Generational Table", "Table", "Genealogy & Claims", hint = "Generation | Ancestor | Partner | Notable Notes"),
            FieldSpec("Cadet Branches", "Repeatable group", "Genealogy & Claims", repeatable = true),
            FieldSpec("Disputed Claims & Bastardry", "Long text", "Genealogy & Claims")
        )
    ),
    TemplateSpec(
        slug = "party",
        title = "Party, Crew & Ensemble Roster",
        category = "Characters & Lineage",
        defaultKind = "Faction",
        description = "Manage an adventuring company, ship crew, heist team, or fellowship.",
        sections = listOf("Company Overview", "Member Roster", "Dynamics & Shared Assets"),
        fields = listOf(
            FieldSpec("Group Name", "Short text", "Company Overview", required = true),
            FieldSpec("Shared Purpose / Oath", "Long text", "Company Overview"),
            FieldSpec("Home Base or Vessel", "Relationship", "Company Overview"),
            FieldSpec("Roster", "Table", "Member Roster", hint = "Character | Role | Specialty | Current Status"),
            FieldSpec("Internal Friction & Alliances", "Long text", "Dynamics & Shared Assets"),
            FieldSpec("Shared Treasury & Gear", "Repeatable group", "Dynamics & Shared Assets", repeatable = true),
            FieldSpec("Reputation Across Regions", "Long text", "Dynamics & Shared Assets")
        )
    ),
    TemplateSpec(
        slug = "arc",
        title = "Character Arc",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Plot an individual character's emotional transformation across the narrative.",
        sections = listOf("Arc Polarity", "Turning Points", "Resolution"),
        fields = listOf(
            FieldSpec("Character", "Relationship", "Arc Polarity", required = true),
            FieldSpec("Arc Type", "Dropdown", "Arc Polarity", options = "Positive Change,Flat / Steadfast,Fall / Corruption,Disillusionment,Redemption"),
            FieldSpec("Starting Belief (The Lie)", "Long text", "Arc Polarity"),
            FieldSpec("Catalyst & First Threshold", "Long text", "Turning Points"),
            FieldSpec("Midpoint Mirror", "Long text", "Turning Points"),
            FieldSpec("Dark Night / Lowest Ebb", "Long text", "Turning Points"),
            FieldSpec("Final Choice & Transformation", "Long text", "Resolution")
        )
    ),
    TemplateSpec(
        slug = "relationship",
        title = "Relationship Profile",
        category = "Characters & Lineage",
        defaultKind = "Character",
        description = "Chart evolving trust, tension, shared history, and unspoken truths between two figures.",
        sections = listOf("Parties & Bond", "Dynamics & History", "Evolution"),
        fields = listOf(
            FieldSpec("Relationship Title", "Short text", "Parties & Bond", required = true),
            FieldSpec("First Character", "Relationship", "Parties & Bond"),
            FieldSpec("Second Character", "Relationship", "Parties & Bond"),
            FieldSpec("Bond Type", "Dropdown", "Parties & Bond", options = "Siblings,Found Family,Romance,Rivalry,Mentor-Apprentice,Sworn Enemies,Unlikely Allies,Oathbound"),
            FieldSpec("Shared History", "Long text", "Dynamics & History"),
            FieldSpec("What Each Wants From the Other", "Long text", "Dynamics & History"),
            FieldSpec("Core Source of Tension", "Long text", "Dynamics & History"),
            FieldSpec("Progression Beats", "Table", "Evolution", hint = "Stage | Event | Trust Level")
        )
    ),

    // ── FLORA & ECOLOGY ─────────────────────────────────────────────────────
    TemplateSpec(
        slug = "plant",
        title = "Fantasy Flora & Plant",
        category = "Flora & Ecology",
        defaultKind = "Flora",
        description = "Comprehensive botanical compendium covering habitat, lifecycle, culinary/alchemical uses, and arcane traits.",
        sections = listOf("Botanical Identity", "Habitat & Climate", "Anatomy & Lifecycle", "Ecological Role", "Culinary & Alchemical Uses", "Magical Properties & Hazards", "Harvesting & Lore"),
        fields = listOf(
            FieldSpec("Common Name", "Short text", "Botanical Identity", required = true),
            FieldSpec("Scientific / Arcane Binomial", "Short text", "Botanical Identity"),
            FieldSpec("Local Regional Names", "Short text", "Botanical Identity"),
            FieldSpec("Plant Category", "Dropdown", "Botanical Identity", options = "Herb,Tree,Shrub,Vine,Aquatic,Epiphyte,Carnivorous,Crystal-Flora,Parasitic,Other"),
            FieldSpec("Botanical Illustration", "Image", "Botanical Identity"),
            FieldSpec("Native Regions", "Repeatable group", "Habitat & Climate", repeatable = true),
            FieldSpec("Preferred Biome", "Relationship", "Habitat & Climate"),
            FieldSpec("Climate & Light Needs", "Dropdown", "Habitat & Climate", options = "Tropical,Temperate,Alpine,Arid,Subterranean,Twilight/Nocturnal,Astral,Arctic"),
            FieldSpec("Soil & Moisture", "Short text", "Habitat & Climate"),
            FieldSpec("Physical Description", "Long text", "Anatomy & Lifecycle"),
            FieldSpec("Foliage, Bark & Bloom Color", "Color", "Anatomy & Lifecycle", options = "#5B8A72"),
            FieldSpec("Flowering & Fruiting Cycle", "Long text", "Anatomy & Lifecycle"),
            FieldSpec("Pollination & Seed Dispersal", "Long text", "Anatomy & Lifecycle"),
            FieldSpec("Symbiotic & Pollinator Species", "Repeatable group", "Ecological Role", repeatable = true),
            FieldSpec("Grazer & Pest Interactions", "Long text", "Ecological Role"),
            FieldSpec("Edible Parts & Flavor", "Long text", "Culinary & Alchemical Uses"),
            FieldSpec("Medicinal & Alchemical Uses", "Long text", "Culinary & Alchemical Uses"),
            FieldSpec("Toxicity Rating", "Rating", "Culinary & Alchemical Uses"),
            FieldSpec("Has Magical Properties?", "Checkbox", "Magical Properties & Hazards"),
            FieldSpec("Arcane Effects & Potency", "Long text", "Magical Properties & Hazards", conditionField = "Has Magical Properties?", conditionValue = "true"),
            FieldSpec("Defenses & Hazards", "Long text", "Magical Properties & Hazards", hint = "Thorns, illusions, luring scent, spores"),
            FieldSpec("Harvesting Method & Taboos", "Long text", "Harvesting & Lore"),
            FieldSpec("Cultivation & Rarity", "Dropdown", "Harvesting & Lore", options = "Ubiquitous,Common,Uncommon,Rare,Endangered,Mythic"),
            FieldSpec("Folklore & Symbolism", "Long text", "Harvesting & Lore"),
            FieldSpec("Gallery", "Gallery", "Harvesting & Lore")
        )
    ),
    TemplateSpec(
        slug = "herb",
        title = "Magical Herb & Alchemical Reagent",
        category = "Flora & Ecology",
        defaultKind = "Herb",
        description = "Focused apothecary sheet for potion reagents, harvest timing, preparation, and antidotes.",
        sections = listOf("Reagent Identity", "Harvest Conditions", "Preparation & Effects", "Risks & Trade"),
        fields = listOf(
            FieldSpec("Reagent Name", "Short text", "Reagent Identity", required = true),
            FieldSpec("Source Plant / Organism", "Relationship", "Reagent Identity"),
            FieldSpec("Part Used", "Dropdown", "Reagent Identity", options = "Root,Leaf,Blossom,Sap/Resin,Seed,Bark,Pollen,Nectar,Whole Plant"),
            FieldSpec("Potency", "Rating", "Reagent Identity"),
            FieldSpec("Illustration", "Image", "Reagent Identity"),
            FieldSpec("Harvest Moon / Season", "Short text", "Harvest Conditions"),
            FieldSpec("Required Tools or Ritual", "Long text", "Harvest Conditions"),
            FieldSpec("Preparation Method", "Dropdown", "Preparation & Effects", options = "Decoction,Tincture,Poultice,Distillation,Dried Powder,Fresh Infusion,Ash/Burned"),
            FieldSpec("Primary Alchemical Effects", "Long text", "Preparation & Effects"),
            FieldSpec("Synergies & Incompatible Reagents", "Repeatable group", "Preparation & Effects", repeatable = true),
            FieldSpec("Side Effects & Overdose", "Long text", "Risks & Trade"),
            FieldSpec("Antidote / Neutralizer", "Short text", "Risks & Trade"),
            FieldSpec("Market Value & Legality", "Short text", "Risks & Trade")
        )
    ),
    TemplateSpec(
        slug = "sacred_tree",
        title = "Sacred Tree & Ancient Grove",
        category = "Flora & Ecology",
        defaultKind = "Flora",
        description = "Detail world-trees, heartwoods, druid groves, and living botanical landmarks.",
        sections = listOf("Origin & Myth", "Scale & Ecology", "Arcane Resonance", "Guardians & Rites"),
        fields = listOf(
            FieldSpec("Tree or Grove Name", "Short text", "Origin & Myth", required = true),
            FieldSpec("Location", "Relationship", "Origin & Myth"),
            FieldSpec("Age & Planting Myth", "Long text", "Origin & Myth"),
            FieldSpec("Canopy, Roots & Microclimate", "Long text", "Scale & Ecology"),
            FieldSpec("Image", "Image", "Scale & Ecology"),
            FieldSpec("Leyline or Spiritual Power", "Long text", "Arcane Resonance"),
            FieldSpec("Boon or Blessing Granted", "Long text", "Arcane Resonance"),
            FieldSpec("Guardians & Keepers", "Relationship", "Guardians & Rites"),
            FieldSpec("Pilgrimage Rites & Taboos", "Long text", "Guardians & Rites")
        )
    ),
    TemplateSpec(
        slug = "fungus",
        title = "Fungus, Spore & Mycelium",
        category = "Flora & Ecology",
        defaultKind = "Fungus",
        description = "Subterranean glow-caps, hive mycelia, parasitic spores, and twilight decomposers.",
        sections = listOf("Identification", "Substrate & Ecology", "Spores & Effects", "Uses & Hazards"),
        fields = listOf(
            FieldSpec("Name", "Short text", "Identification", required = true),
            FieldSpec("Scientific Name", "Short text", "Identification"),
            FieldSpec("Form & Bioluminescence", "Long text", "Identification"),
            FieldSpec("Glow Color", "Color", "Identification", options = "#5ED0C5"),
            FieldSpec("Image", "Image", "Identification"),
            FieldSpec("Substrate & Habitat", "Long text", "Substrate & Ecology", hint = "Deadwood, cavern stone, living hosts"),
            FieldSpec("Mycelial Network Behavior", "Long text", "Substrate & Ecology"),
            FieldSpec("Spore Dispersal & Triggers", "Long text", "Spores & Effects"),
            FieldSpec("Edibility / Psychoactivity / Toxicity", "Dropdown", "Uses & Hazards", options = "Choice Edible,Medicinal,Hallucinogenic,Paralytic,Lethal,Mind-Altering,Inedible"),
            FieldSpec("Culinary, Dye, or Arcane Uses", "Long text", "Uses & Hazards")
        )
    ),
    TemplateSpec(
        slug = "biome",
        title = "Biome & Ecosystem",
        category = "Flora & Ecology",
        defaultKind = "Biome",
        description = "Model climate bands, keystone species, food webs, and magical environmental pressures.",
        sections = listOf("Climate & Geography", "Living Web", "Phenomena & Resources", "Inhabitants & Hazards"),
        fields = listOf(
            FieldSpec("Biome Name", "Short text", "Climate & Geography", required = true),
            FieldSpec("Climate Classification", "Dropdown", "Climate & Geography", options = "Tropical Rainforest,Savanna,Desert,Temperate Forest,Boreal Taiga,Tundra,Alpine,Wetland,Abyssal/Cavern,Arcane/Unnatural"),
            FieldSpec("Temperature & Precipitation", "Long text", "Climate & Geography"),
            FieldSpec("Terrain & Soil", "Long text", "Climate & Geography"),
            FieldSpec("Visual Reference", "Image", "Climate & Geography"),
            FieldSpec("Keystone Flora", "Repeatable group", "Living Web", repeatable = true),
            FieldSpec("Apex & Keystone Fauna", "Repeatable group", "Living Web", repeatable = true),
            FieldSpec("Food Web Summary", "Long text", "Living Web"),
            FieldSpec("Natural & Arcane Resources", "Repeatable group", "Phenomena & Resources", repeatable = true),
            FieldSpec("Environmental Hazards", "Long text", "Inhabitants & Hazards"),
            FieldSpec("Native Peoples & Adaptations", "Long text", "Inhabitants & Hazards")
        )
    ),
    TemplateSpec(
        slug = "weather",
        title = "Weather & Arcane Climate Phenomenon",
        category = "Flora & Ecology",
        defaultKind = "Biome",
        description = "Design mana-storms, ash-winters, starfalls, and seasonal weather anomalies.",
        sections = listOf("Phenomenon Profile", "Triggers & Cycles", "Impact & Survival"),
        fields = listOf(
            FieldSpec("Phenomenon Name", "Short text", "Phenomenon Profile", required = true),
            FieldSpec("Nature", "Dropdown", "Phenomenon Profile", options = "Natural Meteorological,Magical Storm,Planar Bleed,Curse-Driven,Seasonal Tide"),
            FieldSpec("Sensory Description", "Long text", "Phenomenon Profile"),
            FieldSpec("Omen / Warning Signs", "Long text", "Triggers & Cycles"),
            FieldSpec("Seasonality & Duration", "Short text", "Triggers & Cycles"),
            FieldSpec("Effect on Magic, Travel & Crops", "Long text", "Impact & Survival"),
            FieldSpec("How Locals Shelter or Exploit It", "Long text", "Impact & Survival")
        )
    ),

    // ── FAUNA & MONSTERS ────────────────────────────────────────────────────
    TemplateSpec(
        slug = "animal",
        title = "Fantasy Fauna & Beast",
        category = "Fauna & Monsters",
        defaultKind = "Fauna",
        description = "Naturalist field guide for wildlife, beasts of burden, birds, and aquatic creatures.",
        sections = listOf("Taxonomy & Form", "Habitat & Range", "Diet & Behavior", "Lifecycle & Adaptations", "Human Interaction & Lore"),
        fields = listOf(
            FieldSpec("Common Name", "Short text", "Taxonomy & Form", required = true),
            FieldSpec("Scientific / Scholarly Name", "Short text", "Taxonomy & Form"),
            FieldSpec("Classification", "Dropdown", "Taxonomy & Form", options = "Mammal,Avian,Reptile,Amphibian,Fish,Invertebrate,Chimeric,Elemental,Spirit-Beast,Other"),
            FieldSpec("Size & Weight", "Short text", "Taxonomy & Form"),
            FieldSpec("Illustration", "Image", "Taxonomy & Form"),
            FieldSpec("Anatomy, Coat & Markings", "Long text", "Taxonomy & Form"),
            FieldSpec("Primary Biome", "Relationship", "Habitat & Range"),
            FieldSpec("Territory & Migration", "Long text", "Habitat & Range"),
            FieldSpec("Trophic Role & Diet", "Dropdown", "Diet & Behavior", options = "Herbivore,Carnivore,Omnivore,Insectivore,Filter Feeder,Scavenger,Mana-Feeder,Lithovore"),
            FieldSpec("Foraging / Hunting Tactics", "Long text", "Diet & Behavior"),
            FieldSpec("Social Structure & Vocalizations", "Long text", "Diet & Behavior"),
            FieldSpec("Reproduction & Lifespan", "Long text", "Lifecycle & Adaptations"),
            FieldSpec("Special Adaptations", "Repeatable group", "Lifecycle & Adaptations", repeatable = true),
            FieldSpec("Domestication Potential", "Dropdown", "Human Interaction & Lore", options = "Widely Domesticated,Tameable with Effort,Semi-Feral,Untameable,Sacred/Protected"),
            FieldSpec("Harvestable Materials & Uses", "Repeatable group", "Human Interaction & Lore", repeatable = true),
            FieldSpec("Threat Level", "Rating", "Human Interaction & Lore"),
            FieldSpec("Folklore & Heraldic Meaning", "Long text", "Human Interaction & Lore")
        )
    ),
    TemplateSpec(
        slug = "monster",
        title = "Monster & Aberration",
        category = "Fauna & Monsters",
        defaultKind = "Monster",
        description = "Bestiary entry for apex predators, cursed beasts, eldritch horrors, and dungeon terrors.",
        sections = listOf("Classification & Origin", "Dread Anatomy", "Lair & Hunting", "Abilities & Weaknesses", "Bounty & Folklore"),
        fields = listOf(
            FieldSpec("Monster Name", "Short text", "Classification & Origin", required = true),
            FieldSpec("Category", "Dropdown", "Classification & Origin", options = "Apex Beast,Aberration,Fey Horror,Fiend,Cursed Construct,Shadowspawn,Chimera,Titan"),
            FieldSpec("Origin Myth or Creation", "Long text", "Classification & Origin"),
            FieldSpec("Threat Rating", "Rating", "Classification & Origin"),
            FieldSpec("Illustration", "Image", "Dread Anatomy"),
            FieldSpec("Appearance, Scent & Sounds", "Long text", "Dread Anatomy"),
            FieldSpec("Preferred Lair & Signs of Presence", "Long text", "Lair & Hunting"),
            FieldSpec("Prey & Hunting Strategy", "Long text", "Lair & Hunting"),
            FieldSpec("Supernatural Abilities", "Repeatable group", "Abilities & Weaknesses", repeatable = true),
            FieldSpec("Weaknesses & Silver Bullets", "Repeatable group", "Abilities & Weaknesses", repeatable = true),
            FieldSpec("Wards & Countermeasures", "Long text", "Abilities & Weaknesses"),
            FieldSpec("Alchemical Harvests / Trophies", "Repeatable group", "Bounty & Folklore", repeatable = true),
            FieldSpec("Famous Encounters & Songs", "Long text", "Bounty & Folklore")
        )
    ),
    TemplateSpec(
        slug = "species",
        title = "Sapient Species & Fantasy Race",
        category = "Fauna & Monsters",
        defaultKind = "Species",
        description = "Build distinct non-human or divergent sapient peoples without falling into monocultures.",
        sections = listOf("Biology & Senses", "Lifespan & Development", "Subgroups & Cultures", "Innate Traits & Magic", "Inter-Species Relations"),
        fields = listOf(
            FieldSpec("Species Name", "Short text", "Biology & Senses", required = true),
            FieldSpec("Endonym (Own Name)", "Short text", "Biology & Senses"),
            FieldSpec("Portrait / Reference", "Image", "Biology & Senses"),
            FieldSpec("Anatomy, Height & Variation", "Long text", "Biology & Senses"),
            FieldSpec("Sensory Differences", "Long text", "Biology & Senses", hint = "Darkvision, echolocation, scent memory, magic-sight"),
            FieldSpec("Average & Maximum Lifespan", "Short text", "Lifespan & Development"),
            FieldSpec("Maturity & Aging Stages", "Long text", "Lifespan & Development"),
            FieldSpec("Diet & Metabolism", "Long text", "Lifespan & Development"),
            FieldSpec("Major Cultural Subgroups", "Repeatable group", "Subgroups & Cultures", repeatable = true),
            FieldSpec("Primary Languages", "Repeatable group", "Subgroups & Cultures", repeatable = true),
            FieldSpec("Naming Traditions", "Long text", "Subgroups & Cultures"),
            FieldSpec("Innate Physiological or Arcane Traits", "Repeatable group", "Innate Traits & Magic", repeatable = true),
            FieldSpec("Biological Vulnerabilities", "Long text", "Innate Traits & Magic"),
            FieldSpec("Relations & Stereotypes with Other Peoples", "Table", "Inter-Species Relations", hint = "Species | Standing | Historical Cause")
        )
    ),
    TemplateSpec(
        slug = "dragon",
        title = "Dragon & Mythic Leviathan",
        category = "Fauna & Monsters",
        defaultKind = "Dragon",
        description = "Specialized sheet for ancient wyrms, sea leviathans, and sky-titans.",
        sections = listOf("Lineage & Scale", "Anatomy & Breath", "Lair & Hoard", "Mind & Legend"),
        fields = listOf(
            FieldSpec("Name / True Name", "Short text", "Lineage & Scale", required = true),
            FieldSpec("Flight / Wyrm Classification", "Dropdown", "Lineage & Scale", options = "True Dragon,Wyvern,Sea Leviathan,Feathered Serpent,Drake,Elder Wyrm,Void Dragon"),
            FieldSpec("Scale Color & Sheen", "Color", "Lineage & Scale", options = "#A8423B"),
            FieldSpec("Image", "Image", "Lineage & Scale"),
            FieldSpec("Wingspan & Anatomy", "Long text", "Anatomy & Breath"),
            FieldSpec("Breath Weapon or Cataclysmic Power", "Long text", "Anatomy & Breath"),
            FieldSpec("Weak Spots & Vulnerability", "Long text", "Anatomy & Breath"),
            FieldSpec("Lair Location", "Relationship", "Lair & Hoard"),
            FieldSpec("Nature of Hoard", "Long text", "Lair & Hoard", hint = "Gold, books, memories, souls, star-metal"),
            FieldSpec("Intelligence, Speech & Personality", "Long text", "Mind & Legend"),
            FieldSpec("Age & Dormancy Cycle", "Short text", "Mind & Legend"),
            FieldSpec("Myths, Cults & Slayers", "Long text", "Mind & Legend")
        )
    ),
    TemplateSpec(
        slug = "construct",
        title = "Undead, Construct & Summoned Entity",
        category = "Fauna & Monsters",
        defaultKind = "Monster",
        description = "Golems, clockwork automatons, revenants, liches, and bound planar entities.",
        sections = listOf("Creation & Binding", "Form & Mechanics", "Limitations & Destruction"),
        fields = listOf(
            FieldSpec("Entity Name", "Short text", "Creation & Binding", required = true),
            FieldSpec("Category", "Dropdown", "Creation & Binding", options = "Corporeal Undead,Spectral Undead,Clay/Stone Golem,Clockwork Automaton,Bound Spirit,Planar Summon"),
            FieldSpec("Creator / Binder", "Relationship", "Creation & Binding"),
            FieldSpec("Animation Core or Anchor", "Long text", "Creation & Binding", hint = "Phylactery, rune-heart, unfinished oath"),
            FieldSpec("Form & Sensory Tell", "Long text", "Form & Mechanics"),
            FieldSpec("Will & Autonomy", "Dropdown", "Form & Mechanics", options = "Mindless Automaton,Literal Instruction-Bound,Semi-Autonomous,Full Sapient Will,Rebellious"),
            FieldSpec("Abilities & Immunities", "Repeatable group", "Form & Mechanics", repeatable = true),
            FieldSpec("Banishment or Destruction Method", "Long text", "Limitations & Destruction")
        )
    ),
    TemplateSpec(
        slug = "mount",
        title = "Mount, Warbeast & Pack Animal",
        category = "Fauna & Monsters",
        defaultKind = "Fauna",
        description = "Detail fantasy steeds, griffons, giant beetles, war-elephants, and caravan beasts.",
        sections = listOf("Breed Profile", "Performance & Care", "Tack & Culture"),
        fields = listOf(
            FieldSpec("Breed Name", "Short text", "Breed Profile", required = true),
            FieldSpec("Base Species", "Relationship", "Breed Profile"),
            FieldSpec("Role", "Dropdown", "Breed Profile", options = "Cavalry Charger,Courier Steed,Aerial Mount,Aquatic Mount,Heavy Caravan Beast,Noble Showbreed"),
            FieldSpec("Daily Range & Speed", "Short text", "Performance & Care"),
            FieldSpec("Carry Capacity & Endurance", "Short text", "Performance & Care"),
            FieldSpec("Feed, Stabling & Temperament", "Long text", "Performance & Care"),
            FieldSpec("Specialized Harness & Armor", "Long text", "Tack & Culture"),
            FieldSpec("Cultural Prestige & Cost", "Short text", "Tack & Culture")
        )
    ),

    // ── LOCATIONS & REALMS ──────────────────────────────────────────────────
    TemplateSpec(
        slug = "world_overview",
        title = "World & Setting Bible",
        category = "Locations & Realms",
        defaultKind = "World",
        description = "High-level anchor sheet defining a fictional world's cosmology, premise, and core tensions.",
        sections = listOf("Premise & Atmosphere", "Geography & Cosmos", "Magic & Technology Baseline", "Powers & Core Conflicts"),
        fields = listOf(
            FieldSpec("World Name", "Short text", "Premise & Atmosphere", required = true),
            FieldSpec("Genre & Tone", "Short text", "Premise & Atmosphere"),
            FieldSpec("Elevator Premise", "Long text", "Premise & Atmosphere"),
            FieldSpec("World Map", "Image", "Geography & Cosmos"),
            FieldSpec("Continents, Seas & Skies", "Repeatable group", "Geography & Cosmos", repeatable = true),
            FieldSpec("Suns, Moons & Cosmology", "Long text", "Geography & Cosmos"),
            FieldSpec("Tech & Magic Era", "Dropdown", "Magic & Technology Baseline", options = "Mythic Bronze,High Medieval,Renaissance/Flintlock,Arcane Industrial,Post-Apocalyptic Fantasy,Gaslamp,Other"),
            FieldSpec("Governing Metaphysical Rules", "Long text", "Magic & Technology Baseline"),
            FieldSpec("Major Civilizations", "Repeatable group", "Powers & Core Conflicts", repeatable = true),
            FieldSpec("Age-Defining Conflicts", "Long text", "Powers & Core Conflicts")
        )
    ),
    TemplateSpec(
        slug = "region",
        title = "Continent & Major Region",
        category = "Locations & Realms",
        defaultKind = "Region",
        description = "Topography, climate zones, natural borders, resources, and regional history.",
        sections = listOf("Geography & Climate", "Settlements & Landmarks", "Resources & Hazards", "Politics & History"),
        fields = listOf(
            FieldSpec("Region Name", "Short text", "Geography & Climate", required = true),
            FieldSpec("Parent World / Continent", "Relationship", "Geography & Climate"),
            FieldSpec("Regional Map", "Image", "Geography & Climate"),
            FieldSpec("Terrain & Natural Borders", "Long text", "Geography & Climate"),
            FieldSpec("Climate & Seasons", "Long text", "Geography & Climate"),
            FieldSpec("Major Settlements", "Repeatable group", "Settlements & Landmarks", repeatable = true),
            FieldSpec("Wonders & Landmarks", "Repeatable group", "Settlements & Landmarks", repeatable = true),
            FieldSpec("Key Resources & Exports", "Repeatable group", "Resources & Hazards", repeatable = true),
            FieldSpec("Regional Threats & Monsters", "Long text", "Resources & Hazards"),
            FieldSpec("Controlling Powers & History", "Long text", "Politics & History")
        )
    ),
    TemplateSpec(
        slug = "country",
        title = "Kingdom, Nation & Empire",
        category = "Locations & Realms",
        defaultKind = "Kingdom",
        description = "Sovereignty, borders, demographics, economy, diplomacy, and internal fissures.",
        sections = listOf("Sovereignty & Symbols", "Demographics & Lands", "Economy & Military", "Diplomacy & Fault Lines"),
        fields = listOf(
            FieldSpec("Nation Name", "Short text", "Sovereignty & Symbols", required = true),
            FieldSpec("Government Type", "Dropdown", "Sovereignty & Symbols", options = "Absolute Monarchy,Constitutional Monarchy,Merchant Republic,Theocracy,Magocracy,Imperial Federation,Tribal Confederacy,City-State League"),
            FieldSpec("Current Ruler / Council", "Relationship", "Sovereignty & Symbols"),
            FieldSpec("Banner & Heraldry", "Image", "Sovereignty & Symbols"),
            FieldSpec("National Color", "Color", "Sovereignty & Symbols", options = "#3F608C"),
            FieldSpec("Capital City", "Relationship", "Demographics & Lands"),
            FieldSpec("Provinces & Holdings", "Repeatable group", "Demographics & Lands", repeatable = true),
            FieldSpec("Population & Primary Languages", "Long text", "Demographics & Lands"),
            FieldSpec("Core Industries & Currency", "Long text", "Economy & Military"),
            FieldSpec("Standing Forces & Defenses", "Long text", "Economy & Military"),
            FieldSpec("Allies & Treaties", "Repeatable group", "Diplomacy & Fault Lines", repeatable = true),
            FieldSpec("Rivals & Wars", "Repeatable group", "Diplomacy & Fault Lines", repeatable = true),
            FieldSpec("Internal Unrest & Succession Crises", "Long text", "Diplomacy & Fault Lines")
        )
    ),
    TemplateSpec(
        slug = "city",
        title = "City, Town & Settlement",
        category = "Locations & Realms",
        defaultKind = "Settlement",
        description = "Sensory atmosphere, wards, economy, governance, defenses, and hidden underworld.",
        sections = listOf("Overview & Atmosphere", "Districts & Landmarks", "Governance & Economy", "Defenses & Secrets"),
        fields = listOf(
            FieldSpec("Settlement Name", "Short text", "Overview & Atmosphere", required = true),
            FieldSpec("Size Tier", "Dropdown", "Overview & Atmosphere", options = "Hamlet,Village,Market Town,Walled City,Metropolis,Nomadic Camp,Sky/Sea City"),
            FieldSpec("Approximate Population", "Number", "Overview & Atmosphere"),
            FieldSpec("Parent Nation / Region", "Relationship", "Overview & Atmosphere"),
            FieldSpec("Sights, Sounds & Smells", "Long text", "Overview & Atmosphere"),
            FieldSpec("City Map", "Image", "Districts & Landmarks"),
            FieldSpec("Districts / Wards", "Table", "Districts & Landmarks", hint = "Ward | Character | Key Landmark"),
            FieldSpec("Local Government & Watch", "Long text", "Governance & Economy"),
            FieldSpec("Primary Trade, Guilds & Markets", "Long text", "Governance & Economy"),
            FieldSpec("Walls, Gates & Weaknesses", "Long text", "Defenses & Secrets"),
            FieldSpec("Underworld, Sewers & Rumors", "Long text", "Defenses & Secrets")
        )
    ),
    TemplateSpec(
        slug = "district",
        title = "Settlement District & Ward",
        category = "Locations & Realms",
        defaultKind = "Location",
        description = "Zoom into a distinct quarter, harbor, mage-ward, or slum within a larger city.",
        sections = listOf("Character & Layout", "People & Economy", "Tensions & Secrets"),
        fields = listOf(
            FieldSpec("District Name", "Short text", "Character & Layout", required = true),
            FieldSpec("Parent Settlement", "Relationship", "Character & Layout"),
            FieldSpec("Architecture & Street Vibe", "Long text", "Character & Layout"),
            FieldSpec("Notable Establishments", "Repeatable group", "Character & Layout", repeatable = true),
            FieldSpec("Dominant Factions & Residents", "Long text", "People & Economy"),
            FieldSpec("Street Commerce & Contraband", "Long text", "People & Economy"),
            FieldSpec("Curfews, Hazards & Secrets", "Long text", "Tensions & Secrets")
        )
    ),
    TemplateSpec(
        slug = "tavern",
        title = "Tavern, Inn & Guild Shop",
        category = "Locations & Realms",
        defaultKind = "Location",
        description = "Memorable inns, apothecaries, smithies, and guildhalls with menus, staff, and rumors.",
        sections = listOf("Storefront & Atmosphere", "Staff & Clientele", "Menu, Stock & Prices", "Secrets & Hooks"),
        fields = listOf(
            FieldSpec("Establishment Name", "Short text", "Storefront & Atmosphere", required = true),
            FieldSpec("Type", "Dropdown", "Storefront & Atmosphere", options = "Tavern/Inn,Apothecary,Blacksmith,Bookshop/Archive,Guildhall,Black Market Stall,Teahouse,Other"),
            FieldSpec("Location", "Relationship", "Storefront & Atmosphere"),
            FieldSpec("Signboard & Interior", "Long text", "Storefront & Atmosphere"),
            FieldSpec("Proprietor", "Relationship", "Staff & Clientele"),
            FieldSpec("Usual Crowd", "Long text", "Staff & Clientele"),
            FieldSpec("Menu / Inventory & Prices", "Table", "Menu, Stock & Prices", hint = "Item or Drink | Price | Notes"),
            FieldSpec("House Rules & Quirks", "Long text", "Secrets & Hooks"),
            FieldSpec("Backrooms, Vaults & Rumors", "Long text", "Secrets & Hooks")
        )
    ),
    TemplateSpec(
        slug = "stronghold",
        title = "Stronghold, Castle & Keep",
        category = "Locations & Realms",
        defaultKind = "Stronghold",
        description = "Fortifications, garrison strength, water/supply resilience, and siege vulnerabilities.",
        sections = listOf("Command & Site", "Fortifications & Layout", "Garrison & Supply", "Siege Weaknesses"),
        fields = listOf(
            FieldSpec("Stronghold Name", "Short text", "Command & Site", required = true),
            FieldSpec("Owner / Commander", "Relationship", "Command & Site"),
            FieldSpec("Strategic Terrain", "Long text", "Command & Site"),
            FieldSpec("Floor Plan / Elevation", "Image", "Fortifications & Layout"),
            FieldSpec("Walls, Towers & Wards", "Long text", "Fortifications & Layout"),
            FieldSpec("Key Chambers", "Repeatable group", "Fortifications & Layout", repeatable = true),
            FieldSpec("Garrison Size", "Number", "Garrison & Supply"),
            FieldSpec("Water, Granary & Siege Endurance", "Short text", "Garrison & Supply"),
            FieldSpec("Postern Gates & Known Flaws", "Long text", "Siege Weaknesses")
        )
    ),
    TemplateSpec(
        slug = "dungeon",
        title = "Ruin, Dungeon & Vault",
        category = "Locations & Realms",
        defaultKind = "Dungeon",
        description = "Ancient tombs, sunken temples, and arcane vaults with room tables, traps, and guardians.",
        sections = listOf("Origin & Entrance", "Layout & Chambers", "Traps, Guardians & Treasure", "Lore & Aftermath"),
        fields = listOf(
            FieldSpec("Site Name", "Short text", "Origin & Entrance", required = true),
            FieldSpec("Original Builders & Purpose", "Long text", "Origin & Entrance"),
            FieldSpec("Danger Level", "Rating", "Origin & Entrance"),
            FieldSpec("Entrance & Seal", "Long text", "Origin & Entrance"),
            FieldSpec("Map / Layout", "Image", "Layout & Chambers"),
            FieldSpec("Key Chambers", "Table", "Layout & Chambers", hint = "Room | Feature | Hazard/Denizen"),
            FieldSpec("Traps, Puzzles & Wards", "Repeatable group", "Traps, Guardians & Treasure", repeatable = true),
            FieldSpec("Guardians & Inhabitants", "Repeatable group", "Traps, Guardians & Treasure", repeatable = true),
            FieldSpec("Relics & Hidden Loot", "Repeatable group", "Traps, Guardians & Treasure", repeatable = true),
            FieldSpec("What Awakes If Disturbed", "Long text", "Lore & Aftermath")
        )
    ),
    TemplateSpec(
        slug = "temple",
        title = "Temple, Shrine & Sacred Site",
        category = "Locations & Realms",
        defaultKind = "Location",
        description = "Sanctuaries, pilgrimage shrines, and oracular groves dedicated to gods or spirits.",
        sections = listOf("Sanctuary & Patron", "Architecture & Relics", "Clergy & Rites"),
        fields = listOf(
            FieldSpec("Sanctuary Name", "Short text", "Sanctuary & Patron", required = true),
            FieldSpec("Dedicated To", "Relationship", "Sanctuary & Patron"),
            FieldSpec("Location", "Relationship", "Sanctuary & Patron"),
            FieldSpec("Sacred Architecture & Altar", "Long text", "Architecture & Relics"),
            FieldSpec("Housed Relics or Miracles", "Repeatable group", "Architecture & Relics", repeatable = true),
            FieldSpec("Resident Clergy & Caretakers", "Long text", "Clergy & Rites"),
            FieldSpec("Offerings, Prayers & Taboos", "Long text", "Clergy & Rites")
        )
    ),
    TemplateSpec(
        slug = "realm",
        title = "Plane, Realm & Afterlife",
        category = "Locations & Realms",
        defaultKind = "Realm",
        description = "Feywilds, shadow realms, elemental planes, dreamlands, and halls of the dead.",
        sections = listOf("Cosmological Nature", "Physical & Time Laws", "Landscape & Denizens", "Access & Hazards"),
        fields = listOf(
            FieldSpec("Plane / Realm Name", "Short text", "Cosmological Nature", required = true),
            FieldSpec("Plane Type", "Dropdown", "Cosmological Nature", options = "Material Prime,Fey/Twilight,Shadow/Umbral,Elemental,Astral/Dream,Celestial,Infernal/Abyssal,Afterlife,Pocket Dimension"),
            FieldSpec("Visual & Sensory Essence", "Long text", "Cosmological Nature"),
            FieldSpec("Governing Laws & Magic Shifts", "Long text", "Physical & Time Laws"),
            FieldSpec("Time Flow Ratio", "Short text", "Physical & Time Laws", hint = "e.g., 1 day inside = 1 year outside"),
            FieldSpec("Native Inhabitants & Rulers", "Repeatable group", "Landscape & Denizens", repeatable = true),
            FieldSpec("Entry Methods & Keys", "Long text", "Access & Hazards"),
            FieldSpec("Price of Lingering Too Long", "Long text", "Access & Hazards")
        )
    ),
    TemplateSpec(
        slug = "portal",
        title = "Portal, Leyline & Nexus",
        category = "Locations & Realms",
        defaultKind = "Location",
        description = "Ancient waystones, planar gates, and magical conduits connecting distant realms.",
        sections = listOf("Endpoints & Form", "Activation & Costs", "Stability & Guardians"),
        fields = listOf(
            FieldSpec("Gate / Nexus Name", "Short text", "Endpoints & Form", required = true),
            FieldSpec("Origin Location", "Relationship", "Endpoints & Form"),
            FieldSpec("Destination", "Relationship", "Endpoints & Form"),
            FieldSpec("Physical Appearance", "Long text", "Endpoints & Form"),
            FieldSpec("Key, Phrase, or Astral Alignment", "Long text", "Activation & Costs"),
            FieldSpec("Energy Cost & Side Effects", "Long text", "Activation & Costs"),
            FieldSpec("Stability & Restrictions", "Long text", "Stability & Guardians"),
            FieldSpec("Guardians & Control", "Long text", "Stability & Guardians")
        )
    ),

    // ── ITEMS & ARTIFACTS ───────────────────────────────────────────────────
    TemplateSpec(
        slug = "artifact",
        title = "Item, Relic & Artifact",
        category = "Items & Artifacts",
        defaultKind = "Artifact",
        description = "Legendary relics, cursed heirlooms, and enchanted tools with origin, costs, and owner lineage.",
        sections = listOf("Overview & Craft", "Origin & Maker", "Powers & Mechanics", "Curses & Limitations", "Provenance"),
        fields = listOf(
            FieldSpec("Item / Relic Name", "Short text", "Overview & Craft", required = true),
            FieldSpec("Item Category", "Dropdown", "Overview & Craft", options = "Weapon,Armor,Jewelry/Focus,Tome,Vessel,Key,Relic/Idol,Tool,Instrument,Other"),
            FieldSpec("Rarity", "Dropdown", "Overview & Craft", options = "Common,Uncommon,Rare,Legendary,Mythic,One-of-a-Kind"),
            FieldSpec("Illustration", "Image", "Overview & Craft"),
            FieldSpec("Materials & Inscriptions", "Long text", "Overview & Craft"),
            FieldSpec("Creator & Age Forged", "Long text", "Origin & Maker"),
            FieldSpec("Intended Purpose", "Long text", "Origin & Maker"),
            FieldSpec("Primary Powers & Activation", "Long text", "Powers & Mechanics"),
            FieldSpec("Charges / Attunement Rules", "Long text", "Powers & Mechanics"),
            FieldSpec("Is Cursed or Sentient?", "Checkbox", "Curses & Limitations"),
            FieldSpec("Curse / Sentience Personality", "Long text", "Curses & Limitations", conditionField = "Is Cursed or Sentient?", conditionValue = "true"),
            FieldSpec("Hard Limitations & Cost to Wield", "Long text", "Curses & Limitations"),
            FieldSpec("Current Holder", "Relationship", "Provenance"),
            FieldSpec("Lineage of Previous Owners", "Table", "Provenance", hint = "Owner | Era | How Lost or Passed On")
        )
    ),
    TemplateSpec(
        slug = "weapon",
        title = "Weapon, Armor & Martial Gear",
        category = "Items & Artifacts",
        defaultKind = "Item",
        description = "Specs, metallurgy, runes, fighting style synergy, and maintenance for signature gear.",
        sections = listOf("Classification & Specs", "Materials & Enchantments", "Combat & History"),
        fields = listOf(
            FieldSpec("Name", "Short text", "Classification & Specs", required = true),
            FieldSpec("Gear Class", "Dropdown", "Classification & Specs", options = "Blade,Polearm,Bow/Crossbow,Blunt,Shield,Light Armor,Heavy Armor,Arcane Focus,Sidearm"),
            FieldSpec("Weight, Balance & Dimensions", "Short text", "Classification & Specs"),
            FieldSpec("Illustration", "Image", "Classification & Specs"),
            FieldSpec("Alloy / Special Materials", "Relationship", "Materials & Enchantments"),
            FieldSpec("Runes or Enchantments", "Repeatable group", "Materials & Enchantments", repeatable = true),
            FieldSpec("Strengths & Drawbacks in Combat", "Long text", "Combat & History"),
            FieldSpec("Maintenance & Wielder Requirements", "Long text", "Combat & History"),
            FieldSpec("Known History & Named Strikes", "Long text", "Combat & History")
        )
    ),
    TemplateSpec(
        slug = "grimoire",
        title = "Grimoire, Tome & Forbidden Scroll",
        category = "Items & Artifacts",
        defaultKind = "Item",
        description = "Catalog spellbooks, ancient codices, illuminated manuscripts, and ciphered journals.",
        sections = listOf("Binding & Script", "Contents & Spells", "Wards & Provenance"),
        fields = listOf(
            FieldSpec("Title", "Short text", "Binding & Script", required = true),
            FieldSpec("Author / Scribe", "Short text", "Binding & Script"),
            FieldSpec("Language & Script", "Relationship", "Binding & Script"),
            FieldSpec("Binding, Ink & Paper", "Long text", "Binding & Script"),
            FieldSpec("Chapters & Contained Spells", "Repeatable group", "Contents & Spells", repeatable = true),
            FieldSpec("Dangerous Knowledge Within", "Long text", "Contents & Spells"),
            FieldSpec("Protective Wards or Ciphers", "Long text", "Wards & Provenance"),
            FieldSpec("Current Keeper & Copies Known", "Long text", "Wards & Provenance")
        )
    ),
    TemplateSpec(
        slug = "potion",
        title = "Potion, Elixir & Poison",
        category = "Items & Artifacts",
        defaultKind = "Potion",
        description = "Brewing formulas, sensory cues, onset/duration, side effects, and antidotes.",
        sections = listOf("Brew Profile", "Formula & Brewing", "Effects & Antidote"),
        fields = listOf(
            FieldSpec("Brew Name", "Short text", "Brew Profile", required = true),
            FieldSpec("Classification", "Dropdown", "Brew Profile", options = "Healing Elixir,Stat/Enhancement Draught,Utility Vial,Ingested Poison,Contact Toxin,Inhaled Vapor,Truth Serum,Curse-Breaker"),
            FieldSpec("Color, Viscosity & Scent", "Long text", "Brew Profile"),
            FieldSpec("Vial Color", "Color", "Brew Profile", options = "#70C1B3"),
            FieldSpec("Key Ingredients", "Table", "Formula & Brewing", hint = "Ingredient | Quantity | Substitution"),
            FieldSpec("Brewing Process & Skill Required", "Long text", "Formula & Brewing"),
            FieldSpec("Onset, Effects & Duration", "Long text", "Effects & Antidote"),
            FieldSpec("Side Effects & Overdose Risk", "Long text", "Effects & Antidote"),
            FieldSpec("Antidote & Black-Market Price", "Long text", "Effects & Antidote")
        )
    ),
    TemplateSpec(
        slug = "mineral",
        title = "Mineral, Ore, Gem & Fantasy Material",
        category = "Items & Artifacts",
        defaultKind = "Material",
        description = "Star-metals, soul-gems, living wood, and alchemical ores.",
        sections = listOf("Properties & Origin", "Extraction & Refining", "Craft & Economy"),
        fields = listOf(
            FieldSpec("Material Name", "Short text", "Properties & Origin", required = true),
            FieldSpec("Category", "Dropdown", "Properties & Origin", options = "Metal/Alloy,Gemstone,Crystal,Stone,Timber,Bone/Chitin,Alchemical Salt,Planar Matter"),
            FieldSpec("Appearance & Color", "Color", "Properties & Origin", options = "#A7C4E0"),
            FieldSpec("Physical & Magical Properties", "Long text", "Properties & Origin"),
            FieldSpec("Where Found & Mining Hazards", "Long text", "Extraction & Refining"),
            FieldSpec("Smelting / Shaping Requirements", "Long text", "Extraction & Refining"),
            FieldSpec("Masterwork & Arcane Uses", "Long text", "Craft & Economy"),
            FieldSpec("Rarity & Trade Monopolies", "Long text", "Craft & Economy")
        )
    ),
    TemplateSpec(
        slug = "vessel",
        title = "Vessel, Airship & Caravan Wagon",
        category = "Items & Artifacts",
        defaultKind = "Vessel",
        description = "Sailing ships, sky-galleons, subterranean crawlers, and armored merchant wagons.",
        sections = listOf("Vessel Identity", "Propulsion & Crew", "Armament, Cargo & History"),
        fields = listOf(
            FieldSpec("Vessel Name / Class", "Short text", "Vessel Identity", required = true),
            FieldSpec("Medium", "Dropdown", "Vessel Identity", options = "Oceanic Ship,Airship / Skyvessel,Land Caravan,Subterranean,Planar Skiff"),
            FieldSpec("Dimensions & Layout", "Long text", "Vessel Identity"),
            FieldSpec("Blueprint / Sketch", "Image", "Vessel Identity"),
            FieldSpec("Propulsion & Fuel / Magic", "Long text", "Propulsion & Crew"),
            FieldSpec("Minimum & Full Crew", "Short text", "Propulsion & Crew"),
            FieldSpec("Speed & Max Range", "Short text", "Propulsion & Crew"),
            FieldSpec("Defenses, Weapons & Cargo Hold", "Long text", "Armament, Cargo & History"),
            FieldSpec("Captain & Voyages", "Relationship", "Armament, Cargo & History")
        )
    ),
    TemplateSpec(
        slug = "inventory",
        title = "Personal Inventory & Treasure Hoard",
        category = "Items & Artifacts",
        defaultKind = "Item",
        description = "Track carried gear, pack weight, currency pouches, or vault contents.",
        sections = listOf("Owner & Vault", "Item Ledger", "Wealth & Notes"),
        fields = listOf(
            FieldSpec("Inventory / Hoard Title", "Short text", "Owner & Vault", required = true),
            FieldSpec("Owner / Keeper", "Relationship", "Owner & Vault"),
            FieldSpec("Items Ledger", "Table", "Item Ledger", hint = "Item | Qty | Weight/Notes | Origin"),
            FieldSpec("Coinage & Gems", "Long text", "Wealth & Notes"),
            FieldSpec("Hidden Compartments or Curses", "Long text", "Wealth & Notes")
        )
    )
)
