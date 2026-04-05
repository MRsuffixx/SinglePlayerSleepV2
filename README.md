# SinglePlayerSleep

> Skip the night on a configurable condition. Ultra‑optimized.

SinglePlayerSleep is a lightweight, highly‑configurable sleep system for modern Minecraft servers. It supports **single‑player skip**, **percentage‑based voting**, **AFK exclusion**, **countdowns**, **particle/sound effects**, **phantom reset**, **stats tracking**, and **PlaceholderAPI** integration.

---

## Table of Contents

- [Features](#features)
- [Compatibility](#compatibility)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
  - [Sleep Behavior](#sleep-behavior)
  - [AFK System](#afk-system)
  - [Countdown](#countdown)
  - [Messages](#messages)
  - [Effects (Particles & Sounds)](#effects-particles--sounds)
  - [World Filtering](#world-filtering)
  - [Phantom Reset](#phantom-reset)
  - [Update Checker](#update-checker)
  - [Command Settings](#command-settings)
  - [Statistics](#statistics)
  - [Debug](#debug)
- [Commands](#commands)
- [Permissions](#permissions)
- [Placeholders (PlaceholderAPI)](#placeholders-placeholderapi)
- [Data Files & Storage](#data-files--storage)
- [Building from Source](#building-from-source)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- ✅ **Single‑player sleep** (one player can skip the night).
- ✅ **Percentage‑based voting** for multiplayer worlds.
- ✅ **AFK detection** with configurable timeout + optional exclusion.
- ✅ **Countdown** before skipping night (actionbar + chat support).
- ✅ **Particles & sounds** with smart scaling to reduce lag.
- ✅ **Per‑world enable/disable** (whitelist or blacklist mode).
- ✅ **Phantom timer reset** on night skip (Paper‑aware).
- ✅ **Update checker** via GitHub releases.
- ✅ **Stats tracking** (global + per‑player) persisted to `stats.yml`.
- ✅ **PlaceholderAPI integration** (optional soft dependency).

---

## Compatibility

- **Target API:** `1.21` (Paper API 1.21.1)
- **Recommended server:** Paper 1.21+ (or compatible forks)
- **Should also run on:** Spigot 1.21+ (Paper‑only phantom event is automatically skipped)
- **Java version:** **Java 21**

---

## Quick Start

1. Drop the plugin JAR into `plugins/`.
2. Restart the server (first start generates `config.yml`).
3. (Optional) Install PlaceholderAPI for placeholders.
4. Configure as needed, then run `/sps reload`.

---

## Installation

### 1) Server Install

1. Download a release JAR (or [build from source](#building-from-source)).
2. Place it in your server’s `plugins/` folder.
3. Start or restart the server.
4. Configure `plugins/SinglePlayerSleep/config.yml`.
5. Apply changes with `/sps reload`.

> **Important:** If you change `command.sleep-command-name`, the new name must also exist in `plugin.yml`. In practice, keep this as `sleep` and use aliases instead.

### 2) Optional PlaceholderAPI

- PlaceholderAPI is **optional** and listed as a `softdepend`.
- If PlaceholderAPI is present, SinglePlayerSleep registers `%sps_*%` placeholders automatically.

---

## Configuration

Config file path: `plugins/SinglePlayerSleep/config.yml`

### Sleep Behavior

| Key | Default | Description |
| --- | --- | --- |
| `sleep.mode` | `"single"` | `single` = one player sleeping skips night; `percentage` = vote system. |
| `sleep.percentage` | `50` | Required % of **non‑AFK** players (only in `percentage` mode). |
| `sleep.delay-ticks` | `100` | Delay before night skip (20 ticks = 1 second). |
| `sleep.cooldown-seconds` | `60` | Time before a new sleep session can start. |
| `sleep.clear-weather` | `true` | Clears rain/thunder after skip. |
| `sleep.auto-save` | `true` | Calls `world.save()` after skip. |

### AFK System

| Key | Default | Description |
| --- | --- | --- |
| `afk.enabled` | `true` | Enables AFK tracking. |
| `afk.timeout-seconds` | `300` | Inactivity time before AFK. |
| `afk.exclude-from-count` | `true` | AFK players are excluded from required sleepers. |
| `afk.check-interval-ticks` | `200` | How often AFK checks run. |

### Countdown

| Key | Default | Description |
| --- | --- | --- |
| `countdown.enabled` | `true` | Enables countdown before skip. |
| `countdown.duration-seconds` | `5` | Countdown length. |
| `countdown.show-actionbar` | `true` | Shows countdown in action bar. |
| `countdown.show-chat` | `false` | Also posts countdown to chat. |
| `countdown.sound-on-each-tick` | `true` | Tick sound each second. |

### Messages

Message keys live under `messages:` and support `&` color codes and placeholders:

Common placeholders:
- `{player}` – Player name
- `{current}` / `{required}` – Vote counts
- `{seconds}` – Remaining time
- `{version}` – Plugin version

### Effects (Particles & Sounds)

| Key | Default | Description |
| --- | --- | --- |
| `effects.particles.enabled` | `true` | Enables particles. |
| `effects.particles.type` | `CLOUD` | Bukkit `Particle` enum. |
| `effects.particles.smart-scale` | `true` | Reduces particle count with player count. |
| `effects.sounds.enabled` | `true` | Enables sounds. |
| `effects.sounds.sleep-start` | `ENTITY_PLAYER_BREATH` | Sound played on sleep start. |
| `effects.sounds.night-skip` | `UI_TOAST_CHALLENGE_COMPLETE` | Sound played on skip. |
| `effects.sounds.countdown-tick` | `BLOCK_NOTE_BLOCK_HAT` | Countdown tick sound. |

### World Filtering

| Key | Default | Description |
| --- | --- | --- |
| `worlds.enabled` | `["world"]` | World list used by mode below. |
| `worlds.mode` | `whitelist` | `whitelist` = only listed worlds enabled; `blacklist` = all except listed. |

### Phantom Reset

| Key | Default | Description |
| --- | --- | --- |
| `phantom.reset-on-skip` | `true` | Resets phantom timers for all online players after night skip. |

### Update Checker

| Key | Default | Description |
| --- | --- | --- |
| `update-checker.enabled` | `true` | Enables GitHub update check. |
| `update-checker.github-user` | `mrsuffix` | GitHub owner/user. |
| `update-checker.github-repo` | `SinglePlayerSleepV2` | GitHub repo name. |
| `update-checker.notify-on-join` | `true` | Notify ops on join if update available. |
| `update-checker.check-interval-hours` | `24` | Update check interval. |

### Command Settings

| Key | Default | Description |
| --- | --- | --- |
| `command.sleep-command-name` | `sleep` | Base command name (requires restart & plugin.yml alignment). |
| `command.sleep-aliases` | `goodnight`, `slp` | Additional aliases. |

### Statistics

| Key | Default | Description |
| --- | --- | --- |
| `stats.enabled` | `true` | Enables stats tracking. |
| `stats.persist` | `true` | Saves stats to disk (`stats.yml`). |
| `stats.track-per-player` | `true` | Per‑player stats. |

### Debug

| Key | Default | Description |
| --- | --- | --- |
| `debug.enabled` | `false` | Enables debug logging. |

---

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/sleep` | Vote for skipping the night (percentage mode). | `singleplayersleep.sleep` |
| `/sps reload` | Reload config + reset sessions. | `singleplayersleep.admin` |
| `/sps stats [player]` | View stats (global or per‑player). | `singleplayersleep.admin` |
| `/sps afk` | Toggle your AFK status manually. | `singleplayersleep.admin` |
| `/sps version` | Plugin info + update status. | `singleplayersleep.admin` |
| `/sps debug` | Toggle debug logging. | `singleplayersleep.admin` |

> In **single** mode, `/sleep` provides helpful info, but actual skipping is based on bed sleep events.

---

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `singleplayersleep.sleep` | `true` | Use `/sleep`. |
| `singleplayersleep.admin` | `op` | Use `/sps` and admin features. |
| `singleplayersleep.bypasscooldown` | `op` | Bypass sleep cooldown. |
| `singleplayersleep.bypassafk` | `op` | Never marked AFK. |
| `singleplayersleep.stats` | `true` | View statistics (reserved). |

---

## Placeholders (PlaceholderAPI)

> Requires PlaceholderAPI installed. Expansion identifier: `sps`

| Placeholder | Description |
| --- | --- |
| `%sps_sleeping%` | Current sleeping players in the viewer’s world. |
| `%sps_required%` | Required sleepers in the viewer’s world. |
| `%sps_is_night%` | `true/false` if it’s currently night. |
| `%sps_is_processing%` | `true/false` if night skip is in progress. |
| `%sps_cooldown%` | Remaining cooldown (seconds). |
| `%sps_is_afk%` | `true/false` if the player is AFK. |
| `%sps_nights_skipped%` | Total nights skipped (global). |
| `%sps_player_times_slept%` | Times the player has slept. |
| `%sps_mode%` | `single` or `percentage`. |
| `%sps_percentage%` | Required percentage. |

Example:

```
Sleeping: %sps_sleeping% / %sps_required%
Cooldown: %sps_cooldown%s
```

---

## Data Files & Storage

- **`config.yml`** – main configuration.
- **`stats.yml`** – stored in `plugins/SinglePlayerSleep/` if stats are enabled and persistence is on.

Tracked stats:
- Global: total nights skipped, total sleep events, last skip timestamp
- Per‑player: times slept, nights contributed

---

## Building from Source

Requirements:
- **Java 21**
- **Maven 3.9+**

Build:

```bash
mvn clean package
```

Output JAR:

```
./target/SinglePlayerSleep-2.0.0.jar
```

---

## Troubleshooting

**“/sleep doesn’t register after changing command name”**
- Bukkit commands must exist in `plugin.yml`. Use aliases instead of renaming, or update the plugin.yml and rebuild.

**“Update checker fails / no update messages”**
- Check `update-checker.enabled` and verify `github-user` / `github-repo` are correct.
- GitHub API may be rate‑limited; try increasing `check-interval-hours`.

**“Countdown effects are missing”**
- Ensure `effects.*` and `countdown.*` are enabled.

---

## Contributing

Contributions are welcome!

1. Fork the repo
2. Create a feature branch
3. Commit changes with clear messages
4. Open a PR describing the change

Please keep changes focused and respect existing style.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
