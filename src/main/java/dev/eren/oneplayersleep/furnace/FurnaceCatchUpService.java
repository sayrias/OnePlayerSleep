package dev.eren.oneplayersleep.furnace;

import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.platform.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class FurnaceCatchUpService implements Listener {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final SchedulerAdapter scheduler;
    private final NamespacedKey pendingXpKey;
    private final NamespacedKey pendingItemsKey;
    private final NamespacedKey pendingMaterialKey;
    private volatile List<CookingRecipe<?>> cookingRecipes = new ArrayList<CookingRecipe<?>>();

    public FurnaceCatchUpService(JavaPlugin plugin, PluginSettings settings, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.settings = settings;
        this.scheduler = scheduler;
        pendingXpKey = new NamespacedKey(plugin, "catchup_xp");
        pendingItemsKey = new NamespacedKey(plugin, "catchup_items");
        pendingMaterialKey = new NamespacedKey(plugin, "catchup_material");
        reloadRecipes();
    }

    public void reloadRecipes() {
        List<CookingRecipe<?>> recipes = new ArrayList<CookingRecipe<?>>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof FurnaceRecipe || recipe instanceof SmokingRecipe || recipe instanceof BlastingRecipe) {
                recipes.add((CookingRecipe<?>) recipe);
            }
        }
        cookingRecipes = recipes;
    }

    /**
     * @return number of furnaces processed, or scheduled for processing on Folia
     */
    public int catchUp(World world, long skippedTicks) {
        if (!settings.isFurnaceCatchUp() || skippedTicks <= 0 || settings.getMaxFurnacesPerSkip() <= 0) {
            return 0;
        }
        final long ticks = Math.min(skippedTicks, settings.getMaxSkippedTicks());
        final AtomicInteger processed = new AtomicInteger();

        if (scheduler.isFolia()) {
            for (final Chunk chunk : world.getLoadedChunks()) {
                if (processed.get() >= settings.getMaxFurnacesPerSkip()) {
                    break;
                }
                Location regionLocation = new Location(world, (chunk.getX() << 4) + 8, world.getMinHeight(),
                        (chunk.getZ() << 4) + 8);
                scheduler.runAt(regionLocation, new Runnable() {
                    @Override
                    public void run() {
                        scanChunk(chunk, ticks, processed);
                    }
                });
            }
            return processed.get();
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            scanChunk(chunk, ticks, processed);
            if (processed.get() >= settings.getMaxFurnacesPerSkip()) {
                break;
            }
        }
        return processed.get();
    }

    private void scanChunk(Chunk chunk, long ticks, AtomicInteger processed) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Furnace) || !isTypeEnabled((Furnace) state)) {
                continue;
            }
            int slot = processed.incrementAndGet();
            if (slot > settings.getMaxFurnacesPerSkip()) {
                processed.decrementAndGet();
                return;
            }
            simulate((Furnace) state, ticks);
        }
    }

    private boolean isTypeEnabled(Furnace furnace) {
        if (furnace instanceof BlastFurnace) return settings.isCatchUpBlastFurnace();
        if (furnace instanceof Smoker) return settings.isCatchUpSmoker();
        return settings.isCatchUpFurnace();
    }

    private void simulate(Furnace furnace, long ticks) {
        // Work on the same snapshot that update() persists. Mutating the live inventory and then
        // applying the BlockState snapshot would restore its old items and discard the catch-up.
        FurnaceInventory inventory = furnace.getSnapshotInventory();
        long remainingTicks = ticks;
        int burnRemaining = Math.max(0, furnace.getBurnTime());
        int cookProgress = Math.max(0, furnace.getCookTime());
        int cookTotal = Math.max(1, furnace.getCookTimeTotal());
        boolean changed = false;

        while (remainingTicks > 0) {
            ItemStack source = inventory.getSmelting();
            CookingRecipe<?> recipe = findRecipe(furnace, source);
            if (recipe == null || !canMerge(inventory.getResult(), recipe.getResult())) {
                int burned = (int) Math.min(remainingTicks, burnRemaining);
                burnRemaining -= burned;
                cookProgress = burnRemaining > 0 ? 0
                        : Math.max(0, cookProgress - (int) Math.min(Integer.MAX_VALUE, remainingTicks * 2L));
                changed |= burned > 0;
                break;
            }
            cookTotal = Math.max(1, recipe.getCookingTime());

            if (burnRemaining <= 0) {
                burnRemaining = igniteFuel(furnace, inventory);
                if (burnRemaining <= 0) {
                    cookProgress = Math.max(0,
                            cookProgress - (int) Math.min(Integer.MAX_VALUE, remainingTicks * 2L));
                    changed |= cookProgress != furnace.getCookTime();
                    break;
                }
                changed = true;
            }

            int untilCooked = Math.max(1, cookTotal - cookProgress);
            int step = (int) Math.min(remainingTicks, Math.min((long) burnRemaining, (long) untilCooked));
            cookProgress += step;
            burnRemaining -= step;
            remainingTicks -= step;
            changed |= step > 0;

            if (cookProgress >= cookTotal) {
                ItemStack proposed = recipe.getResult().clone();
                FurnaceSmeltEvent event = new FurnaceSmeltEvent(furnace.getBlock(), source.clone(), proposed);
                Bukkit.getPluginManager().callEvent(event);
                ItemStack result = event.getResult();
                if (event.isCancelled() || isEmpty(result) || !canMerge(inventory.getResult(), result)) {
                    cookProgress = Math.max(0, cookTotal - 1);
                    int burned = (int) Math.min(remainingTicks, burnRemaining);
                    burnRemaining -= burned;
                    remainingTicks -= burned;
                    break;
                }

                consumeOneSource(inventory);
                mergeResult(inventory, result);
                recordExperience(furnace, result.getType(), result.getAmount(), recipe.getExperience());
                cookProgress = 0;

                if (isEmpty(inventory.getSmelting())) {
                    int burned = (int) Math.min(remainingTicks, burnRemaining);
                    burnRemaining -= burned;
                    remainingTicks -= burned;
                    break;
                }
            }
        }

        if (changed) {
            furnace.setBurnTime((short) Math.min(Short.MAX_VALUE, Math.max(0, burnRemaining)));
            furnace.setCookTime((short) Math.min(Short.MAX_VALUE, Math.max(0, cookProgress)));
            furnace.setCookTimeTotal(cookTotal);
            furnace.update(true, false);
        }
    }

    private int igniteFuel(Furnace furnace, FurnaceInventory inventory) {
        ItemStack fuel = inventory.getFuel();
        if (isEmpty(fuel)) {
            return 0;
        }
        int defaultTicks = FuelDurations.ticks(fuel.getType(), settings.getCustomFuelBurnTimes());
        FurnaceBurnEvent event = new FurnaceBurnEvent(furnace.getBlock(), fuel.clone(), defaultTicks);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !event.isBurning() || event.getBurnTime() <= 0) {
            return 0;
        }
        consumeOneFuel(inventory);
        return Math.min(Short.MAX_VALUE, event.getBurnTime());
    }

    private CookingRecipe<?> findRecipe(Furnace furnace, ItemStack source) {
        if (isEmpty(source)) {
            return null;
        }
        for (CookingRecipe<?> recipe : cookingRecipes) {
            if (!recipeMatchesFurnace(furnace, recipe)) {
                continue;
            }
            try {
                if (recipe.getInputChoice().test(source)) {
                    return recipe;
                }
            } catch (RuntimeException ignored) {
                // A malformed custom recipe should not abort the night skip.
            }
        }
        return null;
    }

    private static boolean recipeMatchesFurnace(Furnace furnace, CookingRecipe<?> recipe) {
        if (furnace instanceof BlastFurnace) return recipe instanceof BlastingRecipe;
        if (furnace instanceof Smoker) return recipe instanceof SmokingRecipe;
        return recipe instanceof FurnaceRecipe;
    }

    private static boolean canMerge(ItemStack existing, ItemStack result) {
        if (isEmpty(result)) return false;
        if (isEmpty(existing)) return result.getAmount() <= result.getMaxStackSize();
        return existing.isSimilar(result) && existing.getAmount() + result.getAmount() <= existing.getMaxStackSize();
    }

    private static void mergeResult(FurnaceInventory inventory, ItemStack produced) {
        ItemStack existing = inventory.getResult();
        if (isEmpty(existing)) {
            inventory.setResult(produced.clone());
        } else {
            existing.setAmount(existing.getAmount() + produced.getAmount());
            inventory.setResult(existing);
        }
    }

    private static void consumeOneSource(FurnaceInventory inventory) {
        ItemStack source = inventory.getSmelting();
        if (source == null || source.getAmount() <= 1) {
            inventory.setSmelting(null);
        } else {
            source.setAmount(source.getAmount() - 1);
            inventory.setSmelting(source);
        }
    }

    private static void consumeOneFuel(FurnaceInventory inventory) {
        ItemStack fuel = inventory.getFuel();
        if (fuel == null) return;
        if (fuel.getAmount() <= 1) {
            inventory.setFuel(fuel.getType() == Material.LAVA_BUCKET ? new ItemStack(Material.BUCKET) : null);
        } else {
            fuel.setAmount(fuel.getAmount() - 1);
            inventory.setFuel(fuel);
        }
    }

    private void recordExperience(Furnace furnace, Material material, int items, float experience) {
        if (items <= 0 || experience <= 0.0F) return;
        PersistentDataContainer data = furnace.getPersistentDataContainer();
        String storedMaterial = data.get(pendingMaterialKey, PersistentDataType.STRING);
        if (storedMaterial != null && !storedMaterial.equals(material.name())) {
            data.remove(pendingXpKey);
            data.remove(pendingItemsKey);
        }
        Double pendingXp = data.get(pendingXpKey, PersistentDataType.DOUBLE);
        Integer pendingItems = data.get(pendingItemsKey, PersistentDataType.INTEGER);
        data.set(pendingXpKey, PersistentDataType.DOUBLE, (pendingXp == null ? 0.0D : pendingXp) + experience);
        data.set(pendingItemsKey, PersistentDataType.INTEGER, (pendingItems == null ? 0 : pendingItems) + items);
        data.set(pendingMaterialKey, PersistentDataType.STRING, material.name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        BlockState state = event.getBlock().getState();
        if (!(state instanceof Furnace)) return;
        Furnace furnace = (Furnace) state;
        PersistentDataContainer data = furnace.getPersistentDataContainer();
        String material = data.get(pendingMaterialKey, PersistentDataType.STRING);
        Double pendingXp = data.get(pendingXpKey, PersistentDataType.DOUBLE);
        Integer pendingItems = data.get(pendingItemsKey, PersistentDataType.INTEGER);
        if (material == null || pendingXp == null || pendingItems == null || pendingItems <= 0
                || !material.equals(event.getItemType().name())) {
            return;
        }

        int creditedItems = Math.min(event.getItemAmount(), pendingItems);
        double creditedXp = pendingXp * creditedItems / pendingItems;
        int wholeXp = (int) Math.floor(creditedXp);
        if (ThreadLocalRandom.current().nextDouble() < creditedXp - wholeXp) {
            wholeXp++;
        }
        event.setExpToDrop(event.getExpToDrop() + wholeXp);

        int itemsLeft = pendingItems - creditedItems;
        double xpLeft = Math.max(0.0D, pendingXp - creditedXp);
        if (itemsLeft <= 0 || xpLeft <= 0.000001D) {
            data.remove(pendingXpKey);
            data.remove(pendingItemsKey);
            data.remove(pendingMaterialKey);
        } else {
            data.set(pendingXpKey, PersistentDataType.DOUBLE, xpLeft);
            data.set(pendingItemsKey, PersistentDataType.INTEGER, itemsLeft);
        }
        furnace.update(true, false);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }
}
