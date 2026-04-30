package ua.pp.shurgent.railward;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.AllInteractionBehaviours.interactionBehaviour;
import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MaterialColor;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerInteractionBehaviour;
import ua.pp.shurgent.railward.block.train.parts.TrainBufferBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerInteractionBehaviour;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainFloorBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlock;

public class AllBlocks {

	static final String PATH_TRAINPARTS = "block/train/parts/";
	static final String PATH_TRAINSIGNAL = "block/train/signal/";

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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_blue_window_big", TrainWallBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_blue_window_big", TrainWallPanelBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_GREEN_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_green_window_big", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_GREEN_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_green_window_big", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_GREEN))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallBlock> TRAIN_WALL_LIGHT_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_light_blue_window_big", properties -> new TrainWallBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
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
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

	public static final BlockEntry<TrainWallPanelBlock> TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG = REGISTRATE
			.block("train/parts/train_wall_panel_light_blue_window_big", properties -> new TrainWallPanelBlock(properties, MaterialColor.COLOR_LIGHT_BLUE))
			.initialProperties(() -> Blocks.IRON_BLOCK).addLayer(() -> RenderType::translucent)
			.properties(p -> p.noOcclusion().requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item((block, properties) -> new BlockItem(block, properties.tab(Railward.RAILWARD_TAB)))
			.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc(PATH_TRAINPARTS + ctx.getName())))
			.build().register();

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

	public static final BlockEntry<BulkCargoContainerBlock> BULK_CARGO_CONTAINER = REGISTRATE
			.block("train/parts/bulk_cargo_container", BulkCargoContainerBlock::new)
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false)
				.requiresCorrectToolForDrops())
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.onRegister(interactionBehaviour(new BulkCargoContainerInteractionBehaviour()))
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

	public static void register() {
	}

}
