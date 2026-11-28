# Campfire Write Replacement — Feature Parity Plan

## Product direction

Ink & Quill is the offline-first, private, no-AI Campfire Write alternative. This plan uses Campfire's publicly described module categories as a feature-coverage checklist, while all template wording, layouts, implementation, and visual design remain original. Do not copy Campfire source code, proprietary template text, artwork, or branding.

Non-negotiable Ink & Quill requirements remain in force:
- No AI features, model calls, AI SDKs, or AI writing assistants.
- Android-first, phone-friendly, offline-first, no mandatory account or subscription.
- Local data remains the source of truth; user-selected encrypted backup is optional.
- Preserve all existing records, templates, links, attachments, and backups through additive migrations.
- Every built-in template is editable and duplicable; users can create, import, export, and restore their own templates.
- Free-form user text is always allowed. Preset values are suggestions, never restrictions.

## Full module checklist

| Campfire-style module | Ink & Quill foundation | Full replacement target |
|---|---|---|
| Manuscript | Novel Studio, rich editor, books/chapters/scenes, series, covers | Multiple manuscripts per world; outline/index-card/corkboard modes; word goals, statistics, tags/mentions, notes sidebar, focus mode, autosave, history, export |
| Characters | Character and NPC templates, world records | Deep configurable sheets, 100+ optional attribute prompts, portraits/galleries, traits, skills/stats, biography, character arcs, inventories, relationships |
| Locations | Location/region/settlement/kingdom templates and hierarchy | Nested location trees, location-specific sheets, linked inhabitants/events/items, map placement |
| Maps | Map boards and linked pins | Multiple maps, nested maps, zones/regions, pins, labels, measuring/annotation tools, linked records, image import |
| Research | Research notes and attachments | Research library for links, PDFs, images, videos, books, citations, annotations, source reliability, collections, backlinks |
| Timeline | World timelines and event records | Multiple timelines, lanes/rows, eras, event cards, durations, dependencies, color-coded links, story plotting templates |
| Arcs | Arc template | Dedicated arc workspace; character/story arcs, stages/beats, parallel arcs, progress/status, timeline links |
| Relationships | Explicit record links, relationship graph/family-tree boards | Dedicated relationship webs, family trees, custom edge labels, directionality, generations, relationship notes, alternate/ambiguous links |
| Encyclopedia | 108 built-in Android templates and web article catalog | Full wiki with custom article types, nested pages, summaries, infoboxes, galleries, tables, tags, backlinks, citations, parent/child navigation |
| Magic | Magic system, spell, enchantment templates | Dedicated systems, disciplines, rules, costs, limitations, abilities, spells, artifacts, progression and cross-links |
| Species | Species, flora, fauna, monster, dragon, plant templates | Taxonomy, anatomy, habitat, behavior, lifecycle, ecology, variants, images and cross-links |
| Cultures | Culture, faction, festival, cuisine, clothing, economy, government, military templates | Dedicated culture dossiers, customs, values, social structures, traditions, language, history, subcultures and comparisons |
| Items | Item, artifact, weapon, grimoire, potion, mineral, vessel, inventory templates | Item catalog, ownership/history, properties, crafting, components, condition, images, inventory views |
| Systems | Flowchart boards and world records | Dedicated system diagrams for organizations, power structures, magic/technology, processes, rules, dependencies and custom connectors |
| Languages | Language, script, naming, glossary templates | Conlang workspace with phonology, grammar, scripts/glyphs, dictionary, inflections, translations, dialects and search |
| Religions | Religion, deity, myth, prophecy, ritual templates | Pantheons, deities, beliefs, sacred texts, rites, holidays, cosmology, sects, symbols and relationships |
| Philosophies | General records and belief templates | Dedicated schools of thought, ethics, principles, arguments, thinkers, texts, contradictions and historical influence |
| Calendar | Custom world calendars with eras, months, weekdays, seasons, moons and recurring dates | Multiple calendars per world, custom date arithmetic, conversion between calendars, recurring events, birthdays, holidays and timeline integration |

## Template coverage policy

The native Android catalog now contains 120 built-in templates plus custom-template support. The web/PWA catalog now contains 120 built-ins plus its custom option. Twelve matching subject-specific templates were added for research sources and annotations, philosophy, character and story arcs, relationship webs, family trees, constructed-language dictionaries, scripts, map atlases, system diagrams, and calendar observances. Treat this as a growing starter library, not a finished ceiling.

The complete starter library must cover:
- Every module above with multiple subject-specific templates where useful.
- Character variants (protagonist, antagonist, supporting cast, NPC, historical figure, monarch, family, lineage, party).
- Story structures and planning (three-act, Hero's Journey, Save the Cat, mystery, romance, quest, subplot, scene, chapter, beat, conflict, twist, clue, revelation).
- World and place variants (world, continent, region, country, kingdom, city, district, settlement, building, landmark, dungeon, temple, realm, planet, biome).
- People and society (culture, faction, organization, government, military, economy, festival, cuisine, clothing, religion, philosophy).
- Natural life (species, animal, monster, dragon, construct, plant, herb, fungus, forest, river, mountain, ecosystem).
- Systems and objects (magic system, discipline, spell, enchantment, ability, technology, invention, affliction, artifact, weapon, potion, material, vessel, inventory).
- Time and plot (timeline, era, event, war, quest, calendar, moon, season, character arc, story arc, continuity record).
- Language and knowledge (language, script, naming system, glossary, research note, source, citation, book, image reference, document, annotation, collection).
- Visual planning (map, regional map, floor plan, relationship web, family tree, flowchart, plot diagram, corkboard, moodboard, reference board).

Each template must have its own relevant sections and field types, not a generic title/description form. Support images/galleries, relationships, tags, tables, repeatable groups, dates, numbers, checkboxes, and optional/conditional fields where relevant. Users must be able to edit, duplicate, reorder, group, hide/show, import, export, and restore templates without losing existing entry values.

## Delivery order

1. **Template completeness:** audit the full built-in catalog against the module and subject matrix above; add missing subject-specific templates to Android and web; verify identical template IDs and equivalent field structures across platforms.
2. **Module completeness:** give each module a dedicated entry point and workflow instead of exposing only generic record lists or boards.
3. **Cross-linking:** every module uses stable record IDs, shared tags, backlinks, scoped links, and global search.
4. **Visual workspaces:** finish timeline, relationship/family-tree, map, flowchart, corkboard, and moodboard experiences with mobile-friendly touch controls.
5. **Manuscript integration:** tag/link world elements in prose; open notes and references alongside the editor; jump to exact linked records; retain chapter/scene organization and word-count goals.
6. **Portability and QA:** verify Android and web parity, offline operation, backup/restore, additive database migrations, accessibility, and real-device behavior before declaring a module complete.

## Definition of done

A module is not complete merely because a record type or template exists. It must have:
- A dedicated navigation entry and useful list/detail views.
- Create, edit, duplicate, delete, search, filter, and sort workflows.
- Its own subject-specific editable starter templates.
- Image/file support where applicable.
- Links, backlinks, and shared tags.
- Backup/restore coverage and migration safety.
- Android and web/PWA behavior parity, or an explicitly documented platform limitation.
- Automated tests for persistence and core workflows.

## Public feature references

The module names and publicly described feature categories used for this parity checklist are documented at:
- https://www.campfirewriting.com/apps
- https://www.campfirewriting.com/write
- https://www.campfirewriting.com/worldbuilding-tools
- https://www.campfirewriting.com/character-builder
