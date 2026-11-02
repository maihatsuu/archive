package maihatsuu.archive.tome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import maihatsuu.archive.Archive;
import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.registry.ModLootFunctions;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loot function {@code archive:set_tome}: sets which enchant a tome holds.
 * Refuses unobtainable enchants and curses (the tome is dropped from the loot).
 */
public class SetTomeFunction extends LootItemConditionalFunction {
	public static final MapCodec<SetTomeFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
			.and(Enchantment.CODEC.fieldOf("enchantment").forGetter(function -> function.enchant))
			.apply(instance, SetTomeFunction::new));

	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	private final Holder<Enchantment> enchant;

	private SetTomeFunction(List<LootItemCondition> conditions, Holder<Enchantment> enchant) {
		super(conditions);
		this.enchant = enchant;
	}

	public static Builder<?> builder(Holder<Enchantment> enchant) {
		return simpleBuilder(conditions -> new SetTomeFunction(conditions, enchant));
	}

	@Override
	public LootItemFunctionType<SetTomeFunction> getType() {
		return ModLootFunctions.SET_TOME;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		// Checked at roll time: enchantment tags aren't bound yet when loot tables load.
		if (!ModItems.isTomeEnchant(this.enchant)) {
			if (WARNED.add(this.enchant.getRegisteredName())) {
				Archive.LOGGER.warn("archive:set_tome refused {}: it is unobtainable or a curse", this.enchant.getRegisteredName());
			}
			return ItemStack.EMPTY;
		}
		stack.set(ModComponents.TOME_ENCHANTMENT, this.enchant);
		return stack;
	}
}
