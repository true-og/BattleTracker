package org.battleplugins.tracker.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Soft, reflection-based integration with the Duels plugin (Duels-OG).
 *
 * <p>Kills and deaths that happen inside a Duels match must not be recorded by
 * BattleTracker. This helper answers "is this player currently in a duel match?"
 * without a compile-time dependency on Duels: if Duels is not installed, or its
 * API ever changes, queries return {@code false} and tracking proceeds exactly
 * as before.</p>
 *
 * <h2>Death-event ordering</h2>
 * <p>Duels-OG handles {@link PlayerDeathEvent} at {@link EventPriority#HIGHEST}
 * and synchronously marks the dead player as removed from the match, which makes
 * a <em>live</em> {@link #isInMatch(Player)} query return {@code false} for the
 * dead player by the time another {@code HIGHEST} listener runs. To avoid that
 * race, {@link DeathGuard} records match membership at {@link EventPriority#LOWEST}
 * (before Duels mutates the match) and the death listeners consult
 * {@link #isDuelDeath(Player)} instead of querying live state.</p>
 *
 * <p>Resolution targets Duels' public API ({@code me.realized.duels.api.Duels})
 * via {@code getArenaManager().isInMatch(Player)}. Both the renamed fork
 * ("Duels-OG") and the upstream plugin ("Duels") are recognised.</p>
 */
public final class DuelsHook {

    private static final String[] PLUGIN_NAMES = {"Duels-OG", "Duels"};

    // Cache keyed to the Duels plugin instance so a reload/re-enable (new instance) forces re-resolution.
    private static volatile Plugin cachedPlugin;
    private static volatile java.lang.reflect.Method getArenaManager;
    private static volatile java.lang.reflect.Method isInMatch;

    private static volatile boolean warned;

    // UUIDs whose in-progress death began in a duel; populated at LOWEST and cleared at MONITOR by DeathGuard.
    private static final Set<UUID> duelDeaths = ConcurrentHashMap.newKeySet();

    private DuelsHook() {
    }

    /**
     * Records, before other plugins can mutate match state, whether each player
     * death is happening inside a duel. Must be registered exactly once.
     */
    public static final class DeathGuard implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPreDeath(PlayerDeathEvent event) {
            if (isInMatch(event.getEntity())) {
                duelDeaths.add(event.getEntity().getUniqueId());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPostDeath(PlayerDeathEvent event) {
            duelDeaths.remove(event.getEntity().getUniqueId());
        }
    }

    /**
     * Whether the player's in-progress death began inside a duel match. Valid
     * only while a {@link PlayerDeathEvent} for that player is being processed;
     * use this (not {@link #isInMatch(Player)}) inside death handlers.
     *
     * @param player the dying player (may be null)
     * @return {@code true} if the death should be excluded from tracking
     */
    public static boolean isDuelDeath(Player player) {
        return player != null && duelDeaths.contains(player.getUniqueId());
    }

    /**
     * Returns whether the given player is, right now, in an active Duels match.
     *
     * <p>Safe for live use outside death handling (e.g. damage recap, mob kills),
     * where match state is not being torn down. For {@link PlayerDeathEvent}
     * handlers use {@link #isDuelDeath(Player)} instead.</p>
     *
     * @param player the player to check (may be null)
     * @return {@code true} only if Duels is installed, enabled, and reports the
     *         player as being in a match; {@code false} otherwise
     */
    public static boolean isInMatch(Player player) {
        if (player == null) {
            return false;
        }

        Plugin duels = getDuels();
        if (duels == null) {
            return false;
        }

        try {
            resolve(duels);

            if (getArenaManager == null || isInMatch == null) {
                return false;
            }

            Object arenaManager = getArenaManager.invoke(duels);
            if (arenaManager == null) {
                return false;
            }

            return (boolean) isInMatch.invoke(arenaManager, player);
        } catch (Throwable t) {
            // Never let a reflection/API problem break tracking, but surface it once for diagnosis.
            if (!warned) {
                warned = true;
                Bukkit.getLogger().log(Level.WARNING,
                    "[BattleTracker] Failed to query Duels match state; duel kills/deaths "
                        + "may be tracked until this is resolved.", t);
            }
            return false;
        }
    }

    private static Plugin getDuels() {
        for (String name : PLUGIN_NAMES) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            if (plugin != null && plugin.isEnabled()) {
                return plugin;
            }
        }

        return null;
    }

    private static void resolve(Plugin duels) throws ReflectiveOperationException {
        if (duels == cachedPlugin && getArenaManager != null && isInMatch != null) {
            return;
        }

        java.lang.reflect.Method arenaManagerMethod = duels.getClass().getMethod("getArenaManager");
        Object arenaManager = arenaManagerMethod.invoke(duels);
        java.lang.reflect.Method inMatchMethod = arenaManager.getClass().getMethod("isInMatch", Player.class);

        getArenaManager = arenaManagerMethod;
        isInMatch = inMatchMethod;
        cachedPlugin = duels;
    }
}
