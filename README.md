# Ink & Quill

A native Android writing studio for stories, worldbuilding, poetry, journals, notes, documents, and creative projects.

## Product principles
- Offline-first: local device storage is primary.
- No AI features, services, or dependencies.
- No mandatory account or subscription.
- Optional, provider-flexible encrypted backup is planned.
- Rich editing, customizable templates, and linked worldbuilding are core product goals.

## Build
Open the repository in Android Studio, or build from a configured Android/Gradle environment:

```bash
gradle :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Current status
Initial Android/Compose foundation and navigation shell. Persistent document storage, rich editor, template builder, worldbuilding database, automatic mention linking, export/import, and backup workflows are planned next.
