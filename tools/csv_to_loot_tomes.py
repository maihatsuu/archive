#!/usr/bin/env python3
"""Turn the tome loot spreadsheets (CSV exports) into data/archive/loot_tomes/ files.

Both sheets: first row is "Enchantment" then one column per pool. Then an "empty_weight" row and one row per
enchant id, with weights in the cells (blank or 0 = not in that pool). "Sum"/"Chance" rows are ignored.

  python3 tools/csv_to_loot_tomes.py <sheet.csv>
      Loot table sheet: each column header is a loot table id (e.g. chests/stronghold_library).
      Writes data/archive/loot_tomes/<table path>.json.

  python3 tools/csv_to_loot_tomes.py --fishing-biomes <sheet.csv>
      Fishing biome sheet: each column header is a comma-separated list of biomes (ids or #tags).
      Each column becomes a pool on fishing treasure that only rolls in those biomes.
      Writes data/archive/loot_tomes/fishing_biomes/<first biome>.json.

Each mode replaces only its own files.
"""
import csv
import json
import shutil
import sys
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/archive/loot_tomes"
# Copies for config resilience: false (pack.mcmeta overlay), with Resilience swapped for the three protections.
OVERLAY = Path(__file__).resolve().parent.parent / "src/main/resources/no_resilience/data/archive/loot_tomes"
PROTECTIONS = ["minecraft:fire_protection", "minecraft:blast_protection", "minecraft:projectile_protection"]
# Hand-written files the spreadsheet doesn't cover.
KEEP = {"shelves"}
BIOME_DIR = "fishing_biomes"
FISHING_TREASURE = "minecraft:gameplay/fishing/treasure"
IGNORED_ROWS = {"sum", "chance", ""}
TYPOS = {"chests/abandonded_mineshaft": "chests/abandoned_mineshaft", "lukewarn_ocean": "lukewarm_ocean"}


def full_id(name: str) -> str:
    name = TYPOS.get(name.strip(), name.strip())
    if name.startswith("#"):
        return name if ":" in name else "#minecraft:" + name[1:]
    return name if ":" in name else "minecraft:" + name


def weight(cell: str) -> int:
    cell = cell.strip()
    return int(float(cell)) if cell else 0


def read_columns(path: str):
    """Yields (header, empty_weight, {enchant: weight}) per column."""
    with open(path, newline="", encoding="utf-8") as f:
        rows = list(csv.reader(f))
    headers = rows[0][1:]
    empty = [0] * len(headers)
    tomes = [{} for _ in headers]
    for row in rows[1:]:
        name = row[0].strip()
        if name.lower() in IGNORED_ROWS:
            continue
        for i, cell in enumerate(row[1:len(headers) + 1]):
            w = weight(cell)
            if name == "empty_weight":
                empty[i] = w
            elif w > 0:
                tomes[i][full_id(name)] = w
    return list(zip(headers, empty, tomes))


def pool(empty: int, tomes: dict, conditions=None) -> dict:
    body = {}
    if conditions:
        body["conditions"] = conditions
    if empty > 0:
        body["empty_weight"] = empty
    body["tomes"] = tomes
    return body


def write(out: Path, table: str, body: dict, label: str) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"tables": {table: body}}, indent="\t") + "\n", encoding="utf-8")
    if "archive:resilience" in body["tomes"]:
        swapped = {}
        for tome, w in body["tomes"].items():
            swapped.update({p: w for p in PROTECTIONS} if tome == "archive:resilience" else {tome: w})
        overlay = OVERLAY / out.relative_to(OUT)
        overlay.parent.mkdir(parents=True, exist_ok=True)
        overlay.write_text(json.dumps({"tables": {table: {**body, "tomes": swapped}}}, indent="\t") + "\n", encoding="utf-8")
    total = body.get("empty_weight", 0) + sum(body["tomes"].values())
    print(f"{label}: {len(body['tomes'])} tomes, {100 * (1 - body.get('empty_weight', 0) / total):.1f}% chance")


def loot_tables(path: str) -> None:
    for root in (OUT, OVERLAY):
        for child in root.glob("*") if root.exists() else []:
            if child.name != BIOME_DIR and child.name not in KEEP:
                shutil.rmtree(child) if child.is_dir() else child.unlink()
    for header, empty, tomes in read_columns(path):
        table = full_id(header)
        if not tomes:
            print(f"skipping {table}: no tomes")
            continue
        namespace, table_path = table.split(":", 1)
        out = OUT / (table_path + ".json") if namespace == "minecraft" else OUT / namespace / (table_path + ".json")
        write(out, table, pool(empty, tomes), table)


def fishing_biomes(path: str) -> None:
    shutil.rmtree(OUT / BIOME_DIR, ignore_errors=True)
    shutil.rmtree(OVERLAY / BIOME_DIR, ignore_errors=True)
    for header, empty, tomes in read_columns(path):
        biomes = [full_id(b) for b in header.split(",") if b.strip()]
        if not tomes:
            print(f"skipping {biomes}: no tomes")
            continue
        # A single #tag goes in as the tag; otherwise a list of biome ids.
        predicate = biomes[0] if len(biomes) == 1 and biomes[0].startswith("#") else [b for b in biomes if not b.startswith("#")]
        if any(b.startswith("#") for b in biomes) and len(biomes) > 1:
            sys.exit(f"mix of tags and biomes in one column isn't supported: {header}")
        conditions = [{"condition": "minecraft:location_check", "predicate": {"biomes": predicate}}]
        name = biomes[0].lstrip("#").split(":", 1)[1].replace("/", "_")
        write(OUT / BIOME_DIR / (name + ".json"), FISHING_TREASURE, pool(empty, tomes, conditions), f"fishing in {name} (+{len(biomes) - 1})")


if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "--fishing-biomes":
        fishing_biomes(sys.argv[2])
    elif len(sys.argv) == 2:
        loot_tables(sys.argv[1])
    else:
        sys.exit(__doc__)
