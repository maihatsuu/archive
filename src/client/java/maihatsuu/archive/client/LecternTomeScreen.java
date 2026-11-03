package maihatsuu.archive.client;

import maihatsuu.archive.tome.LecternTomeMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The tome page opened from a lectern, with Done and Take Book like vanilla's lectern screen. */
public class LecternTomeScreen extends TomeScreen implements MenuAccess<LecternTomeMenu> {
	private final LecternTomeMenu menu;

	public LecternTomeScreen(LecternTomeMenu menu, Inventory inventory, Component title) {
		super(menu.getTome());
		this.menu = menu;
	}

	@Override
	public LecternTomeMenu getMenu() {
		return this.menu;
	}

	@Override
	protected void addBottomButtons() {
		if (!this.minecraft.player.mayBuild()) {
			super.addBottomButtons();
			return;
		}
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.width / 2 - 100, 196, 98, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"),
						button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, LecternTomeMenu.TAKE_BOOK))
				.bounds(this.width / 2 + 2, 196, 98, 20).build());
	}

	@Override
	public void onClose() {
		this.minecraft.player.closeContainer();
		super.onClose();
	}
}
