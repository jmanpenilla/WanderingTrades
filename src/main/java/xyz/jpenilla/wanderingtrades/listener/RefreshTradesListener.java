package xyz.jpenilla.wanderingtrades.listener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.jpenilla.wanderingtrades.WanderingTrades;
import xyz.jpenilla.wanderingtrades.config.TradeConfig;
import xyz.jpenilla.wanderingtrades.util.Constants;

public class RefreshTradesListener implements Listener {
    private final WanderingTrades plugin;

    public RefreshTradesListener(WanderingTrades instance) {
        plugin = instance;
    }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        final Entity rightClicked = e.getRightClicked();
        if (rightClicked instanceof AbstractVillager) {
            final AbstractVillager abstractVillager = (AbstractVillager) rightClicked;

            // Block Name Tags
            final PlayerInventory playerInventory = e.getPlayer().getInventory();
            if (playerInventory.getItemInMainHand().getType() == Material.NAME_TAG || playerInventory.getItemInOffHand().getType() == Material.NAME_TAG) {
                final PersistentDataContainer persistentDataContainer = abstractVillager.getPersistentDataContainer();
                if (persistentDataContainer.has(Constants.CONFIG_NAME, PersistentDataType.STRING) || persistentDataContainer.has(Constants.PROTECT, PersistentDataType.STRING)) {
                    e.setCancelled(true);
                }
            }

            // Update trades
            if (plugin.worldGuardHook() != null) {
                if (plugin.worldGuardHook().passesWhiteBlackList(abstractVillager.getLocation())) {
                    updateTrades(abstractVillager);
                }
            } else {
                updateTrades(abstractVillager);
            }

        }
    }

    private void updateTrades(AbstractVillager abstractVillager) {
        final PersistentDataContainer persistentDataContainer = abstractVillager.getPersistentDataContainer();
        final String configName = persistentDataContainer.get(Constants.CONFIG_NAME, PersistentDataType.STRING);
        final boolean refreshNatural = persistentDataContainer.has(Constants.REFRESH_NATURAL, PersistentDataType.STRING);
        if (configName != null || refreshNatural) {
            final long timeAtPreviousRefresh = persistentDataContainer.getOrDefault(Constants.LAST_REFRESH, PersistentDataType.LONG, 0L);
            final LocalDateTime nextAllowedRefresh = Instant.ofEpochMilli(timeAtPreviousRefresh).atZone(ZoneId.systemDefault()).toLocalDateTime().plusMinutes(plugin.config().refreshCommandTradersMinutes());
            if (timeAtPreviousRefresh == 0L || LocalDateTime.now().isAfter(nextAllowedRefresh)) {
                if (configName != null) {
                    final TradeConfig tradeConfig = plugin.config().tradeConfigs().get(configName);
                    abstractVillager.setRecipes(tradeConfig.getTrades(true));
                }
                if (refreshNatural && abstractVillager instanceof WanderingTrader) {
                    plugin.listeners().listener(TraderSpawnListener.class).addTrades((WanderingTrader) abstractVillager, true);
                }
                persistentDataContainer.set(Constants.LAST_REFRESH, PersistentDataType.LONG, System.currentTimeMillis());
            }
        }
    }
}
