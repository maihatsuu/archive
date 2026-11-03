package maihatsuu.archive.tome;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A tome holds one enchant (no level). Shelved in a chiseled bookshelf near an enchanting table,
 * it unlocks every level of that enchant. Right-click to read it.
 */
public class TomeItem extends Item {
	/** Set by the client entrypoint; opens the reading screen. Keeps client classes out of common code. */
	public static Consumer<ItemStack> clientOpener = stack -> {
	};

	public TomeItem(Properties properties) {
		super(properties);
	}

	public static ItemStack of(Holder<Enchantment> enchant) {
		ItemStack stack = new ItemStack(ModItems.TOME);
		stack.set(ModComponents.TOME_ENCHANTMENT, enchant);
		return stack;
	}

	@Nullable
	public static Holder<Enchantment> getEnchant(ItemStack stack) {
		return stack.get(ModComponents.TOME_ENCHANTMENT);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return false;
	}

	@Override
	public Component getName(ItemStack stack) {
		Holder<Enchantment> enchant = getEnchant(stack);
		if (enchant == null) {
			return super.getName(stack);
		}
		if (enchant.is(EnchantmentTags.CURSE)) {
			// "Cursed Tome of Binding" rather than "Tome of Curse of Binding". The short name comes from
			// the lang key enchantment.<namespace>.<path>.cursed_tome; curses without one use their full name.
			String key = enchant.unwrapKey()
					.map(k -> "enchantment." + k.location().getNamespace() + "." + k.location().getPath() + ".cursed_tome")
					.orElse("");
			return Component.translatable("item.archive.tome.cursed",
					Component.translatableWithFallback(key, enchant.value().description().getString()));
		}
		return Component.translatable("item.archive.tome.named", enchant.value().description());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (getEnchant(stack) == null) {
			return InteractionResultHolder.pass(stack);
		}
		if (level.isClientSide) {
			clientOpener.accept(stack);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
