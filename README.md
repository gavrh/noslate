# Noslate  
_Deepslate Hider Plugin_

[![modrinth](https://cdn.jsdelivr.net/gh/Andre601/devins-badges@13e0142/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/noslate)

## Overview

Noslate is a plugin that hides all terrain below Y=0 unless the player is underground. It is built to improve fairness in SMP and survival servers by preventing cheaters from revealing hidden bases, caves or ores using hacked clients or freecam mods. The plugin is lightweight and runs without modifying the world. It intercepts and filters chunk data to hide terrain below ground level and replaces it with fake data such as barrier blocks to block vision and weather effects.

When players go underground they are shown a configurable radius of real terrain around them. Optionally connected cave systems can be revealed to create a smooth and immersive exploration experience.


## Key Features

- **Anti-Xray for Deepslate:** Hides underground bases, farms, and caves below Y=0
- **Survival Friendly:** Makes cheating harder without hurting legit players
- **Fake Chunk Barriers:** Blocks vision and weather from above
- **Dynamic Reveal:** Real terrain appears as players explore deeper
- **Zero World Changes:** No blocks are placed or removed
- **Folia Compatible:** Fully async-safe, built for modern multi-threaded servers

## Ideal For

- ✅ SMPs and Survival Servers  
- ✅ Base protection without rollback or block logs  
- ✅ Lightweight anti-cheat alternative  
- ❌ Not designed for creative worlds or minigames

## Supported Platforms

| Platform  | Support |
|-----------|---------|
| **Folia** | ✅ Full  |
| **Paper** | ✅ Full  |
| **Purpur**| ✅ Full  |
| **Spigot**| ⚠️ Partial (sync-only) |

> 💡 Best used on Paper or Folia for full performance and async safety.

## Installation

### Modrinth
1. Download the latest `.jar` from [Modrinth](https://modrinth.com/plugin/noslate)
2. Drop it into your `plugins/` folder
3. Restart your server
4. Configure settings in `plugins/Noslate/config.yml`

### Source
coming soon...

## Configuration Options

- `reveal-radius`: Chunks to show around players underground (default: 2)
- `extend-caves`: Whether to reveal connected cave systems
- `simulate-barriers`: Whether to send fake barriers above Y=0
- `debug-mode`: For testing or logging sent chunk info

_No commands or permissions are currently included._

## License

Noslate is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

See [LICENSE](./LICENSE) for full details.

---

_Protect your underground world. Keep survival fair._
