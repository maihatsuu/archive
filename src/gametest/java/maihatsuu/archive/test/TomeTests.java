package maihatsuu.archive.test;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

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
				&& contents.getKey().equals("item.archive.tome.named"), "tome name should be 'Tome of ...'");
		helper.assertTrue(tome.is(ItemTags.BOOKSHELF_BOOKS), "tome should fit in chiseled bookshelves");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tomeEnchantFilter(GameTestHelper helper) {
		helper.assertTrue(ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "sharpness")), "sharpness tomes allowed");
		helper.assertTrue(ModItems.isTomeEnchant(DataTests.enchant(helper, "archive", "resilience")), "resilience tomes allowed");
		helper.assertTrue(!ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "mending")), "no mending tomes");
		helper.assertTrue(!ModItems.isTomeEnchant(DataTests.enchant(helper, "minecraft", "binding_curse")), "no curse tomes");
		helper.succeed();
	}
}
