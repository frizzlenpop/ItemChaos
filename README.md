# RandomItem - Pure Random Drops

A Minecraft Paper plugin designed for YouTube content creation. Every block broken and every mob killed drops a completely random item. Earn coins, buy upgrades, sabotage your friends, fight bosses, and compete on the leaderboard.

## Installation

1. Build the plugin with `./gradlew build`
2. The JAR is automatically copied to `release/RandomItem.jar` (also available at `build/libs/`)
3. Copy `release/RandomItem.jar` to your server's `plugins/` folder
4. Start the server - all config files generate automatically on first run
5. Edit any `.yml` file in `plugins/RandomItem/` and restart to apply changes

## Core Mechanic

Every block you break and every mob you kill drops a **completely random item** from the game. This includes blocks destroyed by **explosions** (TNT, creepers, etc.) - each exploded block drops a random item too.

**Spawn Egg Surprise:** When a spawn egg is rolled as a random drop, there's a **30% chance** the mob will spawn alive at the drop location instead of dropping the egg. Ender Dragons and Withers are excluded from this and always drop as eggs.

Each drop earns you **coins** based on the item's rarity tier:

| Tier | Coin Value | Example Items |
|------|-----------|---------------|
| Legendary | 50 | Netherite, Elytra, Beacon, Nether Star, Trident |
| Rare | 20 | Diamond, Emerald, Shulker, Enchanted Book, Music Disc |
| Uncommon | 8 | Iron, Gold, Lapis, Redstone, Copper, Quartz |
| Common | 3 | Stone, Wood, Coal, Leather, Wool, Sand |
| Junk | 1 | Everything else |

---

## Commands Reference

### Player Commands (Everyone)

| Command | Description |
|---------|-------------|
| `/chaos coins` | Check your coin balance |
| `/upgrade` | Open the upgrade shop GUI |
| `/shop` | Open the item shop GUI |
| `/tradeup` | Open the trade-up GUI |
| `/sabotage` | Open the sabotage target picker |
| `/gamble <amount>` | Gamble your coins |
| `/bounty set <player> <amount>` | Place a bounty on a player |
| `/bounty list` | View all active bounties |
| `/winners` | Show final coin standings |
| `/blockadex` | View your item collection progress and milestones |
| `/blockadex top` | See the top 5 item collectors |

### Admin Commands (OP Only)

| Command | Description |
|---------|-------------|
| `/chaos toggle` | Enable/disable the random drop mechanic |
| `/coins give <player> <amount>` | Give coins to a player |
| `/coins remove <player> <amount>` | Remove coins from a player |
| `/coins set <player> <amount>` | Set a player's exact coin balance |
| `/teams create <name>` | Create a team (max 4) |
| `/teams add <team> <player>` | Add a player to a team |
| `/teams remove <player>` | Remove a player from their team |
| `/teams list` | Show all teams and members |
| `/teams disband <team>` | Delete a team |
| `/boss spawn <name>` | Spawn a boss at your location |
| `/boss setanchor <team>` | Set a team's boss spawn anchor at your location |
| `/boss list` | List all configured bosses |
| `/pinata spawn <type>` | Spawn a loot pinata at your location |
| `/pinata setlocation <name>` | Save a named spawn point |
| `/pinata list` | List all configured pinatas |
| `/crate spawn` | Force-spawn a mystery crate |
| `/crate toggle` | Enable/disable auto-spawning crates |
| `/hotzone start` | Start a hot zone event |
| `/hotzone stop` | Stop the current hot zone |
| `/hotzone setcenter` | Set the fixed hot zone center at your location |
| `/event trigger <name>` | Manually trigger a random event |
| `/event toggle` | Enable/disable auto random events |
| `/event list` | List all configured events |
| `/bounty toggle` | Enable/disable the bounty system |
| `/gamble toggle` | Enable/disable gambling |
| `/deathpenalty toggle` | Enable/disable death coin scatter |
| `/leaderboard toggle` | Show/hide the sidebar scoreboard |
| `/leaderboard reset` | Refresh the leaderboard display |
| `/blockadex toggle` | Enable/disable the Blockadex system |

---

## Features & Configuration

All config files are located in `plugins/RandomItem/` and are generated on first run with sensible defaults. Edit them and restart the server to apply changes.

---

### Upgrades (`/upgrade`)

Players spend coins on permanent upgrades via a chest GUI. 5 upgrades, each with 3 levels:

