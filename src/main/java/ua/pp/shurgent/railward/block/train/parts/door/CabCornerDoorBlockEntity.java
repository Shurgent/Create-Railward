package ua.pp.shurgent.railward.block.train.parts.door;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class CabCornerDoorBlockEntity extends SlidingDoorBlockEntity {

	private static final String LOWER_PANEL_KEY = "LowerPanel";
	private static final String UPPER_PANEL_KEY = "UpperPanel";

	private CabCornerDoorPanelVariant lowerPanel = CabCornerDoorPanelVariant.SMOOTH;
	private CabCornerDoorPanelVariant upperPanel = CabCornerDoorPanelVariant.SMOOTH;

	public CabCornerDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public CabCornerDoorPanelVariant getPanel(DoubleBlockHalf half) {
		return half == DoubleBlockHalf.UPPER ? upperPanel : lowerPanel;
	}

	public void cyclePanel(DoubleBlockHalf half) {
		if (half == DoubleBlockHalf.UPPER)
			upperPanel = upperPanel.next();
		else
			lowerPanel = lowerPanel.next();
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
		writePanels(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		writePanels(tag);
	}

	private void writePanels(CompoundTag tag) {
		tag.putString(LOWER_PANEL_KEY, lowerPanel.getSerializedName());
		tag.putString(UPPER_PANEL_KEY, upperPanel.getSerializedName());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		lowerPanel = CabCornerDoorPanelVariant.byName(tag.getString(LOWER_PANEL_KEY));
		upperPanel = CabCornerDoorPanelVariant.byName(tag.getString(UPPER_PANEL_KEY));
	}
}
