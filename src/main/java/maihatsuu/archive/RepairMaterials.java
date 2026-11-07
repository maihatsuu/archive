package maihatsuu.archive;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The anvil repair material of every item in Archive's durability table, as an item id or a {@code #tag}: vanilla's
 * materials plus Archive's additions for gear vanilla gives none (vanilla only lets those combine with a copy of
 * themselves, which Archive turns off). The config's {@code repairOverrides} go on top, and an empty string means
 * no repair material. With durability off, or for items not listed (modded gear), vanilla decides.
 */
public final class RepairMaterials {
	/** Material by item id prefix: diamond_sword and diamond_helmet both repair with diamonds. */
	private static final Map<String, String> BY_MATERIAL = Map.of(
			"leather", "minecraft:leather",
			"wooden", "#minecraft:planks",
			"stone", "#minecraft:stone_tool_materials",
			"chainmail", "minecraft:iron_ingot",
			"iron", "minecraft:iron_ingot",
			"golden", "minecraft:gold_ingot",
			"diamond", "minecraft:diamond",
			"netherite", "minecraft:netherite_ingot");

	/** Everything else, by item id. Archive's additions: bow to warped fungus on a stick. */
	private static final Map<String, String> BY_ITEM = Map.ofEntries(
			Map.entry("minecraft:turtle_helmet", "minecraft:turtle_scute"),
			Map.entry("minecraft:wolf_armor", "minecraft:armadillo_scute"),
			Map.entry("minecraft:shield", "#minecraft:planks"),
			Map.entry("minecraft:elytra", "minecraft:phantom_membrane"),
			Map.entry("minecraft:mace", "minecraft:breeze_rod"),
			Map.entry("minecraft:trident", "minecraft:prismarine_shard"),
			Map.entry("minecraft:bow", "minecraft:string"),
			Map.entry("minecraft:crossbow", "minecraft:string"),
			Map.entry("minecraft:fishing_rod", "minecraft:string"),
			Map.entry("minecraft:carrot_on_a_stick", "minecraft:carrot"),
			Map.entry("minecraft:warped_fungus_on_a_stick", "minecraft:warped_fungus"),
			Map.entry("minecraft:flint_and_steel", "minecraft:flint"),
			Map.entry("minecraft:shears", "minecraft:iron_ingot"),
			Map.entry("minecraft:brush", "minecraft:feather"));

	/** Archive's default material for each item in {@link GearComponents#DEFAULT_DURABILITY}, in the same order. */
	public static final Map<String, String> DEFAULTS;

	static {
		Map<String, String> defaults = new LinkedHashMap<>();
		for (String id : GearComponents.DEFAULT_DURABILITY.keySet()) {
			String material = BY_ITEM.getOrDefault(id, BY_MATERIAL.get(materialPrefix(id)));
			if (material != null) {
				defaults.put(id, material);
			}
		}
		DEFAULTS = Collections.unmodifiableMap(defaults);
	}

	/** The part of an item id's path before the first underscore ("diamond" for minecraft:diamond_sword), or "". */
	public static String materialPrefix(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		int underscore = path.indexOf('_');
		return underscore < 0 ? "" : path.substring(0, underscore);
	}

	/** The item's repair material (override, then Archive's default), or null when vanilla decides. */
	public static String material(Item item) {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.durability) {
			return null;
		}
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		String override = config.repairOverrides.get(id);
		return override != null ? override : DEFAULTS.get(id);
	}

	/** Whether {@code repair} repairs {@code tool} at the anvil. */
	public static boolean isValidRepair(ItemStack tool, ItemStack repair) {
		String material = material(tool.getItem());
		return material == null ? tool.getItem().isValidRepairItem(tool, repair) : matches(material, repair);
	}

	public static boolean matches(String material, ItemStack stack) {
		if (material.isEmpty()) {
			return false;
		}
		boolean tag = material.startsWith("#");
		ResourceLocation id = ResourceLocation.tryParse(tag ? material.substring(1) : material);
		if (id == null) {
			return false;
		}
		return tag ? stack.is(TagKey.create(Registries.ITEM, id))
				: BuiltInRegistries.ITEM.getOptional(id).map(stack::is).orElse(false);
	}

	/** An empty string (no material), a #tag, or the id of a registered item. Tags can't be checked before a world loads. */
	public static boolean isValidMaterial(String material) {
		if (material.isEmpty()) {
			return true;
		}
		boolean tag = material.startsWith("#");
		ResourceLocation id = ResourceLocation.tryParse(tag ? material.substring(1) : material);
		return id != null && (tag || BuiltInRegistries.ITEM.containsKey(id));
	}

	private RepairMaterials() {
	}
}
