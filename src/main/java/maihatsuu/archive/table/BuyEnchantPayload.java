package maihatsuu.archive.table;

import maihatsuu.archive.Archive;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.enchantment.Enchantment;

/** Client → server: buy the next level of an enchant at the open library table. */
public record BuyEnchantPayload(int containerId, Holder<Enchantment> enchant) implements CustomPacketPayload {
	public static final Type<BuyEnchantPayload> TYPE = new Type<>(Archive.id("buy_enchant"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuyEnchantPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, BuyEnchantPayload::containerId,
			Enchantment.STREAM_CODEC, BuyEnchantPayload::enchant,
			BuyEnchantPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
