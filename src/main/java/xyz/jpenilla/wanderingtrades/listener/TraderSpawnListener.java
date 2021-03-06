package xyz.jpenilla.wanderingtrades.listener;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.jmplib.RandomCollection;
import xyz.jpenilla.wanderingtrades.WanderingTrades;
import xyz.jpenilla.wanderingtrades.util.VillagerReflection;

import static io.papermc.lib.PaperLib.getMinecraftVersion;
import static io.papermc.lib.PaperLib.isPaper;

public class TraderSpawnListener implements Listener {
    private final WanderingTrades plugin;
    private final Collection<UUID> traderBlacklistCache = new HashSet<>();

    public TraderSpawnListener(WanderingTrades wt) {
        this.plugin = wt;
    }

    public static boolean randBoolean(double p) {
        return Math.random() < p;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPortal(EntityPortalEvent e) {
        if (e.getEntityType() == EntityType.WANDERING_TRADER) {
            traderBlacklistCache.add(e.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        final Entity entity = e.getEntity();
        if (entity instanceof WanderingTrader && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.MOUNT) {
            final WanderingTrader wanderingTrader = (WanderingTrader) entity;
            if (plugin.config().traderWorldWhitelist()) {
                if (plugin.config().traderWorldList().contains(e.getEntity().getWorld().getName())) {
                    this.addTrades(wanderingTrader, false);
                }
            } else {
                if (!plugin.config().traderWorldList().contains(e.getEntity().getWorld().getName())) {
                    this.addTrades(wanderingTrader, false);
                }
            }
        }
    }

    public void addTrades(WanderingTrader wanderingTrader, boolean refresh) {
        if (traderBlacklistCache.remove(wanderingTrader.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MerchantRecipe> newTrades = new ArrayList<>();
            if (plugin.config().playerHeadConfig().playerHeadsFromServer() && randBoolean(plugin.config().playerHeadConfig().playerHeadsFromServerChance())) {
                newTrades.addAll(plugin.storedPlayers().randomlySelectPlayerHeads());
            }
            if (plugin.config().allowMultipleSets()) {
                ImmutableList.copyOf(plugin.config().tradeConfigs().values()).forEach(config -> {
                    if (randBoolean(config.chance())) {
                        newTrades.addAll(config.getTrades(false));
                    }
                });
            } else {
                RandomCollection<String> configNames = new RandomCollection<>();
                plugin.config().tradeConfigs().forEach((key, value) -> configNames.add(value.chance(), key));
                String chosenConfig = configNames.next();
                if (chosenConfig != null) {
                    newTrades.addAll(plugin.config().tradeConfigs().get(chosenConfig).getTrades(false));
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (refresh) {
                    if (!plugin.config().removeOriginalTrades()) {
                        if (isPaper() && getMinecraftVersion() >= 16) {
                            wanderingTrader.resetOffers();
                            newTrades.addAll(wanderingTrader.getRecipes());
                        } else if (getMinecraftVersion() <= 17) {
                            // This branch is executed when the plugin is run on Paper 1.14 or 1.15, or on Spigot 1.14-1.17.
                            // The above if statement, and the below method should be updated when Spigot's version/mappings
                            // change, if maintaining Spigot support for this feature is desired.
                            this.resetOffersUsingReflection(wanderingTrader);
                            newTrades.addAll(wanderingTrader.getRecipes());
                        }
                    }
                } else {
                    newTrades.addAll(wanderingTrader.getRecipes());
                }
                wanderingTrader.setRecipes(newTrades);
            });

        });
    }

    /**
     * Clear this {@link AbstractVillager}'s offers and acquire new ones.
     *
     * <p>Reflection-based implementation of Paper's Villager-resetOffers API</p>
     *
     * @param trader the trader to act on
     */
    private void resetOffersUsingReflection(@NonNull AbstractVillager trader) {
        final List<MerchantRecipe> oldOffers = trader.getRecipes();
        try {
            VillagerReflection.resetOffers(trader);
        } catch (final Throwable throwable) {
            trader.setRecipes(oldOffers);
            this.plugin.getLogger().log(
                    Level.WARNING,
                    String.format("Failed to reset trades! Please report this bug to the issue tracker at %s !", plugin.getDescription().getWebsite()),
                    throwable
            );
        }
    }

    public Collection<UUID> getTraderBlacklistCache() {
        return this.traderBlacklistCache;
    }
}
