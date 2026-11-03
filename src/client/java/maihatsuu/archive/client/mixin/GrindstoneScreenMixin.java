package maihatsuu.archive.client.mixin;

import maihatsuu.archive.Archive;
import maihatsuu.archive.ArchiveConfig;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Draws Archive's single-input grindstone background while the grindstone rework is on. */
@Mixin(GrindstoneScreen.class)
public class GrindstoneScreenMixin {
	@Unique
	private static final ResourceLocation ARCHIVE_TEXTURE = Archive.id("textures/gui/container/grindstone.png");

	@ModifyArg(method = "renderBg", index = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
	private ResourceLocation archive$singleSlotTexture(ResourceLocation texture) {
		return ArchiveConfig.get().grindstone ? ARCHIVE_TEXTURE : texture;
	}
}
