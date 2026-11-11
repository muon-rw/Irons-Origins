package dev.muon.irons_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_origins.spell.BientitySpellCastAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {

    @ModifyReturnValue(method = "getLookAngle", at = @At("RETURN"))
    private Vec3 irons_origins$aimLookAngleAtSpellTarget(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getForward", at = @At("RETURN"))
    private Vec3 irons_origins$aimForwardAtSpellTarget(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getXRot", at = @At("RETURN"))
    private float irons_origins$aimXRotAtSpellTarget(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getYRot", at = @At("RETURN"))
    private float irons_origins$aimYRotAtSpellTarget(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }

    @ModifyReturnValue(method = "getRotationVector", at = @At("RETURN"))
    private Vec2 irons_origins$aimRotationVectorAtSpellTarget(Vec2 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.rotationVector() : original;
    }

    @ModifyReturnValue(method = "getViewVector", at = @At("RETURN"))
    private Vec3 irons_origins$aimViewVectorAtSpellTarget(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float irons_origins$aimViewXRotAtSpellTarget(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float irons_origins$aimViewYRotAtSpellTarget(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }
}
