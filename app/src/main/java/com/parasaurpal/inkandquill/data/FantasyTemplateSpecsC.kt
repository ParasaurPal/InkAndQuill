package com.parasaurpal.inkandquill.data

private fun wikiFields(section: String, vararg names: String): List<FieldSpec> =
    names.mapIndexed { index, name ->
        val type = when {
            name.contains("Image", true) || name.contains("Portrait", true) || name.contains("Map", true) || name.contains("Diagram", true) -> "Image"
            name.contains("Gallery", true) || name.contains("Visual references", true) -> "Gallery"
            name.contains("Related", true) || name.contains("Members", true) || name.contains("Species", true) || name.contains("Locations", true) || name.contains("People", true) || name.contains("Source", true) || name.contains("Target", true) -> "Relationship"
            name.contains("Timeline", true) || name.contains("Stages", true) || name.contains("Records", true) || name.contains("Examples", true) || name.contains("Events", true) || name.contains("Phases", true) -> "Table"
            else -> "Long text"
        }
        FieldSpec(name, type, section, required = index == 0 && name.contains("Name", true))
    }

private fun wikiSpec(
    slug: String,
    title: String,
    category: String,
    kind: String,
    description: String,
    sections: List<Pair<String, List<FieldSpec>>>
) = TemplateSpec(slug, title, category, kind, description, sections.map { it.first }, sections.flatMap { it.second })

