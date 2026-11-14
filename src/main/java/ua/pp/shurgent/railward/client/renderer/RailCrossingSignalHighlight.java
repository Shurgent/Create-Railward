package ua.pp.shurgent.railward.client.renderer;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
//? if create6
/*import net.createmod.catnip.outliner.Outliner;*/

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalItem;
import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;
import ua.pp.shurgent.railward.client.compat.RailwardValueBoxTransform;
import ua.pp.shurgent.railward.compat.RailwardComponents;

public final class RailCrossingSignalHighlight {

	private static final ResourceLocation SETTINGS_OUTLINE = Railward.asResource("crossing_settings");
	private static final ResourceLocation CABINET_OUTLINE = Railward.asResource("crossing_cabinet");
	private static final String SETTINGS = "railward.rail_crossing_signal.configuration";
	private static final RailwardValueBoxTransform SETTINGS_TRANSFORM = new RailwardValueBoxTransform() {
		@Override
		public Vec3 getLocalOffset(BlockState state) {
			return new Vec3(.5, .5, .5).add(Vec3.atLowerCornerOf(
				state.getValue(RailCrossingSignalBlock.FACING).getNormal()).scale(1.25 / 16d));
		}

		@Override
		public void rotate(BlockState state, PoseStack pose) {
			RailwardTransformStack.cast(pose)
				.rotateY(180 - state.getValue(RailCrossingSignalBlock.FACING).toYRot());
		}

		@Override
		public boolean testHit(BlockState state, Vec3 localHit) {
			return RailCrossingSignalBlock.isConfigurationOffset(state, localHit);
		}

		@Override
		public float getScale() {
			return .25f;
		}
	};

	private RailCrossingSignalHighlight() {
	}

	public static void tick(ClientTickEvent event) {
		if (event.phase != Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		updateSettings(level, player, mc.screen == null ? mc.hitResult : null);
		updateCabinet(level, player);
	}

	private static void updateSettings(ClientLevel level, LocalPlayer player, HitResult target) {
		var outliner = outliner();
		if (level != null && player != null && player.mayBuild() && !player.isSpectator()
			&& target instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (AllBlocks.RAIL_CROSSING_SIGNAL.has(state)
				&& hit.getDirection() == state.getValue(RailCrossingSignalBlock.FACING)) {
				ValueBox box = new ValueBox(RailwardComponents.translatable(SETTINGS),
					new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5), pos, state)
						.transform(SETTINGS_TRANSFORM).wideOutline();
				//? if create6
				/*outliner.showOutline(SETTINGS_OUTLINE, box);*/
				//? if !create6
				outliner.showValueBox(SETTINGS_OUTLINE, box);
				if (RailCrossingSignalBlock.isConfigurationHit(state, hit))
					CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(List.of(RailwardComponents.translatable(SETTINGS),
						RailwardComponents.translatable("railward.rail_crossing_signal.right_click_to_configure")));
				return;
			}
		}
		outliner.remove(SETTINGS_OUTLINE);
	}

	private static void updateCabinet(ClientLevel level, LocalPlayer player) {
		var outliner = outliner();
		if (level != null && player != null && !player.isSpectator()) {
			BlockPos cabinet = selectedCabinet(level, player.getMainHandItem());
			if (cabinet == null)
				cabinet = selectedCabinet(level, player.getOffhandItem());
			if (cabinet != null) {
				outliner.showAABB(CABINET_OUTLINE, level.getBlockState(cabinet).getShape(level, cabinet)
					.bounds().move(cabinet)).colored(0xffcb74).lineWidth(1 / 16f);
				return;
			}
		}
		outliner.remove(CABINET_OUTLINE);
	}

	private static BlockPos selectedCabinet(ClientLevel level, ItemStack stack) {
		if (!(stack.getItem() instanceof RailCrossingSignalItem))
			return null;
		BlockPos pos = RailCrossingSignalItem.selectedCabinet(stack, level);
		return pos != null && level.isLoaded(pos) && AllBlocks.RELAY_CASE.has(level.getBlockState(pos)) ? pos : null;
	}

	//? if create6 {
	/*private static Outliner outliner() {
		return Outliner.getInstance();
	}
	*///?} else {
	private static com.simibubi.create.foundation.outliner.Outliner outliner() {
		return CreateClient.OUTLINER;
	}
	//?}
}
