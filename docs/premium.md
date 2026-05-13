# Premium bypass

AuthMe can let players with a legitimate Mojang account skip password authentication entirely.
When a premium-enrolled player connects, AuthMe independently verifies their identity by
running a cryptographic handshake with Mojang's session server — no password prompt, no
dialog box.

## Requirements

- **Direct connections (no proxy):**
  - the backend must run in **offline mode**
  - **PacketEvents** must be installed on the backend server
- **Behind a proxy:**
  - install the matching AuthMe proxy plugin:
    - **authme-velocity** for Velocity
    - **authme-bungee** for BungeeCord / Waterfall
  - set `Hooks.bungeecord: true` on every backend
  - set the same shared secret in the proxy config and in `Hooks.proxySharedSecret` on every backend
  - choose the proxy UUID mode with `premium.keepOfflineUuidCompatibility`:
    - `false` (**default**) keeps the Mojang UUID on the backend
    - `true` preserves the backend offline UUID v3 for plugin compatibility

Without the required premium-verification component for your topology, AuthMe fails closed
and falls back to normal password authentication.

## Setup

### 1. Install the required verification component

- **Direct backend only:** install **PacketEvents 2.x** in `plugins/`
- **Velocity proxy:** install **AuthMe Velocity**
- **BungeeCord / Waterfall proxy:** install **AuthMe Bungee**
  - install **PacketEvents 2.x** on the proxy only if `premium.keepOfflineUuidCompatibility: true`

### 2. Enable premium mode in `config.yml`

```yaml
settings:
  enablePremium: true

Hooks:
  # Required only when using AuthMe behind Velocity/Bungee
  bungeecord: true
  proxySharedSecret: "same-secret-as-proxy"
```

```yaml
# Proxy config (Velocity or Bungee)
premium:
  keepOfflineUuidCompatibility: false
```

### 3. Enroll players

Players must opt in individually after the admin enables premium mode.

**Player commands:**
```text
/premium    — opt in  (must be logged in with password first)
/freemium   — opt out (reverts to password authentication)
```
AuthMe fetches the player's Mojang UUID on opt-in and stores it in the database.
From the next login onward the player bypasses the password prompt.

**Admin commands:**
```text
/authme premium <player>    — enrol a player
/authme freemium <player>   — remove a player from premium bypass
```

---

## How it works

When a premium-enrolled player connects, AuthMe intercepts the Minecraft login handshake at
the packet level:

```
Client                         Server (AuthMe + PacketEvents)        Mojang
  |                                  |                                 |
  |--LOGIN_START(name)-------------->|                                 |
  |                                  | ① DB: isPremium → true (async)  |
  |<--ENCRYPTION_REQUEST-------------|                                 |
  |  (RSA-1024 public key +          |                                 |
  |   random verify token)           |                                 |
  |  POST /session/minecraft/join ---------------------------------->  |
  |--ENCRYPTION_RESPONSE------------>|                                 |
  |  (enc(sharedSecret) +            |                                 |
  |   enc(verifyToken), RSA)         |                                 |
  |                                  | ② RSA-decrypt sharedSecret      |
  |                                  |   (sync, on event-loop)         |
  |                                  | ③ Install AES/CFB8 Netty ciphers|
  |                                  |   (sync — client already sends  |
  |                                  |    encrypted bytes from here)   |
  |                                  | ④ Verify token + GET /hasJoined |
  |                                  |   (async) --------------------->|
  |                                  |<--- {uuid, name, properties} ---|
  |                                  | ⑤ Store verified UUID (60 s TTL)|
  |                                  | ⑥ Re-inject LOGIN_START         |
  |     ... player joins PLAY ...    |                                 |
  |                                  |                                 |
  AsynchronousJoin: getVerifiedUuid(name) == auth.getPremiumUuid() → auto-login
```

**The cryptographic guarantee:** the server generates a fresh RSA key pair at startup and a
new random verify token per connection. Mojang's `hasJoined` endpoint only returns a 200
response if the client called `/session/join` with the correct server-id hash derived from
the shared AES key. An attacker who knows only the player's name cannot forge this exchange.

**Pre-join dialogs (Paper/Folia):** if `settings.registration.dialog.preJoin.enable` is also
enabled, the pre-join dialog is skipped entirely for verified premium players — no blocking
UI shown, no password field displayed.

---

## Behind a proxy

### AuthMe proxy plugin flow

For premium mode behind a proxy, use the matching AuthMe proxy plugin instead of relying on
plain proxy UUID forwarding.

The proxy verifies premium players first, then forwards a **signed** premium claim to the
backend. The backend UUID behavior depends on `premium.keepOfflineUuidCompatibility`.

**Backend configuration:**

```yaml
settings:
  enablePremium: true

Hooks:
  bungeecord: true
  proxySharedSecret: "same-secret-as-proxy"
```

**Per-proxy behavior:**

