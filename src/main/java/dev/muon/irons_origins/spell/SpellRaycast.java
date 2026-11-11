package dev.muon.irons_origins.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SpellRaycast {

    private SpellRaycast() {
    }

    /**
     * Finds the living entity the caster looks at. The ray passes through grass, flowers, and other blocks without
     * collision or hardness; multipart entities resolve to their parent.
     */
    @Nullable
    public static LivingEntity findLookTarget(LivingEntity caster, double distance) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        HitResult blockHit = clipIgnoringPassableBlocks(caster.level(), eyePos, eyePos.add(look.scale(distance)));
        double searchDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : distance;
        Vec3 searchEnd = eyePos.add(look.scale(searchDist));
        AABB searchBox = caster.getBoundingBox().expandTowards(look.scale(searchDist)).inflate(1.0);

        LivingEntity closest = null;
        double closestDist = searchDist;
        for (Entity entity : caster.level().getEntities(caster, searchBox, e -> !e.isSpectator() && e.isPickable())) {
            LivingEntity living = asLiving(entity);
            if (living == null) {
                continue;
            }
            Optional<Vec3> hit = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(eyePos, searchEnd);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = living;
                }
            }
        }
        return closest;
    }

    @Nullable
    private static LivingEntity asLiving(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return null;
    }

    private static HitResult clipIgnoringPassableBlocks(BlockGetter level, Vec3 start, Vec3 end) {
        return BlockGetter.traverseBlocks(start, end, null, (context, pos) -> {
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty() || state.getDestroySpeed(level, pos) == 0.0F) {
                return null;
            }
            return shape.clip(start, end, pos);
        }, context -> {
            Vec3 direction = start.subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(end));
        });
    }
}
