package maihatsuu.archive.tome;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import maihatsuu.archive.Archive;
import maihatsuu.archive.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adds tome pools to loot tables, from datapack files {@code data/<namespace>/loot_tomes/<any>.json}:
 * <pre>
 * { "tables": { "minecraft:chests/stronghold_library": {
 *     "empty_weight": 10,
 *     "tomes": { "minecraft:sharpness": 5, "minecraft:efficiency": 5 } } } }
 * </pre>
 * Each table listed in each file gets one extra pool with one roll; {@code empty_weight} is the
 * weight of "no tome". Unknown enchantments are skipped, so files can name enchants from optional mods.
 * <p>
 * Loot tables are built before reload listeners run, so the files are read by
 * {@code ReloadableServerRegistriesMixin} at the start of each reload, just before the tables are parsed.
 */
public final class LootTomes {
	private record TableEntry(ResourceLocation file, int emptyWeight, Map<ResourceLocation, Integer> tomes) {
	}

	private static final String DIRECTORY = "loot_tomes";
	private static final Gson GSON = new Gson();
	private static volatile Map<ResourceLocation, List<TableEntry>> tables = Map.of();

	public static void register() {
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			List<TableEntry> entries = tables.get(key.location());
			if (entries == null) {
				return;
			}
			var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
			for (TableEntry entry : entries) {
				LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
				if (entry.emptyWeight() > 0) {
					pool.add(EmptyLootItem.emptyItem().setWeight(entry.emptyWeight()));
				}
				entry.tomes().forEach((id, weight) -> {
					Holder<Enchantment> enchant = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
					if (enchant == null) {
						Archive.LOGGER.warn("loot_tomes {}: skipping unknown enchantment {} for {}", entry.file(), id, key.location());
						return;
					}
					pool.add(LootItem.lootTableItem(ModItems.TOME).setWeight(weight).apply(SetTomeFunction.builder(enchant)));
				});
				builder.withPool(pool);
			}
		});
	}

	/** Reads every {@code loot_tomes} file from the reloading datapacks. */
	public static void load(ResourceManager manager) {
		Map<ResourceLocation, JsonElement> files = new HashMap<>();
		SimpleJsonResourceReloadListener.scanDirectory(manager, DIRECTORY, GSON, files);
		Map<ResourceLocation, List<TableEntry>> parsed = new HashMap<>();
		// Sorted so pool order doesn't depend on hash order.
		new TreeMap<>(files).forEach((file, json) -> {
			try {
				// Parse the whole file first, so a broken file adds nothing.
				Map<ResourceLocation, TableEntry> fromFile = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> table : json.getAsJsonObject().getAsJsonObject("tables").entrySet()) {
					JsonObject body = table.getValue().getAsJsonObject();
					Map<ResourceLocation, Integer> tomes = new LinkedHashMap<>();
					for (Map.Entry<String, JsonElement> tome : body.getAsJsonObject("tomes").entrySet()) {
						tomes.put(ResourceLocation.parse(tome.getKey()), tome.getValue().getAsInt());
					}
					int emptyWeight = body.has("empty_weight") ? body.get("empty_weight").getAsInt() : 0;
					fromFile.put(ResourceLocation.parse(table.getKey()), new TableEntry(file, emptyWeight, Collections.unmodifiableMap(tomes)));
				}
				fromFile.forEach((table, entry) -> parsed.computeIfAbsent(table, k -> new ArrayList<>()).add(entry));
			} catch (Exception e) {
				Archive.LOGGER.error("Invalid loot_tomes file {}", file, e);
			}
		});
		parsed.replaceAll((table, entries) -> List.copyOf(entries));
		tables = Map.copyOf(parsed);
		Archive.LOGGER.info("Loaded loot tomes for {} loot tables", parsed.size());
	}

	private LootTomes() {
	}
}
