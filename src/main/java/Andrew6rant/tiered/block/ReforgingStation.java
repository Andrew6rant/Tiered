package Andrew6rant.tiered.block;

import Andrew6rant.tiered.Tiered;
import Andrew6rant.tiered.api.ModifierUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ReforgingStation extends BarrelBlock implements BlockEntityProvider {
    public static final DirectionProperty FACING;
    public static final BooleanProperty OPEN;

    public ReforgingStation(AbstractBlock.Settings settings) {
        super(settings.nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState blockState, BlockView view, BlockPos pos, ShapeContext context) {
        Direction dir = blockState.get(Properties.FACING);
        switch (dir) {
            case NORTH, SOUTH, UP, DOWN -> {
                VoxelShape shape = VoxelShapes.empty();
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(-0.0625, 0, 0, 1.0625, 0.0625, 1), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0, 0.0625, 0.0625, 1, 1, 0.9375), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(-0.25, 0.8125, 0, 0.0625, 1.25, 1), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.9375, 0.8125, 0, 1.25, 1.25, 1), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.0625, 1, 0, 0.9375, 1.25, 1), BooleanBiFunction.OR);
                return shape;
            }
            case EAST, WEST -> {
                VoxelShape shape = VoxelShapes.empty();
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0, 0, -0.0625, 1, 0.0625, 1.0625), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0.0625, 0.0625, 0, 0.9375, 1, 1), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0, 0.8125, 0.9375, 1, 1.25, 1.25), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0, 0.8125, -0.25, 1, 1.25, 0.0625), BooleanBiFunction.OR);
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.cuboid(0, 1, 0.0625, 1, 1.25, 0.9375), BooleanBiFunction.OR);
                return shape;
            }
        };
        return null;
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FACING, OPEN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        BarrelBlockEntity reforgingStationBlockEntity = new BarrelBlockEntity(pos, state);
        reforgingStationBlockEntity.setCustomName(Text.translatable("container.reforging_station"));
        return reforgingStationBlockEntity;
    }

    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit){
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        else {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if(((hit.getPos().x - pos.getX()) > 0.2
                && (hit.getPos().x - pos.getX()) < 0.81
                && (hit.getPos().y - pos.getY()) > 0.17
                && (hit.getPos().y - pos.getY()) < 0.71
                && (hit.getPos().z - pos.getZ()) == 0.0625
                && ((blockState.get(Properties.FACING) == Direction.NORTH) || (blockState.get(Properties.FACING) == Direction.UP) || (blockState.get(Properties.FACING) == Direction.DOWN)))
                ||
                ((hit.getPos().x - pos.getX()) > 0.2
                &&(hit.getPos().x - pos.getX()) < 0.81
                && (hit.getPos().y - pos.getY()) > 0.17
                && (hit.getPos().y - pos.getY()) < 0.71
                && (hit.getPos().z - pos.getZ()) == 0.9375
                && (blockState.get(Properties.FACING) == Direction.SOUTH))
                ||
                ((hit.getPos().x - pos.getX()) == 0.9375
                && (hit.getPos().y - pos.getY()) > 0.17
                && (hit.getPos().y - pos.getY()) < 0.71
                && (hit.getPos().z - pos.getZ()) > 0.2
                && (hit.getPos().z - pos.getZ()) < 0.81
                && (blockState.get(Properties.FACING) == Direction.EAST))
                ||
                ((hit.getPos().x - pos.getX()) == 0.0625
                && (hit.getPos().y - pos.getY()) > 0.17
                && (hit.getPos().y - pos.getY()) < 0.71
                && (hit.getPos().z - pos.getZ()) > 0.2
                && (hit.getPos().z - pos.getZ()) < 0.81
                && (blockState.get(Properties.FACING) == Direction.WEST))) {
                    // If the player hit the drawers section of the table
                    player.openHandledScreen((BarrelBlockEntity)blockEntity);
                    return ActionResult.CONSUME;
            }
            if(stack.getSubNbt(Tiered.NBT_SUBTAG_KEY) != null && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                if (!player.isCreative() && (player.experienceLevel == 0 && (MathHelper.floor(player.experienceProgress * (float)player.getNextLevelExperience())) < Tiered.reforgeCostGetter(stack))) {
                    // super strange that Mojang made a nice method for finding xp levels but not points
                    player.playSound(SoundEvents.BLOCK_WOOD_HIT, 1, 1);
                    player.sendMessage(Text.translatable("message.tiered.no_xp"), true);
                } else {
                    if (!player.isCreative()) {
                        player.addExperience(-Tiered.reforgeCostGetter(stack)); // this is the negative of reforge_cost
                    }
                    Identifier potentialAttributeID = ModifierUtils.getWeightedAttributeIDFor(stack);
                    if(potentialAttributeID != null) {
                        stack.getOrCreateSubNbt(Tiered.NBT_SUBTAG_KEY).putString(Tiered.NBT_SUBTAG_DATA_KEY, potentialAttributeID.toString());
                    }
                    player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.FAIL;
    }

    static {
        FACING = Properties.FACING;
        OPEN = Properties.OPEN;
    }
}
