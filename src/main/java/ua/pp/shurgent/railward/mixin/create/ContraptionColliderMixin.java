package ua.pp.shurgent.railward.mixin.create;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.block.train.parts.RailingBlock;
import ua.pp.shurgent.railward.block.train.parts.RailingContraptionCollision;

@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class ContraptionColliderMixin {

	@Inject(method = "getPotentiallyCollidedShapes", at = @At("HEAD"), cancellable = true)
	private static void railward$getConfiguredRailingCollision(Level world, Contraption contraption, AABB localBB,
			CallbackInfoReturnable<List<VoxelShape>> cir) {
		double height = localBB.getYsize();
		double width = localBB.getXsize();
		double horizontalFactor = height > width && width != 0 ? height / width : 1;
		double verticalFactor = width > height && height != 0 ? width / height : 1;
		AABB blockScanBB = localBB.inflate(.5f)
			.inflate(horizontalFactor, verticalFactor, horizontalFactor);

		BlockPos min = new BlockPos(blockScanBB.minX, blockScanBB.minY, blockScanBB.minZ);
		BlockPos max = new BlockPos(blockScanBB.maxX, blockScanBB.maxY, blockScanBB.maxZ);
		List<BlockPos> candidates = new ArrayList<>();
		boolean containsRailing = false;

		for (BlockPos scannedPos : BlockPos.betweenClosed(min, max)) {
			BlockPos pos = scannedPos.immutable();
			StructureBlockInfo info = contraption.getBlocks().get(pos);
			if (info == null || contraption.isHiddenInPortal(pos))
				continue;
			candidates.add(pos);
			containsRailing |= info.state.getBlock() instanceof RailingBlock;
		}

		if (!containsRailing)
			return;

		List<VoxelShape> potentialHits = new ArrayList<>();
		for (BlockPos pos : candidates) {
			StructureBlockInfo info = contraption.getBlocks().get(pos);
			VoxelShape collisionShape = info.state.getBlock() instanceof RailingBlock
				? RailingContraptionCollision.shapeFrom(info)
				: info.state.getCollisionShape(world, pos);
			if (collisionShape == null || collisionShape.isEmpty())
				continue;
			potentialHits.add(collisionShape.move(info.pos.getX(), info.pos.getY(), info.pos.getZ()));
		}

		cir.setReturnValue(potentialHits);
	}
}
