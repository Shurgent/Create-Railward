package ua.pp.shurgent.railward;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.AllInteractionBehaviours.interactionBehaviour;
import static com.simibubi.create.AllMovementBehaviours.movementBehaviour;
import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInteractionBehaviour;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import ua.pp.shurgent.railward.block.train.bogey.FreightBogey18_100Block;
import ua.pp.shurgent.railward.block.train.bogey.TripleAxleEqualizedLocomotiveBogeyBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerInteractionBehaviour;
import ua.pp.shurgent.railward.block.train.parts.CabSignalBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockItem;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightMovementBehaviour;
import ua.pp.shurgent.railward.block.train.parts.PilotBlock;
import ua.pp.shurgent.railward.block.train.parts.PilotType;
import ua.pp.shurgent.railward.block.train.parts.TrainBufferBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerInteractionBehaviour;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlock;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerMovementBehaviour;
import ua.pp.shurgent.railward.block.train.parts.RailingBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainFloorBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofSlopeBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatSlabBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelCornerBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelHorizontalSliceBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelVerticalSliceBlock;
import ua.pp.shurgent.railward.block.train.parts.door.TrainDoorAnimation;
import ua.pp.shurgent.railward.block.train.parts.door.TrainDoorBlock;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlock;
import ua.pp.shurgent.railward.block.train.station.CisternHatchMovement;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlock;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlock;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlock;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchMovement;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlock;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlock;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlock;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlockItem;
import ua.pp.shurgent.railward.client.model.TrainRoofModelRegistration;
import ua.pp.shurgent.railward.client.model.WallPanelCopycatModelRegistration;

public class AllBlocks {

	static final String PATH_TRAINPARTS = "block/train/parts/";
	static final String PATH_TRAINSIGNAL = "block/train/signal/";
	private static final List<BlockEntry<TrainWallPanelBlock>> TRAIN_WALL_PANEL_ENTRIES = new ArrayList<>();
	private static final List<BlockEntry<TrainWallPanelBlock>> TRAIN_WALL_PANEL_ATTACHABLE_ENTRIES = new ArrayList<>();
	private static final LocomotiveSpotlightMovementBehaviour LOCOMOTIVE_SPOTLIGHT_MOVEMENT =
		new LocomotiveSpotlightMovementBehaviour();
	private static final ModernTrainControllerMovementBehaviour MODERN_TRAIN_CONTROLLER_MOVEMENT =
		new ModernTrainControllerMovementBehaviour();
	private static final List<BlockEntry<TrainDoorBlock>> HINGED_TRAIN_DOOR_ENTRIES = new ArrayList<>();
	private static final List<BlockEntry<TrainDoorBlock>> SLIDING_TRAIN_DOOR_ENTRIES = new ArrayList<>();
	private static final DyeColor[] TRAIN_DOOR_SOLID_COLORS = {
			DyeColor.BLUE, DyeColor.GREEN, DyeColor.LIGHT_BLUE
	};
	private static final DyeColor[] TRAIN_DOOR_WINDOW_COLORS = {
			DyeColor.BLUE, DyeColor.GREEN, DyeColor.LIGHT_BLUE
	};
	private static final String[] TRAIN_DOOR_STRIPPED_WOOD_TYPES = {
			"stripped_oak",
			"stripped_spruce",
			"stripped_birch",
			"stripped_jungle",
			"stripped_acacia",
			"stripped_dark_oak",
			"stripped_crimson",
			"stripped_warped"
	};

