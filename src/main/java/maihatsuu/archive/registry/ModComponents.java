package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModComponents {
	/** The enchant a tome holds. Tomes have no level: one tome unlocks every level of its enchant. */
	public static final DataComponentType<Holder<Enchantment>> TOME_ENCHANTMENT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, Archive.id("tome_enchantment"),
			DataComponentType.<Holder<Enchantment>>builder()
					.persistent(Enchantment.CODEC)
					.networkSynchronized(Enchantment.STREAM_CODEC)
					.build());

	public static void init() {
	}

	private ModComponents() {
	}
}
