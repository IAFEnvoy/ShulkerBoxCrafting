package iafenvoy.shulkerboxcrafting.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin {
    private boolean isOnce;

    @Inject(method = "fromTag", at = @At("HEAD"))
    public void fromTag(BlockState state, NbtCompound tag, CallbackInfo ci) {
        if (tag.contains("isOnce"))
            isOnce = tag.getInt("isOnce") > 0;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    public void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        System.out.println(cir.getReturnValue());
    }

    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    public void canInsert(int slot, ItemStack stack, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (isOnce)
            cir.setReturnValue(false);
    }
}
