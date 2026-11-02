package maihatsuu.archive.tome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maihatsuu.archive.Archive;
import maihatsuu.archive.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds tome pools to structure loot tables, from the bundled {@code /archive/structure_tomes.json}:
 * <pre>
 * { "tables": { "minecraft:chests/stronghold_library": {
 *     "empty_weight": 10,
 *     "tomes": { "minecraft:sharpness": 5, "minecraft:efficiency": 5 } } } }
 * </pre>
 * Each listed table gets one extra pool with one roll; {@code empty_weight} is the weight of "no tome".
 */
public final class StructureTomes {
	private record TableEntry(int emptyWeight, Map<ResourceLocation, Integer> tomes) {
	}

	private static final String PATH = "/archive/structure_tomes.json";

	public static void register() {
		Map<ResourceLocation, TableEntry> tables = load();
		if (tables.isEmpty()) {
			return;
		}
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			TableEntry entry = tables.get(key.location());
			if (entry == null) {
				return;
			}
			var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
			LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
			if (entry.emptyWeight() > 0) {
				pool.add(EmptyLootItem.emptyItem().setWeight(entry.emptyWeight()));
			}
			entry.tomes().forEach((id, weight) -> {
				Holder<Enchantment> enchant = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
				if (enchant == null) {
					Archive.LOGGER.warn("structure_tomes: unknown enchantment {} for {}", id, key.location());
					return;
				}
				pool.add(LootItem.lootTableItem(ModItems.TOME).setWeight(weight).apply(SetTomeFunction.builder(enchant)));
			});
			builder.withPool(pool);
		});
	}

	private static Map<ResourceLocation, TableEntry> load() {
		Map<ResourceLocation, TableEntry> tables = new LinkedHashMap<>();
		try (InputStream in = StructureTomes.class.getResourceAsStream(PATH)) {
			if (in == null) {
				return tables;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> table : root.getAsJsonObject("tables").entrySet()) {
				JsonObject body = table.getValue().getAsJsonObject();
				Map<ResourceLocation, Integer> tomes = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> tome : body.getAsJsonObject("tomes").entrySet()) {
					tomes.put(ResourceLocation.parse(tome.getKey()), tome.getValue().getAsInt());
				}
				int emptyWeight = body.has("empty_weight") ? body.get("empty_weight").getAsInt() : 0;
				tables.put(ResourceLocation.parse(table.getKey()), new TableEntry(emptyWeight, tomes));
			}
		} catch (Exception e) {
			Archive.LOGGER.error("Failed to read {}", PATH, e);
		}
		return tables;
	}

	private StructureTomes() {
	}
}
