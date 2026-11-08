package maihatsuu.archive.mixin.gear;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Replaces an item's default components after startup, the way Fabric's DefaultItemComponentEvents does. */
@Mixin(Item.class)
public interface ItemComponentsAccessor {
	@Mutable
	@Accessor("components")
	void archive$setComponents(DataComponentMap components);
}
