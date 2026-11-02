package maihatsuu.archive.client;

import maihatsuu.archive.tome.TomeItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/** Read-only page for a tome: enchant name, max level, description and the gear it applies to. */
public class TomeScreen extends Screen {
	private static final int BOOK_WIDTH = 192;
	private static final int TEXT_LEFT = 36;
	private static final int TEXT_WIDTH = 114;
	private static final int TEXT_BOTTOM = 172;
	private static final int ICON_STEP = 18;

	private final Holder<Enchantment> enchant;
	private final List<ItemStack> appliesTo;
	private List<FormattedCharSequence> descriptionLines = List.of();

	public TomeScreen(ItemStack tome) {
		super(tome.getHoverName());
		this.enchant = TomeItem.getEnchant(tome);
		this.appliesTo = enchant.value().definition().supportedItems().stream()
				.map(item -> new ItemStack(item))
				.toList();
	}

	@Override
	protected void init() {
		this.descriptionLines = this.font.split(description(), TEXT_WIDTH);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.width / 2 - 100, 196, 200, 20).build());
	}

	private Component description() {
		String key = this.enchant.unwrapKey()
				.map(k -> "enchantment." + k.location().getNamespace() + "." + k.location().getPath() + ".desc")
				.orElse("");
		return I18n.exists(key) ? Component.translatable(key) : Component.translatable("archive.tome.no_description");
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(graphics);
		graphics.blit(BookViewScreen.BOOK_LOCATION, (this.width - BOOK_WIDTH) / 2, 2, 0, 0, BOOK_WIDTH, BOOK_WIDTH);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int left = (this.width - BOOK_WIDTH) / 2 + TEXT_LEFT;
		int y = 16;

		graphics.drawString(this.font, this.enchant.value().description(), left, y, 0x5A2E8C, false);
		y += 12;
		Component maxLevel = Component.translatable("enchantment.level." + this.enchant.value().getMaxLevel());
		graphics.drawString(this.font, Component.translatable("archive.tome.max_level", maxLevel), left, y, 0x555555, false);
		y += 14;

		for (FormattedCharSequence line : this.descriptionLines) {
			graphics.drawString(this.font, line, left, y, 0, false);
			y += 9;
		}
		y += 5;

		graphics.drawString(this.font, Component.translatable("archive.tome.applies_to"), left, y, 0x555555, false);
		y += 11;

		int perRow = TEXT_WIDTH / ICON_STEP;
		int rows = Math.max(0, (TEXT_BOTTOM - y) / ICON_STEP);
		int capacity = perRow * rows;
		boolean overflow = this.appliesTo.size() > capacity;
		int shown = overflow ? Math.max(0, capacity - 1) : this.appliesTo.size();
		ItemStack hovered = ItemStack.EMPTY;
		for (int i = 0; i < shown; i++) {
			int x = left + (i % perRow) * ICON_STEP;
			int iy = y + (i / perRow) * ICON_STEP;
			ItemStack stack = this.appliesTo.get(i);
			graphics.renderItem(stack, x, iy);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= iy && mouseY < iy + 16) {
				hovered = stack;
			}
		}
		if (overflow) {
			int x = left + (shown % perRow) * ICON_STEP;
			int iy = y + (shown / perRow) * ICON_STEP;
			graphics.drawString(this.font, "+" + (this.appliesTo.size() - shown), x, iy + 4, 0x555555, false);
		}
		if (!hovered.isEmpty()) {
			graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
