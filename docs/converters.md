## Converters

Converters allow you to migrate player data from another authentication plugin into AuthMe.
Run a converter with `/authme converter <name>` (requires `authme.admin.converter`).

---

AuthMeReloaded currently ships the **Auth+** converter and the built-in database migration helpers below.

---

### Auth+ → `authplus`

Migrates accounts from the **Auth+** plugin.

**Before running:**
1. Set `passwordHash` to `PBKDF2BASE64` in AuthMe's `config.yml`
2. Set `pbkdf2Rounds` to `120000` under `settings.security` in `config.yml`

**Source file:** `plugins/Auth/players.yml`

**Notes:**
- Auth+ stores accounts by UUID only. AuthMe resolves each UUID to a player name via the server's
  local cache (`usercache.json`). Players who have never joined the server will be skipped.
- Players already present in AuthMe's database are skipped automatically.

---

### SQLite → SQL → `sqlitetosql`

Copies AuthMe data from a SQLite database into the configured SQL database.

---

### MySQL → SQLite → `mysqltosqlite`

Copies AuthMe data from the configured MySQL database into a SQLite file.
