package maihatsuu.archive.test;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.LecternTomeMenu;
import maihatsuu.archive.tome.TomeItem;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

import java.util.List;

/** Phase 5: the tome item. */
public class TomeTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void tomeHoldsAnEnchantWithoutGlint(GameTestHelper helper) {
		ItemStack tome = TomeItem.of(DataTests.enchant(helper, "minecraft", "sharpness"));
		helper.assertTrue(tome.get(ModComponents.TOME_ENCHANTMENT).is(DataTests.enchant(helper, "minecraft", "sharpness").unwrapKey().orElseThrow()),
				"tome should store sharpness");
		helper.assertTrue(!tome.hasFoil(), "tome should have no glint");
		helper.assertTrue(tome.getMaxStackSize() == 1, "tomes should not stack");
		helper.assertTrue(tome.getHoverName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("item.archive.tome"), "tome name should be just 'Tome'");
		helper.assertTrue(TomeItem.enchantLine(DataTests.enchant(helper, "minecraft", "sharpness")).getStyle().getColor()
				.equals(TextColor.fromLegacyFormat(ChatFormatting.AQUA)), "enchant line should be light blue");
		helper.assertTrue(tome.is(ItemTags.BOOKSHELF_BOOKS), "tome should fit in chiseled bookshelves");
		ItemStack cursed = TomeItem.of(DataTests.enchant(helper, "minecraft", "binding_curse"));
		helper.assertTrue(cursed.getHoverName().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("item.archive.tome"), "curse tome name should be just 'Tome'");
		helper.assertTrue(TomeItem.enchantLine(DataTests.enchant(helper, "minecraft", "binding_curse")).getStyle().getColor()
				.equals(TextColor.fromLegacyFormat(ChatFormatting.RED)), "curse line should be red");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tomeGoesOnALecternAndComesBack(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.LECTERN);
		ItemStack tome = TomeItem.of(DataTests.enchant(helper, "minecraft", "sharpness"));
		helper.assertTrue(tome.is(ItemTags.LECTERN_BOOKS), "tomes should be lectern books");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(LecternBlock.tryPlaceBook(player, helper.getLevel(), absolute, helper.getBlockState(pos), tome),
				"tome should go on the lectern");
		LecternBlockEntity lectern = (LecternBlockEntity) helper.getBlockEntity(pos);
		helper.assertTrue(helper.getBlockState(pos).getValue(LecternBlock.HAS_BOOK) && LecternTomeMenu.holdsTome(lectern),
				"lectern should hold the tome");

		LecternTomeMenu menu = LecternTomeMenu.forTest(lectern);
		helper.assertTrue(menu.clickMenuButton(player, LecternTomeMenu.TAKE_BOOK), "take book should work");
		helper.assertTrue(!helper.getBlockState(pos).getValue(LecternBlock.HAS_BOOK), "lectern should be empty");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(ModItems.TOME)), "player should get the tome back");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tomeEnchantFilter(GameTestHelper helper) {
		helper.assertTrue(ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "sharpness")), "sharpness tomes allowed");
		helper.assertTrue(ModItems.isTomeEnchant(DataTests.enchant(helper, "archive", "resilience")), "resilience tomes allowed");
		helper.assertTrue(!ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "mending")), "no mending tomes");
		helper.assertTrue(ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "binding_curse")), "curse tomes allowed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void creativeTabsHideBooksAndDamagedAnvils(GameTestHelper helper) {
		CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
		boolean anvilShown = false;
		for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
			for (ItemStack stack : tab.getDisplayItems()) {
				helper.assertTrue(!stack.is(Items.ENCHANTED_BOOK), "enchanted book shown in " + tab.getDisplayName().getString());
				helper.assertTrue(!stack.is(Items.CHIPPED_ANVIL) && !stack.is(Items.DAMAGED_ANVIL),
						"chipped/damaged anvil shown in " + tab.getDisplayName().getString());
				anvilShown |= stack.is(Items.ANVIL);
			}
			for (ItemStack stack : tab.getSearchTabDisplayItems()) {
				helper.assertTrue(!stack.is(Items.ENCHANTED_BOOK) && !stack.is(Items.CHIPPED_ANVIL) && !stack.is(Items.DAMAGED_ANVIL),
						"hidden item in search for " + tab.getDisplayName().getString());
			}
		}
		helper.assertTrue(anvilShown, "the normal anvil should still be in creative");
		List<ItemStack> ingredients = List.copyOf(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INGREDIENTS).getDisplayItems());
		helper.assertTrue(ingredients.stream().anyMatch(stack -> stack.is(ModItems.TOME)), "ingredients tab should list tomes");
		helper.assertTrue(ingredients.get(ingredients.size() - 1).is(ModItems.TOME), "tomes should be at the end of ingredients");
		helper.succeed();
	}
}
