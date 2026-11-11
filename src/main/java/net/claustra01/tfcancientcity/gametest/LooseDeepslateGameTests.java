package net.claustra01.tfcancientcity.gametest;

import java.util.List;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.block.LooseDeepslateBlock;
import net.claustra01.tfcancientcity.registry.ModBlocks;
import net.claustra01.tfcancientcity.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LooseDeepslateGameTests {
    @GameTest(template = "empty")
    public static void blockItemPlacesCountOneAndConsumesOne(GameTestHelper helper) {
        Item item = ModItems.LOOSE_DEEPSLATE.get();
        require(item instanceof BlockItem blockItem && blockItem.getBlock() == ModBlocks.LOOSE_DEEPSLATE.get(), "BlockItem does not reference Loose Deepslate");

        BlockPos supportPos = new BlockPos(1, 0, 1);
        helper.setBlock(supportPos, Blocks.STONE);
        helper.setBlock(supportPos.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(supportPos.above())));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
        BlockPos absoluteSupportPos = helper.absolutePos(supportPos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absoluteSupportPos).add(0, 0.5, 0), Direction.UP, absoluteSupportPos, false);
        BlockPlaceContext placementContext = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        require(placementContext.canPlace(), "placement context could not replace " + helper.getLevel().getBlockState(placementContext.getClickedPos()));
        require(ModBlocks.LOOSE_DEEPSLATE.get().getStateForPlacement(placementContext) != null,
            "Loose Deepslate could not survive over " + helper.getLevel().getBlockState(placementContext.getClickedPos().below()));
        InteractionResult placementResult = ((BlockItem)item).place(placementContext);

        require(placementResult.consumesAction(), "BlockItem rejected placement with " + placementResult);
        require(helper.getBlockState(supportPos.above()).is(ModBlocks.LOOSE_DEEPSLATE.get()), "Loose Deepslate was not placed");
        require(helper.getBlockState(supportPos.above()).getValue(LooseDeepslateBlock.COUNT) == 1, "placed count was not one");
        require(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "placement did not consume exactly one item");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void stacksToFourAndPreservesEveryCountOnRecovery(GameTestHelper helper) {
        Item item = ModItems.LOOSE_DEEPSLATE.get();
        BlockPos stackingPos = new BlockPos(1, 1, 1);
        helper.setBlock(stackingPos.below(), Blocks.STONE);
        helper.setBlock(stackingPos.relative(Direction.NORTH).below(), Blocks.STONE);
        helper.setBlock(stackingPos.relative(Direction.NORTH), Blocks.AIR);
        helper.setBlock(stackingPos, ModBlocks.LOOSE_DEEPSLATE.get().defaultBlockState());
        Player stackingPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        stackingPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(stackingPos)));
        stackingPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 4));
        for (int expected = 2; expected <= 4; expected++) {
            helper.useBlock(stackingPos, stackingPlayer);
            require(helper.getBlockState(stackingPos).getValue(LooseDeepslateBlock.COUNT) == expected, "stack did not reach count " + expected);
        }
        require(stackingPlayer.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "stacking consumed the wrong number of items");
        helper.useBlock(stackingPos, stackingPlayer);
        require(helper.getBlockState(stackingPos).getValue(LooseDeepslateBlock.COUNT) == 4, "a fifth rock changed the block state");
        require(helper.getBlockState(stackingPos.relative(Direction.NORTH)).isAir(), "a fifth rock was placed next to the full stack");
        require(stackingPlayer.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "a fifth rock was consumed");

        for (int count = 1; count <= 4; count++) {
            BlockPos pos = new BlockPos(count, 1, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, ModBlocks.LOOSE_DEEPSLATE.get().defaultBlockState().setValue(LooseDeepslateBlock.COUNT, count));
            Player recoveryPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
            recoveryPlayer.setPos(Vec3.atCenterOf(helper.absolutePos(pos)));
            helper.useBlock(pos, recoveryPlayer);
            require(helper.getBlockState(pos).isAir(), "bare-hand recovery did not remove count " + count);
            require(recoveryPlayer.getInventory().countItem(item) == count,
                "bare-hand recovery returned " + recoveryPlayer.getInventory().countItem(item) + " items for count " + count);
        }

        for (int count = 1; count <= 4; count++) {
            BlockPos pos = new BlockPos(count * 6, 1, 3);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, ModBlocks.LOOSE_DEEPSLATE.get().defaultBlockState().setValue(LooseDeepslateBlock.COUNT, count));
            helper.getLevel().destroyBlock(helper.absolutePos(pos), true, null);
        }

        helper.runAfterDelay(2, () -> {
            for (int count = 1; count <= 4; count++) {
                BlockPos pos = new BlockPos(count * 6, 1, 3);
                require(countLooseItems(helper, pos) == count, "normal breaking dropped the wrong count at " + pos);
            }
            looseItems(helper).forEach(ItemEntity::discard);

            for (int count = 1; count <= 4; count++) {
                BlockPos pos = new BlockPos(count * 6, 1, 4);
                helper.setBlock(pos.below(), Blocks.STONE);
                helper.setBlock(pos, ModBlocks.LOOSE_DEEPSLATE.get().defaultBlockState().setValue(LooseDeepslateBlock.COUNT, count));
                helper.getLevel().destroyBlock(helper.absolutePos(pos.below()), false, null);
            }
            helper.runAfterDelay(2, () -> {
                for (int count = 1; count <= 4; count++) {
                    BlockPos pos = new BlockPos(count * 6, 1, 4);
                    require(countLooseItems(helper, pos) == count, "support removal dropped the wrong count at " + pos);
                }
                helper.succeed();
            });
        });
    }

    private static int countLooseItems(GameTestHelper helper, BlockPos pos) {
        AABB dropArea = new AABB(helper.absolutePos(pos)).inflate(1.5);
        return helper.getLevel().getEntities(EntityType.ITEM, dropArea,
            entity -> entity.getItem().is(ModItems.LOOSE_DEEPSLATE.get())).stream()
            .mapToInt(entity -> entity.getItem().getCount())
            .sum();
    }

    private static List<ItemEntity> looseItems(GameTestHelper helper) {
        AABB dropArea = new AABB(
            Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
            Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(30, 4, 6)))
        );
        return helper.getLevel().getEntities(EntityType.ITEM, dropArea, entity -> entity.getItem().is(ModItems.LOOSE_DEEPSLATE.get()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private LooseDeepslateGameTests() {
    }
}
