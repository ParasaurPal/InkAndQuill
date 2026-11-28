# World Encyclopedia

Ink & Quill's World Encyclopedia is a private, offline-first wiki for a fantasy setting. It is available in the web/PWA client and the native Android app.

## Article model

The encyclopedia contains 49 built-in specialist article types plus a custom-template option. A specialist template defines its own sections and fields; the shared editor and storage layer do not impose one universal schema on every subject. For example, a tree has botanical taxonomy, trunk and canopy architecture, leaves, flowers, fruit, growth, habitat, ecology and human uses. A character has identity, appearance, psychology, biography, abilities, relationships and narrative continuity.

Built-in subject areas cover people and lineages, flora and fauna, places and polities, belief and myth, societies, language and knowledge, magic and metaphysics, objects and technology, and history and time.

## Web / PWA

Open **Story Bible → World encyclopedia**.

- Create an article from one of the specialist templates or a custom schema.
- Search titles, summaries, structured fields and tags; filter by category.
- Add a lead summary, subtitle, tags, parent article and structured field values.
- Use nested custom sections created from indented template outlines.
- Add image and gallery fields; large static images are resized when appropriate. Animated GIFs and SVGs are preserved.
- Enter table data one row per line, with columns separated by `|`; the reader renders it as a table.
- Link related articles, navigate parent/child articles and follow backlinks.
- Attach supporting files and images to an article.
- Read the saved article in a wiki-style page and return to editing it.

The web/PWA stores data in that browser's IndexedDB. It does not automatically synchronize with Android or upload content to a hosted wiki. Use the existing project backup/export and user-configured encrypted sync features to move or back up data.

## Native Android

Open a world, choose **Modules → World Encyclopedia**, then switch between **Articles** and **Templates**.

- Search article titles, summaries, tags and structured field values.
- Filter by the template category.
- Choose a specialist or custom template to create an article inside the current world.
- Open the existing structured sheet editor to fill in its sections and fields.
- Read the article in the native reader, which shows both prose and structured sheet content.
- Article cards show a cover/attached image when one is available, plus a short field preview and completion count.

The native catalog contains 108 built-in templates in total, including the 49 specialist encyclopedia subjects and the existing writing/planning templates. New templates are seeded through the existing database template system; existing records and saved field values are preserved.

## Storage and boundaries

- Android records and structured values are stored in the existing local SQLite database.
- Web records are stored in the browser's local IndexedDB database.
- There is no public wiki hosting, shared multi-user editing, or automatic cross-device synchronization in this implementation.
- Back up your browser project before clearing browser storage or changing devices. Android users should use the existing backup/export tools.
