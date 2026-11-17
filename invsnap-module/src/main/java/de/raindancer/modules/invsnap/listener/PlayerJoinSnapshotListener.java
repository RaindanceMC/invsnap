package de.raindancer.modules.invsnap.listener;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.invsnap.service.AutoSnapshotService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.UUID;

/**
 * A snapshot of what somebody arrives with. A tick after joining, so items other plugins hand back on join (the
 * insurance's returns) are already in it.
 */
public final class PlayerJoinSnapshotListener implements IInvSnapListener {

    private final Plugin plugin;
    private final AutoSnapshotService autoSnapshots;

    public PlayerJoinSnapshotListener(Plugin plugin, AutoSnapshotService autoSnapshots) {
        this.plugin = plugin;
        this.autoSnapshots = autoSnapshots;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        Scheduling.entityLater(plugin, player, 1, () -> {
            if (player.isOnline()) {
                autoSnapshots.joined(player, Instant.now());
            }
        });
    }

    @Override
    public void forget(UUID player) {
        // Remembers nobody; the quit listener clears the timer's memory.
    }

    @Override
    public String describe() {
        return "a snapshot of what somebody arrives with";
    }
}
