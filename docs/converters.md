## Converters

Converters allow you to migrate player data from another authentication plugin into AuthMe.
Run a converter with `/authme converter <name>` (requires `authme.admin.converter`).

---

AuthMeReloaded currently ships the **Auth+**, **LibreLogin**, **LimboAuth**, and **nLogin** converters, plus the built-in database migration helpers below.

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

### LibreLogin → `librelogin`

Migrates accounts from the **LibreLogin** plugin.

**Requirement:** LibreLogin and AuthMe must share the same MySQL/MariaDB database (same host, port, and database name as configured in AuthMe's `config.yml`). SQLite databases are not supported by this converter.

**Source table:** `librepremium_data`

**Before running**, set `passwordHash` in AuthMe's `config.yml` to match the algorithm used by LibreLogin. LibreLogin stores a per-account algorithm identifier, so the mapping is:

| LibreLogin algorithm | AuthMe `passwordHash` |
|---|---|
| `BCrypt-2A` (default) | `BCRYPT` |
| `Argon2-ID` | `ARGON2` |
| `SHA-256` | `SHA256` |
| `SHA-512` | `DOUBLE_SHA512` |
| `LOGIT-SHA-256` | `SALTEDSHA256` |

If your LibreLogin database contains accounts with **mixed algorithms**, set `passwordHash` to the most common one and ask remaining players to reset their password after migration.

**Notes:**
- Premium UUIDs are preserved (players enrolled in premium bypass are migrated as-is).
- TOTP secrets are migrated.
- Players already present in AuthMe's database are skipped automatically.

---

### LimboAuth → `limboauth`

Migrates accounts from the **LimboAuth** plugin.

**Requirement:** LimboAuth and AuthMe must share the same MySQL/MariaDB database.

**Source table:** `AUTH`

**Before running:**
1. Set `passwordHash` to `BCRYPT` in AuthMe's `config.yml` — LimboAuth uses BCrypt exclusively for new registrations.

**Notes:**
- LimboAuth does not store email addresses; the email field will be empty for migrated accounts.
- Premium UUIDs and TOTP secrets are migrated.
- Players already present in AuthMe's database are skipped automatically.

---

### nLogin → `nlogin`

Migrates accounts from the **nLogin** plugin.

**Requirement:** nLogin and AuthMe must share the same MySQL/MariaDB database.

**Source table:** `nlogin`

**Before running**, set `passwordHash` in AuthMe's `config.yml` to match the algorithm configured in nLogin. nLogin deliberately reuses AuthMe's hash formats, so hashes are transferred as-is:

| nLogin algorithm | AuthMe `passwordHash` |
|---|---|
| BCrypt (default) | `BCRYPT` or `BCRYPT2Y` |
| SHA-256 (`$SHA$…`) | `SHA256` |
| Argon2 | `ARGON2` |

**Notes:**
- Email addresses and last-login timestamps are migrated.
- Players already present in AuthMe's database are skipped automatically.

---

### SQLite → SQL → `sqlitetosql`

Copies AuthMe data from a SQLite database into the configured SQL database.

---

### MySQL → SQLite → `mysqltosqlite`

Copies AuthMe data from the configured MySQL database into a SQLite file.
