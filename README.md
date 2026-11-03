# Archive

An enchanting and progression overhaul for Fabric 1.21.1. Install it on both the client and the server.

- **Tomes replace enchanted books.** A tome holds one enchant, with no level. Shelve tomes in chiseled bookshelves near an enchanting table to unlock them. Tomes are never used up. Right-click a tome to read it.
- **Library enchanting table.** The table lists every shelved enchant that fits the item and sells it one level at a time for a fixed XP + lapis cost, with no randomness. Conflicting enchants are blocked; use the grindstone first.
- **Bookshelves unlock low levels.** Without a tome, plain bookshelves around the table unlock common enchants up to a per-enchant limit: level I with the table alone, II with 5 bookshelves, III with 10. Higher levels need a tome. Chiseled bookshelves don't count.
- **Shelf particles.** Enchant particles float to the table from every bookshelf and chiseled bookshelf within range (a 9×9×9 cube by default), so you can see which shelves count.
- **Removed enchants.** Mending, Unbreaking, and Fire/Blast/Projectile Protection can no longer be obtained, but they keep working on existing items.
- **Changed enchants.**
  - Protection maxes at III.
  - Thorns is chestplate-only.
  - Depth Strider moves to leggings.
  - New **Resilience** (chestplate, max III): reduces fire, explosion and projectile damage, burn time and knockback. It stacks with Protection.
- **No vanilla enchanted books** from loot chests, fishing, bartering, hero-of-the-village gifts or villager trades.
- **Flat XP curve.** Every level costs 50 XP, levels cap at 100, a Bottle o' Enchanting gives one level, and you drop 50% of your XP on death.
- **Gear.**
  - Netherite gear is unbreakable.
  - The trident repairs with prismarine shards and the mace with breeze rods (vanilla), one unit for a full repair.
  - Diamond mining speed is 12 and netherite is 19.
  - Deepslate blocks have hardness 2.0, and deepslate ores 3.5.
  - Obsidian, crying obsidian, respawn anchors and ender chests have hardness 20.0.
- **Anvil** only repairs and renames, always for free, and never chips (chipped and damaged anvils are hidden from creative). One unit of the base material (e.g. 1 iron ingot) fully repairs an item.
- **Grindstone** only disenchants a single item and refunds part of the table cost.

## Config

`config/archive.json` is created on first launch.

| Key | Default | |
|---|---|---|
| `xpCurve`, `durability`, `mining`, `anvil`, `grindstone`, `libraryTable`, `stripBookLoot` | `true` | Toggle each subsystem |
| `xpPerLevel` / `levelCap` / `deathXpFraction` | `50` / `100` / `0.5` | XP curve |
| `tableRadius` | `4` | How far the table looks for bookshelves: a cube this many blocks out, so 4 = 9×9×9 |
| `shelfSteps` | `[0, 5, 10]` | Plain bookshelves needed for each level unlocked without a tome |
| `grindstoneRefundFraction` | `0.5` | Share of the table cost refunded by the grindstone |
| `diamondSpeed` / `netheriteSpeed` | `12` / `19` | Tool mining speed (restart needed) |
| `deepslateHardness` / `deepslateOreHardness` / `obsidianHardness` | `2.0` / `3.5` / `20.0` | Block hardness |
| `durabilityOverrides` | `{}` | Item id → max durability, e.g. `{"minecraft:iron_pickaxe": 500}` (restart needed) |

Client and server should use the same XP, mining, hardness and `tableRadius` values, or the client will predict them wrong (the client draws the bookshelf particles from its own `tableRadius`).

The enchantment and tag changes are data, not code. They're always on, and a datapack can override them.

## Data formats

**Enchant costs:** `data/<namespace>/enchant_costs/<any>.json`. Both fields are optional. Enchants without `levels`, and levels past the end of the list, use `5 × level` XP levels and `level` lapis. `shelf_max` is the highest level plain bookshelves can unlock without a tome (missing or `0`: tome only, capped by the number of `shelfSteps`). Archive ships `shelf_max` files in `data/archive/enchant_costs/`. Several files may name the same enchant: they're applied in file id order, and each field a later file sets replaces the earlier value, so a pack can add costs without losing Archive's `shelf_max`.
```json
{ "enchantment": "minecraft:sharpness",
  "shelf_max": 2,
  "levels": [ { "xp": 5, "lapis": 1 }, { "xp": 10, "lapis": 2 } ] }
```

**Structure tomes:** `data/<namespace>/structure_tomes/<any>.json`, from any datapack or mod, reloaded with `/reload`. Each loot table listed in a file gets one extra pool with one roll from that file, so two files naming the same table give two rolls. `empty_weight` is the weight of "no tome". Unknown enchantments are skipped with a warning, so a file can list enchants from mods that may not be installed. To change or remove another pack's entries, override its file at the same path.
```json
{ "tables": { "minecraft:chests/stronghold_library": {
    "empty_weight": 10,
    "tomes": { "minecraft:sharpness": 5, "minecraft:efficiency": 5 } } } }
```

The `archive:set_tome` loot function (`{"function": "archive:set_tome", "enchantment": "minecraft:sharpness"}`) can also be used in your own loot tables. It refuses unobtainable enchants and curses.

**Block tags:** `#archive:deepslate_hardness`, `#archive:deepslate_ore_hardness` and `#archive:obsidian_hardness` choose which blocks get the new hardness values.

## Development

```
./gradlew build          # build the jar into build/libs/
./gradlew runClient      # dev client
./gradlew runGametest    # headless server that runs every game test in src/gametest and exits (non-zero on failure)
```

## License

This project is available under the CC0 license.
