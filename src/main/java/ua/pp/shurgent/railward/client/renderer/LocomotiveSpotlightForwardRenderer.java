package ua.pp.shurgent.railward.client.renderer;

import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.FloatBuffer;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.system.MemoryStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.RailwardConfig;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState.DirectionState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightMovementBehaviour;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachmentRegistry;

public final class LocomotiveSpotlightForwardRenderer {

	public static final int MAX_VISIBLE_LIGHTS = 4;

	private static final String PANEL_ATTACHMENT_KEY = "PanelAttachment";
	private static final String COUNT_UNIFORM = "RailwardSpotlightCount";
	private static final String LENGTH_UNIFORM = "RailwardSpotlightLength";
	private static final String RADIUS_UNIFORM = "RailwardSpotlightRadius";
	private static final String CONTRAPTION_PASS_UNIFORM = "RailwardContraptionPass";
	private static final String INVERSE_VIEW_ROTATION_UNIFORM = "RailwardInverseViewRotation";
	private static final String LIGHT_SAMPLER_UNIFORM = "RailwardLightSampler";
	private static final Map<Contraption, List<LocalEmitter>> EMITTER_CACHE = new WeakHashMap<>();
	private static final Map<Integer, UniformLocations> UNIFORM_CACHE = new HashMap<>();

	private static List<WorldEmitter> activeEmitters = List.of();
	private static Vec3 cameraPosition = Vec3.ZERO;
	private static float beamLength = 64;
	private static float beamRadius = 16;
	private static boolean contraptionFallbackPass;

	private LocomotiveSpotlightForwardRenderer() {
	}

	public static void prepare(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !RailwardConfig.CLIENT.locomotiveSpotlightProjectedLight.get()) {
			activeEmitters = List.of();
			return;
		}

