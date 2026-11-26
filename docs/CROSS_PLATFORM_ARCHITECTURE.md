# Cross-platform Novel Writing & Worldbuilding Architecture

This document extends the existing Ink & Quill product contract. It is an implementation plan and schema contract; a feature is not considered implemented merely because it is described here.

## Product invariants

- **Offline first:** every writing and worldbuilding workflow works without network access. Local persistence is committed before any sync attempt.
- **Creator ownership:** no generative AI, text completion, manuscript scraping, analytics by default, or mandatory account. All user content is exportable.
- **Universal custom text:** every user-authored label, status, heading, note, table header/cell, timeline value, board card, connector label, annotation, snippet, and editor insert is free text. Presets are suggestions only and never constrain entry.
- **Stable identity:** entities use immutable IDs; display names and labels may be changed without breaking links.
- **Cross-platform parity:** shared domain rules, validation, serialization, and conflict semantics; native responsive UI per platform. Android and desktop/web must not become separate product editions.
- **Portable data:** versioned, documented JSON project format with attachment manifests and integrity checks.

## Target architecture

Use a shared Kotlin Multiplatform core for domain models, use cases, validation, ordering, search/link matching, backup serialization, and sync conflict logic. Keep platform adapters for persistence, files, notifications, widgets, rich-text rendering, and canvas input.

- Android: Jetpack Compose UI, SQLite/Room-compatible persistence, Android document providers, local notifications/widgets.
- Desktop: Compose Multiplatform desktop UI, SQLite, native file pickers, keyboard-first multi-pane layout.
- Web: Kotlin/JS or Kotlin/Wasm shared domain layer where practical, responsive web UI, IndexedDB persistence, browser file APIs, installable PWA/offline cache. Browser storage must be treated as user-cleareable and export reminders must be clear.
- Persistence interfaces must be platform-neutral. A transaction boundary is required for each user-visible save. Sync is an optional adapter and must never be called by editor save paths.
- Rich text is stored as a versioned structured document model (blocks and inline spans), not platform-specific editor HTML. HTML is an import/export/rendering format.
- Board documents use normalized coordinates and stable object IDs so layouts survive screen-size changes.

## Relational domain model

All records have UUID primary keys, created/updated timestamps, and schema version metadata. Ordered siblings use a fractional or sparse position key; reindex transactionally after reorder.

### Manuscript hierarchy

- Universe/Project(id, title, author, description, settings)
- Book(id, universe_id, title, position, custom_label)
- Chapter(id, book_id, title, position, status_key, custom_status_text, manual_status_override)
- Scene(id, chapter_id, title, position, body_document_id, summary, goal, conflict, pov_character_id, sensory_details, hook, custom_fields_json)
- Document(id, format_version, structured_content_json, plain_text_cache, word_count_cache)
- InlineAnnotation(id, document_id, anchor_start, anchor_end, kind, custom_label, body_text, color)

Status keys are stable machine values (NOT_STARTED, OUTLINING, WRITING, FINISHED); status display labels are user-editable free text. Automatic status suggestions never overwrite an explicit manual override. A chapter becomes OUTLINING when planning metadata exists and WRITING when scene prose exists, unless the user has overridden it.

### Shared universe story bible

- Entity(id, universe_id, entity_type, title, custom_label, description, fields_json, parent_entity_id)
- Built-in entity types: Character, CharacterGroup, Location, Item, Species, Language.
- Location parent_entity_id supports arbitrary nested location trees; Item parent_entity_id supports nested item/component trees.
- EntityLink(id, source_entity_id, target_entity_id, relation_label, relation_kind, custom_text, created_at). Links are directional records; UI renders both outgoing links and backlinks. Scene-to-entity and entity-to-scene references use the same stable-link model.
- Tag(id, universe_id, text, color); EntityTag(entity_id, tag_id).
- Attachment(id, owner_type, owner_id, storage_key, mime_type, file_name, checksum, size_bytes, caption, position).
- Entity notes and custom labels are free text, not enum-only fields.

### Custom schemas and tables

- EntityCategory(id, universe_id, name, icon_key, color, schema_version)
- CategoryField(id, category_id, key, label, type, position, config_json, required)
- Supported field types: TEXT_BLOCK, SLIDER, DROPDOWN, ENTITY_REFERENCE, plus extensible future types.
- EmbeddedTable(id, owner_entity_id, title, position); TableColumn(id, table_id, key, label, type, position, config_json); TableRow(id, table_id, position); TableCell(id, row_id, column_id, value_json).
- Table column types: TEXT, CHECKBOX, DROPDOWN, ENTITY_LINK. Headers and TEXT cells accept arbitrary user text. Dropdown options are user-authored and editable.
- Category templates serialize to a versioned JSON package with schema, icons/labels, field configs, and migration metadata. Import validates before mutation and never silently drops unknown fields.

### Timelines, boards, and goals

