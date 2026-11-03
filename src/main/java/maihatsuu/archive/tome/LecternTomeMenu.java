package maihatsuu.archive.tome;

import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.registry.ModMenus;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Reading a tome on a lectern. Vanilla's lectern screen only knows written books, so a lectern holding
 * a tome opens this instead: the client shows the tome page, and button {@link #TAKE_BOOK} takes the tome.
 */
public class LecternTomeMenu extends AbstractContainerMenu {
	public static final int TAKE_BOOK = 0;

	private final ItemStack tome;
	/** Null on the client. */
	@Nullable
	private final LecternBlockEntity lectern;

	/** Client side: the tome comes from the opening data. */
	public LecternTomeMenu(int containerId, Inventory inventory, ItemStack tome) {
		this(containerId, tome, null);
	}

	private LecternTomeMenu(int containerId, ItemStack tome, @Nullable LecternBlockEntity lectern) {
		super(ModMenus.LECTERN_TOME, containerId);
		this.tome = tome;
		this.lectern = lectern;
	}

	public static boolean holdsTome(LecternBlockEntity lectern) {
		return lectern.getBook().is(ModItems.TOME);
	}

	/** Opens the tome menu for a lectern holding a tome. */
	public static void open(Player player, LecternBlockEntity lectern) {
		player.openMenu(new ExtendedScreenHandlerFactory<ItemStack>() {
			@Override
			public ItemStack getScreenOpeningData(ServerPlayer serverPlayer) {
				return lectern.getBook().copy();
			}

			@Override
			public Component getDisplayName() {
				return lectern.getBook().getHoverName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new LecternTomeMenu(containerId, lectern.getBook().copy(), lectern);
			}
		});
		player.awardStat(Stats.INTERACT_WITH_LECTERN);
	}

	/** A server-side menu without opening a screen, for game tests. */
	public static LecternTomeMenu forTest(LecternBlockEntity lectern) {
		return new LecternTomeMenu(0, lectern.getBook().copy(), lectern);
	}

	public ItemStack getTome() {
		return this.tome;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id != TAKE_BOOK || this.lectern == null || !player.mayBuild() || !holdsTome(this.lectern)) {
			return false;
		}
		// Same as vanilla's Take Book: empty the lectern, then give the book to the player.
		ItemStack book = this.lectern.getBook();
		this.lectern.setBook(ItemStack.EMPTY);
		LecternBlock.resetBookState(player, this.lectern.getLevel(), this.lectern.getBlockPos(), this.lectern.getBlockState(), false);
		if (!player.getInventory().add(book)) {
			player.drop(book, false);
		}
		// The lectern is empty now, so stillValid fails and the menu closes on the next tick.
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.lectern == null || (Container.stillValidBlockEntity(this.lectern, player) && holdsTome(this.lectern));
	}
}
