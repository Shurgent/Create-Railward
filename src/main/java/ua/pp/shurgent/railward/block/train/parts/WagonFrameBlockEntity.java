package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WagonFrameBlockEntity extends SmartBlockEntity {

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
		for (WagonFrameSide side : WagonFrameSide.values())
			members[side.ordinal()] = WagonFrameMemberType.byName(membersTag.getString(side.getSerializedName()));
		equipment = WagonFrameEquipmentType.byName(tag.getString("Equipment"));
		handBrakeValve = WagonFrameHandBrakeValveSide.byName(tag.getString("HandBrakeValve"));
	}
}
