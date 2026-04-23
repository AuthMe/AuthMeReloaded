# AuthMe — Proxy Setup Guide

This guide covers configuring AuthMe in a BungeeCord or Velocity network. The setup involves two components:

- **Proxy plugin** — AuthMe Bungee or AuthMe Velocity, installed on the proxy.
- **Backend plugin** — the standard AuthMe jar (Paper/Spigot/Folia), installed on every backend server.

---

## Architecture overview

```
              Player
                │
                ▼
┌──────────────────────────────────┐
│  Proxy (BungeeCord / Velocity)   │
│  AuthMe Bungee / AuthMe Velocity │
│  – tracks auth state per player  │
│  – blocks commands/chat          │
│  – blocks non-auth server switch │
│  – forwards perform.login        │
└────────────┬─────────────────────┘
             │  plugin messaging (authme:main)
     ┌───────┴────────┐
     │                │
  lobby (auth)    survival (game)
  AuthMe backend  AuthMe backend
```

Auth state flows from backend → proxy (login/logout messages) and from proxy → backend (perform.login after server switch).

---

## Installation

| Component | Where to put the jar |
|---|---|
| AuthMe Velocity | `velocity/plugins/` |
| AuthMe Bungee | `BungeeCord/plugins/` |
| AuthMe backend (Paper/Spigot/Folia) | `plugins/` of every backend server |

Do **not** install the proxy plugin on a backend server, or the backend plugin on the proxy.

---

## Backend configuration (every backend server)

### 1. Enable BungeeCord/Velocity in Spigot

In `spigot.yml` (Spigot) or `config/paper-global.yml` (Paper):

```yaml
# spigot.yml
settings:
  bungeecord: true
```

```yaml
# paper-global.yml  (Paper 1.19+)
proxies:
  velocity:
    enabled: true           # if using Velocity
    secret: ""              # leave blank for BungeeCord; see Paper docs for Velocity
  bungee-cord:
    online-mode: true       # if using BungeeCord
```

This is required for plugin messaging to work between the proxy and the backend.

### 2. Enable the hook in AuthMe config

In `plugins/AuthMe/config.yml` on every backend server:

```yaml
Hooks:
  bungeecord: true

  # Shared secret — must match proxySharedSecret in the proxy plugin config.
  # Leave empty only for testing; always set in production.
  proxySharedSecret: ""

  # Optional: redirect players to this backend after login/register.
  # Leave empty to keep players on the current server.
  sendPlayerTo: ""
```

---

## Proxy configuration

The proxy plugin creates its config file on first start and **auto-generates a random `proxySharedSecret`**. You copy that value to every backend.

### AuthMe Velocity

Config file: `plugins/authmevelocity/config.yml`

### AuthMe Bungee

Config file: `plugins/AuthMeBungee/config.yml`

Both share the same settings model. Reference config files are available at:

- [`docs/proxies/velocity/config.yml`](velocity/config.yml)
- [`docs/proxies/bungee/config.yml`](bungee/config.yml)

---

## Setting up the shared secret

The shared secret prevents malicious backend servers (or other plugins) from forging `perform.login` messages to auto-authenticate players they shouldn't.

1. Start the proxy once. A random 64-character hex secret is written to `proxySharedSecret` in the proxy config.
2. Copy that value to `Hooks.proxySharedSecret` in the AuthMe `config.yml` of **every backend server**.
3. Restart all backend servers (or run `/authme reload`).

If `Hooks.proxySharedSecret` is left empty on a backend, HMAC verification still runs — using an empty string as the key. This means the backend will only accept `perform.login` messages signed with the same empty key, which provides no real security. Always set a real secret in production.

---

## Configuration reference

### Auth servers — `authServers`

The list of backend server names that run AuthMe and act as login/registration points. Players connecting to these servers are subject to the unauthenticated-player restrictions below.

```yaml
authServers:
  - lobby
  - auth
```