| Upgrade | Effect | L1 Cost | L2 Cost | L3 Cost |
|---------|--------|---------|---------|---------|
| Double Drop | 10%/20%/30% chance for 2 items per break/kill | 100 | 300 | 600 |
| Lucky Drops | 5%/10%/15% chance to drop a rare item | 150 | 400 | 800 |
| Coin Multiplier | 1.5x/2x/3x coins from drops | 200 | 500 | 1000 |
| Haste | Permanent Haste I/II/III while online | 100 | 250 | 500 |
| Health Boost | Permanent +2/+4/+6 extra hearts | 150 | 350 | 700 |

Haste and Health Boost reapply automatically on login and respawn.

---

### Item Shop - `shop.yml`

A paginated GUI where players buy specific items with coins.

```yaml
items:
  - material: DIAMOND_SWORD    # Bukkit Material name (required)
    name: "&bDiamond Sword"    # Display name with & color codes (optional)
    amount: 1                  # Stack size given on purchase (required)
    cost: 500                  # Coin cost (required)
```

**Fields:**
- `material` - Any valid [Bukkit Material name](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)
- `name` - Display name shown in the GUI. Supports `&` color codes (e.g., `&b` = aqua, `&6` = gold, `&l` = bold)
- `amount` - How many of the item the player receives per purchase
- `cost` - How many coins it costs

Add as many items as you want. The GUI automatically paginates (45 items per page with next/prev arrows).

---

### Boss Events - `bosses.yml`

Custom boss mobs with configurable stats, loot, and optional add spawns. Coin rewards are split proportionally among all players who dealt damage.

```yaml
bosses:
  wardens_wrath:                    # Unique ID (used in /boss spawn wardens_wrath)
    name: "&4&lThe Warden's Wrath"  # Display name (& color codes supported)
    entity-type: WARDEN             # Bukkit EntityType
    health: 500                     # Max HP
    speed: 0.15                     # Movement speed (default mob speed is ~0.25)
    damage: 20                      # Attack damage (1 damage = 0.5 hearts)
    coin-reward: 2000               # Total coins distributed on kill
    proximity-radius: 50            # Used for team anchor proximity
    loot:                           # Items dropped on death
      - material: NETHERITE_INGOT
        amount: 2
    # Optional: spawn additional mobs periodically during the fight
    spawn-adds: true                # Enable add spawning (default: false)
    add-entity-type: SPIDER         # What mob type to spawn as adds
    add-count: 5                    # How many adds per wave
    add-interval-seconds: 15        # Seconds between add waves
```

**Fields:**
- `entity-type` - Any Bukkit EntityType that is a living entity (WARDEN, BLAZE, ZOMBIE, ENDERMAN, CAVE_SPIDER, etc.)
- `health` - The boss's max HP. For reference, a normal Zombie has 20 HP
- `speed` - Movement speed. Normal walk speed is ~0.25, sprinting is ~0.4
- `damage` - Attack damage in half-hearts. 20 damage = 10 hearts
- `coin-reward` - Split proportionally among all damage dealers when the boss dies
- `proximity-radius` - Distance in blocks for team anchor spawning
- `spawn-adds` - Set to `true` to have the boss spawn helper mobs during the fight
- `add-entity-type` / `add-count` / `add-interval-seconds` - Controls the add waves

**Team Anchors:** Use `/boss setanchor <team>` while standing at the desired location. The anchor is saved to the `anchors:` section of this file automatically.

**Default Bosses:**
1. **The Warden's Wrath** - 500 HP, slow but hits for 10 hearts, drops netherite
2. **Blaze King** - 300 HP, fast, drops blaze rods and diamonds
3. **Spider Queen** - 200 HP, spawns spider adds every 15s
4. **Zombie Horde Leader** - 400 HP, spawns 8 zombies every 10s
5. **Ender Guardian** - 400 HP, fast, drops elytra and ender pearls

---

### Loot Pinatas - `pinatas.yml`

Fun destructible mobs that explode into loot and scattered coins when killed.

```yaml
pinatas:
  gold_rush:                        # Unique ID (used in /pinata spawn gold_rush)
    name: "&6&lGold Rush"           # Display name
    entity-type: IRON_GOLEM         # What mob to use as the pinata
    health: 150                     # How much HP
    coin-scatter: 500               # Total coins scattered as gold nuggets on death
    loot:                           # Items dropped with chance %
      - material: GOLD_INGOT
        amount: 16
        chance: 40                  # 40% chance this item drops
```

**Fields:**
- `entity-type` - The mob used as the pinata body
- `health` - How tough the pinata is
- `coin-scatter` - Coins scattered as physical gold nuggets anyone can pick up
- `loot[].chance` - Percentage chance (0-100) that each loot entry drops

**Named Spawn Points:** Use `/pinata setlocation <name>` to save locations. They're stored in the `locations:` section of the file.