| Proxy | `premium.keepOfflineUuidCompatibility: false` (default) | `premium.keepOfflineUuidCompatibility: true` |
|---|---|---|
| Velocity | Native per-player online-mode login, Mojang UUID forwarded to backend | Native per-player online-mode login + rewrite back to offline UUID v3 |
| BungeeCord / Waterfall | Local per-player online-mode handshake, Mojang UUID forwarded to backend | PacketEvents login-phase verification, then resume offline login with offline UUID v3 |

**Requirements by mode:**

| Mode | Velocity | BungeeCord / Waterfall |
|---|---|---|
| `false` | No PacketEvents required | No PacketEvents required |
| `true` | No PacketEvents required | PacketEvents required on the proxy |

**What the backend trusts:**

1. the `perform.login` message must have a valid HMAC using `Hooks.proxySharedSecret`
2. the optional Mojang UUID inside that message must match either:
   - the player's stored premium UUID, or
   - the pending premium enrollment being finalized

If either check fails, the premium auto-login request is rejected.

**Premium cache synchronization:**

- When the proxy plugin starts, the backend sends the full list of enrolled premium usernames.
- When a player runs `/premium` or `/authme premium <player>`, the backend notifies the proxy
  immediately so the cache stays up to date.

> **Security:** `Hooks.bungeecord: true` enables proxy-backed login handling, so backend ports
> must only be reachable by the proxy. Do not expose backend servers directly to players.

### Choosing the backend UUID mode

`premium.keepOfflineUuidCompatibility` is a **proxy-side feature flag**:

- `false` (**default**): premium players keep their **Mojang UUID v4** on the backend
- `true`: premium players keep the backend **offline UUID v3** while the proxy still proves their premium identity

Use `false` if you want the simplest premium proxy flow. Use `true` only when backend-side
plugin compatibility requires offline UUID semantics.

### Plain online-mode proxy forwarding

If you run a proxy in normal online mode **without** the AuthMe proxy plugin, the backend sees
the forwarded Mojang UUID from the proxy.

That setup can work for general proxy forwarding, but it does **not** preserve the backend UUID
on the offline v3 value. If you need premium bypass **and** backend plugin compatibility based on
offline UUIDs, use the AuthMe proxy plugin flow above instead.

---

## Configuration reference

```yaml
settings:
  # Enable premium mode: players with an official Minecraft account
  # can skip password authentication.
  # Verification method is chosen automatically:
  #   - direct offline-mode backend: PacketEvents verifies the Mojang session.
  #   - behind AuthMe Velocity/Bungee proxy: the proxy verifies premium players
  #     and sends a signed premium claim.
  #     premium.keepOfflineUuidCompatibility=false (default) keeps the Mojang UUID.
  #     premium.keepOfflineUuidCompatibility=true keeps the backend offline UUID.
  #   - plain online-mode proxy forwarding also forwards the Mojang UUID, but
  #     without the AuthMe proxy plugin's signed premium flow.
  # If verification is unavailable, premium auto-login is disabled (fail closed).
  # Players must use /premium to opt in.
  enablePremium: false
```

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/premium` | `authme.player.premium` | Enrol the calling player in premium bypass (must be logged in). |
| `/freemium` | `authme.player.freemium` | Remove the calling player from premium bypass. |
| `/authme premium <player>` | `authme.admin.setpremium` | Enrol a player (admin). |
| `/authme freemium <player>` | `authme.admin.setfreemium` | Remove a player from premium bypass (admin). |

---

## Frequently asked questions

**Q: Can I use this on an online-mode server?**  
A: Online-mode servers already enforce Mojang authentication at the server level — you do not
need AuthMe's premium bypass at all. AuthMe is primarily designed for offline-mode servers, or
for proxy setups where the proxy verifies premium identity but the backend still keeps offline
UUID semantics.

**Q: What happens if Mojang's session server is down?**  
A: The Minecraft client must contact `sessionserver.mojang.com/session/minecraft/join` before
sending `ENCRYPTION_RESPONSE` — if that call fails, the client drops the connection entirely and
the player cannot join at all. On the server side, if `hasJoined` were to return an error anyway,
the verified UUID is not stored and `canBypassWithPremium()` returns `false`, so the feature
always fails closed: connectivity problems never grant unverified access.

**Q: What happens if a premium player changes their Minecraft username?**  
A: The premium bypass is keyed on the **Mojang UUID** (not the name), so a name change does
not invalidate the enrolment. However, since AuthMe accounts are keyed on the lowercase
player name, the player may need to be re-enrolled with `/authme premium` after a rename,
depending on your account-linking configuration.

**Q: Can a non-premium player impersonate a premium player?**  
A: No. In direct mode, the verify-token check and the `hasJoined` call ensure that only a client
which actually holds the Mojang session for that account can complete the handshake. Behind a
proxy, the backend additionally requires a valid HMAC-signed `perform.login` payload and refuses
any premium UUID that does not match stored or pending premium state. An attacker who merely knows
the username cannot forge those checks.
