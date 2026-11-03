package maihatsuu.archive.client;

import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.TomeItem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

/** Read-only page for a tome: enchant name, max level, description and the gear it applies to. */
public class TomeScreen extends Screen {
	private static final int BOOK_WIDTH = 192;
	private static final int TEXT_LEFT = 36;
	private static final int TEXT_WIDTH = 114;
	// One page: text from y 16 to 172. Several pages: like the vanilla book, the page indicator sits at
	// y 18, text runs from 32 and stops above the page-turn arrows at 159.
	private static final int SINGLE_TOP = 16;
	private static final int SINGLE_BOTTOM = 172;
	private static final int MULTI_TOP = 32;
	private static final int MULTI_BOTTOM = 157;
	private static final int LINE_HEIGHT = 9;
	private static final int HEADER_HEIGHT = 26;
	private static final int APPLIES_GAP = 5;
	private static final int APPLIES_HEADER = 11;
	private static final int ICON_STEP = 18;
	private static final long CYCLE_MS = 1000;
	/** Items in one of these tags share a slot that cycles through them, like the recipe book. */
	private static final List<TagKey<Item>> GROUPS = List.of(
			ItemTags.SWORDS, ItemTags.AXES, ItemTags.PICKAXES, ItemTags.SHOVELS, ItemTags.HOES,
			ItemTags.HEAD_ARMOR, ItemTags.SKULLS, ItemTags.CHEST_ARMOR, ItemTags.LEG_ARMOR, ItemTags.FOOT_ARMOR);

	private final Holder<Enchantment> enchant;
	/** One slot per kind of gear; a slot with several items cycles through them. */
	private final List<List<ItemStack>> appliesTo;
	/** What one page shows: the name header (first page only), description lines, and maybe "Applies to". */
	private record Page(boolean header, List<FormattedCharSequence> lines, boolean appliesTo) {
	}

	private List<Page> pages = List.of();
	private int top = SINGLE_TOP;
	private int bottom = SINGLE_BOTTOM;
	private int currentPage;
	private PageButton forwardButton;
	private PageButton backButton;

	public TomeScreen(ItemStack tome) {
		super(tome.getHoverName());
		this.enchant = TomeItem.getEnchant(tome);
		this.appliesTo = groupItems(enchant.value().definition().supportedItems());
	}

	/** Groups items by the first {@link #GROUPS} tag they're in; items in none get their own slot. */
	private static List<List<ItemStack>> groupItems(HolderSet<Item> items) {
		Map<Object, List<ItemStack>> slots = new LinkedHashMap<>();
		for (Holder<Item> item : items) {
			Object key = GROUPS.stream().filter(item::is).findFirst().<Object>map(tag -> tag).orElse(item);
			slots.computeIfAbsent(key, k -> new ArrayList<>()).add(new ItemStack(item));
		}
		return List.copyOf(slots.values());
	}

	@Override
	protected void init() {
		List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(description(), TEXT_WIDTH));
		List<Component> conflicts = incompatibleEnchants();
		if (!conflicts.isEmpty()) {
			lines.add(FormattedCharSequence.EMPTY);
			lines.addAll(this.font.split(Component.translatable("archive.tome.incompatible_with").withColor(0x555555), TEXT_WIDTH));
			lines.addAll(this.font.split(ComponentUtils.formatList(conflicts, Component.literal(", ")), TEXT_WIDTH));
		}
		this.top = SINGLE_TOP;
		this.bottom = SINGLE_BOTTOM;
		this.pages = paginate(lines);
		if (this.pages.size() > 1) {
			// The indicator and arrows take room, so lay out again with the smaller page.
			this.top = MULTI_TOP;
			this.bottom = MULTI_BOTTOM;
			this.pages = paginate(lines);
		}
		this.currentPage = Math.min(this.currentPage, this.pages.size() - 1);