Use `allServersAreAuthServers: true` to treat every backend as an auth server without listing them individually.

---

### Auto-login — `autoLogin`

When enabled, the proxy sends a signed `perform.login` message to the backend whenever an already-authenticated player connects to an auth server. This lets players stay logged in as they switch between servers without re-entering their password.

```yaml
autoLogin: false   # default; set to true to enable
```

**Requires** `Hooks.bungeecord: true` and a valid `proxySharedSecret` on every backend.

---

### Server switch gating — `serverSwitch`

Prevents unauthenticated players from switching to non-auth backends. Players already on an auth server are not affected.

```yaml
serverSwitch:
  requiresAuth: true
  kickMessage: "Authentication required."
```

When `requiresAuth` is `true`:

- A player trying to join a non-auth backend directly (e.g. via `/server survival`) is blocked and shown `kickMessage`.
- A player who is not yet on any server (initial connection) and whose first destination is a non-auth backend is disconnected.

---

### Logout redirect — `sendOnLogout`

When a backend sends a `logout` message (e.g. on `/logout`), the proxy can redirect the player to a dedicated server.

```yaml
sendOnLogout: false
unloggedUserServer: ""   # e.g. "limbo" or "auth"
```

Both settings must be configured together. If `sendOnLogout` is `true` but `unloggedUserServer` is empty, a warning is logged and the redirect is skipped.

---

### Command blocking — `commands.requireAuth`

Blocks unauthenticated players on auth servers from running commands that are not on the whitelist.

```yaml
commands:
  requireAuth: true
  whitelist:
    - /login
    - /register
    - /l
    - /reg
    - /email
    - /captcha
    - /2fa
    - /totp
    - /log
```

Commands are matched by alias (the first word), case-insensitively, with or without the leading `/`. Players on non-auth servers are not affected.

---

### Chat blocking — `chatRequiresAuth`

Blocks unauthenticated players on auth servers from sending chat messages.

```yaml
chatRequiresAuth: true
```

---

## Multi-proxy setup

Some networks run multiple proxy instances behind a load balancer (e.g., two Velocity nodes behind HAProxy or TCPShield). The following explains what works automatically and what requires manual coordination.

### Shared secret across proxy instances

Each proxy instance generates its own `proxySharedSecret` on first start. However, the backend only stores a **single** `Hooks.proxySharedSecret` value, used to verify every incoming `perform.login` regardless of which proxy sent it. For the HMAC check to pass, all proxy instances must sign with the same secret.

**Setup:**

1. Start one proxy instance and let it generate its `proxySharedSecret`.
2. **Copy that value** into `proxySharedSecret` of every other proxy instance's config (replacing their auto-generated values).
3. Copy the same value to `Hooks.proxySharedSecret` on every backend server.

```yaml
# proxy-1/plugins/authmevelocity/config.yml
proxySharedSecret: "a3f8c2...same on all proxies..."

# proxy-2/plugins/authmevelocity/config.yml
proxySharedSecret: "a3f8c2...same on all proxies..."

# backend plugins/AuthMe/config.yml  (all backends)
Hooks:
  proxySharedSecret: "a3f8c2...same on all proxies..."
```

### Auth state is local to each proxy instance

Each proxy instance tracks authenticated players in its own in-memory store (a `ConcurrentHashMap`). This store is populated when a backend sends a `login` message through that proxy's connection. Because plugin messages travel through the specific proxy-player connection, the proxy the player is currently connected through always has correct auth state for that player.

This means **normal operation works correctly**: commands are blocked, server switches are gated, and `autoLogin` forwards `perform.login` — all through the proxy instance the player is on, which has the right state.

### Reconnect through a different proxy instance

If a player disconnects and reconnects through a **different proxy instance** (e.g., the load balancer picks a different node), the new proxy has no record of them being authenticated. The consequences depend on which features are enabled:

