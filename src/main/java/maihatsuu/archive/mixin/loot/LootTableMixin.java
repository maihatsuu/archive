package maihatsuu.archive.mixin.loot;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.tome.BookSources;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * Drops enchanted books (and empty stacks) from every loot table's output. Vanilla tables make
 * books with {@code minecraft:book} + {@code enchant_randomly}, so this filters the produced stack,
 * after the table's own functions have run. Nested tables pass through here too.
 */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
	@ModifyVariable(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
			at = @At("HEAD"), argsOnly = true)
	private Consumer<ItemStack> archive$stripBooks(Consumer<ItemStack> consumer, LootContext context) {
		if (!ArchiveConfig.get().stripBookLoot) {
			return consumer;
		}
		return stack -> {
			if (!stack.isEmpty() && !BookSources.isEnchantedBook(stack)) {
				consumer.accept(stack);
			}
		};
	}
}
