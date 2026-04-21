## Converters

Converters allow you to migrate player data from another authentication plugin into AuthMe.
Run a converter with `/authme converter <name>` (requires `authme.admin.converter`).

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

### CrazyLogin → `crazylogin`

Migrates accounts from the **CrazyLogin** plugin.

**Source file:** configured via `Converter.crazyloginFileName` in AuthMe's `config.yml` (default: `crazylogin.db`).

---

### LoginSecurity → `loginsecurity`

Migrates accounts from the **LoginSecurity** plugin. Supports both SQLite and MySQL sources,
configured under `Converter.LoginSecurity` in AuthMe's `config.yml`.

**After running:** add `BCRYPT` to `legacyHashes` in your `config.yml`.

---

### RakamakConverter → `rakamak`

Migrates accounts from the **Rakamak** plugin.

**Source file:** configured via `Converter.rakamakFile` in AuthMe's `config.yml`.

---

### RoyalAuth → `royalauth`

Migrates accounts from the **RoyalAuth** plugin.

**Source file:** `plugins/RoyalAuth/players.yml`

---

### vAuth → `vauth`

Migrates accounts from the **vAuth** plugin.

**Source file:** `plugins/vAuth/passwords.yml`

---

### SQLite → SQL → `sqlitetosql`

Copies AuthMe data from a SQLite database into the configured SQL database.

---

### MySQL → SQLite → `mysqltosqlite`

Copies AuthMe data from the configured MySQL database into a SQLite file.
