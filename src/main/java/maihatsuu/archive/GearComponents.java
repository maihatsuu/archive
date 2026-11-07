package maihatsuu.archive;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default item components: Archive's durability values, then the config's {@code durabilityOverrides} on top.
 * A durability of 0 or less makes the item unbreakable instead (netherite gear by default).
 */
public final class GearComponents {
	/** Archive's durability table (from the "Durability Values" spreadsheet), by item id, in display order. 0 = unbreakable. */
	public static final Map<String, Integer> DEFAULT_DURABILITY = ordered(
			Map.entry("minecraft:wooden_sword", 32),
			Map.entry("minecraft:wooden_pickaxe", 32),
			Map.entry("minecraft:wooden_axe", 32),
			Map.entry("minecraft:wooden_shovel", 32),
			Map.entry("minecraft:wooden_hoe", 32),
			Map.entry("minecraft:stone_sword", 128),
			Map.entry("minecraft:stone_pickaxe", 128),
			Map.entry("minecraft:stone_axe", 128),
			Map.entry("minecraft:stone_shovel", 128),
			Map.entry("minecraft:stone_hoe", 128),
			Map.entry("minecraft:iron_sword", 512),
			Map.entry("minecraft:iron_pickaxe", 512),
			Map.entry("minecraft:iron_axe", 512),
			Map.entry("minecraft:iron_shovel", 512),
			Map.entry("minecraft:iron_hoe", 512),
			Map.entry("minecraft:golden_sword", 64),
			Map.entry("minecraft:golden_pickaxe", 64),
			Map.entry("minecraft:golden_axe", 64),
			Map.entry("minecraft:golden_shovel", 64),
			Map.entry("minecraft:golden_hoe", 64),
			Map.entry("minecraft:diamond_sword", 2048),
			Map.entry("minecraft:diamond_pickaxe", 2048),
			Map.entry("minecraft:diamond_axe", 2048),
			Map.entry("minecraft:diamond_shovel", 2048),
			Map.entry("minecraft:diamond_hoe", 2048),
			Map.entry("minecraft:netherite_sword", 0),
			Map.entry("minecraft:netherite_pickaxe", 0),
			Map.entry("minecraft:netherite_axe", 0),
			Map.entry("minecraft:netherite_shovel", 0),
			Map.entry("minecraft:netherite_hoe", 0),
			Map.entry("minecraft:leather_helmet", 64),
			Map.entry("minecraft:leather_chestplate", 64),
			Map.entry("minecraft:leather_leggings", 64),
			Map.entry("minecraft:leather_boots", 64),
			Map.entry("minecraft:chainmail_helmet", 128),
			Map.entry("minecraft:chainmail_chestplate", 128),
			Map.entry("minecraft:chainmail_leggings", 128),
			Map.entry("minecraft:chainmail_boots", 128),
			Map.entry("minecraft:iron_helmet", 512),
			Map.entry("minecraft:iron_chestplate", 512),
			Map.entry("minecraft:iron_leggings", 512),
			Map.entry("minecraft:iron_boots", 512),
			Map.entry("minecraft:golden_helmet", 64),
			Map.entry("minecraft:golden_chestplate", 64),
			Map.entry("minecraft:golden_leggings", 64),
			Map.entry("minecraft:golden_boots", 64),
			Map.entry("minecraft:diamond_helmet", 2048),
			Map.entry("minecraft:diamond_chestplate", 2048),
			Map.entry("minecraft:diamond_leggings", 2048),
			Map.entry("minecraft:diamond_boots", 2048),
			Map.entry("minecraft:netherite_helmet", 0),
			Map.entry("minecraft:netherite_chestplate", 0),
			Map.entry("minecraft:netherite_leggings", 0),
			Map.entry("minecraft:netherite_boots", 0),
			Map.entry("minecraft:turtle_helmet", 512),
			Map.entry("minecraft:bow", 512),
			Map.entry("minecraft:crossbow", 512),
			Map.entry("minecraft:trident", 2048),
			Map.entry("minecraft:mace", 2048),
			Map.entry("minecraft:shield", 512),
			Map.entry("minecraft:fishing_rod", 256),
			Map.entry("minecraft:shears", 256),
			Map.entry("minecraft:flint_and_steel", 256),
			Map.entry("minecraft:elytra", 2048),
			Map.entry("minecraft:brush", 256),
			Map.entry("minecraft:wolf_armor", 64),
			Map.entry("minecraft:carrot_on_a_stick", 256),
			Map.entry("minecraft:warped_fungus_on_a_stick", 256)
	);

	@SafeVarargs
	private static Map<String, Integer> ordered(Map.Entry<String, Integer>... entries) {
		Map<String, Integer> map = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : entries) {
			map.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(map);
	}

	static void register() {
		DefaultItemComponentEvents.MODIFY.register(context -> {
			ArchiveConfig config = ArchiveConfig.get();
			if (!config.durability) {
				return;
			}
			Map<String, Integer> durability = new LinkedHashMap<>(DEFAULT_DURABILITY);
			durability.putAll(config.durabilityOverrides);
			for (Map.Entry<String, Integer> entry : durability.entrySet()) {
				ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
				Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
				if (item == null || !item.components().has(DataComponents.MAX_DAMAGE)) {
					Archive.LOGGER.warn("durabilityOverrides: '{}' is not a damageable item, skipping", entry.getKey());
					continue;
				}
				int value = entry.getValue();
				// Unbreakable items keep their vanilla max damage; the component just stops it being used.
				context.modify(item, builder -> {
					if (value <= 0) {
						builder.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
					} else {
						builder.set(DataComponents.MAX_DAMAGE, value);
					}
				});
			}
		});
	}

	private GearComponents() {
	}
}
