package maihatsuu.archive;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * Repair materials for vanilla gear that has none (vanilla only lets these combine with a copy of
 * themselves, which Archive turns off). Kept out of the mixin: a static field there would initialise
 * with {@code Item}, before {@code Items} exists.
 */
public final class RepairMaterials {
	public static final Map<Item, Item> MAP = Map.of(
			Items.BOW, Items.STRING,
			Items.CROSSBOW, Items.STRING,
			Items.FISHING_ROD, Items.STRING,
			Items.CARROT_ON_A_STICK, Items.CARROT,
			Items.WARPED_FUNGUS_ON_A_STICK, Items.WARPED_FUNGUS,
			Items.FLINT_AND_STEEL, Items.FLINT,
			Items.SHEARS, Items.IRON_INGOT,
			Items.BRUSH, Items.FEATHER,
			Items.TRIDENT, Items.PRISMARINE_SHARD);

	private RepairMaterials() {
	}
}
