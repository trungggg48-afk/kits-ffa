# HyperFFA - Modern Standalone Kit, Scoreboard & FFA Engine

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Paper%20%2F%20Folia%20%2F%20Purpur-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![PAPI](https://img.shields.io/badge/PlaceholderAPI-Supported-purple.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**HyperFFA** is an ultra-fast, zero-lag, standalone PvP Practice FFA Engine tailored specifically for **Paper 1.21.x & Folia 1.21.x** (Java 21).

---

## 🌟 Key Features

- **Multi-Platform Multi-Threaded Engine**:
  - Fully compatible with **Folia 1.21** (Threaded Regionized Server) and **Paper/Purpur 1.21**.
  - Powered by custom `PlatformScheduler` for async and regionized multi-threaded tasks.
- **Redesigned GUI Matching In-Game Style**:
  - **Kit Room (54 slots)**: 3 Free Default slots (1-3), 3 VIP/Premium slots (4-6) with lock mechanics (`GRAY_DYE`), Clear Inventory (`TNT`), Item Room (`BARREL`), Premade Kit (`WRITABLE_BOOK`).
  - **Item Room (54 slots)**:
    - `Slot 45` (`AMETHYST_SHARD`): **Refill** (Restocks all 45 GUI slots without touching player inventory).
    - `Slot 47` (`NETHERITE_SWORD`): **Gear** (Mace 1 [Breach IV], Mace 2 [Wind Burst III / Density V], Netherite/Diamond Armor & Tools Max Enchant).
    - `Slot 48` (`POTION`): **Potions** (7 columns of Splash Potions + 2 columns of Tipped Arrows).
    - `Slot 49` (`TOTEM_OF_UNDYING`): **Consumables** (18 Totems, Regular Golden Apples, Ender Pearls, EXP Bottles).
    - `Slot 50` (`END_CRYSTAL`): **Explosives** (Minerals row [Copper, Iron, Diamond, Gold, Emerald, Lapis, Netherite, Redstone, Coal], Obsidian, Powered Rails, Glowstone, **TNT Minecarts**).
    - `Slot 51` (`PURPLE_SHULKER_BOX`): **Miscellaneous** (Buckets, Armor Trims [Silence, Vex, Ward], Netherite Upgrade, Honey, Slime, 9 Concrete Colors).
    - `Slot 53` (`BARRIER`): **Back / Save Kit**.
  - **Interactive Item Transfer**: Taking an item transfers it directly to your inventory and removes the item from the GUI slot; clicking Refill restocks all slots.
- **PlaceholderAPI (PAPI) Expansion**:
  - Full native support for Killstreak, Kills, Deaths, KDR, Playtime, Tier, and Currency placeholders.
- **Dynamic Practice Sidebar Scoreboard**:
  - Real-time display for Player Name, Team, K/D, Xu (🪙), Tiền (₫), Killstreak, Tier (LT5/HT5), Server IP, and Online Player Count.
- **Combat & Killstreak System**:
  - Broadcast milestones (5, 10, 15, 20, 25, 30, 50, 100 kills).
  - Custom PvP death messages showing killer's remaining hearts `[3❤]`.
  - Anti Combat-Log penalty detection.
- **Persistent Leaderboard & Player Stats**:
  - SQLite database tracking Top 10 Kills (`/topkills`), Top 10 Deaths (`/topdeaths`), and Top 10 Playtime (`/toptime`), including personal ranking display (`#-1`).
- **Zero-Bug SpotBugs Static Analysis**: 100% verified with 0 bugs, 0 errors, 0 memory leaks.

---

## 📊 PlaceholderAPI (PAPI) Placeholders

| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%hyperffa_killstreak%` / `%hyperffa_streak%` | Player's current killstreak (Chuỗi hạ gục hiện tại của người chơi) | `5` |
| `%hyperffa_best_killstreak%` / `%hyperffa_best_streak%` | Server's all-time highest killstreak (Kỷ lục killstreak của người giỏi nhất server) | `18` |
| `%hyperffa_best_killstreak_player%` | Name of the player holding the server best killstreak (Tên người giữ kỷ lục server) | `Maz52` |

---

## 📋 Commands & Permissions

### Player Commands
| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/kit` | `/kits`, `/kitroom` | Open Kit Room menu | `hyperkit.use` |
| `/kit1` .. `/kit6` | | Direct shortcut to load slots 1 through 6 | `hyperkit.use` |
| `/killstreak` | `/ks`, `/chuoi` | View current and best killstreak | `hyperkit.use` |
| `/stats` | `/profile`, `/thongke` | View personal or target player stats | `hyperkit.use` |
| `/topkills` | `/topkill`, `/topk` | View Top 10 Kills leaderboard | `hyperkit.use` |
| `/topdeaths` | `/topdeath`, `/topd` | View Top 10 Deaths leaderboard | `hyperkit.use` |
| `/toptime` | `/topplaytime`, `/topt` | View Top 10 Playtime leaderboard | `hyperkit.use` |
| `/discord` | | Get Discord server invite link | `hyperkit.use` |
| `/rtpq` | `/queue` | Join the FFA match queue | `hyperkit.use` |

### Slot Permissions
- `hyperkit.slot.1`, `hyperkit.slot.2`, `hyperkit.slot.3` (Default slots 1-3)
- `hyperkit.slot.4`, `hyperkit.slot.5`, `hyperkit.slot.6` (Premium slots 4-6)
- `hyperkit.vip`, `hyperkit.premium`, `hyperkit.admin` (Access to all slots)

### Administrator Commands
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/kitadmin mode create <mode>` | Create a new PvP mode | `hyperkit.admin` |
| `/kitadmin mode delete <mode>` | Delete an existing PvP mode | `hyperkit.admin` |
| `/kitadmin setpremade <mode> <name>` | Save current admin inventory as a Premade Kit | `hyperkit.admin` |
| `/kitadmin setcategory <mode> <category>` | Save admin inventory into an Item Room category | `hyperkit.admin` |
| `/kitadmin give <player> <mode> <slot>` | Load a kit for a target player | `hyperkit.admin` |
| `/kitadmin reload` | Reload configurations, messages, and modes | `hyperkit.admin` |

---

## 🏗️ Build

```bash
mvn clean package spotbugs:check
```
Output artifact: `target/HyperFFA.jar`.
