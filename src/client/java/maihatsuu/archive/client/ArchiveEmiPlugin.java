package maihatsuu.archive.client;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import maihatsuu.archive.Archive;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.RepairMaterials;
import maihatsuu.archive.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * EMI entrypoint, only loaded when EMI is installed. Makes EMI's recipe list match Archive: removes the
 * anvil combining and book recipes, grindstone repairs and crafting-grid repairs, redraws material
 * repairs as full repairs and disenchanting on Archive's single-slot grindstone, drops them for unbreakable gear, and adds the repair materials EMI doesn't
 * know about (it hardcodes repairs for tool tiers, armor, the elytra and the shield, not even the mace). Enchanted books are hidden through the
 * {@code #c:hidden_from_recipe_viewers} tag, which JEI and REI read too.
 */
public final class ArchiveEmiPlugin implements EmiPlugin {
	@Override
	public void register(EmiRegistry registry) {
		ArchiveConfig config = ArchiveConfig.get();
		registry.removeRecipes(recipe -> {
			ResourceLocation id = recipe.getId();
			if (id == null || !id.getNamespace().equals("emi")) {
				return false;
			}
			String path = id.getPath();
			if (config.anvil && (path.startsWith("/anvil/repairing/") || path.startsWith("/anvil/enchanting/")
					|| path.startsWith("/crafting/repairing/"))) {
				// Material repairs included: EMI draws them as vanilla's 25% per unit, so they're re-added below.
				return true;
			}
			if (config.grindstone && path.startsWith("/grindstone/")) {
				// Disenchanting included: EMI draws vanilla's two-slot grindstone, so it's re-added below.
				return true;
			}
			return config.durability && path.startsWith("/anvil/repairing/material/") && isUnbreakable(recipe);
		});

		// With the anvil rework on, every material repair is Archive's (one unit, full repair). Otherwise
		// EMI's own vanilla ones stay and only the materials it doesn't know about are added.
		Map<Item, Ingredient> materials = new LinkedHashMap<>();
		if (config.anvil) {
			for (Item item : BuiltInRegistries.ITEM) {
				Ingredient material = vanillaMaterial(item);
				if (material != null && !material.isEmpty()) {
					materials.put(item, material);
				}
			}
		}
		if (config.durability) {
			RepairMaterials.MAP.forEach((tool, material) -> materials.put(tool, Ingredient.of(material)));
			if (!config.anvil) {
				// Vanilla repairs the mace with breeze rods, but EMI doesn't list it.
				materials.put(Items.MACE, Ingredient.of(Items.BREEZE_ROD));
			}
		}
		materials.forEach((tool, material) -> {
			ItemStack stack = new ItemStack(tool);
			if (stack.getMaxDamage() > 0 && !stack.has(DataComponents.UNBREAKABLE)) {
				registry.addRecipe(new RepairRecipe(tool, EmiIngredient.of(material), config.anvil));
			}
		});

		ClientLevel level = Minecraft.getInstance().level;
		if (config.grindstone && level != null) {
			List<Holder<Enchantment>> enchants = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders()
					.filter(e -> !e.is(ModTags.UNOBTAINABLE) && !e.is(EnchantmentTags.CURSE))
					.<Holder<Enchantment>>map(e -> e).toList();
			for (Item item : BuiltInRegistries.ITEM) {
				ItemStack stack = new ItemStack(item);
				List<Holder<Enchantment>> fitting = enchants.stream().filter(e -> e.value().isSupportedItem(stack)).toList();
				if (!fitting.isEmpty() && stack.isEnchantable()) {
					registry.addRecipe(new DisenchantRecipe(item, fitting));
				}
			}
		}
	}

	/** The repair material vanilla gives an item, found the way EMI finds it, plus the mace. */
	private static Ingredient vanillaMaterial(Item item) {
		if (item instanceof ArmorItem armor) {
			return armor.getMaterial().value().repairIngredient().get();
		} else if (item instanceof TieredItem tiered) {
			return tiered.getTier().getRepairIngredient();
		} else if (item == Items.ELYTRA) {
			return Ingredient.of(Items.PHANTOM_MEMBRANE);
		} else if (item == Items.SHIELD) {
			return Ingredient.of(ItemTags.PLANKS);
		} else if (item == Items.MACE) {
			return Ingredient.of(Items.BREEZE_ROD);
		}
		return null;
	}

	private static boolean isUnbreakable(EmiRecipe recipe) {
		return recipe.getOutputs().stream().anyMatch(stack -> stack.getItemStack().has(DataComponents.UNBREAKABLE));
	}

	/** An anvil repair drawn like EMI's own: one unit fully repairs (Archive's anvil) or restores 25% (vanilla's). */
	private static final class RepairRecipe extends BasicEmiRecipe {
		private final Item tool;
		private final boolean full;

		RepairRecipe(Item tool, EmiIngredient material, boolean full) {
			super(VanillaEmiRecipeCategories.ANVIL_REPAIRING, Archive.id("/anvil_repair/"
					+ BuiltInRegistries.ITEM.getKey(tool).toString().replace(':', '/')), 125, 18);
			this.tool = tool;
			this.full = full;
			this.inputs = List.of(EmiStack.of(tool), material);
			this.outputs = List.of(EmiStack.of(tool));
		}

		@Override
		public boolean supportsRecipeTree() {
			return false;
		}

		@Override
		public void addWidgets(WidgetHolder widgets) {
			ItemStack damaged = new ItemStack(tool);
			damaged.setDamageValue(damaged.getMaxDamage() * 3 / 4);
			ItemStack repaired = new ItemStack(tool);
			repaired.setDamageValue(full ? 0 : damaged.getDamageValue() - damaged.getMaxDamage() / 4);
			widgets.addTexture(EmiTexture.PLUS, 27, 3);
			widgets.addTexture(EmiTexture.EMPTY_ARROW, 75, 1);
			widgets.addSlot(EmiStack.of(damaged), 0, 0);
			widgets.addSlot(inputs.get(1), 49, 0);
			widgets.addSlot(EmiStack.of(repaired), 107, 0).recipeContext(this);
		}
	}

	/** Archive's single-slot grindstone: an enchanted item in, the same item without its enchants out. */
	private static final class DisenchantRecipe extends BasicEmiRecipe {
		private static final ResourceLocation BACKGROUND = Archive.id("textures/gui/container/grindstone.png");
		private final int unique = new Random().nextInt();
		private final Item tool;
		private final List<Holder<Enchantment>> enchants;

		DisenchantRecipe(Item tool, List<Holder<Enchantment>> enchants) {
			super(VanillaEmiRecipeCategories.GRINDING, Archive.id("/disenchant/"
					+ BuiltInRegistries.ITEM.getKey(tool).toString().replace(':', '/')), 116, 56);
			this.tool = tool;
			this.enchants = enchants;
			this.inputs = List.of(EmiStack.of(tool));
			this.outputs = List.of(EmiStack.of(tool));
		}

		@Override
		public boolean supportsRecipeTree() {
			return false;
		}

		@Override
		public void addWidgets(WidgetHolder widgets) {
			// The same crop of the screen texture EMI uses for vanilla's grindstone, so slots sit 31, 16 left/up.
			widgets.addTexture(BACKGROUND, 0, 0, 116, 56, 30, 15);
			widgets.addGeneratedSlot(this::enchanted, unique, 18, 14).drawBack(false);
			widgets.addSlot(EmiStack.of(tool), 98, 18).drawBack(false).recipeContext(this);
		}

		/** One to three compatible enchants at random levels, like EMI's own samples. */
		private EmiIngredient enchanted(Random random) {
			ItemStack stack = new ItemStack(tool);
			List<Holder<Enchantment>> chosen = new ArrayList<>();
			int count = 1 + random.nextInt(3);
			for (int i = 0; i < count * 4 && chosen.size() < count; i++) {
				Holder<Enchantment> enchant = enchants.get(random.nextInt(enchants.size()));
				if (chosen.stream().allMatch(other -> Enchantment.areCompatible(other, enchant))) {
					chosen.add(enchant);
					stack.enchant(enchant, 1 + random.nextInt(enchant.value().getMaxLevel()));
				}
			}
			return EmiStack.of(stack);
		}
	}
}
