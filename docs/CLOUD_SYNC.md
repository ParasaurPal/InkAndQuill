# Encrypted cloud sync

Ink & Quill keeps the phone's SQLite database as the primary working copy. Cloud sync is optional and uses Android's Storage Access Framework, so the user selects a destination exposed by an installed document-provider app (for example, a cloud-drive app). Ink & Quill does not require a provider account or embed provider credentials.

## Configure

1. Open **Settings → Encrypted cloud sync destination**.
2. Choose a destination file through Android's document picker.
3. Enter a backup password of at least 10 characters.
4. Keep the password safe. It is required to decrypt the cloud copy on another device.

The backup is encrypted on-device using AES-256-GCM with a password-derived key. The scheduled worker protects the saved password with an Android Keystore key. The cloud provider receives only the encrypted backup bytes.

## Sync behavior

- A first sync writes to an empty destination. If the selected destination already contains a backup and this device has no shared sync baseline yet, Ink & Quill asks which copy to keep.
- Sync is queued through WorkManager, runs when a network connection is available, and is scheduled approximately every 12 hours. **Sync now** starts an immediate check.
- A change hash detects whether the phone copy, cloud copy, or both have changed since the last successful sync.
- If only the phone changed, the encrypted phone snapshot is uploaded.
- If only the cloud copy changed, the cloud snapshot is restored to the phone.
- If both changed, Ink & Quill pauses automatic replacement and asks the user to **Keep this phone version** or **Use the cloud version**. Choosing the cloud version replaces the local database contents with that cloud snapshot.
- If the device is offline or the provider is temporarily unavailable, WorkManager retries the queued operation.

This is whole-snapshot synchronization, not field-by-field merging. Keep independent devices synced regularly and resolve conflicts deliberately. It does not attempt to merge simultaneous edits to the same document.

## Included data

The encrypted snapshot includes writing records, templates and template fields, settings, tags, folders, collections, tabs, relationships, timelines, calendars, planning boards, attachment bytes, board backgrounds, and board-card images.

## Disable

Choosing **Disable scheduled cloud backup** stops scheduled and queued sync work and clears the saved destination and protected password from the app. It does not delete the file already stored with the provider.