internal val fantasySpecsPartC: List<TemplateSpec> = listOf(
    wikiSpec("historical-person", "Historical Figure & Biography", "Characters & Lineage", "Character",
        "A historically grounded biography tracking a person's life, context, influence, sources and contested legacy.",
        listOf(
            "Identity & chronology" to wikiFields("Identity & chronology", "Full Name", "Aliases & Titles", "Birth and Death", "Portrait / Historical depiction", "Places of origin and death"),
            "Life & context" to wikiFields("Life & context", "Family and formative years", "Education and mentors", "Social class and historical context", "Beliefs and worldview", "Personality and private life"),
            "Career & actions" to wikiFields("Career & actions", "Offices, occupations and roles", "Major actions and achievements", "Works, discoveries or campaigns", "Allies, rivals and associates", "Chronology of major events"),
            "Legacy & evidence" to wikiFields("Legacy & evidence", "Immediate consequences", "Long-term influence", "Later interpretations and controversies", "Primary sources and citations", "Reliability and disputed claims")
        )),
    wikiSpec("monarch", "Monarch & Sovereign", "Characters & Lineage", "Character",
        "A sovereign's personal dossier connected to the institutions, succession, court and reign they shaped.",
        listOf(
            "Sovereign identity" to wikiFields("Sovereign identity", "Regnal Name", "Birth Name and Epithets", "Portrait and Regalia", "Dynasty and Claim", "Reign Start and End"),
            "Accession & legitimacy" to wikiFields("Accession & legitimacy", "Succession Law and Claim", "Coronation and Recognition", "Rivals and Pretenders", "Regency or Co-rule", "Legitimacy Challenges"),
            "Rule & court" to wikiFields("Rule & court", "Governing Philosophy", "Court and Household", "Advisers and Ministers", "Laws and Reforms", "Foreign Policy and Diplomacy"),
            "Personal & historical record" to wikiFields("Personal & historical record", "Consorts and Heirs", "Family and Private Life", "Wars and Crises", "Major Decisions and Turning Points", "Death, Abdication and Succession", "Historical Reputation")
        )),
    wikiSpec("tree", "Tree & Woody Plant", "Flora & Ecology", "Flora",
        "A botanical dossier for woody plants, from taxonomy and anatomy to phenology, reproduction and ecological roles.",
        listOf(
            "Taxonomy & identity" to wikiFields("Taxonomy & identity", "Common Name", "Scientific / Taxonomic Name", "Family, Genus and Species", "Synonyms and Regional Names", "Reference Image and Leaf Detail"),
            "Anatomy & morphology" to wikiFields("Anatomy & morphology", "Growth Form and Mature Dimensions", "Root System", "Trunk, Bark and Wood", "Branches, Buds and Canopy", "Leaves / Needles and Seasonal Color", "Flowers, Cones and Fruit"),
            "Life cycle & habitat" to wikiFields("Life cycle & habitat", "Pollination and Reproduction", "Seed Dispersal and Germination", "Growth Rate and Lifespan", "Native Range and Habitat", "Soil, Water and Climate Requirements", "Seasonal Phenology"),
            "Ecology & human use" to wikiFields("Ecology & human use", "Associated Species and Food Web", "Ecological Services and Keystone Role", "Pests, Diseases and Threats", "Timber, Food, Medicine and Craft Uses", "Cultural and Symbolic Importance", "Cultivation and Conservation")
        )),
    wikiSpec("creature", "Creature & Animal Profile", "Fauna & Monsters", "Fauna",
        "A creature profile emphasizing classification, body plan, senses, behavior, habitat, ecology and interaction with people.",
        listOf(
            "Classification & appearance" to wikiFields("Classification & appearance", "Creature Name", "Taxonomy and Related Species", "Anatomy / Silhouette Image", "Size, Mass and Sexual Dimorphism", "Coloration, Covering and Distinctive Features"),
            "Anatomy & physiology" to wikiFields("Anatomy & physiology", "Skeletal and Muscular Structure", "Organs and Internal Systems", "Locomotion and Movement", "Senses and Perception", "Diet, Digestion and Metabolism", "Defenses, Venom or Special Adaptations"),
            "Behavior & life history" to wikiFields("Behavior & life history", "Temperament and Intelligence", "Social Structure and Communication", "Reproduction and Mating", "Development and Life Stages", "Lifespan and Mortality"),
            "Ecology & relationship" to wikiFields("Ecology & relationship", "Habitat and Geographic Range", "Niche, Predators and Prey", "Seasonal and Migration Patterns", "Domestication and Human Interactions", "Conservation / Threat Status", "Folklore and Cultural Meaning")
        )),
    wikiSpec("kingdom", "Kingdom & Sovereign State", "Locations & Realms", "Kingdom",
        "A state-level reference for territory, institutions, law, succession, population, economy, military and diplomacy.",
        listOf(
            "Territory & identity" to wikiFields("Territory & identity", "Kingdom Name", "Map and Territorial Extent", "Capital and Major Regions", "Borders and Geographic Features", "Population and Demographics"),
            "Government & law" to wikiFields("Government & law", "Government Form and Constitutional Basis", "Monarch / Head of State", "Succession and Legitimacy", "Administrative Divisions", "Courts, Laws and Rights", "Nobility and Bureaucracy"),
            "Society & economy" to wikiFields("Society & economy", "Languages and Cultural Groups", "Religion and Institutions", "Class Structure and Citizenship", "Agriculture, Industry and Trade", "Currency, Taxation and Resources"),
            "Power & history" to wikiFields("Power & history", "Military Organization and Doctrine", "Allies, Rivals and Treaties", "Wars, Revolutions and Crises", "Founding and Historical Periods", "Current Conflicts and Stability")
        )),
    wikiSpec("settlement", "Settlement & Community", "Locations & Realms", "Settlement",
        "A settlement record from village to metropolis, covering plan, districts, services, population and local life.",
        listOf(
            "Identity & setting" to wikiFields("Identity & setting", "Settlement Name", "Settlement Type and Status", "Map / Aerial View", "Location and Surrounding Terrain", "Founding Date and Founder"),
            "Layout & architecture" to wikiFields("Layout & architecture", "Street Plan and Urban Form", "Districts and Neighborhoods", "Walls, Gates and Defenses", "Architecture and Building Materials", "Landmarks and Public Spaces"),
            "Population & institutions" to wikiFields("Population & institutions", "Population and Demographics", "Government and Local Authority", "Guilds, Temples and Institutions", "Services, Markets and Infrastructure", "Culture, Festivals and Daily Life"),
            "Economy & change" to wikiFields("Economy & change", "Local Industries and Trade", "Food, Water and Waste Systems", "Transport and Connections", "Threats, Disasters and Defenses", "Growth, Decline and Current Issues")
        )),
    wikiSpec("landmark", "Landmark & Point of Interest", "Locations & Realms", "Location",
        "A focused place entry for notable natural, built, sacred, political or historical sites.",
        listOf(
            "Identity & location" to wikiFields("Identity & location", "Landmark Name", "Type and Significance", "Image / Site Plan", "Location and Access", "Dimensions and Distinctive Features"),
            "Origin & structure" to wikiFields("Origin & structure", "Creator or Natural Formation", "Date and Construction History", "Materials and Engineering", "Layout and Components", "Restorations and Alterations"),
            "Meaning & use" to wikiFields("Meaning & use", "Original Purpose", "Current Function and Ownership", "Religious, Cultural or Political Meaning", "Associated People and Events", "Rituals, Customs and Visitor Practices"),
            "Condition & records" to wikiFields("Condition & records", "Condition and Preservation", "Hazards and Restrictions", "Legends and Unverified Claims", "Related Places and Records", "Sources and Field Notes")
        )),
    wikiSpec("religion", "Religion & Religious Tradition", "Belief & Myth", "Religion",
        "A full account of a faith tradition, theology, institutions, sacred texts, worship, ethics and historical change.",
        listOf(
            "Identity & theology" to wikiFields("Identity & theology", "Religion Name", "Tradition and Denominations", "Founding Figure / Revelation", "Cosmology and Nature of the Divine", "Core Doctrines and Truth Claims"),
            "Sacred knowledge" to wikiFields("Sacred knowledge", "Sacred Texts and Oral Traditions", "Canon Formation and Interpretation", "Creation, Afterlife and Eschatology", "Theological Disputes and Heresies", "Priesthood and Religious Offices"),
            "Worship & practice" to wikiFields("Worship & practice", "Worship Forms and Prayer", "Rituals, Sacraments and Offerings", "Holy Days and Calendar", "Temples, Shrines and Sacred Sites", "Dietary Rules, Ethics and Taboos"),
            "Community & history" to wikiFields("Community & history", "Followers and Demographics", "Institutions and Organization", "Conversion, Mission and Education", "Schisms, Reforms and Persecution", "Political and Cultural Influence", "Modern Practice and Regional Variants")
        )),
    wikiSpec("organization", "Organization & Institution", "Cultures & Societies", "Faction",
        "An institutional dossier for civic bodies, companies, academies, orders and other organized groups.",
        listOf(
            "Identity & mandate" to wikiFields("Identity & mandate", "Organization Name", "Organization Type", "Emblem / Official Image", "Founding Date and Founders", "Mission, Mandate and Public Image"),
            "Structure & membership" to wikiFields("Structure & membership", "Leadership and Governance", "Departments and Internal Hierarchy", "Membership Requirements and Ranks", "Recruitment, Training and Discipline", "Headquarters and Branches"),
            "Operations & resources" to wikiFields("Operations & resources", "Core Activities and Methods", "Assets, Funding and Property", "Territorial Reach and Influence", "Records, Secrets and Security", "Services and Responsibilities"),
            "Relationships & history" to wikiFields("Relationships & history", "Allies, Rivals and Regulators", "Key Members and Associated Figures", "Major Operations and Events", "Scandals, Splits and Reforms", "Current Status and Future Plans")
        )),
    wikiSpec("magic-system", "Magic System", "Magic & Systems", "Magic System",
        "The governing specification for supernatural mechanics, including sources, laws, costs, limits, interactions and exceptions.",
        listOf(
            "Foundation & source" to wikiFields("Foundation & source", "System Name", "Source and Nature of Power", "Who or What Can Use It", "Access, Awakening and Aptitude", "Visual / Conceptual Reference"),
            "Mechanics & laws" to wikiFields("Mechanics & laws", "Core Mechanisms and Processes", "Rules, Laws and Invariants", "Inputs, Components and Catalysts", "Casting / Activation Procedure", "Measurement, Detection and Verification"),
            "Disciplines & progression" to wikiFields("Disciplines & progression", "Schools and Disciplines", "Learning, Training and Mastery", "Power Scale and Progression", "Specializations and Applications", "Interactions Between Disciplines"),
            "Costs & boundaries" to wikiFields("Costs & boundaries", "Physical, Mental and Social Costs", "Hard Limits and Failure Conditions", "Counters, Resistances and Nullification", "Risks, Corruption and Side Effects", "Exceptions, Edge Cases and Unresolved Rules")
        )),
    wikiSpec("discipline", "Magic Discipline & School", "Magic & Systems", "Magic System",
        "A specialized branch of a magical system with its own methods, curriculum, capabilities and restrictions.",
        listOf(
            "Identity & philosophy" to wikiFields("Identity & philosophy", "Discipline Name", "Parent Magic System", "Philosophy and Governing Principle", "Symbols, Colors and Visual Language", "Historical Founder"),
            "Practice & curriculum" to wikiFields("Practice & curriculum", "Core Techniques and Methods", "Required Knowledge and Aptitudes", "Training Stages and Initiation", "Tools, Components and Focuses", "Exercises and Standard Tests"),
            "Capabilities & limitations" to wikiFields("Capabilities & limitations", "Known Effects and Applications", "Advanced and Forbidden Practices", "Costs, Risks and Side Effects", "Counters and Interactions", "Ethical Rules and Taboos"),
            "Community & record" to wikiFields("Community & record", "Practitioners and Orders", "Institutions and Archives", "Historical Practitioners", "Regional Variants and Rival Schools", "Open Questions")
        )),
    wikiSpec("material", "Material & Substance", "Items & Artifacts", "Material",
        "A material-science record for natural, crafted, magical or synthetic substances and their practical behavior.",
        listOf(
            "Identity & origin" to wikiFields("Identity & origin", "Material Name", "Classification and Composition", "Source, Deposit or Manufacture", "Appearance / Sample Image", "Natural or Synthetic Origin"),
            "Physical properties" to wikiFields("Physical properties", "Density, Hardness and Strength", "Texture, Color and Luster", "Thermal and Electrical Properties", "Solubility and Chemical Reactivity", "Durability, Aging and Degradation"),
            "Processing & interaction" to wikiFields("Processing & interaction", "Extraction and Refinement", "Crafting and Working Methods", "Alloys, Composites and Compatibility", "Magical or Unusual Properties", "Hazards and Safe Handling"),
            "Uses & availability" to wikiFields("Uses & availability", "Common and Specialized Uses", "Scarcity, Cost and Trade", "Geographic Availability", "Substitutes and Countermaterials", "Cultural and Historical Significance")
        )),
    wikiSpec("calendar", "Calendar & Timekeeping System", "Time & History", "Calendar",
        "A complete civil, religious or astronomical timekeeping system with eras, cycles, leap rules and conventions.",
        listOf(
            "Calendar identity" to wikiFields("Calendar identity", "Calendar Name", "Origin and Cultural Context", "Calendar Image / Diagram", "Primary Users and Regions", "Epoch and Era Names"),
            "Structure & cycles" to wikiFields("Structure & cycles", "Months and Month Lengths", "Weekdays and Week Structure", "Seasons and Seasonal Boundaries", "Years, Cycles and Larger Periods", "Moons and Astronomical Cycles"),
            "Rules & conversion" to wikiFields("Rules & conversion", "Leap Days and Intercalation Rules", "Date Numbering and Naming", "Time of Day and Clock Units", "Conversion to Other Calendars", "Uncertainty and Historical Reforms"),
            "Use & observance" to wikiFields("Use & observance", "Civil and Administrative Use", "Festivals and Holy Days", "Agricultural and Seasonal Observances", "Regional Variants", "Historical Events and Calendar Changes")
        )),
    wikiSpec("cosmology", "Cosmology & Cosmic Structure", "Time & History", "Celestial",
        "The large-scale structure of reality: planes, dimensions, cosmic laws, origins and relationships between worlds.",
        listOf(
            "Cosmic model" to wikiFields("Cosmic model", "Cosmology Name", "Cosmic Map / Diagram", "Origin and Creation Model", "Shape and Boundaries of Reality", "Known Worlds and Realms"),
            "Structure & hierarchy" to wikiFields("Structure & hierarchy", "Planes, Dimensions and Layers", "Celestial Bodies and Systems", "Gods, Primordials and Cosmic Entities", "Connections, Portals and Travel", "Distances and Relative Scale"),
            "Laws & phenomena" to wikiFields("Laws & phenomena", "Fundamental Physical Laws", "Metaphysical Laws and Constraints", "Time, Causality and Fate", "Cosmic Cycles and Catastrophes", "Unknown Regions and Anomalies"),
            "Knowledge & interpretation" to wikiFields("Knowledge & interpretation", "Scientific and Scholarly Models", "Religious Interpretations", "Conflicting Theories and Evidence", "Exploration and Discoveries", "Unresolved Mysteries")
        )),
    wikiSpec("timeline-era", "Historical Era & Age", "Time & History", "Timeline Event",
        "A defined period of history with boundaries, defining conditions, major transitions and regional perspectives.",
        listOf(
            "Era definition" to wikiFields("Era definition", "Era Name", "Start and End Dates", "Alternate Names and Dating Systems", "Historical Map / Period Artwork", "Defining Characteristics"),
            "World conditions" to wikiFields("World conditions", "Political Landscape", "Cultures and Population Shifts", "Technology and Knowledge", "Magic, Religion and Philosophy", "Climate and Environmental Conditions"),
            "Events & transitions" to wikiFields("Events & transitions", "Major Events and Turning Points", "Wars, Disasters and Revolutions", "Founding and Collapse of States", "Beginning and Ending Causes", "Continuities with Earlier Ages"),
            "Historical interpretation" to wikiFields("Historical interpretation", "Primary Sources and Evidence", "Competing Periodizations", "Regional Differences", "Later Memory and Myth", "Open Historical Questions")
        )),
    wikiSpec("spirit", "Spirit, Ghost & Elemental", "Fauna & Monsters", "Fauna",
        "A supernatural being profile for origin, manifestation, attachment, agency, powers and banishment.",
        listOf(
            "Nature & origin" to wikiFields("Nature & origin", "Spirit Name", "Spirit Type and Classification", "Origin, Death or Emergence", "Manifestation Image", "Associated Realm or Element"),
            "Manifestation & presence" to wikiFields("Manifestation & presence", "Visible and Invisible Forms", "Signs, Sounds and Environmental Effects", "Range, Tether and Place of Manifestation", "Needs, Anchors and Sustaining Conditions"),
            "Mind & behavior" to wikiFields("Mind & behavior", "Memory, Identity and Awareness", "Temperament and Motives", "Communication and Languages", "Behavioral Rules, Compulsions and Taboos"),
            "Power & release" to wikiFields("Power & release", "Abilities and Influence", "Limitations and Vulnerabilities", "Binding, Bargains and Summoning", "Banishment, Appeasement or Release", "Consequences of Release or Destruction")
        )),
    wikiSpec("forest", "Forest & Woodland", "Flora & Ecology", "Biome",
        "A forest ecosystem record for canopy layers, succession, soils, hydrology, species and human use.",
        listOf(
            "Forest identity" to wikiFields("Forest identity", "Forest Name", "Forest Type", "Forest Extent Map", "Location and Boundaries", "Climate Zone"),
            "Structure & environment" to wikiFields("Structure & environment", "Canopy Layers and Vertical Structure", "Dominant Tree Species", "Understory and Ground Layer", "Soils, Leaf Litter and Fungi", "Streams, Wetlands and Moisture", "Climate and Seasonal Cycle"),
            "Ecology & species" to wikiFields("Ecology & species", "Characteristic Plants", "Animals and Food Webs", "Fungi and Decomposition", "Succession, Disturbance and Regeneration", "Keystone Species and Dependencies"),
            "People & stewardship" to wikiFields("People & stewardship", "Communities and Traditional Relationships", "Timber, Food, Medicine and Resources", "Sacred Groves and Cultural Meaning", "Threats and Invasive Species", "Stewardship and Conservation")
        )),
    wikiSpec("river", "River & Watercourse", "Locations & Realms", "Location",
        "A hydrological record for source, tributaries, flow, watershed, flood behavior, ecology and navigation.",
        listOf(
            "Identity & course" to wikiFields("Identity & course", "River Name", "Alternate and Historical Names", "Source / Headwaters", "Mouth / Endpoint", "River Basin Map", "Length, Elevation and Gradient"),
            "Hydrology & watershed" to wikiFields("Hydrology & watershed", "Watershed and Drainage Basin", "Tributaries and Connected Waters", "Flow Rate and Seasonal Variation", "Flood Cycles and Droughts", "Water Quality, Salinity and Sediment"),
            "Landscape & features" to wikiFields("Landscape & features", "Upper, Middle and Lower Course", "Falls, Rapids, Meanders and Deltas", "Banks, Floodplain and Erosion", "Fords, Bridges, Ferries and Crossings"),
            "Ecology & human use" to wikiFields("Ecology & human use", "Aquatic and Riparian Species", "Fisheries and Food Resources", "Settlements Along the River", "Navigation, Trade and Transport", "Water Rights, Pollution and Conflicts")
        )),
    wikiSpec("mountain", "Mountain & Mountain Range", "Locations & Realms", "Location",
        "A geomorphological record for elevation, geology, climate zones, routes, hazards and cultural meaning.",
        listOf(
            "Identity & location" to wikiFields("Identity & location", "Mountain / Range Name", "Alternate and Local Names", "Topographic Map", "Location and Extent", "Elevation, Prominence and Dimensions"),
            "Geology & formation" to wikiFields("Geology & formation", "Formation Process and Geological Age", "Rock Types and Mineral Composition", "Ridges, Peaks, Passes and Valleys", "Volcanic, Tectonic or Glacial Activity"),
            "Climate & hazards" to wikiFields("Climate & hazards", "Altitudinal Climate Zones", "Weather Patterns and Seasons", "Avalanches, Landslides and Storms", "Glaciers, Snowpack and Headwaters"),
            "Ecology & people" to wikiFields("Ecology & people", "Mountain Biomes and Species", "Trails, Passes and Climbing Routes", "Mountain Settlements and Communities", "Resources and Extraction", "Cultural and Legendary Significance")
        )),
    wikiSpec("planet", "Planet & World Body", "Locations & Realms", "Celestial",
        "A planetary science profile for orbit, geophysics, atmosphere, climate, biosphere and habitability.",
        listOf(
            "Astronomical identity & orbit" to wikiFields("Astronomical identity & orbit", "World Name", "World-Body Class", "Primary Star / System", "Orbit, Year Length and Eccentricity", "Rotation, Day Length and Axial Tilt", "Orbital / Surface Image"),
            "Interior & surface" to wikiFields("Interior & surface", "Core, Mantle and Internal Structure", "Gravity and Mass", "Surface Composition and Major Terrain", "Tectonics, Volcanism and Seismicity", "Moons and Satellites", "Rings and Orbital Debris"),
            "Atmosphere & climate" to wikiFields("Atmosphere & climate", "Atmospheric Composition and Pressure", "Weather Systems and Circulation", "Climate Zones and Long-Term Cycles", "Oceans, Seas and Hydrological Cycle", "Seasonal Patterns and Extremes"),
            "Biosphere & civilization" to wikiFields("Biosphere & civilization", "Habitability and Life-Support Conditions", "Biosphere and Major Ecosystems", "Inhabiting Species and Civilizations", "Resources and Environmental Constraints", "Planetary History and Terraforming")
        )),
    wikiSpec("ritual", "Ritual & Ceremony", "Magic & Systems", "Spell",
        "A repeatable rite with purpose, participants, sacred space, materials, sequence, timing and consequences.",
        listOf(
            "Ritual identity & purpose" to wikiFields("Ritual identity & purpose", "Ritual Name", "Tradition / Magical Discipline", "Purpose and Intended Outcome", "Ritual Class", "Ceremonial Image / Diagram"),
            "Preparation & requirements" to wikiFields("Preparation & requirements", "Participants and Roles", "Required Location and Sacred Space", "Timing, Calendar and Astronomical Conditions", "Materials, Offerings and Tools", "Preparation, Cleansing and Restrictions"),
            "Ritual sequence" to wikiFields("Ritual sequence", "Step-by-Step Procedure", "Prayers, Chants, Formulae and Gestures", "Symbols, Diagrams and Arrangements", "Duration and Repetition"),
            "Outcome, risks & tradition" to wikiFields("Outcome, risks & tradition", "Expected Effects and Signs of Success", "Costs, Sacrifices and Obligations", "Risks, Taboos and Failure Conditions", "Ending, Reversal and Cleanup", "Regional Variants and Historical Changes")
        ))
)
