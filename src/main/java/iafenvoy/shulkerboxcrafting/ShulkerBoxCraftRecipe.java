package iafenvoy.shulkerboxcrafting;

import com.ibm.icu.impl.Pair;
import iafenvoy.shulkerboxcrafting.mixins.CraftingInventoryAccessor;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class ShulkerBoxCraftRecipe extends ShapedRecipe {
    private final DefaultedList<Pair<Item, Integer>> items;

    public ShulkerBoxCraftRecipe(CraftingRecipe recipe, DefaultedList<Pair<Item, Integer>> items, DefaultedList<Ingredient> input, ItemStack output) {
        super(new Identifier("sbc", recipe.getId().getPath()), recipe.getGroup(), 3, 3, input, output);
        this.items = items;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(CraftingInventory craft) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craft.getStack(i);
            Pair<Item, Integer> item = pickShulkerBoxItem(stack);
            if (item == null) continue;
            assert item.first == this.items.get(i).first;
            remainder.set(i, buildShulkerBox(item.first, item.second - this.items.get(i).second));
        }
        return remainder;
    }

    private static Pair<Item, Integer> pickShulkerBoxItem(ItemStack item) {
        if (!(item.getItem() instanceof BlockItem)) return null;
        BlockItem block = ((BlockItem) item.getItem());
        if (!(block.getBlock() instanceof ShulkerBoxBlock)) return null;
        NbtCompound nbt = item.getSubTag("BlockEntityTag");
        assert nbt != null;
        DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
        if (nbt.contains("Items", 9))
            Inventories.readNbt(nbt, inventory);
        Item base = null;
        int count = 0;
        for (int i = 0; i < 27; i++)
            if (!inventory.get(i).isEmpty()) {
                if (base == null) base = inventory.get(i).getItem();
                else if (base != inventory.get(i).getItem()) return null;
                count += inventory.get(i).getCount();
            }
        if (base == null) return null;
        return Pair.of(base, count);
    }

    private static ItemStack buildShulkerBox(Item item, int count) {
        ItemStack stack = new ItemStack(Items.SHULKER_BOX);
        DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
        int flag = 0;
        while (count > 0 && flag < 27) {
            inventory.set(flag, new ItemStack(item, Math.min(count, item.getMaxCount())));
            count -= item.getMaxCount();
            flag++;
        }
        if (count > 0) throw new RuntimeException("Items count should be 0");
        NbtCompound compound = new NbtCompound();
        Inventories.writeNbt(compound, inventory, true);
        stack.getOrCreateSubTag("BlockEntityTag").put("Items", compound.get("Items"));
        stack.getOrCreateSubTag("BlockEntityTag").put("isOnce", NbtInt.of(1));
        return stack;
    }

    public static ShulkerBoxCraftRecipe create(CraftingInventory craft, World world) {
        //判断是否执行
        if (world.isClient) return null;

        //拆包潜影盒+计算最少的数量
        int minCount = Integer.MAX_VALUE;
        CraftingInventory items = new CraftingInventory(null, craft.getWidth(), craft.getHeight());
        List<Integer> counts = Arrays.asList(new Integer[9]);
        for (int i = 0; i < craft.getWidth() * craft.getHeight(); i++) {
            if (craft.getStack(i).isEmpty()) continue;
            Pair<Item, Integer> item = pickShulkerBoxItem(craft.getStack(i));
            if (item == null) return null;
            ((CraftingInventoryAccessor) items).getStacks().set(i, new ItemStack(item.first, 1));
            counts.set(i, item.second);
            minCount = Math.min(minCount, item.second);
        }

        //获取对应合成表
        assert world.getServer() != null;
        List<CraftingRecipe> list = world.getServer().getRecipeManager().getAllMatches(RecipeType.CRAFTING, items, world);
        if (list.size() == 0) return null;
        CraftingRecipe recipe = list.get(0);

        //计算输出最大数量
        int maxOut = 27 * recipe.getOutput().getItem().getMaxCount();
        int maxCraft = maxOut / recipe.getOutput().getCount();
        int craftTime = Math.min(maxCraft, minCount);

        //生成最终合成表
        DefaultedList<Pair<Item, Integer>> ingredients = DefaultedList.ofSize(9, Pair.of(Items.AIR, 0));
        DefaultedList<Ingredient> input = DefaultedList.ofSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) {
            ingredients.set(i, Pair.of(items.getStack(i).getItem(), craftTime));
            input.set(i, Ingredient.ofStacks(buildShulkerBox(items.getStack(i).getItem(), craftTime)));
        }
        return new ShulkerBoxCraftRecipe(recipe, ingredients, input, buildShulkerBox(recipe.getOutput().getItem(), craftTime * recipe.getOutput().getCount()));
    }
}