		beamLength = RailwardConfig.CLIENT.locomotiveSpotlightBeamLength.get();
		beamRadius = RailwardConfig.CLIENT.locomotiveSpotlightBeamWidth.get() * .5f;
		cameraPosition = event.getCamera().getPosition();
		activeEmitters = collectActiveEmitters(mc, event.getPartialTick(), event.getFrustum(), beamLength, beamRadius);
	}

	public static void uploadCurrentProgram(boolean absolutePositions) {
		int program = glGetInteger(GL_CURRENT_PROGRAM);
		if (program == 0)
			return;

		int countLocation = glGetUniformLocation(program, COUNT_UNIFORM);
		if (countLocation < 0) {
			UNIFORM_CACHE.remove(program);
			return;
		}
		UniformLocations uniforms = UNIFORM_CACHE.get(program);
		if (uniforms == null || uniforms.count() != countLocation) {
			uniforms = new UniformLocations(program, countLocation);
			UNIFORM_CACHE.put(program, uniforms);
		}
		if (!uniforms.isSupported())
			return;

		int count = Math.min(activeEmitters.size(), MAX_VISIBLE_LIGHTS);
		glUniform1i(uniforms.count(), count);
		glUniform1f(uniforms.length(), beamLength);
		glUniform1f(uniforms.radius(), beamRadius);
		if (uniforms.contraptionPass() >= 0) {
			glUniform1i(uniforms.contraptionPass(), contraptionFallbackPass ? 1 : 0);
		}
		if (uniforms.inverseViewRotation() >= 0) {
			try (MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer matrixBuffer = stack.mallocFloat(9);
				RenderSystem.getInverseViewRotationMatrix().store(matrixBuffer);
				glUniformMatrix3fv(uniforms.inverseViewRotation(), false, matrixBuffer);
			}
		}
		if (uniforms.lightSampler() >= 0)
			glUniform1i(uniforms.lightSampler(), 2);
		for (int i = 0; i < count; i++) {
			WorldEmitter emitter = activeEmitters.get(i);
			Vec3 position = absolutePositions ? emitter.origin() : emitter.origin().subtract(cameraPosition);
			glUniform3f(uniforms.positions()[i], (float)position.x, (float)position.y, (float)position.z);
			glUniform3f(uniforms.directions()[i], (float)emitter.direction().x, (float)emitter.direction().y,
				(float)emitter.direction().z);
		}
	}

	public static void setContraptionFallbackPass(boolean active) {
		contraptionFallbackPass = active;
	}

	public static boolean isContraptionFallbackPass() {
		return contraptionFallbackPass;
	}

	public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		activeEmitters = List.of();
		cameraPosition = Vec3.ZERO;
		EMITTER_CACHE.clear();
		UNIFORM_CACHE.clear();
		contraptionFallbackPass = false;
	}

	private static List<WorldEmitter> collectActiveEmitters(Minecraft mc, float partialTicks, Frustum frustum,
		float currentBeamLength, float currentBeamRadius) {
		List<WorldEmitter> emitters = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof CarriageContraptionEntity carriageEntity))
				continue;
			if (!(carriageEntity.getContraption() instanceof CarriageContraption carriageContraption))
				continue;
			if (LocomotiveSpotlightMovementBehaviour.isDisabled(carriageContraption))
				continue;

			DirectionState state = TrainDirectionState.resolve(carriageEntity);
			if (state == DirectionState.NONE)
				continue;

			Direction activeForward = state == DirectionState.FORWARD
				? carriageContraption.getAssemblyDirection()
				: carriageContraption.getAssemblyDirection().getOpposite();
			for (LocalEmitter local : EMITTER_CACHE.computeIfAbsent(carriageContraption,
				LocomotiveSpotlightForwardRenderer::findLocalEmitters)) {
				if (local.beamDirection().getOpposite() != activeForward)
					continue;

				Vec3 worldOrigin = carriageEntity.toGlobalVector(local.origin(), partialTicks);
				Vec3 localDirection = Vec3.atLowerCornerOf(local.beamDirection().getNormal());
				Vec3 worldDirection = carriageEntity.toGlobalVector(local.origin().add(localDirection), partialTicks)
					.subtract(worldOrigin)
					.normalize();
				Vec3 beamEnd = worldOrigin.add(worldDirection.scale(currentBeamLength));
				AABB bounds = new AABB(worldOrigin, beamEnd).inflate(currentBeamRadius);
				if (!frustum.isVisible(bounds))
					continue;

				emitters.add(new WorldEmitter(worldOrigin, worldDirection, worldOrigin.distanceToSqr(cameraPosition)));
			}
		}

		emitters.sort(Comparator.comparingDouble(WorldEmitter::distanceToCameraSqr));
		if (emitters.size() > MAX_VISIBLE_LIGHTS)
			return List.copyOf(emitters.subList(0, MAX_VISIBLE_LIGHTS));
		return List.copyOf(emitters);
	}

	private static List<LocalEmitter> findLocalEmitters(Contraption contraption) {
		List<LocalEmitter> emitters = new ArrayList<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
			BlockPos pos = entry.getKey();
			StructureBlockInfo info = entry.getValue();
			BlockState state = info.state;

			if (AllBlocks.LOCOMOTIVE_SPOTLIGHT.has(state)) {
				Direction beamDirection = state.getValue(LocomotiveSpotlightBlock.FACING);
				LocomotiveSpotlightVariant variant = state.getValue(LocomotiveSpotlightBlock.VARIANT);
				emitters.add(createLocalEmitter(pos, beamDirection, variant, .43));
				continue;
			}

			if (!AllBlocks.isAttachableTrainWallPanelState(state))
				continue;
			TrainPanelAttachment attachment = getPanelAttachment(contraption, pos, info);
			if (!(attachment instanceof LocomotiveSpotlightAttachment spotlight))
				continue;

			Direction beamDirection = state.getValue(TrainWallPanelBlock.FACING).getOpposite();
			emitters.add(createLocalEmitter(pos, beamDirection, spotlight.getVariant(), .25));
		}
		return List.copyOf(emitters);
	}

	@Nullable
	private static TrainPanelAttachment getPanelAttachment(Contraption contraption, BlockPos pos,
		StructureBlockInfo info) {
		BlockEntity blockEntity = ua.pp.shurgent.railward.client.compat.RailwardContraptionBlockEntities.get(contraption, pos);
		if (blockEntity instanceof TrainWallPanelBlockEntity panel && panel.getAttachment() != null)
			return panel.getAttachment();
		if (info.nbt != null && info.nbt.contains(PANEL_ATTACHMENT_KEY, Tag.TAG_COMPOUND))
			return TrainPanelAttachmentRegistry.read(info.nbt.getCompound(PANEL_ATTACHMENT_KEY));
		return null;
	}

	private static LocalEmitter createLocalEmitter(BlockPos pos, Direction beamDirection,
		LocomotiveSpotlightVariant variant, double normalOffset) {
		double y = variant == LocomotiveSpotlightVariant.LARGE ? .5 : .625;
		Vec3 direction = Vec3.atLowerCornerOf(beamDirection.getNormal());
		Vec3 origin = new Vec3(pos.getX() + .5, pos.getY() + y, pos.getZ() + .5)
			.subtract(direction.scale(normalOffset));
		return new LocalEmitter(origin, beamDirection);
	}

	private record LocalEmitter(Vec3 origin, Direction beamDirection) {
	}

	private record WorldEmitter(Vec3 origin, Vec3 direction, double distanceToCameraSqr) {
	}

	private record UniformLocations(int count, int length, int radius, int contraptionPass,
		int inverseViewRotation, int lightSampler, int[] positions, int[] directions) {
		private UniformLocations(int program, int countLocation) {
			this(countLocation, glGetUniformLocation(program, LENGTH_UNIFORM),
				glGetUniformLocation(program, RADIUS_UNIFORM), glGetUniformLocation(program, CONTRAPTION_PASS_UNIFORM),
				glGetUniformLocation(program, INVERSE_VIEW_ROTATION_UNIFORM),
				glGetUniformLocation(program, LIGHT_SAMPLER_UNIFORM),
				findLocations(program, "RailwardSpotlightPosition"),
				findLocations(program, "RailwardSpotlightDirection"));
		}

		private boolean isSupported() {
			return count >= 0 && length >= 0 && radius >= 0;
		}

		private static int[] findLocations(int program, String prefix) {
			int[] locations = new int[MAX_VISIBLE_LIGHTS];
			for (int i = 0; i < locations.length; i++)
				locations[i] = glGetUniformLocation(program, prefix + i);
			return locations;
		}
	}
}
