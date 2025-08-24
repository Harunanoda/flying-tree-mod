package com.github.harunanoda.flyingtree;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class FlyingTreeMod implements ModInitializer {

    public static final String MOD_ID = "flying-tree";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean COLLISION_ENABLED = true;

    // EntityTypeの登録。ファクトリーメソッドとしてFallingBlockEntity::newを使用
    public static final EntityType<FlyingBlockEntity> FLYING_BLOCK_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "flying_block"),
            FabricEntityTypeBuilder.<FlyingBlockEntity>create(SpawnGroup.MISC, FlyingBlockEntity::new)
                    .dimensions(EntityDimensions.fixed(0.98f, 0.98f))
                    .trackRangeBlocks(128)
                    .build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Flying Tree Mod is loaded!");

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            BlockState state = world.getBlockState(pos);
            if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) {
                Queue<BlockPos> queue = new ArrayDeque<>();
                Set<BlockPos> visited = new HashSet<>();
                queue.add(pos);

                int count = 0;
                int maxBlocks = 500;

                while (!queue.isEmpty() && count < maxBlocks) {
                    BlockPos currentPos = queue.poll();
                    if (visited.contains(currentPos)) {
                        continue;
                    }

                    BlockState currentState = world.getBlockState(currentPos);

                    if (currentState.isIn(BlockTags.LOGS) || currentState.isIn(BlockTags.LEAVES)) {
                        visited.add(currentPos);
                        count++;

                        world.removeBlock(currentPos, false);

                        // FallingBlockEntityをスポーンさせるための推奨される静的メソッドを使用
                        // これにより、コンストラクタの引数の型やアクセス修飾子の問題を回避できます
                        FallingBlockEntity fbe = FallingBlockEntity.spawnFromBlock(world, currentPos, currentState);

                        if (fbe != null) {
                            fbe.setVelocity(0, 0.5, 0);
                            world.spawnEntity(fbe);
                        }

                        for (Direction dir : Direction.values()) {
                            queue.add(currentPos.offset(dir));
                        }
                    }
                }
            }

            return ActionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("flyingtree");

            LiteralArgumentBuilder<ServerCommandSource> collision = CommandManager.literal("collision")
                    .then(CommandManager.literal("on")
                            .executes(context -> {
                                COLLISION_ENABLED = true;
                                context.getSource().sendMessage(Text.literal("木の当たり判定が有効になりました。"));
                                return 1;
                            }))
                    .then(CommandManager.literal("off")
                            .executes(context -> {
                                COLLISION_ENABLED = false;
                                context.getSource().sendMessage(Text.literal("木の当たり判定が無効になりました。"));
                                return 1;
                            }));

            dispatcher.register(root.then(collision));
        });
    }
}
