package maihatsuu.archive.client;

import com.mojang.blaze3d.systems.RenderSystem;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.table.BuyEnchantPayload;
import maihatsuu.archive.table.LibraryEnchantMenu;
import maihatsuu.archive.table.LibraryEntry;
import maihatsuu.archive.table.LibraryOptions.Option;
import maihatsuu.archive.table.LibraryOptions.State;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Library enchanting table screen: a scrollable list of the enchants available for the item in the slot,
 * drawn with the vanilla enchanting table's background and row sprites.
 */
public class LibraryEnchantScreen extends AbstractContainerScreen<LibraryEnchantMenu> {
	private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/enchanting_table.png");
	private static final ResourceLocation ROW = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot");
	private static final ResourceLocation ROW_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot_highlighted");
	private static final ResourceLocation ROW_DISABLED = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot_disabled");

	private static final int LIST_X = 60;
	private static final int LIST_Y = 14;
	private static final int ROW_WIDTH = 108;
	private static final int ROW_HEIGHT = 19;
	private static final int VISIBLE_ROWS = 3;
	private static final int TEXT_WIDTH = 86;

	// Vanilla enchanting screen colours.
	private static final int NAME_COLOR = 0x685E4A;
	private static final int NAME_HOVER_COLOR = 0xFFFF80;
	private static final int NAME_DISABLED_COLOR = 0x342F25;
	private static final int COST_COLOR = 0x80FF20;
	private static final int COST_DISABLED_COLOR = 0x407F10;

	private List<Option> options = List.of();
	private int scroll;

	public LibraryEnchantScreen(LibraryEnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.options = this.menu.options(this.minecraft.player);
		this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
	}