	public static final BlockEntry<FreightBogey18_100Block> FREIGHT_BOGEY_18_100 = REGISTRATE
			.block("train/bogey/freight_18_100", FreightBogey18_100Block::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.color(MaterialColor.PODZOL).sound(SoundType.NETHERITE_BLOCK).noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.register();

	public static final BlockEntry<TripleAxleEqualizedLocomotiveBogeyBlock>
			TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY = REGISTRATE
			.block("train/bogey/triple_axle_equalized_locomotive",
					TripleAxleEqualizedLocomotiveBogeyBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.color(MaterialColor.PODZOL).sound(SoundType.NETHERITE_BLOCK).noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.register();

	static {
		registerTrainDoorSet("train_hinged_door", TrainDoorAnimation.HINGED, HINGED_TRAIN_DOOR_ENTRIES);
		registerTrainDoorSet("train_sliding_door", TrainDoorAnimation.SLIDING, SLIDING_TRAIN_DOOR_ENTRIES);
	}

	public static final BlockEntry<WallPanelVerticalSliceBlock> WALL_PANEL_VERTICAL_SLICE = REGISTRATE
			.block("train/parts/wall_panel_vertical_slice", WallPanelVerticalSliceBlock::new)
			.transform(BuilderTransformers.copycat())
			.onRegister(WallPanelCopycatModelRegistration.verticalSlice())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
					prov.modLoc(PATH_TRAINPARTS + "copycat_base/wall_panel_vertical_slice")))
			.build()
			.register();

