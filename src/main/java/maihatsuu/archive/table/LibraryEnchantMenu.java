package maihatsuu.archive.table;

import com.mojang.datafixers.util.Pair;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.registry.ModMenus;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;

/**
 * The library enchanting table: an item slot, a lapis slot, and the enchants of the tomes shelved nearby.
 * Each purchase adds one level of one enchant for a fixed XP + lapis cost.
 */
public class LibraryEnchantMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int LAPIS_SLOT = 1;
	private static final ResourceLocation EMPTY_SLOT_LAPIS = ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli");

	private final Container enchantSlots = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			LibraryEnchantMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;
	private final List<LibraryEntry> entries;

	/** Client side: entries arrive with the open-screen packet. */
	public LibraryEnchantMenu(int containerId, Inventory inventory, List<LibraryEntry> entries) {
		this(containerId, inventory, entries, ContainerLevelAccess.NULL);
	}

	public LibraryEnchantMenu(int containerId, Inventory inventory, List<LibraryEntry> entries, ContainerLevelAccess access) {
		super(ModMenus.LIBRARY_ENCHANT, containerId);
		this.access = access;
		this.entries = entries;
		this.addSlot(new Slot(this.enchantSlots, ITEM_SLOT, 15, 47) {
			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.enchantSlots, LAPIS_SLOT, 35, 47) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.LAPIS_LAZULI);
			}

			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_LAPIS);
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}
	}

	/** Server: scan the shelves and build the data sent to the client. */
	public static List<LibraryEntry> scanEntries(Level level, BlockPos pos) {
		return LibraryScan.scan(level, pos, ArchiveConfig.get().tableRadius).stream()
				.map(enchant -> new LibraryEntry(enchant, EnchantCosts.allLevels(enchant)))
				.toList();
	}

	public List<LibraryEntry> getEntries() {
		return this.entries;
	}

	public List<LibraryOptions.Option> options(Player player) {
		List<Holder<Enchantment>> available = this.entries.stream().map(LibraryEntry::enchant).toList();
		return LibraryOptions.compute(available, this.getSlot(ITEM_SLOT).getItem(), player, this.getSlot(LAPIS_SLOT).getItem().getCount(),
				(enchant, level) -> this.entries.stream().filter(entry -> entry.enchant().equals(enchant)).findFirst()
						.map(entry -> entry.cost(level)).orElseGet(() -> EnchantCosts.cost(enchant, level)));
	}

	/**
	 * Server: buy the next level of {@code enchant}. Everything is re-checked against the world:
	 * the tome must still be shelved, and costs come from the server's cost table.
	 */
	public boolean tryBuy(ServerPlayer player, Holder<Enchantment> enchant) {
		return this.access.evaluate((level, pos) -> {
			List<Holder<Enchantment>> shelved = LibraryScan.scan(level, pos, ArchiveConfig.get().tableRadius);
			if (!shelved.contains(enchant)) {
				return false;
			}
			ItemStack item = this.getSlot(ITEM_SLOT).getItem();
			ItemStack lapis = this.getSlot(LAPIS_SLOT).getItem();
			Optional<LibraryOptions.Option> option = LibraryOptions.compute(List.of(enchant), item, player, lapis.getCount(), EnchantCosts::cost)
					.stream().findFirst();
			if (option.isEmpty() || option.get().state() != LibraryOptions.State.AVAILABLE) {
				return false;
			}
			LibraryOptions.Option chosen = option.get();
			EnchantmentHelper.updateEnchantments(item, enchantments -> enchantments.set(enchant, chosen.nextLevel()));
			if (!player.hasInfiniteMaterials()) {
				player.giveExperienceLevels(-chosen.cost().xpLevels());
				lapis.shrink(chosen.cost().lapis());
			}
			this.enchantSlots.setChanged();
			player.awardStat(Stats.ENCHANT_ITEM);
			CriteriaTriggers.ENCHANTED_ITEM.trigger(player, item, chosen.cost().xpLevels());
			level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
			this.broadcastChanges();
			return true;
		}).orElse(false);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.enchantSlots));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index == ITEM_SLOT || index == LAPIS_SLOT) {
			if (!this.moveItemStackTo(stack, 2, 38, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(Items.LAPIS_LAZULI)) {
			if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			Slot itemSlot = this.slots.get(ITEM_SLOT);
			if (itemSlot.hasItem() || !itemSlot.mayPlace(stack)) {
				return ItemStack.EMPTY;
			}
			itemSlot.setByPlayer(stack.split(1));
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
