# Product specification

## Product promise
Ink & Quill is a private, offline-first creative writing studio for Android. It supports novels, interconnected fictional worlds, poetry, journals, essays, documents, notes, research, and open-ended creative projects.

**Non-negotiable constraints**
- No AI of any kind: no AI features, APIs, SDKs, model downloads, or AI writing assistance.
- Local device storage is the primary source of truth.
- No mandatory account, subscription, or network connection.
- Cloud backup/sync is optional, provider-flexible, and designed around encrypted user-controlled data.
- Users own their writing and can export it in open, useful formats.

## Main workspaces
- Novels & Stories
- Worldbuilding
- Poetry
- Journal
- Documents & Essays
- Notes
- Creative Projects
- Research & Reference

A project can be independent or connected to a fictional World. Do not force journals, poems, notes, or documents into a world hierarchy.

## World-first fiction organization
A World is a hub. It can contain multiple independent Series and standalone Stories. A Series contains books/volumes, chapters, and scenes. A standalone Story contains chapters and scenes.

World-level shared resources include characters, locations, cultures, factions, creatures, history, calendars, magic/technology systems, religions, artifacts, maps, timelines, and research. Records have explicit scope (World, Series, Story, or selected projects) and can be selectively shared.

A character, city, event, or system can appear in multiple projects. Provide bidirectional links and backlinks. Support alternate versions and historical snapshots so changing a current character profile does not rewrite how that character appeared in an earlier story.

## Writing editor
The target editor is rich-text-first, with structured formatting rather than storing formatting as plain-text conventions. Required formatting includes bold, italic, underline, strikethrough, headings, paragraphs, ordered/unordered lists, indentation, alignment, block quotes, links, text color, highlight, font family, and font size.

Editor capabilities:
- Continuous-flow and page-based modes.
- Autosave with visible save state and recovery after interruption.
- Undo/redo, find/replace, word and character counts.
- Focus mode and customizable toolbar.
- Customizable margins, page width, line spacing, paragraph spacing, backgrounds, and typography.
- Multiple open documents/tabs and quick switching.
- Attachments and images.
- Version history with restore/compare.
- Export/import: TXT, Markdown, HTML, PDF, DOCX, and EPUB where technically feasible.

## Universal template system
Ship a broad original template library inspired by the breadth of established writing/worldbuilding tools, without copying proprietary template text. Users can edit every template and create new ones.

Template categories:
- Characters: full profiles, quick profiles, NPCs, character arcs, relationships, family trees, groups, inventories, backstories.
- Worldbuilding: worlds, continents, regions, countries, cities, settlements, buildings, locations, cultures, societies, species, creatures, history, encyclopedia entries, factions, organizations.
- Magic, technology, and belief: systems, rules, limitations, spells, abilities, artifacts, inventions, technologies, religions, deities, philosophies, languages, writing systems.
- Story planning: outlines, three-act structure, Hero's Journey, Save the Cat, plot arcs, subplots, chapters, scenes, events, twists, conflicts, mysteries, clues, revelations.
- Time: historical timelines, character timelines, event timelines, calendars, eras, months, weekdays, seasons, multiple moons, birthdays, ages, chronology.
- Visual planning: maps, regional maps, floor plans, family trees, flowcharts, relationship diagrams, plot diagrams, corkboards, moodboards, reference boards.
- Research: research notes, sources, citations, web references, books, images, documents, collections, annotations, lore records.

Universal field types: short text, long text, rich text, number, date, dropdown, multi-select, checkbox, image, gallery, attachment, color, rating, table, relationship, repeatable group, and calculated field.

Template editor must allow users to add, rename, reorder, group, conditionally show, mark required/optional, and remove fields and sections; customize icons, colors, and layouts; duplicate, version, organize, import, export, and restore originals. Removing a field must never silently destroy existing entry data; retain removed values in recoverable legacy data.

## Automatic reference and connection engine
Implement deterministic, fully local text matching. It is not semantic AI.

- Match exact names and user-defined aliases in documents, chapters, scenes, and notes.
- Offer automatic linking, approval-required suggestions, and manual-only modes.
- Support case sensitivity, alias matching, exclusions, duplicate prevention, ambiguity handling, and retroactive scans.
- A mention opens the referenced record; each record lists backlinks to exact document locations.
- Renames and moves preserve stable record IDs and existing links.
- Semantic relationship claims (for example, parentage, ownership, allegiance) must never be silently inferred. They require explicit user confirmation.
- Provide undo and a connection manager.
- Provide a visual connection explorer/knowledge graph with filters across worlds, series, and stories.

## Worldbuilding and planning tools
Provide structured records and visual workspaces for characters, locations, factions, histories, custom calendars, maps, timelines, relationship networks, family trees, inventories, flowcharts, corkboards, moodboards, and research collections. Support custom categories and user-defined relationships.

## Appearance and personalization
Adaptive system/light/dark appearance by default. Offer custom palettes and themes; font families and sizes; typography and spacing; page width and margins; editor backgrounds/textures; navigation and home-section customization; icon choices; and toolbar customization.

## Privacy and resilience
Local-first storage, no analytics by default, no mandatory sign-in, optional app lock, and encrypted export/backup. Cloud providers must be selectable by the user rather than hard-coded to one provider. Backup and sync must never be required to write or open local work.

## Delivery priorities
1. Reliable Android foundation, local document model, autosave, and rich editor.
2. Library, folders, tags, search, favorites, tabs, and history/recovery.
3. World/Series/Story hierarchy and shared worldbuilding records.
4. Universal editable template builder and starter template library.
5. Deterministic mention links, backlinks, and connection explorer.
6. Export/import, attachments, visual planning tools, and encrypted provider-flexible backup.
7. Extensive appearance, navigation, and editor customization.