	public static final BlockEntry<WallPanelHorizontalSliceBlock> WALL_PANEL_HORIZONTAL_SLICE = REGISTRATE
			.block("train/parts/wall_panel_horizontal_slice", WallPanelHorizontalSliceBlock::new)
			.transform(BuilderTransformers.copycat())
			.onRegister(WallPanelCopycatModelRegistration.horizontalSlice())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
					prov.modLoc(PATH_TRAINPARTS + "copycat_base/wall_panel_horizontal_slice")))
			.build()
			.register();

	public static final BlockEntry<WallPanelCornerBlock> WALL_PANEL_CORNER = REGISTRATE
			.block("train/parts/wall_panel_corner", WallPanelCornerBlock::new)
			.transform(BuilderTransformers.copycat())
			.onRegister(WallPanelCopycatModelRegistration.corner())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
					prov.modLoc(PATH_TRAINPARTS + "copycat_base/wall_panel_corner")))
			.build()
			.register();

	public static final BlockEntry<HalfWallPanelBlock> HALF_WALL_PANEL = REGISTRATE
			.block("train/parts/half_wall_panel", HalfWallPanelBlock::new)
			.transform(BuilderTransformers.copycat())
			.onRegister(WallPanelCopycatModelRegistration.halfPanel())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
					prov.modLoc(PATH_TRAINPARTS + "copycat_base/half_wall_panel")))
			.build()
			.register();

	public static final BlockEntry<CopycatSlabBlock> COPYCAT_SLAB = REGISTRATE
			.block("train/parts/copycat_slab", CopycatSlabBlock::new)
			.transform(BuilderTransformers.copycat())
			.onRegister(WallPanelCopycatModelRegistration.slab())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
					prov.modLoc(PATH_TRAINPARTS + "copycat_base/copycat_slab")))
			.build()
			.register();

	public static final BlockEntry<TrainFloorBlock> TRAIN_FLOOR_BLUE = REGISTRATE
			.block("train/parts/train_floor_blue", TrainFloorBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainFloorBlock> TRAIN_FLOOR_GREEN = REGISTRATE
			.block("train/parts/train_floor_green", properties -> new TrainFloorBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainFloorBlock> TRAIN_FLOOR_LIGHT_BLUE = REGISTRATE
			.block("train/parts/train_floor_light_blue", properties -> new TrainFloorBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE = REGISTRATE
			.block("train/parts/train_wall_blue", TrainWallBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_blue_stripe_yellow", TrainWallBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_blue_stripe_red", TrainWallBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_blue_window_small", TrainWallBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_blue_window_big", TrainWallBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE = REGISTRATE
			.block("train/parts/train_wall_panel_blue", TrainWallPanelBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_panel_blue_stripe_yellow", TrainWallPanelBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_panel_blue_stripe_red", TrainWallPanelBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();
	
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_panel_blue_window_small", TrainWallPanelBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_blue_window_big", TrainWallPanelBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN = REGISTRATE
			.block("train/parts/train_wall_green", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_green_stripe_yellow", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_green_stripe_red", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_green_window_small", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_green_window_big", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN = REGISTRATE
			.block("train/parts/train_wall_panel_green", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_panel_green_stripe_yellow", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_panel_green_stripe_red", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_panel_green_window_small", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_green_window_big", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE = REGISTRATE
			.block("train/parts/train_wall_light_blue", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_light_blue_stripe_yellow", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_light_blue_stripe_red", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_light_blue_window_small", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_light_blue_window_big", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue_stripe_yellow", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue_stripe_red", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue_window_small", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue_window_big", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::cutout)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_SMOOTH = trainWallVariant("train_wall_blue_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_HOOD = trainWallVariant("train_wall_blue_hood", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_GRILLE = trainWallVariant("train_wall_blue_grille", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_YELLOW_SMOOTH = trainWallVariant("train_wall_blue_stripe_yellow_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_YELLOW_GRILLE = trainWallVariant("train_wall_blue_stripe_yellow_grille", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_RED_SMOOTH = trainWallVariant("train_wall_blue_stripe_red_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_STRIPE_RED_DOUBLE_SMOOTH = trainWallVariant("train_wall_blue_stripe_red_double_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_SMALL_SMOOTH = trainWallVariant("train_wall_blue_window_small_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_ROUND = trainWallVariant("train_wall_blue_window_round", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_ROUND_SMOOTH = trainWallVariant("train_wall_blue_window_round_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_BIG_SMOOTH = trainWallVariant("train_wall_blue_window_big_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_BIG_FRAME_WHITE = trainWallVariant("train_wall_blue_window_big_frame_white", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallVariant("train_wall_blue_window_big_smooth_frame_white", MaterialColor.COLOR_BLUE, true);

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_HOOD = trainWallPanelVariant("train_wall_panel_blue_hood", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_GRILLE = trainWallPanelVariant("train_wall_panel_blue_grille", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_stripe_yellow_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW_GRILLE = trainWallPanelVariant("train_wall_panel_blue_stripe_yellow_grille", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_RED_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_stripe_red_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_STRIPE_RED_DOUBLE_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_stripe_red_double_smooth", MaterialColor.COLOR_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_window_small_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_ROUND = trainWallPanelVariant("train_wall_panel_blue_window_round", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_ROUND_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_window_round_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_BIG_SMOOTH = trainWallPanelVariant("train_wall_panel_blue_window_big_smooth", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_BIG_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_blue_window_big_frame_white", MaterialColor.COLOR_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_blue_window_big_smooth_frame_white", MaterialColor.COLOR_BLUE, true);

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_SMOOTH = trainWallVariant("train_wall_green_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_HOOD = trainWallVariant("train_wall_green_hood", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_GRILLE = trainWallVariant("train_wall_green_grille", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_YELLOW_SMOOTH = trainWallVariant("train_wall_green_stripe_yellow_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_YELLOW_GRILLE = trainWallVariant("train_wall_green_stripe_yellow_grille", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_RED_SMOOTH = trainWallVariant("train_wall_green_stripe_red_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_STRIPE_RED_DOUBLE_SMOOTH = trainWallVariant("train_wall_green_stripe_red_double_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_SMALL_SMOOTH = trainWallVariant("train_wall_green_window_small_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_ROUND = trainWallVariant("train_wall_green_window_round", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_ROUND_SMOOTH = trainWallVariant("train_wall_green_window_round_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_BIG_SMOOTH = trainWallVariant("train_wall_green_window_big_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_BIG_FRAME_WHITE = trainWallVariant("train_wall_green_window_big_frame_white", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallVariant("train_wall_green_window_big_smooth_frame_white", MaterialColor.COLOR_GREEN, true);

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_SMOOTH = trainWallPanelVariant("train_wall_panel_green_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_HOOD = trainWallPanelVariant("train_wall_panel_green_hood", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_GRILLE = trainWallPanelVariant("train_wall_panel_green_grille", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW_SMOOTH = trainWallPanelVariant("train_wall_panel_green_stripe_yellow_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW_GRILLE = trainWallPanelVariant("train_wall_panel_green_stripe_yellow_grille", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_RED_SMOOTH = trainWallPanelVariant("train_wall_panel_green_stripe_red_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_STRIPE_RED_DOUBLE_SMOOTH = trainWallPanelVariant("train_wall_panel_green_stripe_red_double_smooth", MaterialColor.COLOR_GREEN, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL_SMOOTH = trainWallPanelVariant("train_wall_panel_green_window_small_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_ROUND = trainWallPanelVariant("train_wall_panel_green_window_round", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_ROUND_SMOOTH = trainWallPanelVariant("train_wall_panel_green_window_round_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_BIG_SMOOTH = trainWallPanelVariant("train_wall_panel_green_window_big_smooth", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_BIG_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_green_window_big_frame_white", MaterialColor.COLOR_GREEN, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_green_window_big_smooth_frame_white", MaterialColor.COLOR_GREEN, true);

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_SMOOTH = trainWallVariant("train_wall_light_blue_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_HOOD = trainWallVariant("train_wall_light_blue_hood", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_GRILLE = trainWallVariant("train_wall_light_blue_grille", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_YELLOW_SMOOTH = trainWallVariant("train_wall_light_blue_stripe_yellow_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_YELLOW_GRILLE = trainWallVariant("train_wall_light_blue_stripe_yellow_grille", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_RED_SMOOTH = trainWallVariant("train_wall_light_blue_stripe_red_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_STRIPE_RED_DOUBLE_SMOOTH = trainWallVariant("train_wall_light_blue_stripe_red_double_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_SMALL_SMOOTH = trainWallVariant("train_wall_light_blue_window_small_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_ROUND = trainWallVariant("train_wall_light_blue_window_round", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_ROUND_SMOOTH = trainWallVariant("train_wall_light_blue_window_round_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_BIG_SMOOTH = trainWallVariant("train_wall_light_blue_window_big_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_BIG_FRAME_WHITE = trainWallVariant("train_wall_light_blue_window_big_frame_white", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallVariant("train_wall_light_blue_window_big_smooth_frame_white", MaterialColor.COLOR_LIGHT_BLUE, true);

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_HOOD = trainWallPanelVariant("train_wall_panel_light_blue_hood", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_GRILLE = trainWallPanelVariant("train_wall_panel_light_blue_grille", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_stripe_yellow_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW_GRILLE = trainWallPanelVariant("train_wall_panel_light_blue_stripe_yellow_grille", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_stripe_red_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED_DOUBLE_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_stripe_red_double_smooth", MaterialColor.COLOR_LIGHT_BLUE, false);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_window_small_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_ROUND = trainWallPanelVariant("train_wall_panel_light_blue_window_round", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_ROUND_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_window_round_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG_SMOOTH = trainWallPanelVariant("train_wall_panel_light_blue_window_big_smooth", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_light_blue_window_big_frame_white", MaterialColor.COLOR_LIGHT_BLUE, true);
	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG_SMOOTH_FRAME_WHITE = trainWallPanelVariant("train_wall_panel_light_blue_window_big_smooth_frame_white", MaterialColor.COLOR_LIGHT_BLUE, true);

	public static final BlockEntry<TrainRoofBlock> TRAIN_ROOF_LIGHT_BLUE = REGISTRATE
			.block("train/parts/train_roof_light_blue", TrainRoofBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.onRegister(TrainRoofModelRegistration.create())
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainRoofSlopeBlock> TRAIN_ROOF_SLOPE_LIGHT_BLUE = REGISTRATE
			.block("train/parts/train_roof_slope_light_blue", TrainRoofSlopeBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<LocomotiveBufferLightsBlock> LOCOMOTIVE_BUFFER_LIGHTS = REGISTRATE
			.block("train/parts/locomotive_buffer_lights", LocomotiveBufferLightsBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.addLayer(() -> RenderType::translucent)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<ModernTrainControllerBlock> MODERN_TRAIN_CONTROLLER = REGISTRATE
			.block("train/parts/modern_train_controller", ModernTrainControllerBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.properties(p -> p.color(MaterialColor.TERRACOTTA_BLUE)
				.sound(SoundType.NETHERITE_BLOCK)
				.noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.addLayer(() -> RenderType::cutoutMipped)
			.blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.get()).forAllStates(state -> {
				String modelName = state.getValue(ControlsBlock.OPEN) ? "block_open" : "block_closed";
				ModelFile model = prov.models().getExistingFile(
					prov.modLoc(PATH_TRAINPARTS + "modern_train_controller/" + modelName));
				int rotation = switch (ModernTrainControllerBlock.getPhysicalFacing(state)) {
				case EAST -> 90;
				case SOUTH -> 180;
				case WEST -> 270;
				default -> 0;
				};
				return ConfiguredModel.builder().modelFile(model).rotationY(rotation).build();
			}))
			.onRegister(movementBehaviour(MODERN_TRAIN_CONTROLLER_MOVEMENT))
			.onRegister(interactionBehaviour(new ControlsInteractionBehaviour()))
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
				prov.modLoc(PATH_TRAINPARTS + "modern_train_controller/block_closed")))
			.build().register();

	public static final BlockEntry<LocomotiveSpotlightBlock> LOCOMOTIVE_SPOTLIGHT = REGISTRATE
			.block("train/parts/locomotive_spotlight", LocomotiveSpotlightBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.addLayer(() -> RenderType::translucent)
			.onRegister(movementBehaviour(LOCOMOTIVE_SPOTLIGHT_MOVEMENT))
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.transform(customItemModel()).register();

	public static final BlockEntry<CabSignalBlock> CAB_SIGNAL = REGISTRATE
			.block("train/parts/cab_signal", CabSignalBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.addLayer(() -> RenderType::translucent)
			.item((block, properties) -> new SurfaceMountedBlockItem(block,
				properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<PilotBlock> PILOT_TYPE_1 = REGISTRATE
			.block("train/parts/pilot_type_1", properties -> new PilotBlock(properties, PilotType.TYPE_1))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<PilotBlock> PILOT_TYPE_2 = REGISTRATE
			.block("train/parts/pilot_type_2", properties -> new PilotBlock(properties, PilotType.TYPE_2))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<TrainBufferBlock> TRAIN_BUFFER = REGISTRATE
			.block("train/parts/train_buffer", TrainBufferBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops().noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainCouplerBlock> TRAIN_COUPLER = REGISTRATE
			.block("train/parts/train_coupler", TrainCouplerBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.requiresCorrectToolForDrops().noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.onRegister(interactionBehaviour(new TrainCouplerInteractionBehaviour()))
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<ModernWhistleBlock> MODERN_WHISTLE = REGISTRATE
			.block("train/parts/modern_whistle", ModernWhistleBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<WagonFrameBlock> WAGON_FRAME = REGISTRATE
			.block("train/parts/wagon_frame", WagonFrameBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<RailingBlock> RAILING_RED =
			railing("railing_red", "red", MaterialColor.COLOR_RED, 0xb43a32);
	public static final BlockEntry<RailingBlock> RAILING_YELLOW =
			railing("railing_yellow", "yellow", MaterialColor.COLOR_YELLOW, 0xe6b944);
	public static final BlockEntry<RailingBlock> RAILING_ORANGE =
			railing("railing_orange", "orange", MaterialColor.COLOR_ORANGE, 0xdb762b);
	public static final BlockEntry<RailingBlock> RAILING_WHITE =
			railing("railing_white", "white", MaterialColor.SNOW, 0xe4e4de);
	public static final BlockEntry<RailingBlock> RAILING_GRAY =
			railing("railing_gray", "gray", MaterialColor.COLOR_GRAY, 0x777d82);

	public static final BlockEntry<BulkCargoContainerBlock> BULK_CARGO_CONTAINER =
			bulkCargoContainer("bulk_cargo_container");

	public static final BlockEntry<BulkCargoContainerBlock> BULK_CARGO_CONTAINER_BLUE =
			bulkCargoContainer("bulk_cargo_container_blue");

	public static final BlockEntry<BulkCargoContainerBlock> BULK_CARGO_CONTAINER_LIGHT_BLUE =
			bulkCargoContainer("bulk_cargo_container_light_blue");

	public static final BlockEntry<CisternBlock> CISTERN = cistern("cistern");
	public static final BlockEntry<CisternBlock> CISTERN_BLUE = cistern("cistern_blue");
	public static final BlockEntry<CisternBlock> CISTERN_LIGHT_BLUE = cistern("cistern_light_blue");
	public static final BlockEntry<CisternBlock> CISTERN_BLACK = cistern("cistern_black");
	public static final BlockEntry<CisternBlock> CISTERN_LIGHT_GRAY = cistern("cistern_light_gray");
	public static final BlockEntry<CisternBlock> CISTERN_YELLOW = cistern("cistern_yellow");
	public static final BlockEntry<CisternBlock> CISTERN_ORANGE = cistern("cistern_orange");

	public static final BlockEntry<CisternHatchBlock> CISTERN_HATCH = REGISTRATE
			.block("train/station/cistern_hatch", CisternHatchBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops()
				.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false))
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.onRegister(movementBehaviour(new CisternHatchMovement()))
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<FluidTerminalBlock> FLUID_TERMINAL = REGISTRATE
			.block("train/station/fluid_terminal", FluidTerminalBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops()
				.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false))
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<UnloadingHatchBlock> UNLOADING_HATCH = REGISTRATE
			.block("train/station/unloading_hatch", UnloadingHatchBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.onRegister(movementBehaviour(new UnloadingHatchMovement()))
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<UnloadingBunkerBlock> UNLOADING_BUNKER = REGISTRATE
			.block("train/station/unloading_bunker", UnloadingBunkerBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<ModernSignalGroundBlock> MODERN_SIGNAL_GROUND = REGISTRATE
			.block("train/signal/modern_signal_ground", ModernSignalGroundBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).properties(p -> p.noOcclusion())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.addLayer(() -> RenderType::translucent)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<ModernSignalBlock> MODERN_SIGNAL = REGISTRATE
			.block("train/signal/modern_signal", ModernSignalBlock::new).initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion()).addLayer(() -> RenderType::translucent)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.transform(customItemModel()).register();

	public static final BlockEntry<WhistleSignBlock> WHISTLE_SIGN = REGISTRATE
			.block("train/signal/whistle_sign", WhistleSignBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.addLayer(() -> RenderType::cutout)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new TrackTargetingBlockItem(block,
				properties.tab(Railward.RAILWARD_TAB), AllEdgePointTypes.WHISTLE_SIGN))
			.transform(customItemModel()).register();

	public static final BlockEntry<SpeedSignBlock> SPEED_SIGN = REGISTRATE
			.block("train/signal/speed_sign", SpeedSignBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.addLayer(() -> RenderType::cutout)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new TrackTargetingBlockItem(block,
				properties.tab(Railward.RAILWARD_TAB), AllEdgePointTypes.SPEED_SIGN))
			.transform(customItemModel()).register();

	public static void register() {
		AllMovementBehaviours.registerBehaviourProvider(state ->
			isAttachableTrainWallPanelState(state) ? LOCOMOTIVE_SPOTLIGHT_MOVEMENT : null);
	}

	public static boolean isTrainWallPanelVariantItem(ItemStack stack) {
		return TRAIN_WALL_PANEL_ENTRIES.stream()
				.anyMatch(entry -> entry.isIn(stack));
	}

	public static boolean isTrainWallPanelVariantState(BlockState state) {
		return TRAIN_WALL_PANEL_ENTRIES.stream()
				.anyMatch(entry -> entry.has(state));
	}

	public static boolean isAttachableTrainWallPanelState(BlockState state) {
		return TRAIN_WALL_PANEL_BLUE.has(state)
				|| TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW.has(state)
				|| TRAIN_WALL_PANEL_BLUE_STRIPE_RED.has(state)
				|| TRAIN_WALL_PANEL_GREEN.has(state)
				|| TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW.has(state)
				|| TRAIN_WALL_PANEL_GREEN_STRIPE_RED.has(state)
				|| TRAIN_WALL_PANEL_LIGHT_BLUE.has(state)
				|| TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW.has(state)
				|| TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED.has(state)
				|| TRAIN_WALL_PANEL_ATTACHABLE_ENTRIES.stream()
						.anyMatch(entry -> entry.has(state));
	}

	@SuppressWarnings("unchecked")
	public static NonNullSupplier<? extends net.minecraft.world.level.block.Block>[] trainWallPanelBlocks() {
		List<NonNullSupplier<? extends net.minecraft.world.level.block.Block>> blocks = new ArrayList<>();
		blocks.add(TRAIN_WALL_PANEL_BLUE);
		blocks.add(TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW);
		blocks.add(TRAIN_WALL_PANEL_BLUE_STRIPE_RED);
		blocks.add(TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL);
		blocks.add(TRAIN_WALL_PANEL_BLUE_WINDOW_BIG);
		blocks.add(TRAIN_WALL_PANEL_GREEN);
		blocks.add(TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW);
		blocks.add(TRAIN_WALL_PANEL_GREEN_STRIPE_RED);
		blocks.add(TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL);
		blocks.add(TRAIN_WALL_PANEL_GREEN_WINDOW_BIG);
		blocks.add(TRAIN_WALL_PANEL_LIGHT_BLUE);
		blocks.add(TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW);
		blocks.add(TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED);
		blocks.add(TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL);
		blocks.add(TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG);
		blocks.addAll(TRAIN_WALL_PANEL_ENTRIES);
		return blocks.toArray(new NonNullSupplier[0]);
	}

	@SuppressWarnings("unchecked")
	public static NonNullSupplier<? extends net.minecraft.world.level.block.Block>[] hingedTrainDoorBlocks() {
		return HINGED_TRAIN_DOOR_ENTRIES.toArray(new NonNullSupplier[0]);
	}

	@SuppressWarnings("unchecked")
	public static NonNullSupplier<? extends net.minecraft.world.level.block.Block>[] slidingTrainDoorBlocks() {
		return SLIDING_TRAIN_DOOR_ENTRIES.toArray(new NonNullSupplier[0]);
	}

	private static void registerTrainDoorSet(String prefix, TrainDoorAnimation animation,
			List<BlockEntry<TrainDoorBlock>> entries) {
		for (DyeColor color : TRAIN_DOOR_SOLID_COLORS)
			entries.add(trainDoor(prefix + "_" + color.getName(), animation, color.getMaterialColor()));

		for (String wood : TRAIN_DOOR_STRIPPED_WOOD_TYPES)
			entries.add(trainDoor(prefix + "_" + wood, animation, MaterialColor.WOOD));

		for (DyeColor color : TRAIN_DOOR_WINDOW_COLORS) {
			entries.add(trainDoor(prefix + "_" + color.getName() + "_window_small", animation,
					color.getMaterialColor()));
			entries.add(trainDoor(prefix + "_" + color.getName() + "_window_big", animation,
					color.getMaterialColor()));
		}
	}

	private static BlockEntry<TrainDoorBlock> trainDoor(String name, TrainDoorAnimation animation,
			MaterialColor color) {
		return REGISTRATE.block("train/parts/" + name, properties -> new TrainDoorBlock(properties, animation))
				.initialProperties(() -> Blocks.OAK_DOOR)
				.properties(p -> p.color(color)
						.sound(SoundType.NETHERITE_BLOCK)
						.strength(3.0f, 6.0f)
						.noOcclusion()
						.requiresCorrectToolForDrops())
				.blockstate((ctx, prov) -> {
					ModelFile bottom = prov.models()
							.getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block_bottom"));
					ModelFile top = prov.models()
							.getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block_top"));
					prov.doorBlock(ctx.get(), bottom, bottom, top, top);
				})
				.addLayer(() -> RenderType::cutout)
				.tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DOORS, BlockTags.WOODEN_DOORS,
						AllBlockTags.NON_DOUBLE_DOOR.tag)
				.onRegister(interactionBehaviour(new DoorMovingInteraction()))
				.onRegister(movementBehaviour(new SlidingDoorMovementBehaviour()))
				.loot((loot, block) -> loot.add(block, BlockLoot.createDoorTable(block)))
				.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.tag(ItemTags.DOORS, AllItemTags.CONTRAPTION_CONTROLLED.tag)
				.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.mcLoc("item/generated"))
						.texture("layer0", prov.modLoc("item/" + ctx.getName())))
				.build()
				.register();
	}

	private static BlockEntry<TrainWallBlock> trainWallVariant(String name, MaterialColor color, boolean cutout) {
		var builder = REGISTRATE.block("train/parts/" + name, properties -> new TrainWallBlock(properties, color))
				.initialProperties(() -> Blocks.IRON_BLOCK);

		if (cutout)
			builder = builder.addLayer(() -> RenderType::cutout);

		return builder
				.properties(p -> cutout ? p.noOcclusion().requiresCorrectToolForDrops()
						: p.requiresCorrectToolForDrops())
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
				.build().register();
	}

	private static BlockEntry<TrainWallPanelBlock> trainWallPanelVariant(String name, MaterialColor color,
			boolean cutout) {
		var builder = REGISTRATE.block("train/parts/" + name,
				properties -> new TrainWallPanelBlock(properties, color))
				.initialProperties(() -> Blocks.IRON_BLOCK);

		if (cutout)
			builder = builder.addLayer(() -> RenderType::cutout);

		BlockEntry<TrainWallPanelBlock> entry = builder
				.properties(p -> cutout ? p.noOcclusion().requiresCorrectToolForDrops()
						: p.requiresCorrectToolForDrops())
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
				.build().register();
		TRAIN_WALL_PANEL_ENTRIES.add(entry);
		if (!cutout)
			TRAIN_WALL_PANEL_ATTACHABLE_ENTRIES.add(entry);
		return entry;
	}

	private static BlockEntry<BulkCargoContainerBlock> bulkCargoContainer(String name) {
		return REGISTRATE.block("train/parts/" + name, BulkCargoContainerBlock::new)
				.initialProperties(() -> Blocks.IRON_BLOCK)
				.properties(p -> p.isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false)
					.isViewBlocking((state, level, pos) -> false))
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.onRegister(interactionBehaviour(new BulkCargoContainerInteractionBehaviour()))
				.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.transform(customItemModel()).register();
	}

	private static BlockEntry<CisternBlock> cistern(String name) {
		return REGISTRATE.block("train/parts/" + name, CisternBlock::new)
				.initialProperties(() -> Blocks.IRON_BLOCK)
				.properties(p -> p.noOcclusion()
					.requiresCorrectToolForDrops()
					.isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false)
					.isViewBlocking((state, level, pos) -> false))
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.item((block, properties) -> new CisternBlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.transform(customItemModel()).register();
	}

	private static BlockEntry<RailingBlock> railing(String name, String colorName, MaterialColor color,
			int renderColor) {
		return REGISTRATE
				.block("train/parts/" + name,
					properties -> new RailingBlock(properties, colorName, renderColor))
				.initialProperties(() -> Blocks.IRON_BLOCK)
				.properties(p -> p.color(color)
					.noOcclusion()
					.requiresCorrectToolForDrops()
					.isRedstoneConductor((state, level, pos) -> false)
					.isSuffocating((state, level, pos) -> false)
					.isViewBlocking((state, level, pos) -> false))
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
				.transform(customItemModel())
				.register();
	}

}
