package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlock;
import ua.pp.shurgent.railward.block.train.station.CisternHatchMovement;

public class CisternHatchActorInstance extends ActorInstance {

	private static final Vec3 LID_PIVOT = new Vec3(.5, 3.5 / 16f, 3 / 16f);
	private static final float OPEN_ANGLE = -80;

	private final ModelData lid;
	private final float angleY;

	public CisternHatchActorInstance(MaterialManager materialManager, VirtualRenderWorld world,
		MovementContext context) {
		super(materialManager, world, context);

		Direction facing = context.state.hasProperty(CisternHatchBlock.FACING)
			? context.state.getValue(CisternHatchBlock.FACING)
			: Direction.NORTH;
		angleY = blockStateRotation(facing);
		lid = materialManager.defaultSolid()
			.material(Materials.TRANSFORMED)
			.getModel(AllPartialModels.CISTERN_HATCH_LID, context.state)
			.createInstance();
		lid.setBlockLight(localBlockLight());
	}

	@Override
	public void beginFrame() {
		float progress = CisternHatchMovement.getAnimation(context)
			.getValue(AnimationTickHolder.getPartialTicks());
		float eased = progress * progress;
		lid.loadIdentity()
			.translate(context.localPos)
			.centre()
			.rotateY(angleY)
			.unCentre()
			.translate(LID_PIVOT)
			.rotateX(OPEN_ANGLE * eased)
			.translateBack(LID_PIVOT);
	}

	private static int blockStateRotation(Direction facing) {
		return switch (facing) {
		case SOUTH -> 180;
		case WEST -> 90;
		case EAST -> 270;
		default -> 0;
		};
	}
}
