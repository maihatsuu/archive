package maihatsuu.archive.mixin.grindstone;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.table.GrindstoneRefund;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The grindstone only disenchants a single item, and refunds part of the table cost. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu {
	@Shadow
	@Final
	private Container resultSlots;
	@Shadow
	@Final
	Container repairSlots;

	@Shadow
	protected abstract ItemStack removeNonCursesFrom(ItemStack stack);

	@Unique
	private static final int INPUT_X = 49;
	@Unique
	private static final int INPUT_Y = 30;

	private GrindstoneMenuMixin(MenuType<?> type, int id) {
		super(type, id);
	}

	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void archive$disenchantOnly(ItemStack top, ItemStack bottom, CallbackInfoReturnable<ItemStack> cir) {
		if (!ArchiveConfig.get().grindstone) {
			return;
		}
		if (!top.isEmpty() && !bottom.isEmpty()) {
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}
		ItemStack input = top.isEmpty() ? bottom : top;
		if (input.isEmpty() || input.getCount() > 1 || !GrindstoneRefund.hasNonCurseEnchants(input)) {
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}
		cir.setReturnValue(this.removeNonCursesFrom(input.copy()));
	}

	/** Swap vanilla's result slot (random XP from enchant min-costs) for one that refunds table costs. */
	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void archive$replaceResultSlot(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		if (!ArchiveConfig.get().grindstone) {
			return;
		}
		Slot vanilla = this.slots.get(2);
		Container repair = this.repairSlots;
		Slot replacement = new Slot(this.resultSlots, 2, vanilla.x, vanilla.y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				int points = GrindstoneRefund.refundPoints(repair.getItem(0)) + GrindstoneRefund.refundPoints(repair.getItem(1));
				access.execute((level, pos) -> {
					if (level instanceof ServerLevel serverLevel && points > 0) {
						ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), points);
					}
					level.levelEvent(1042, pos, 0);
				});
				repair.setItem(0, ItemStack.EMPTY);
				repair.setItem(1, ItemStack.EMPTY);
			}
		};
		replacement.index = vanilla.index;
		this.slots.set(2, replacement);
		archive$useSingleInputSlot();
	}

	/**
	 * One input slot, where Archive's grindstone texture draws it ({@link #INPUT_X}, {@link #INPUT_Y}).
	 * The bottom slot stays in the list, since slot indexes are fixed, but can't be seen or filled.
	 */
	@Unique
	private void archive$useSingleInputSlot() {
		Slot top = this.slots.get(0);
		Slot input = new Slot(this.repairSlots, 0, INPUT_X, INPUT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return top.mayPlace(stack);
			}
		};
		input.index = top.index;
		this.slots.set(0, input);

		Slot bottom = this.slots.get(1);
		Slot hidden = new Slot(this.repairSlots, 1, bottom.x, bottom.y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean isActive() {
				return false;
			}
		};
		hidden.index = bottom.index;
		this.slots.set(1, hidden);
	}
}
