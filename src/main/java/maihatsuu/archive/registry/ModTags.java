package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public final class ModTags {
	/** Enchants that can't be obtained in survival (but keep working on existing items). */
	public static final TagKey<Enchantment> UNOBTAINABLE = TagKey.create(Registries.ENCHANTMENT, Archive.id("unobtainable"));

	public static final TagKey<Block> DEEPSLATE_HARDNESS = TagKey.create(Registries.BLOCK, Archive.id("deepslate_hardness"));
	public static final TagKey<Block> DEEPSLATE_ORE_HARDNESS = TagKey.create(Registries.BLOCK, Archive.id("deepslate_ore_hardness"));
	public static final TagKey<Block> OBSIDIAN_HARDNESS = TagKey.create(Registries.BLOCK, Archive.id("obsidian_hardness"));

	private ModTags() {
	}
}
