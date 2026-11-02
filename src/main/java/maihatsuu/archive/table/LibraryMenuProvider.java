package maihatsuu.archive.table;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Opens the library menu from an enchanting table; the shelf scan is sent along as opening data. */
public record LibraryMenuProvider(Level level, BlockPos pos, Component title) implements ExtendedScreenHandlerFactory<List<LibraryEntry>> {
	@Override
	public List<LibraryEntry> getScreenOpeningData(ServerPlayer player) {
		return LibraryEnchantMenu.scanEntries(this.level, this.pos);
	}

	@Override
	public Component getDisplayName() {
		return this.title;
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new LibraryEnchantMenu(containerId, inventory, LibraryEnchantMenu.scanEntries(this.level, this.pos),
				ContainerLevelAccess.create(this.level, this.pos));
	}
}
