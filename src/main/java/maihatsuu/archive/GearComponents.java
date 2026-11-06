package maihatsuu.archive;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default item components: Archive's durability values, then the config's {@code durabilityOverrides} on top,
 * and unbreakable netherite gear.
 */
public final class GearComponents {
	/** Archive's durability table (from the "Durability Values" spreadsheet), by item id. */
	static final Map<String, Integer> DEFAULT_DURABILITY = Map.ofEntries(
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

	private static final List<Item> UNBREAKABLE = List.of(
			Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
			Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

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
				context.modify(item, builder -> builder.set(DataComponents.MAX_DAMAGE, entry.getValue()));
			}
			context.modify(UNBREAKABLE, (builder, item) -> builder.set(DataComponents.UNBREAKABLE, new Unbreakable(true)));
		});
	}

	private GearComponents() {
	}
}
