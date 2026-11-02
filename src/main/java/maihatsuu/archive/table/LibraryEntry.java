package maihatsuu.archive.table;

import maihatsuu.archive.table.EnchantCosts.LevelCost;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/** An enchant found on the shelves, with its per-level costs, as sent to the client when the table opens. */
public record LibraryEntry(Holder<Enchantment> enchant, List<LevelCost> costs) {
	public static final StreamCodec<RegistryFriendlyByteBuf, LibraryEntry> STREAM_CODEC = StreamCodec.composite(
			Enchantment.STREAM_CODEC, LibraryEntry::enchant,
			LevelCost.STREAM_CODEC.apply(ByteBufCodecs.list()), LibraryEntry::costs,
			LibraryEntry::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<LibraryEntry>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

	public LevelCost cost(int level) {
		return level >= 1 && level <= this.costs.size() ? this.costs.get(level - 1) : EnchantCosts.defaultCost(level);
	}
}