**Default Pinatas:**
1. **Gold Rush** (Iron Golem) - Drops gold, diamonds, emeralds
2. **Food Feast** (Pig) - Drops cooked food and golden apples
3. **War Chest** (Zombie) - Drops weapons and armor
4. **Ender Haul** (Enderman) - Drops ender pearls and elytra

---

### Bounty System - `bounties.yml`

Players place coin bounties on each other. Kill a bountied player to collect.

```yaml
enabled: true        # Toggle the entire system on/off
min-bounty: 50       # Minimum coins for a bounty
max-bounty: 10000    # Maximum coins for a single bounty placement
```

**How it works:**
- `/bounty set <player> <amount>` costs the placer coins and adds to the target's bounty
- Bounties **stack** - multiple people can add bounties to the same player
- When a bountied player is killed, the **killer collects the full bounty**
- All bounty placements and collections are broadcast server-wide
- Bounties persist across server restarts (saved to `bounties.json`)

---

### Mystery Crates - `crates.yml`

Auto-spawning loot crates that appear at random locations.

```yaml
enabled: true                  # Toggle auto-spawning
spawn-interval-minutes: 10     # Time between automatic spawns

bounds:                        # Area where crates can appear
  world: "world"               # World name
  min-x: -500
  max-x: 500
  min-y: 60                   # Minimum Y level (height)
  max-y: 120                  # Maximum Y level
  min-z: -500
  max-z: 500

loot:                          # What the crate can contain
  - material: DIAMOND
    amount: 5
    chance: 20                 # % chance this item is included
```

**How it works:**
- Every X minutes, an **Ender Chest** appears at a random location within bounds
- **Multiple crates can exist** at the same time - each new spawn adds another, it doesn't replace the old one
- The exact **coordinates are broadcast** to all players
- A **beam of particles** shoots upward from each active crate location
- First player to **right-click** a crate claims its loot (each crate is independent)
- Each loot entry rolls independently (you can get multiple items)

**Fields:**
- `bounds` - The rectangular area where crates can spawn. Adjust `min-x`/`max-x`/`min-z`/`max-z` to control how far away crates appear. Uses `getHighestBlockYAt()` so they always appear on the surface
- `loot[].chance` - Each entry rolls independently (not weighted selection)

---

### Gambling - `gambling.yml`

Risk your coins for big rewards or devastating losses.

```yaml
enabled: true              # Toggle gambling
min-bet: 10                # Minimum wager
max-bet: 5000              # Maximum wager
cooldown-seconds: 10       # Cooldown between gambles per player

outcomes:                  # Possible results with weighted chances
  - type: WIN_2X           # Internal type identifier
    display: "&a2x Win!"   # Message shown to player (& colors supported)
    weight: 30             # Relative weight (higher = more likely)
  - type: WIN_3X
    display: "&6&l3x JACKPOT!"
    weight: 10
  - type: LOSE
    display: "&cYou lost it all!"
    weight: 35
  - type: RANDOM_ITEM
    display: "&dRandom Rare Item!"
    weight: 15
  - type: SELF_SABOTAGE
    display: "&4Self-Sabotage!"
    weight: 10
```

**Outcome Types:**
- `WIN_2X` - Player receives 2x their bet back
- `WIN_3X` - Player receives 3x their bet back (broadcast to server as a jackpot)
- `LOSE` - Player loses their entire bet
- `RANDOM_ITEM` - Player receives a random rare item (diamond, netherite, elytra, etc.)
- `SELF_SABOTAGE` - A random sabotage effect is applied to the player

**Weight System:** Weights are relative, not percentages. With the default weights (total = 100), WIN_2X has a 30% chance. If you changed WIN_2X weight to 60, the total would be 130 and WIN_2X would have a 46% chance.

---

### Hot Zone (King of the Hill) - `hotzone.yml`

A timed area that rewards coins to anyone standing inside it.

```yaml
enabled: true
zone-duration-seconds: 120       # How long the zone lasts
coin-rate-per-second: 5          # Coins earned per second while in the zone
zone-radius: 10                  # Radius in blocks from center
broadcast-interval-seconds: 15   # How often to announce who's in the zone

use-fixed-center: false          # true = always use the center below, false = random
center:                          # Fixed center (set via /hotzone setcenter)
  world: "world"
  x: 0
  y: 64
  z: 0

random-bounds:                   # Area for random center placement
  world: "world"
  min-x: -200
  max-x: 200
  min-z: -200
  max-z: 200
```

