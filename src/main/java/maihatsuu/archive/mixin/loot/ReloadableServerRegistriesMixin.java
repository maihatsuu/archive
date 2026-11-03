package maihatsuu.archive.mixin.loot;

import maihatsuu.archive.tome.LootTomes;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reads the {@code loot_tomes} datapack files at the start of each reload. Loot tables are
 * parsed here, before any reload listener runs, so a listener would be too late for
 * {@code LootTableEvents.MODIFY}.
 */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
	@Inject(method = "reload", at = @At("HEAD"))
	private static void archive$loadLootTomes(LayeredRegistryAccess<RegistryLayer> registries, ResourceManager manager, Executor executor,
												   CallbackInfoReturnable<CompletableFuture<LayeredRegistryAccess<RegistryLayer>>> cir) {
		LootTomes.load(manager);
	}
}
