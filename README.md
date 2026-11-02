# Archive

An enchanting and progression overhaul for Fabric 1.21.1. Install it on both the client and the server.

- **Tomes replace enchanted books.** A tome holds one enchant, with no level. Shelve tomes in chiseled bookshelves near an enchanting table to unlock them. Tomes are never used up. Right-click a tome to read it.
- **Library enchanting table.** The table lists every shelved enchant that fits the item and sells it one level at a time for a fixed XP + lapis cost, with no randomness. Conflicting enchants are blocked; use the grindstone first.
- **Removed enchants.** Mending, Unbreaking, and Fire/Blast/Projectile Protection can no longer be obtained, but they keep working on existing items.
- **Changed enchants.**
  - Protection maxes at III.
  - Thorns is chestplate-only.
  - Depth Strider moves to leggings.
  - New **Resilience** (chestplate, max III): reduces fire, explosion and projectile damage, burn time and knockback. It stacks with Protection.
- **No vanilla enchanted books** from loot chests, fishing, bartering, hero-of-the-village gifts or villager trades.
- **Flat XP curve.** Every level costs 50 XP, levels cap at 100, a Bottle o' Enchanting gives one level, and you drop 50% of your XP on death.
- **Gear.**
  - Netherite gear, the trident and the mace are unbreakable.
  - Diamond mining speed is 12 and netherite is 19.
  - Deepslate blocks have hardness 2.0.
  - Obsidian, crying obsidian, respawn anchors and ender chests have hardness 20.0.
- **Anvil** only repairs (with the base material) and renames, always for free, and never chips.
- **Grindstone** only disenchants a single item and refunds part of the table cost.

## Config

`config/archive.json` is created on first launch.

| Key | Default | |
|---|---|---|
| `xpCurve`, `durability`, `mining`, `anvil`, `grindstone`, `libraryTable`, `stripBookLoot` | `true` | Toggle each subsystem |
| `xpPerLevel` / `levelCap` / `deathXpFraction` | `50` / `100` / `0.5` | XP curve |
| `tableRadius` | `6` | Bookshelf scan radius around the table |
| `grindstoneRefundFraction` | `0.5` | Share of the table cost refunded by the grindstone |
| `diamondSpeed` / `netheriteSpeed` | `12` / `19` | Tool mining speed (restart needed) |
| `deepslateHardness` / `obsidianHardness` | `2.0` / `20.0` | Block hardness |
| `durabilityOverrides` | `{}` | Item id → max durability, e.g. `{"minecraft:iron_pickaxe": 500}` (restart needed) |

Client and server should use the same XP, mining and hardness values, or the client will predict them wrong.

The enchantment and tag changes are data, not code. They're always on, and a datapack can override them.

## Data formats

**Enchant costs:** `data/<namespace>/enchant_costs/<any>.json`. Enchants without a file, and levels past the end of the list, use `5 × level` XP levels and `level` lapis.
```json
{ "enchantment": "minecraft:sharpness",
  "levels": [ { "xp": 5, "lapis": 1 }, { "xp": 10, "lapis": 2 } ] }
```

**Structure tomes:** `src/main/resources/archive/structure_tomes.json` (bundled in the jar, read at startup). Each listed loot table gets one extra pool with one roll. `empty_weight` is the weight of "no tome".
```json
{ "tables": { "minecraft:chests/stronghold_library": {
    "empty_weight": 10,
    "tomes": { "minecraft:sharpness": 5, "minecraft:efficiency": 5 } } } }
```

The `archive:set_tome` loot function (`{"function": "archive:set_tome", "enchantment": "minecraft:sharpness"}`) can also be used in your own loot tables. It refuses unobtainable enchants and curses.

**Block tags:** `#archive:deepslate_hardness` and `#archive:obsidian_hardness` choose which blocks get the new hardness values.

## Development

```
./gradlew build          # build the jar into build/libs/
./gradlew runClient      # dev client
./gradlew runGametest    # headless server that runs every game test in src/gametest and exits (non-zero on failure)
```

## License

This project is available under the CC0 license.
