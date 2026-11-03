package maihatsuu.archive.table;

import maihatsuu.archive.table.EnchantCosts.LevelCost;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * An enchant the table offers, as sent to the client when the table opens.
 *
 * @param costs          per-level costs, levels 1..max
 * @param unlockedMax    highest level that can be bought here: the max level with a tome, else the bookshelf limit
 * @param fromTome       whether a tome is shelved nearby
 * @param shelvesForNext bookshelves needed to unlock the level after {@code unlockedMax}; 0 if only a tome can
 */
public record LibraryEntry(Holder<Enchantment> enchant, List<LevelCost> costs, int unlockedMax, boolean fromTome, int shelvesForNext) {
	public static final StreamCodec<RegistryFriendlyByteBuf, LibraryEntry> STREAM_CODEC = StreamCodec.composite(
			Enchantment.STREAM_CODEC, LibraryEntry::enchant,
			LevelCost.STREAM_CODEC.apply(ByteBufCodecs.list()), LibraryEntry::costs,
			ByteBufCodecs.VAR_INT, LibraryEntry::unlockedMax,
			ByteBufCodecs.BOOL, LibraryEntry::fromTome,
			ByteBufCodecs.VAR_INT, LibraryEntry::shelvesForNext,
			LibraryEntry::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<LibraryEntry>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

	public LevelCost cost(int level) {
		return level >= 1 && level <= this.costs.size() ? this.costs.get(level - 1) : EnchantCosts.defaultCost(level);
	}
}
