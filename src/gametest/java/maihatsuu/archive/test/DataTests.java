package maihatsuu.archive.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/** Phase 1: enchantment and tag data overrides. */
public class DataTests implements FabricGameTest {
	private static final List<String> UNOBTAINABLE = List.of(
			"mending", "unbreaking", "fire_protection", "blast_protection", "projectile_protection");
	private static final List<String> ACQUISITION_TAGS = List.of(
			"non_treasure", "treasure", "on_random_loot", "tradeable", "in_enchanting_table",
			"on_mob_spawn_equipment", "on_traded_equipment");

	static Holder<Enchantment> enchant(GameTestHelper helper, String namespace, String path) {
		Registry<Enchantment> registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		return registry.getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
				ResourceLocation.fromNamespaceAndPath(namespace, path)));
	}

	static TagKey<Enchantment> tag(String namespace, String path) {
		return TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(namespace, path));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void unobtainableEnchantsLeaveAcquisitionTags(GameTestHelper helper) {
		for (String id : UNOBTAINABLE) {
			Holder<Enchantment> holder = enchant(helper, "minecraft", id);
			helper.assertTrue(holder.is(tag("archive", "unobtainable")), id + " should be in #archive:unobtainable");
			for (String tag : ACQUISITION_TAGS) {
				helper.assertTrue(!holder.is(tag("minecraft", tag)), id + " should not be in #minecraft:" + tag);
			}
		}
		// Something that should stay obtainable.
		helper.assertTrue(enchant(helper, "minecraft", "sharpness").is(tag("minecraft", "in_enchanting_table")),
				"sharpness should still be in #minecraft:in_enchanting_table");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void resilienceIsChestOnlyAndStacksWithProtection(GameTestHelper helper) {
		Holder<Enchantment> resilience = enchant(helper, "archive", "resilience");
		Holder<Enchantment> protection = enchant(helper, "minecraft", "protection");
		helper.assertTrue(resilience.value().getMaxLevel() == 3, "resilience max level should be 3");
		helper.assertTrue(resilience.value().isSupportedItem(new ItemStack(Items.DIAMOND_CHESTPLATE)), "resilience should fit chestplates");
		helper.assertTrue(!resilience.value().isSupportedItem(new ItemStack(Items.DIAMOND_HELMET)), "resilience should not fit helmets");
		helper.assertTrue(Enchantment.areCompatible(resilience, protection), "resilience should stack with protection");
		helper.assertTrue(resilience.is(tag("minecraft", "non_treasure")), "resilience should be in #minecraft:non_treasure");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void changedVanillaEnchants(GameTestHelper helper) {
		helper.assertTrue(enchant(helper, "minecraft", "protection").value().getMaxLevel() == 3, "protection max level should be 3");

		Enchantment thorns = enchant(helper, "minecraft", "thorns").value();
		helper.assertTrue(thorns.isSupportedItem(new ItemStack(Items.IRON_CHESTPLATE)), "thorns should fit chestplates");
		helper.assertTrue(!thorns.isSupportedItem(new ItemStack(Items.IRON_HELMET)), "thorns should not fit helmets");

		Enchantment depthStrider = enchant(helper, "minecraft", "depth_strider").value();
		helper.assertTrue(depthStrider.isSupportedItem(new ItemStack(Items.IRON_LEGGINGS)), "depth strider should fit leggings");
		helper.assertTrue(!depthStrider.isSupportedItem(new ItemStack(Items.IRON_BOOTS)), "depth strider should not fit boots");
		helper.succeed();
	}
}
