package com.github.harunanoda.flyingtree;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FlyingBlockEntity extends FallingBlockEntity {

    // 新しいコンストラクタを追加し、FallingBlockEntityのコンストラクタを呼び出す
    public FlyingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3d velocity = this.getVelocity();
        // Constant upward velocity
        this.setVelocity(velocity.x, 0.5, velocity.z);

        // Prevent block from becoming an item
        this.timeFalling = 1;

        // Use 'noClip' field directly for collision
        this.noClip = !FlyingTreeMod.COLLISION_ENABLED;

        // Discard entity when it reaches a certain height
        if (this.getY() > 1000.0) {
            this.discard();
        }
    }
}
