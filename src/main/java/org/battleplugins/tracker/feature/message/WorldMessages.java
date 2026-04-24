package org.battleplugins.tracker.feature.message;

import net.kyori.adventure.text.Component;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record WorldMessages(
        boolean enabled,
        Map<EntityDamageEvent.DamageCause, List<Component>> messages,
        List<Component> defaultMessages
) {
    private static final Map<String, EntityDamageEvent.DamageCause> LEGACY_DAMAGE_CAUSES = Map.of(
            "KILL", EntityDamageEvent.DamageCause.SUICIDE
    );

    public static WorldMessages load(ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled");
        if (!enabled) {
            return new WorldMessages(false, Map.of(), List.of());
        }

        Map<EntityDamageEvent.DamageCause, List<Component>> messages = new HashMap<>();
        List<Component> defaultMessages = section.getStringList("messages.default")
                .stream()
                .map(MessageUtil::deserialize)
                .toList();

        ConfigurationSection messagesSection = section.getConfigurationSection("messages");
        messagesSection.getKeys(false).forEach(key -> {
            if (!messagesSection.isList(key)) {
                throw new IllegalArgumentException("Section " + key + " is not a list of messages!");
            }

            if (key.equalsIgnoreCase("default")) {
                return;
            }

            EntityDamageEvent.DamageCause cause = parseDamageCause(key);
            if (cause == null) {
                BattleTracker.getInstance().warn("Skipping unsupported world death message cause '{}' at {}.{}.",
                        key, messagesSection.getCurrentPath(), key);
                return;
            }

            List<String> messageList = messagesSection.getStringList(key);
            if (messages.containsKey(cause)) {
                BattleTracker.getInstance().warn("Ignoring duplicate world death message cause '{}' at {}.{}.",
                        key, messagesSection.getCurrentPath(), key);
                return;
            }

            messages.put(cause, messageList.stream()
                    .map(MessageUtil::deserialize)
                    .collect(Collectors.toList()));
        });

        return new WorldMessages(true, messages, defaultMessages);
    }

    private static @Nullable EntityDamageEvent.DamageCause parseDamageCause(String key) {
        String normalizedKey = key.toUpperCase(Locale.ROOT);
        EntityDamageEvent.DamageCause legacyCause = LEGACY_DAMAGE_CAUSES.get(normalizedKey);
        if (legacyCause != null) {
            return legacyCause;
        }

        try {
            return EntityDamageEvent.DamageCause.valueOf(normalizedKey);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
