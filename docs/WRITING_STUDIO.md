# Writing Studio

Ink & Quill's Writing Studio supports novels and short stories in the same world library.

## Organizing stories

- Create a **Novel** or **Short story** from the Writing Studio.
- Every story remains a direct child of its world, so a side story is still visible in the world's complete story collection.
- Optionally file a story under a **Series**. Series are displayed as image banners; tap a banner to expand or collapse its story collection.
- Add or replace each series banner image at any time.
- Add cover artwork to each book or short story. Stories display as cover cards.
- A side story can be assigned to an existing series, connected to another book, or both. Connecting a story does not move it out of the world-level library.
- Stories without a series remain in the Standalone Stories collection.

## Manuscript editing

The editor includes undo/redo, paragraph styles, heading levels, code blocks, font size, bold, italic, underline, strikethrough, superscript, subscript, ordered and unordered lists, indentation, alignment, text color, highlight, hyperlinks, images, horizontal rules, notes, snippets, scene planning, word counts, autosave, checkpoints, and version history.

## Included open-source typefaces

The manuscript font selector includes five Google Fonts families, each released under the SIL Open Font License:

1. **Literata** — book-oriented serif
2. **Source Serif 4** — editorial serif
3. **Lora** — contemporary literary serif
4. **Crimson Pro** — classic book serif
5. **Merriweather** — high-readability serif

Font files and license metadata are maintained by the Google Fonts project:
- https://github.com/google/fonts/tree/main/ofl/literata
- https://github.com/google/fonts/tree/main/ofl/sourceserif4
- https://github.com/google/fonts/tree/main/ofl/lora
- https://github.com/google/fonts/tree/main/ofl/crimsonpro
- https://github.com/google/fonts/tree/main/ofl/merriweather

The web app and Android WebView load these families from Google Fonts when a network connection is available; normal system serif fallback remains available offline.

## Data and compatibility

Android stores series, banner references, book covers, story type and book-to-book connections in the dedicated Novel Studio SQLite database. Its schema migration is additive and preserves existing manuscripts. Project JSON exports now include series and expanded book metadata. Web records remain in the existing IndexedDB project store and are included in normal project exports.