- Timeline(id, universe_id, name, custom_description, position); Moment(id, timeline_id, date_or_era_text, sort_key, title, custom_text); Event(id, timeline_id, moment_id, description, notes, custom_fields_json).
- EventEntityLink(event_id, entity_id, role_label).
- Board(id, universe_id, title, board_type, viewport_json, background_json); BoardObject(id, board_id, object_type, x, y, width, height, rotation, z_index, payload_json); BoardConnector(id, board_id, source_object_id, target_object_id, direction, label_text, style_json). Board objects include linked records, images, swatches, sticky notes, freeform text, stickers, and plot diagram nodes. Drawing strokes are stored as vector paths.
- RelationshipEdge(id, board_id, source_entity_id, target_entity_id, edge_family, edge_type, custom_label, source_perspective_note, target_perspective_note, color, line_style). Partner and parent-child edge types are extensible; generation tracking is derived and can be manually corrected.
- SprintSession, WritingGoal, DailyWritingLog, PromptEntry store user-authored session notes and metrics locally. Prompt text is editable and can be user-created; rotating prompt packs are bundled content, not remote scraping.

## Editor behavior

- Structured rich text: paragraphs, headings, inline formatting, text color, alignment, links, images, custom inline blocks, callouts, annotations, snippets, and arbitrary custom labels.
- Autosave is immediate and transactional, with a visible saved/unsaved/error state. Preserve caret, selection, and scroll position when opening Quick Explore.
- Focus Mode hides navigation, toolbars, and reference panes but leaves writing and custom text insertion fully available.
- Desktop/web uses resizable split panes. Mobile uses a swipeable Quick Explore sheet/drawer with independent scroll state.
- Shuffle/reorder supports touch drag and desktop pointer/keyboard alternatives; order changes and index recalculation occur in one transaction.
- Version snapshots include manual checkpoints and configurable interval snapshots. Preview/diff is read-only; restore creates a new revision rather than deleting history.

## Canvas and plotting

Boards support pan/zoom, drag/drop, freehand vector strokes, image assets, color swatches, emoji/stickers, free text, linked entity cards, and labeled directional connectors. Plot templates are editable structures, not locked workflows. Family-tree edge labels, colors, line styles, and dual-perspective notes are custom text/configuration. Export renders board dimensions at user-selected resolution to PNG and paginated/vector PDF where possible.

## Productivity

- Sprint timer tracks elapsed time, word delta, live WPM, target progress, pause/resume, and session notes. Word counts are based on saved editor content plus an explicit unsaved buffer count.
- Goal calculator accepts daily/weekly/total targets and active days per week; projection excludes inactive days and recalculates when the schedule changes.
- Analytics include word/character/paragraph counts, estimated reading/speaking time, rolling 7/30-day averages, personal bests, streaks, and daily deltas. All calculations are local and explain their assumptions.
- Reminders distinguish weekdays/weekends. Widgets are platform-specific views of shared local goal data. Share cards use user-editable captions.
- Weekly prompts are bundled or user-authored, editable, and available offline.

## Collaboration and publishing boundaries

Collaboration is an optional later service, never required for local use. Invitations use explicit OWNER/EDITOR/VIEWER roles and scopes at project, book, or selected entity level. Per-author attribution is based on authenticated edit events, not inferred from prose. Chat is isolated from manuscript storage.

Public wiki generation is an explicit publish action. Only individually selected Story Bible entries are included; private notes, manuscripts, attachments not selected, and unpublished custom fields are excluded by allowlist. Show a preview and exact publication scope before creating output.

## Import/export contract

- Import: TXT, DOCX, and full .fortelling JSON/ZIP archive. Parse into a staging database, validate IDs/relationships/attachments, show a preview, then commit transactionally.
- Export: TXT, JSON/ZIP .fortelling archive, DOCX manuscript, and EPUB 3. Include title, author, optional cover, and identifier metadata (ISBN/ASIN only when user supplies a valid value). Never fabricate identifiers.
- Preserve custom text and structured formatting wherever the destination format supports it. Report unsupported conversions instead of silently dropping data.

## Accessibility and appearance

Support light/dark/system modes, at least 15 curated themes plus custom hex accent, dynamic type scaling to 200% or more, screen-reader labels, keyboard navigation, reduced motion, contrast-safe palettes, and selectable typefaces including user-imported OpenDyslexic where licensing and platform support permit.

## Milestone delivery gates

1. **Core manuscript:** relational Universe → Book → Chapter → Scene model; structured rich-text editor; universal custom text; Focus Mode; autosave/recovery; TXT and versioned JSON backup. Verify offline and after process death.
2. **Story Bible:** built-in entities, nested Location/Item trees, many-to-many links/backlinks, attachments/tags, Quick Explore and desktop split view.
3. **Planning/productivity:** synced scene outliner, multiple timelines, sprint timer, goals, analytics, daily logbook, reminders.
4. **Canvas:** pan/zoom boards, custom text/cards/connectors, plot diagrams, family trees, PNG/PDF export.
5. **Extensibility/publishing:** custom schemas, embedded tables, schema templates, DOCX/EPUB, scoped collaboration, public wiki.

A milestone is complete only when data migrations, offline behavior, export/restore, accessibility basics, and automated tests pass on each supported platform. Do not label a platform supported until its build and persistence tests run in CI.
