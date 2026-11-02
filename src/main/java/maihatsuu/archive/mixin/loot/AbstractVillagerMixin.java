package maihatsuu.archive.mixin.loot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.tome.BookSources;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No villager or wandering trader sells enchanted books. Returning null makes vanilla's loop
 * pick another listing instead, so the trader still gets its full number of trades when it can.
 */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
	@WrapOperation(method = "addOffersFromItemListings", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/VillagerTrades$ItemListing;getOffer(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/trading/MerchantOffer;"))
	private MerchantOffer archive$noBookTrades(VillagerTrades.ItemListing listing, Entity trader, RandomSource random, Operation<MerchantOffer> original) {
		MerchantOffer offer = original.call(listing, trader, random);
		if (offer != null && ArchiveConfig.get().stripBookLoot && BookSources.isEnchantedBook(offer.getResult())) {
			return null;
		}
		return offer;
	}
}
