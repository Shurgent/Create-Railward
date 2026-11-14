package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.19.2 {
/*import net.minecraftforge.client.model.data.ModelData;*/
//?} else {
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
//?}
import net.minecraftforge.client.model.data.ModelProperty;

public class WagonFrameBlockEntity extends SmartBlockEntity {

	public static final ModelProperty<FrameConfiguration> FRAME_CONFIGURATION = new ModelProperty<>();

	public record FrameConfiguration(List<WagonFrameMemberType> members, WagonFrameEquipmentType equipment,
			WagonFrameHandBrakeValveSide handBrakeValve) {
		public FrameConfiguration {
			members = List.copyOf(members);
		}

		public WagonFrameMemberType member(WagonFrameSide side) {
			return members.get(side.ordinal());
		}
	}

	private final WagonFrameMemberType[] members = new WagonFrameMemberType[WagonFrameSide.values().length];
	private WagonFrameEquipmentType equipment = WagonFrameEquipmentType.NONE;
	private WagonFrameHandBrakeValveSide handBrakeValve = WagonFrameHandBrakeValveSide.NONE;

	public WagonFrameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		for (WagonFrameSide side : WagonFrameSide.values())
			members[side.ordinal()] = WagonFrameMemberType.NONE;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public WagonFrameMemberType getMember(WagonFrameSide side) {
		return members[side.ordinal()];
	}

	public WagonFrameEquipmentType getEquipment() {
		return equipment;
	}

	public WagonFrameHandBrakeValveSide getHandBrakeValve() {
		return handBrakeValve;
	}

	public void cycleMember(WagonFrameSide side) {
		members[side.ordinal()] = members[side.ordinal()].next();
		configurationChanged();
	}

	public void cycleEquipment() {
		equipment = equipment.next();
		configurationChanged();
	}

	public void toggleHandBrakeValve(WagonFrameHandBrakeValveSide side) {
		if (side == WagonFrameHandBrakeValveSide.NONE)
			return;
		handBrakeValve = handBrakeValve == side ? WagonFrameHandBrakeValveSide.NONE : side;
		configurationChanged();
	}

	private void configurationChanged() {
		setChanged();
		if (level != null && !level.isClientSide)
			notifyUpdate();
	}

	@Override
	//? if >=1.19.2 {
	/*public ModelData getModelData() {
		ModelData.Builder builder = ModelData.builder();
	*///?} else {
	public IModelData getModelData() {
		ModelDataMap.Builder builder = new ModelDataMap.Builder();
	//?}
		return builder.withInitial(FRAME_CONFIGURATION,
			new FrameConfiguration(List.of(members), equipment, handBrakeValve)).build();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeConfiguration(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		writeConfiguration(tag);
	}

	private void writeConfiguration(CompoundTag tag) {
		CompoundTag membersTag = new CompoundTag();
		for (WagonFrameSide side : WagonFrameSide.values())
			membersTag.putString(side.getSerializedName(), getMember(side).getSerializedName());
		tag.put("Members", membersTag);
		tag.putString("Equipment", equipment.getSerializedName());
		tag.putString("HandBrakeValve", handBrakeValve.getSerializedName());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		CompoundTag membersTag = tag.getCompound("Members");
		boolean changed = false;
		for (WagonFrameSide side : WagonFrameSide.values()) {
			WagonFrameMemberType member = WagonFrameMemberType.byName(membersTag.getString(side.getSerializedName()));
			changed |= members[side.ordinal()] != member;
			members[side.ordinal()] = member;
		}
		WagonFrameEquipmentType nextEquipment = WagonFrameEquipmentType.byName(tag.getString("Equipment"));
		WagonFrameHandBrakeValveSide nextValve = WagonFrameHandBrakeValveSide.byName(tag.getString("HandBrakeValve"));
		changed |= equipment != nextEquipment || handBrakeValve != nextValve;
		equipment = nextEquipment;
		handBrakeValve = nextValve;
		if (clientPacket && changed && hasLevel() && !isVirtual()) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
		}
	}
}
