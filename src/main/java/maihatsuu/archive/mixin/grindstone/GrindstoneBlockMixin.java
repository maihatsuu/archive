package maihatsuu.archive.mixin.grindstone;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.GrindstoneBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The grindstone screen is titled "Disenchant", since it no longer repairs. */
@Mixin(GrindstoneBlock.class)
public abstract class GrindstoneBlockMixin {
	@ModifyArg(method = "getMenuProvider", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/SimpleMenuProvider;<init>(Lnet/minecraft/world/inventory/MenuConstructor;Lnet/minecraft/network/chat/Component;)V"))
	private Component archive$disenchantTitle(Component title) {
		return ArchiveConfig.get().grindstone ? Component.translatable("container.archive.disenchant") : title;
	}
}
