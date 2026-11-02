package maihatsuu.archive.mixin.anvil;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The anvil only repairs (one unit of the base material restores full durability) and renames, always for free.
 * No combining items, no applying books or tomes, no prior-work penalty.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	private int repairItemCountCost;
	@Shadow
	private String itemName;
	@Shadow
	@Final
	private DataSlot cost;

	private AnvilMenuMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access) {
		super(type, id, inventory, access);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void archive$repairOrRenameOnly(CallbackInfo ci) {
		if (!ArchiveConfig.get().anvil) {
			return;
		}
		ci.cancel();
		this.cost.set(0);
		this.resultSlots.setItem(0, archive$computeResult());
		this.broadcastChanges();
	}

	private ItemStack archive$computeResult() {
		ItemStack left = this.inputSlots.getItem(0);
		ItemStack right = this.inputSlots.getItem(1);
		if (left.isEmpty()) {
			// Don't reset repairItemCountCost here: vanilla onTake clears the left slot (re-running
			// createResult) before it reads the count to consume the material.
			return ItemStack.EMPTY;
		}
		this.repairItemCountCost = 0;
		ItemStack result = left.copy();
		boolean changed = false;

		if (!right.isEmpty()) {
			// isDamageableItem is false for unbreakable items, so those can't be repaired.
			if (!result.isDamageableItem() || !left.getItem().isValidRepairItem(left, right)) {
				return ItemStack.EMPTY;
			}
			if (result.getDamageValue() <= 0) {
				return ItemStack.EMPTY;
			}
			// A single unit of the base material fully repairs the item.
			result.setDamageValue(0);
			this.repairItemCountCost = 1;
			changed = true;
		}

		if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
			if (!this.itemName.equals(left.getHoverName().getString())) {
				result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
				changed = true;
			}
		} else if (left.has(DataComponents.CUSTOM_NAME)) {
			result.remove(DataComponents.CUSTOM_NAME);
			changed = true;
		}

		return changed ? result : ItemStack.EMPTY;
	}

	/** Vanilla requires cost > 0; with free repairs any result can be taken. */
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void archive$freePickup(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
		if (ArchiveConfig.get().anvil) {
			cir.setReturnValue(hasItem);
		}
	}
}
