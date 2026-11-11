package dev.muon.irons_origins.spell;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * While a frame is active on the server thread, {@link dev.muon.irons_origins.mixin.EntityMixin} steers the caster's
 * reported facing ({@code getLookAngle}, {@code getForward}, yaw/pitch, {@code getViewVector}) toward a targeted
 * cast's target, so spells that aim along the caster's look direction hit it. Nested casts push and pop on a stack.
 * <p>
 * When no cast is active anywhere on the server, the mixin's hot path costs a single volatile read.
 */
public final class BientitySpellCastAim {

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    // Counter across all threads. push/pop happen on the server thread; the mixin reads on any thread.
    private static volatile int activeDepth = 0;

    private BientitySpellCastAim() {
    }

    public static void push(LivingEntity caster, LivingEntity target) {
        STACK.get().push(Frame.fromEyeToEye(caster, target));
        activeDepth++;
    }

    public static void pop() {
        Deque<Frame> deque = STACK.get();
        if (!deque.isEmpty()) {
            deque.pop();
            activeDepth--;
        }
    }

    @Nullable
    public static Frame activeFrameOrNull(Entity entity) {
        if (activeDepth == 0) {
            return null;
        }
        if (entity.level().isClientSide()) {
            return null;
        }
        Deque<Frame> deque = STACK.get();
        if (deque.isEmpty()) {
            return null;
        }
        Frame top = deque.peek();
        return top.casterId().equals(entity.getUUID()) ? top : null;
    }

    public record Frame(UUID casterId, Vec3 lookDirection, float xRot, float yRot, Vec2 rotationVector) {

        static Frame fromEyeToEye(LivingEntity caster, LivingEntity target) {
            Vec3 delta = target.getEyePosition().subtract(caster.getEyePosition());
            if (delta.lengthSqr() < 1e-8) {
                delta = target.position().subtract(caster.position());
            }
            Vec3 look = delta.lengthSqr() < 1e-8 ? new Vec3(0.0, 0.0, 1.0) : delta.normalize();
            float xRot = pitchOf(look);
            float yRot = yawOf(look);
            return new Frame(caster.getUUID(), look, xRot, yRot, new Vec2(xRot, yRot));
        }

        // Inverse of Entity.calculateViewVector, so getLookAngle() and the rotation getters agree.
        private static float pitchOf(Vec3 direction) {
            return (float) (Mth.RAD_TO_DEG * Math.asin(Mth.clamp(-direction.y, -1.0, 1.0)));
        }

        private static float yawOf(Vec3 direction) {
            double horizontalSqr = direction.x * direction.x + direction.z * direction.z;
            return horizontalSqr < 1.0E-8 ? 0.0F : (float) (Mth.RAD_TO_DEG * Mth.atan2(-direction.x, direction.z));
        }
    }
}
