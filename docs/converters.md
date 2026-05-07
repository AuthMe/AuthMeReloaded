## Converters

Converters allow you to migrate player data from another authentication plugin into AuthMe.
Run a converter with `/authme converter <name>` (requires `authme.admin.converter`).

---

AuthMeReloaded currently ships the **Auth+**, **LibreLogin**, **LimboAuth**, **nLogin**, **OpeNLogin**, and **tiAuth** converters, plus the built-in database migration helpers below.

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

**The converter reads `plugins/LibreLogin/config.conf` to determine the storage backend.**

**Supported database types:**

| LibreLogin `database.type` | Requirement |
|---|---|
| `librelogin-sqlite` (default) | Reads the SQLite database at `plugins/LibreLogin/<sqlite.path>` (default: `user-data.db`) directly — no shared database required. |
| `librelogin-mysql` / `librelogin-postgresql` | LibreLogin and AuthMe must share the same database (same host, port, and database name). |

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

**The converter reads `plugins/LimboAuth/config.yml` to determine the storage backend.**

**Supported database types:**

| LimboAuth `database.storage-type` | Requirement |
|---|---|
| `SQLITE` | Reads `plugins/LimboAuth/limboauth.db` directly — no shared database required. |
| `MYSQL` / `MARIADB` / `POSTGRESQL` | LimboAuth and AuthMe must share the same database (same host, port, and database name). |
| `H2` (default) | Not supported. Reconfigure LimboAuth to use `SQLITE` or `MYSQL`/`MARIADB`/`POSTGRESQL`, migrate the data, then re-run this converter. |

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

**The converter reads `plugins/nLogin/config.yml` to determine the storage backend:**

| nLogin `database.type` | Requirement |
|---|---|
| `SQLite` (default) | Reads `plugins/nLogin/nlogin.db` directly — no shared database required. |
| `MySQL` / `MariaDB` | nLogin and AuthMe must share the same database (same host, port, and database name). |

**Source table:** Read from `database.table.account.table-name` in nLogin's config (default: `nlogin`).

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

### OpeNLogin → `openlogin`

Migrates accounts from the **OpeNLogin** plugin.

**Requirement:** OpeNLogin must be (or have been) installed on the same server. The converter reads its SQLite database file directly — no shared database is required.

**Source file:** `plugins/OpeNLogin/accounts.db`

**Before running:**
1. Set `passwordHash` to `BCRYPT` in AuthMe's `config.yml` — OpeNLogin uses BCrypt exclusively.

**Notes:**
- OpeNLogin does not store email addresses or UUIDs; those fields will be empty for migrated accounts.
- Players already present in AuthMe's database are skipped automatically.

---

### tiAuth → `tiauth`

Migrates accounts from the **tiAuth** plugin.

**The converter reads `plugins/tiAuth/config.yml` to determine the storage backend.**

**Supported database types:**

| tiAuth `database.type` | Requirement |
|---|---|
| `SQLITE` | Reads `plugins/tiAuth/auth.db` directly — no shared database required. |
| `MYSQL` / `POSTGRESQL` | tiAuth and AuthMe must share the same database (same host, port, and database name). |
| `H2` (default) | Not supported. Reconfigure tiAuth to use `SQLITE` or `MYSQL`, migrate the data, then re-run this converter. |

**Source table:** `auth_users`

**Before running**, set `passwordHash` in AuthMe's `config.yml` to match the algorithm configured in tiAuth. tiAuth hashes are directly compatible with AuthMe's formats:

| tiAuth algorithm | AuthMe `passwordHash` |
|---|---|
| BCrypt (default, cost 12) | `BCRYPT` |
| SHA-256 (`$SHA$…`) | `SHA256` |

**Notes:**
- Players already present in AuthMe's database are skipped automatically.

---

### SQLite → SQL → `sqlitetosql`

Copies AuthMe data from a SQLite database into the configured SQL database (MySQL, MariaDB, or PostgreSQL). Configure the destination connection in AuthMe's `config.yml` before running.

---

### MySQL → SQLite → `mysqltosqlite`

Copies AuthMe data from the configured MySQL database into a SQLite file.