**How it works:**
- Admin runs `/hotzone start` to activate
- A zone appears with a **flame particle border**
- Players inside earn coins every second
- Every X seconds, the server broadcasts who is currently in the zone
- After the duration expires, the zone ends
- Use `/hotzone setcenter` to lock the zone to a specific location (sets `use-fixed-center: true` in the config)

---

### Random Events - `events.yml`

Server-wide events that trigger automatically at random intervals.

```yaml
enabled: true
min-interval-seconds: 120     # Minimum time between events
max-interval-seconds: 300     # Maximum time between events

events:
  - name: "Double Coins"      # Display name (used in /event trigger "Double Coins")
    description: "All coin earnings doubled!"   # Subtitle shown on screen
    duration-seconds: 60       # How long the event lasts
    weight: 25                 # Relative chance of being selected
    type: DOUBLE_COINS         # Internal event type (see list below)
```

**Event Types:**
- `DOUBLE_COINS` - All coin earnings from drops are doubled for the duration
- `GRAVITY_MADNESS` - All players get Jump Boost IV and Slow Falling
- `LEGENDARY_MINUTE` - Every single drop is from the rare items pool (diamonds, netherite, elytra, etc.)
- `FREEZE_TAG` - All players get Slowness III
- `COIN_RAIN` - Random players receive bonus coins every 5 seconds

**Notes:**
- Events trigger automatically between `min-interval-seconds` and `max-interval-seconds`
- When an event starts, all players see a **title screen announcement** with the event name
- Use `/event trigger <name>` to manually trigger any event (name must match exactly, case-insensitive)
- Weight system works the same as gambling outcomes

---

### Trade-Up System - `tradeup.yml`

Put in 5 items, get back 1 item from the next rarity tier.

```yaml
enabled: true
input-count: 5           # Number of items required

tiers:                   # Tier definitions based on coin values from ItemValueRegistry
  junk:
    max-value: 1         # Items worth 1 coin or less
    display: "&7Junk"    # Display name with color
  common:
    max-value: 3         # Items worth 2-3 coins
    display: "&fCommon"
  uncommon:
    max-value: 8         # Items worth 4-8 coins
    display: "&aUncommon"
  rare:
    max-value: 20        # Items worth 9-20 coins
    display: "&9Rare"
  legendary:
    max-value: 999       # Items worth 21+ coins
    display: "&6Legendary"
```

**How it works:**
1. Run `/tradeup` to open the GUI
2. Place 5 items into the input slots
3. Click the green **Confirm** button
4. The system averages the coin value of your 5 items to determine their tier
5. You receive 1 random item from the **next tier up**
6. If your items are already Legendary tier, you can't trade up further
7. Closing the GUI returns all items to your inventory

**Tier Boundaries:** Each tier's `max-value` is the ceiling. An item worth 8 coins falls in the Uncommon tier (max-value: 8). Trading up 5 Uncommon items gives you 1 Rare item.

---

### Death Penalty - `deathpenalty.yml`

When players die, a percentage of their coins scatter on the ground.

```yaml
enabled: true
drop-percentage: 20      # % of coins dropped on death
scatter-radius: 3        # How far coins scatter from death point (blocks)
nugget-count: 5          # Number of gold nugget items spawned
```

**How it works:**
- When a player dies, `drop-percentage`% of their coins are removed
- The coins appear as **gold nuggets** scattered around the death location
- **Any player** can pick up these nuggets to add coins to their balance
- The nuggets display their coin value in their name
- Creates frantic scrambles at death locations

---

### Leaderboard - `leaderboard.yml`

Live sidebar scoreboard showing top coin earners.

```yaml
enabled: true
max-entries: 10                # How many players to show
update-interval-ticks: 100     # Refresh rate (100 ticks = 5 seconds)
show-team-totals: true         # Show team coin totals below player list
title: "&6&lCoin Leaderboard"  # Scoreboard title (& colors supported)
```

**How it works:**
- Displays as a **sidebar scoreboard** for all online players
- Shows the top `max-entries` players sorted by coin balance
- If `show-team-totals` is true, team totals are shown below
- Updates every `update-interval-ticks` (20 ticks = 1 second)
- Use `/winners` for a formatted chat message of final standings (great for video outros)

---

### Blockadex - `blockadex.yml`

A collection tracker inspired by the Pokedex. Every unique item you receive from random drops is recorded. Reach percentage milestones to unlock permanent enhancements.

