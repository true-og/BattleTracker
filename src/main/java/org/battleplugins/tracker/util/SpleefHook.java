package org.battleplugins.tracker.util;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

/** Soft integration that keeps Spleef activity out of SMP statistics. */
public final class SpleefHook {

    private static final String PLUGIN_NAME = "Spleef-OG";
    private static final String API_CLASS = "net.trueog.spleefog.api.SpleefAPI";
    private static final Set<UUID> SPLEEF_DEATHS = ConcurrentHashMap.newKeySet();
    private static volatile Plugin cachedPlugin;
    private static volatile Method isInSpleef;
    private static volatile boolean warned;

    private SpleefHook() {
    }

    public static final class DeathGuard implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPreDeath(PlayerDeathEvent event) {
            if (isInSpleef(event.getEntity())) {
                SPLEEF_DEATHS.add(event.getEntity().getUniqueId());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPostDeath(PlayerDeathEvent event) {
            SPLEEF_DEATHS.remove(event.getEntity().getUniqueId());
        }
    }

    public static boolean isSpleefDeath(Player player) {
        return player != null && SPLEEF_DEATHS.contains(player.getUniqueId());
    }

    public static boolean isInSpleef(Player player) {
        if (player == null) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            resolve(plugin);
            return isInSpleef != null && Boolean.TRUE.equals(isInSpleef.invoke(null, player));
        } catch (Throwable throwable) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().log(Level.WARNING,
                        "[BattleTracker] Failed to query Spleef state; Spleef activity may be tracked until resolved.",
                        throwable);
            }
            return false;
        }
    }

    private static void resolve(Plugin plugin) throws ReflectiveOperationException {
        if (plugin == cachedPlugin && isInSpleef != null) {
            return;
        }
        Class<?> api = Class.forName(API_CLASS, true, plugin.getClass().getClassLoader());
        isInSpleef = api.getMethod("isInSpleef", Player.class);
        cachedPlugin = plugin;
        warned = false;
    }
}
