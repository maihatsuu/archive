package maihatsuu.archive;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;
import java.util.Map;

/** Default item components: durability overrides from config, and unbreakable rare gear. */
public final class GearComponents {
	private static final List<Item> UNBREAKABLE = List.of(
			Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
			Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
			Items.TRIDENT, Items.MACE);

	static void register() {
		DefaultItemComponentEvents.MODIFY.register(context -> {
			ArchiveConfig config = ArchiveConfig.get();
			if (!config.durability) {
				return;
			}
			for (Map.Entry<String, Integer> entry : config.durabilityOverrides.entrySet()) {
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
