package maihatsuu.archive.registry;

import maihatsuu.archive.table.BuyEnchantPayload;
import maihatsuu.archive.table.LibraryEnchantMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	public static void init() {
		PayloadTypeRegistry.playC2S().register(BuyEnchantPayload.TYPE, BuyEnchantPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(BuyEnchantPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof LibraryEnchantMenu menu && menu.containerId == payload.containerId()) {
				menu.tryBuy(context.player(), payload.enchant());
			}
		});
	}

	private ModNetworking() {
	}
}