| Feature | Behaviour on reconnect to a different proxy |
|---|---|
| `serverSwitch.requiresAuth` | Player can still connect to an auth server (correct — they must re-authenticate). Attempting to reach a non-auth server directly is blocked until they log in. |
| `autoLogin` | The new proxy does not know the player was authenticated, so it **will not** send `perform.login` automatically. The player must log in again on the auth server. |
| `commands.requireAuth` / `chatRequiresAuth` | Enforced correctly from the moment the player reaches an auth server — no stale state. |
| `sendOnLogout` | Works correctly — logout messages flow through the proxy the player is currently on. |

### Mitigating forced re-authentication on proxy switch

If players frequently reconnect to different proxy nodes and re-authenticating on every reconnect is undesirable, enable AuthMe's built-in **session feature** on the backend servers:

```yaml
# plugins/AuthMe/config.yml  (backend)
settings:
  sessions:
    enabled: true
    timeout: 10   # minutes; player is auto-logged in if they reconnect within this window
```

With sessions enabled, a player who reconnects within the timeout window is automatically authenticated by the backend itself, regardless of which proxy they came through. The new proxy's auth store is then updated by the `login` message the backend sends after the session login.

### Summary

| Concern | What to do |
|---|---|
| Shared secret | Pick one proxy's auto-generated secret; manually set the same value on every other proxy and every backend |
| Auth state | No action needed — each proxy tracks state for the players routed to it |
| Re-auth on proxy switch | Enable `settings.sessions.enabled` on backends if you want seamless reconnects |

---

## Reload command

Changes to the proxy config file can be applied without restarting the proxy:

| Proxy | Command | Permission |
|---|---|---|
| Velocity | `/avreloadproxy` | `authmevelocity.reload` |
| BungeeCord | `/abreloadproxy` | `authmebungee.reload` |

---

## Typical setup checklist

- [ ] Backend `spigot.yml` / Paper config has BungeeCord/Velocity forwarding enabled
- [ ] `Hooks.bungeecord: true` in AuthMe `config.yml` on every backend
- [ ] Proxy plugin installed and started at least once (generates the secret)
- [ ] `proxySharedSecret` from proxy config copied to `Hooks.proxySharedSecret` on every backend
- [ ] `authServers` in proxy config lists every server where AuthMe is the gatekeeper
- [ ] `autoLogin: true` if players should stay logged in across server switches
- [ ] `serverSwitch.requiresAuth: true` if unauthenticated players must not reach game servers
- [ ] `sendOnLogout` and `unloggedUserServer` configured if you want explicit logout redirects
- [ ] **Multi-proxy only:** all proxy instances share the same `proxySharedSecret` value

---

## Troubleshooting

**Players are not auto-logged in after switching servers**
- Check `autoLogin: true` in the proxy config.
- Check `Hooks.proxySharedSecret` is set and matches `proxySharedSecret` in the proxy config.
- Check `Hooks.bungeecord: true` in the backend AuthMe config.
- Check `bungeecord: true` in `spigot.yml` (or Paper equivalent) — plugin messaging won't work without it.

**`Rejected perform.login for <player>: invalid HMAC`** in backend logs
- The `proxySharedSecret` on this backend does not match the one in the proxy config. Copy it again and reload.

**`Rejected perform.login for <player>: message has expired`** in backend logs
- The system clocks of the proxy and backend are out of sync by more than 30 seconds. Synchronize them (NTP).

**Players can still chat or run commands while unauthenticated**
- Verify the player is connecting to a server listed in `authServers` (or `allServersAreAuthServers: true`).
- Check `commands.requireAuth: true` and `chatRequiresAuth: true` in the proxy config.

**Multi-proxy: players must re-authenticate after reconnecting to a different proxy node**
- This is expected behaviour — each proxy instance has its own in-memory auth state.
- Enable `settings.sessions.enabled: true` (with a positive `timeout`) in the backend AuthMe config to auto-resume sessions on reconnect, regardless of which proxy handled the previous connection.
