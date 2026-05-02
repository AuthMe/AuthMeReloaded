# Premium bypass

AuthMe can let players with a legitimate Mojang account skip password authentication entirely.
When a premium-enrolled player connects, AuthMe independently verifies their identity by
running a cryptographic handshake with Mojang's session server — no password prompt, no
dialog box.

## Requirements

- **PacketEvents** must be installed as a separate plugin on the server, **unless** you
  are using proxy mode (see [Behind a proxy](#behind-a-proxy)).  
  Without PacketEvents in direct-connection mode, premium bypass is disabled at startup
  (AuthMe logs a warning and falls back to normal password authentication for everyone).
- For direct connections: an **offline-mode server** that clients reach without a proxy.

## Setup

### 1. Install PacketEvents

Download **PacketEvents 2.x** and drop the jar into your `plugins/` folder.

### 2. Enable premium mode in `config.yml`

```yaml
settings:
  enablePremium: true
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

**Pre-join dialogs (Paper/Folia):** if `settings.registration.usePreJoinDialogUi` is also
enabled, the pre-join dialog is skipped entirely for verified premium players — no blocking
UI shown, no password field displayed.

---

## Behind a proxy

### Online-mode proxy (Velocity / BungeeCord online-mode)

When Velocity or BungeeCord runs in **online mode**, the proxy authenticates players with
Mojang before they reach the backend. The proxy then forwards the real Mojang UUID to the
backend via IP forwarding. AuthMe uses that forwarded UUID directly — no PacketEvents
required, no synthetic `ENCRYPTION_REQUEST` sent.

**Backend configuration:**

```yaml
settings:
  enablePremium: true

Hooks:
  bungeecord: true   # trust the UUID forwarded by the proxy
```

**Proxy configuration requirements:**

| Proxy | Required settings |
|---|---|
| Velocity | `player-info-forwarding-mode: MODERN` in `velocity.toml` + shared secret in `paper-global.yml` (or equivalent) |
| BungeeCord | `ip_forward: true` in BungeeCord `config.yml` + `bungeecord: true` in backend `spigot.yml` |

> **Security:** with `Hooks.bungeecord: true` the backend trusts the UUID forwarded by the proxy.
> The backend port **must** be firewalled so only the proxy can reach it — otherwise
> anyone could connect directly with an arbitrary UUID and bypass authentication.

PacketEvents is **not** required in this configuration.

### Offline-mode proxy with AuthMe proxy plugin

When the proxy runs in **offline mode**, install the matching AuthMe proxy plugin on the proxy:

- **authme-velocity** for Velocity
- **authme-bungee** for BungeeCord

These plugins maintain a list of premium-enrolled players and force per-player Mojang
authentication for them via `PreLoginEvent`. The proxy then forwards the verified Mojang UUID
to the backend the same way as in online-mode proxy setup. Set `Hooks.bungeecord: true` on the backend.

The premium player list is synchronised automatically:
- When the proxy plugin starts, the backend sends the full list of enrolled premium usernames.
- When a player runs `/premium` or `/authme premium <player>`, the backend notifies the proxy
  immediately so the cache stays up to date.

---

## Configuration reference

```yaml
settings:
  # Enable premium mode: players with an official Minecraft account
  # can skip password authentication.
  # Verification method is chosen automatically:
  #   - online-mode=true: Bukkit already has the Mojang UUID; no PacketEvents needed.
  #   - offline-mode + proxy: set Hooks.bungeecord=true; UUID is forwarded by proxy.
  #   - offline-mode, no proxy: PacketEvents required for cryptographic verification.
  #     Without PacketEvents, premium auto-login is disabled (fail closed).
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
need AuthMe's premium bypass at all. AuthMe is primarily designed for offline-mode servers.

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
A: No. The verify-token check and the `hasJoined` call together ensure that only a client
which actually holds the Mojang session for that account can complete the handshake. An
attacker who merely knows the username cannot forge the encrypted shared secret.
