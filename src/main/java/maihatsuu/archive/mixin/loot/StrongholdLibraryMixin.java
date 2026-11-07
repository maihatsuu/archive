package maihatsuu.archive.mixin.loot;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.tome.LibraryShelves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.util.RandomSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Swaps some of a stronghold library's bookshelves for chiseled bookshelves holding books and tomes
 * ({@link LibraryShelves} picks them). A piece is generated one chunk at a time, with a different random
 * per chunk, so the plan comes from a random seeded by the world seed and the piece's position: every
 * chunk computes the same plan and places the shelves that fall inside it.
 * <p>
 * The shelves replace the library's chests: both are skipped, along with the air block vanilla carves
 * above the upper-floor chest.
 */
@Mixin(StrongholdPieces.Library.class)
public abstract class StrongholdLibraryMixin extends StructurePiece {
	@Shadow
	@Final
	private boolean isTall;

	private StrongholdLibraryMixin(StructurePieceType type, int depth, BoundingBox box) {
		super(type, depth, box);
	}

	@WrapOperation(method = "postProcess", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$Library;createChest(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;IIILnet/minecraft/resources/ResourceKey;)Z"))
	private boolean archive$noChests(StrongholdPieces.Library piece, WorldGenLevel level, BoundingBox box, RandomSource random,
			int x, int y, int z, ResourceKey<LootTable> table, Operation<Boolean> original) {
		return !ArchiveConfig.get().strongholdShelves && original.call(piece, level, box, random, x, y, z, table);
	}

	@WrapOperation(method = "postProcess", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/structures/StrongholdPieces$Library;placeBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/block/state/BlockState;IIILnet/minecraft/world/level/levelgen/structure/BoundingBox;)V"))
	private void archive$noChestGap(StrongholdPieces.Library piece, WorldGenLevel level, BlockState state, int x, int y, int z,
			BoundingBox box, Operation<Void> original) {
		// The only cave air the library places is the gap above its upper-floor chest.
		if (!(state.is(Blocks.CAVE_AIR) && ArchiveConfig.get().strongholdShelves)) {
			original.call(piece, level, state, x, y, z, box);
		}
	}

	@Inject(method = "postProcess", at = @At("TAIL"))
	private void archive$chiseledShelves(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource chunkRandom, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci) {
		if (!ArchiveConfig.get().strongholdShelves) {
			return;
		}
		BoundingBox piece = this.boundingBox;
		RandomSource random = RandomSource.create(level.getSeed() ^ Mth.getSeed(piece.minX(), piece.minY(), piece.minZ()));
		List<LibraryShelves.Shelf> shelves = LibraryShelves.plan(this.isTall, random, level.getLevel());
		for (LibraryShelves.Shelf shelf : shelves) {
			BlockPos pos = this.getWorldPos(shelf.x(), shelf.y(), shelf.z());
			if (!chunkBox.isInside(pos) || !level.getBlockState(pos).is(Blocks.BOOKSHELF)) {
				continue;
			}
			BlockState state = Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, shelf.facing());
			for (int slot = 0; slot < 6; slot++) {
				state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot), !shelf.books().get(slot).isEmpty());
			}
			// placeBlock turns the local facing by the piece's rotation and mirror.
			this.placeBlock(level, state, shelf.x(), shelf.y(), shelf.z(), chunkBox);
			// The block entity has no level yet during world gen, so setItem (which updates the block) can't be used.
			if (level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity entity) {
				NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
				for (int slot = 0; slot < 6; slot++) {
					items.set(slot, shelf.books().get(slot));
				}
				CompoundTag tag = new CompoundTag();
				ContainerHelper.saveAllItems(tag, items, true, level.registryAccess());
				entity.loadCustomOnly(tag, level.registryAccess());
				entity.setChanged();
			}
		}
	}
}