	private int maxScroll() {
		return Math.max(0, this.options.size() - VISIBLE_ROWS);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.options = this.menu.options(this.minecraft.player);
		this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
		Option hovered = optionAt(mouseX, mouseY);
		if (hovered != null) {
			graphics.renderComponentTooltip(this.font, tooltip(hovered), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;
		graphics.blit(BACKGROUND, left, top, 0, 0, this.imageWidth, this.imageHeight);

		// Book corner: how many tomes are shelved nearby.
		graphics.renderItem(new ItemStack(ModItems.TOME), left + 22, top + 18);
		String count = String.valueOf(this.menu.getEntries().stream().filter(LibraryEntry::fromTome).count());
		graphics.drawString(this.font, count, left + 30 - this.font.width(count) / 2, top + 36, 0x404040, false);

		int listLeft = left + LIST_X;
		int listTop = top + LIST_Y;
		if (this.options.isEmpty()) {
			for (int row = 0; row < VISIBLE_ROWS; row++) {
				RenderSystem.enableBlend();
				graphics.blitSprite(ROW_DISABLED, listLeft, listTop + row * ROW_HEIGHT, ROW_WIDTH, ROW_HEIGHT);
				RenderSystem.disableBlend();
			}
			graphics.drawWordWrap(this.font, emptyMessage(), listLeft + 4, listTop + 16, ROW_WIDTH - 8, NAME_DISABLED_COLOR);
			return;
		}

		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int index = this.scroll + row;
			int rowTop = listTop + row * ROW_HEIGHT;
			if (index >= this.options.size()) {
				RenderSystem.enableBlend();
				graphics.blitSprite(ROW_DISABLED, listLeft, rowTop, ROW_WIDTH, ROW_HEIGHT);
				RenderSystem.disableBlend();
				continue;
			}
			renderRow(graphics, this.options.get(index), listLeft, rowTop, mouseX, mouseY);
		}

		if (maxScroll() > 0) {
			int trackTop = listTop;
			int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
			int thumbHeight = Math.max(6, trackHeight * VISIBLE_ROWS / this.options.size());
			int thumbTop = trackTop + (trackHeight - thumbHeight) * this.scroll / maxScroll();
			graphics.fill(listLeft + ROW_WIDTH + 1, thumbTop, listLeft + ROW_WIDTH + 3, thumbTop + thumbHeight, 0xFF8B8B8B);
		}
	}

	private void renderRow(GuiGraphics graphics, Option option, int x, int y, int mouseX, int mouseY) {
		boolean available = option.state() == State.AVAILABLE;
		boolean hovered = available && mouseX >= x && mouseX < x + ROW_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
		RenderSystem.enableBlend();
		graphics.blitSprite(available ? (hovered ? ROW_HIGHLIGHTED : ROW) : ROW_DISABLED, x, y, ROW_WIDTH, ROW_HEIGHT);
		RenderSystem.disableBlend();

		int nameColor = available ? (hovered ? NAME_HOVER_COLOR : NAME_COLOR) : NAME_DISABLED_COLOR;
		String name = this.font.plainSubstrByWidth(rowName(option).getString(), TEXT_WIDTH);
		graphics.drawString(this.font, name, x + 20, y + 2, nameColor, false);

		if (option.state() == State.MAXED || option.state() == State.LOCKED) {
			Component label = Component.translatable(option.state() == State.MAXED ? "archive.table.max" : "archive.table.locked");
			graphics.drawString(this.font, label, x + 20 + TEXT_WIDTH - this.font.width(label), y + 9, COST_DISABLED_COLOR);
			return;
		}
		ItemStack lapis = new ItemStack(Items.LAPIS_LAZULI, option.cost().lapis());
		graphics.renderItem(lapis, x + 1, y + 1);
		graphics.renderItemDecorations(this.font, lapis, x + 1, y + 1, option.cost().lapis() > 1 ? null : "");
		String xp = String.valueOf(option.cost().xpLevels());
		int costColor = option.state() == State.TOO_EXPENSIVE || option.state() == State.INCOMPATIBLE ? COST_DISABLED_COLOR : COST_COLOR;
		graphics.drawString(this.font, xp, x + 20 + TEXT_WIDTH - this.font.width(xp), y + 9, costColor);
	}

	private static Component rowName(Option option) {
		Component level = Component.translatable("enchantment.level." + option.nextLevel());
		return Component.empty().append(option.enchant().value().description()).append(" ").append(level);
	}

	private Component emptyMessage() {
		if (this.menu.getSlot(LibraryEnchantMenu.ITEM_SLOT).getItem().isEmpty()) {
			return Component.translatable("archive.table.place_item");
		}
		if (this.menu.getEntries().isEmpty()) {
			return Component.translatable("archive.table.no_tomes");
		}
		return Component.translatable("archive.table.nothing_fits");
	}

	private List<Component> tooltip(Option option) {
		List<Component> lines = new ArrayList<>();
		lines.add(rowName(option).copy().withStyle(ChatFormatting.WHITE));
		switch (option.state()) {
			case MAXED -> lines.add(Component.translatable("archive.table.tooltip.maxed").withStyle(ChatFormatting.GRAY));
			case LOCKED -> {
				int shelves = this.menu.entry(option.enchant()).map(LibraryEntry::shelvesForNext).orElse(0);
				lines.add((shelves > 0
						? Component.translatable("archive.table.tooltip.needs_shelves", shelves)
						: Component.translatable("archive.table.tooltip.needs_tome")).withStyle(ChatFormatting.RED));
			}
			case INCOMPATIBLE -> lines.add(Component.translatable("archive.table.tooltip.incompatible",
					option.conflict().value().description()).withStyle(ChatFormatting.RED));
			default -> {
				lines.add(Component.translatable("archive.table.tooltip.cost", option.cost().xpLevels(), option.cost().lapis())
						.withStyle(ChatFormatting.GRAY));
				if (option.state() == State.TOO_EXPENSIVE) {
					lines.add(Component.translatable("archive.table.tooltip.too_expensive").withStyle(ChatFormatting.RED));
				}
			}
		}
		return lines;
	}

	private Option optionAt(double mouseX, double mouseY) {
		int x = (int) mouseX - (this.leftPos + LIST_X);
		int y = (int) mouseY - (this.topPos + LIST_Y);
		if (x < 0 || x >= ROW_WIDTH || y < 0 || y >= VISIBLE_ROWS * ROW_HEIGHT) {
			return null;
		}
		int index = this.scroll + y / ROW_HEIGHT;
		return index < this.options.size() ? this.options.get(index) : null;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Option option = optionAt(mouseX, mouseY);
		if (option != null && option.state() == State.AVAILABLE && button == 0) {
			ClientPlayNetworking.send(new BuyEnchantPayload(this.menu.containerId, option.enchant()));
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll() > 0) {
			this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