		int bookLeft = (this.width - BOOK_WIDTH) / 2;
		this.forwardButton = this.addRenderableWidget(new PageButton(bookLeft + 116, 159, true, button -> turnPage(1), true));
		this.backButton = this.addRenderableWidget(new PageButton(bookLeft + 43, 159, false, button -> turnPage(-1), true));
		updateButtons();
		addBottomButtons();
	}

	/** The buttons under the book; the lectern screen adds Take Book. */
	protected void addBottomButtons() {
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.width / 2 - 100, 196, 200, 20).build());
	}

	/**
	 * Splits the description over pages. "Applies to" goes after the last line if its heading and one
	 * row of icons fit there, otherwise on a page of its own.
	 */
	private List<Page> paginate(List<FormattedCharSequence> lines) {
		List<Page> result = new ArrayList<>();
		boolean header = true;
		int y = this.top + HEADER_HEIGHT;
		List<FormattedCharSequence> pageLines = new ArrayList<>();
		for (FormattedCharSequence line : lines) {
			if (y + LINE_HEIGHT > this.bottom) {
				result.add(new Page(header, List.copyOf(pageLines), false));
				header = false;
				pageLines.clear();
				y = this.top;
			}
			pageLines.add(line);
			y += LINE_HEIGHT;
		}
		boolean appliesFits = y + APPLIES_GAP + APPLIES_HEADER + ICON_STEP <= this.bottom;
		if (appliesFits) {
			result.add(new Page(header, List.copyOf(pageLines), true));
		} else {
			result.add(new Page(header, List.copyOf(pageLines), false));
			result.add(new Page(false, List.of(), true));
		}
		return List.copyOf(result);
	}

	private void turnPage(int delta) {
		this.currentPage = Math.max(0, Math.min(this.pages.size() - 1, this.currentPage + delta));
		updateButtons();
	}

	private void updateButtons() {
		this.forwardButton.visible = this.currentPage < this.pages.size() - 1;
		this.backButton.visible = this.currentPage > 0;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_PAGE_UP && this.backButton.visible) {
			this.backButton.onPress();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN && this.forwardButton.visible) {
			this.forwardButton.onPress();
			return true;
		}
		return false;
	}

	/** Obtainable enchants that can't share an item with this one, by name (both directions of the exclusive sets). */
	private List<Component> incompatibleEnchants() {
		if (this.minecraft.level == null) {
			return List.of();
		}
		return this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
				.filter(other -> !other.equals(this.enchant) && ModItems.isTomeEnchant(other)
						&& !Enchantment.areCompatible(this.enchant, other))
				.map(other -> other.value().description())
				.sorted(Comparator.comparing(Component::getString))
				.toList();
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
		int bookLeft = (this.width - BOOK_WIDTH) / 2;
		int left = bookLeft + TEXT_LEFT;
		int y = this.top;
		Page page = this.pages.get(this.currentPage);

		if (this.pages.size() > 1) {
			Component indicator = Component.translatable("book.pageIndicator", this.currentPage + 1, this.pages.size());
			graphics.drawString(this.font, indicator, bookLeft + BOOK_WIDTH - 44 - this.font.width(indicator), 18, 0, false);
		}
		if (page.header()) {
			graphics.drawString(this.font, this.enchant.value().description(), left, y, 0x5A2E8C, false);
			y += 12;
			Component maxLevel = Component.translatable("enchantment.level." + this.enchant.value().getMaxLevel());
			graphics.drawString(this.font, Component.translatable("archive.tome.max_level", maxLevel), left, y, 0x555555, false);
			y += 14;
		}
		for (FormattedCharSequence line : page.lines()) {
			graphics.drawString(this.font, line, left, y, 0, false);
			y += LINE_HEIGHT;
		}
		if (!page.appliesTo()) {
			return;
		}
		if (!page.lines().isEmpty() || page.header()) {
			y += APPLIES_GAP;
		}

		graphics.drawString(this.font, Component.translatable("archive.tome.applies_to"), left, y, 0x555555, false);
		y += APPLIES_HEADER;

		int perRow = TEXT_WIDTH / ICON_STEP;
		int rows = Math.max(0, (this.bottom - y) / ICON_STEP);
		int capacity = perRow * rows;
		boolean overflow = this.appliesTo.size() > capacity;
		int shown = overflow ? Math.max(0, capacity - 1) : this.appliesTo.size();
		ItemStack hovered = ItemStack.EMPTY;
		long cycle = Util.getMillis() / CYCLE_MS;
		for (int i = 0; i < shown; i++) {
			int x = left + (i % perRow) * ICON_STEP;
			int iy = y + (i / perRow) * ICON_STEP;
			List<ItemStack> slot = this.appliesTo.get(i);
			ItemStack stack = slot.get((int) (cycle % slot.size()));
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