```yaml
enabled: true

milestones:
  10:                              # Percentage threshold
    name: "Novice Collector"       # Milestone title (shown in broadcasts)
    effect: SPEED                  # PotionEffectType name
    amplifier: 0                   # 0 = level I, 1 = level II, etc.
    description: "Permanent Speed I"  # Shown in /blockadex and broadcasts
  25:
    name: "Keen Collector"
    effect: LUCK
    amplifier: 0
    description: "Permanent Luck I"
  50:
    name: "Expert Collector"
    effect: STRENGTH
    amplifier: 0
    description: "Permanent Strength I"
  75:
    name: "Master Collector"
    effect: RESISTANCE
    amplifier: 0
    description: "Permanent Resistance I"
  100:
    name: "Blockadex Complete"
    effect: REGENERATION
    amplifier: 0
    description: "Permanent Regeneration I"
```

**How it works:**
- Every unique item material you receive from random drops is tracked (block breaks, entity kills, double drops)
- Your completion percentage is calculated against all obtainable items in the game
- When you cross a milestone threshold, you get:
  - A **title screen announcement**
  - A **server-wide broadcast**
  - A **permanent potion effect** that persists through death and relogin
- Run `/blockadex` to see your progress, which milestones you've reached, and how many items until the next one
- Run `/blockadex top` to see the top 5 collectors

**Configuring Milestones:**
- Change the percentage keys (10, 25, 50, 75, 100) to any values you want
- `effect` - Any valid PotionEffectType: `SPEED`, `STRENGTH`, `RESISTANCE`, `REGENERATION`, `LUCK`, `JUMP_BOOST`, `FIRE_RESISTANCE`, `NIGHT_VISION`, etc.
- `amplifier` - 0 = level I, 1 = level II, 2 = level III
- Add or remove milestones as needed
- Collection data persists in `blockadex.json`

**Default Milestones:**
| % | Name | Reward |
|---|------|--------|
| 10% | Novice Collector | Permanent Speed I |
| 25% | Keen Collector | Permanent Luck I |
| 50% | Expert Collector | Permanent Strength I |
| 75% | Master Collector | Permanent Resistance I |
| 100% | Blockadex Complete | Permanent Regeneration I |

---

### Sabotage System (`/sabotage`)

Players spend coins to apply disruptive effects on other players or entire teams.

| Sabotage | Cost | Duration | Effect |
|----------|------|----------|--------|
| Inventory Shuffle | 300 | Instant | Randomly rearranges the target's inventory |
| Butter Fingers | 500 | 45s | Target randomly drops their held item every 3 seconds |
| Drunk Vision | 250 | 60s | Nausea + Slowness applied to target |
| Gravity Flip | 400 | 30s | Target launched skyward every 5 seconds |
| TNT Rain | 800 | 30s | TNT spawns above target every 3s (no terrain damage) |
| Chicken Swarm | 350 | 45s | 15 chickens spawn around the target |
| Phantom Menace | 600 | 60s | 4 phantoms spawn and attack the target |
| Fake Death | 200 | Instant | Fake death message, blindness + darkness for 3s |

**Team Targeting:** When sabotaging an entire team, the cost is multiplied by the number of online team members. All online members receive the effect simultaneously.

---

## Data Persistence

The plugin saves the following JSON files to `plugins/RandomItem/`:

| File | Contents | Auto-save |
|------|----------|-----------|
| `coins.json` | Player coin balances | Every 5 minutes + on shutdown |
| `upgrades.json` | Player upgrade levels | Every 5 minutes + on shutdown |
| `teams.json` | Team rosters | Every 5 minutes + on shutdown |
| `bounties.json` | Active bounties | Every 5 minutes + on shutdown |
| `blockadex.json` | Player item collections | Every 5 minutes + on shutdown |

---

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `chaos.admin` | OP | Access to all admin commands (toggle, teams, boss, pinata, crate, hotzone, event, deathpenalty, leaderboard, coins) |

All player-facing commands (`/shop`, `/upgrade`, `/sabotage`, `/gamble`, `/bounty set/list`, `/tradeup`, `/chaos coins`, `/winners`, `/blockadex`, `/blockadex top`) require no special permissions.

---

## Content Creator Tips

1. **Video Intro:** Use `/chaos toggle` to disable random drops while setting up your intro
2. **Give Starting Coins:** Use `/coins give <player> <amount>` to seed players with coins
3. **Dramatic Boss Fights:** Set team anchors with `/boss setanchor`, then spawn bosses for team-vs-boss content
4. **Pinata Breaks:** Spawn pinatas between rounds for loot scrambles
5. **Hot Zone Battles:** Start a hot zone to force PvP encounters
6. **Gambling Segments:** Have players gamble their earnings for high-risk moments
7. **Bounty Drama:** Place large bounties to create player-hunting segments
8. **Blockadex Race:** Challenge players to reach 50% Blockadex completion - the milestone effects create natural power progression
9. **Video Outro:** Use `/winners` and `/blockadex top` to show final standings
