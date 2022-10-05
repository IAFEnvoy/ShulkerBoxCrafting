package iafenvoy.shulkerboxcrafting.mixins;

import iafenvoy.shulkerboxcrafting.ShulkerBoxCraftRecipe;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "getFirstMatch", at = @At("HEAD"), cancellable = true)
    public <C extends Inventory, T extends Recipe<C>> void handleRecipe(RecipeType<T> type, C inventory, World world, CallbackInfoReturnable<Optional<T>> cir) {
        if (type != RecipeType.CRAFTING) return;
        if (inventory instanceof CraftingInventory) {
            CraftingInventory craft = (CraftingInventory) inventory;
            try {
                CraftingRecipe recipe = ShulkerBoxCraftRecipe.create(craft, world);
                if (recipe != null)
                    cir.setReturnValue(Optional.of((T) recipe));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
