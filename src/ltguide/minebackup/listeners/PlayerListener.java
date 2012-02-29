package ltguide.minebackup.listeners;

import ltguide.minebackup.MineBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {
	private final MineBackup plugin;

	public PlayerListener(final MineBackup plugin) {
		this.plugin = plugin;
	}

    @EventHandler
	 public void onPlayerQuit(final PlayerQuitEvent event) {
		plugin.persist.setDirty(event.getPlayer().getWorld());
		plugin.persist.setDirty();
	}

    @EventHandler
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		if (!event.isCancelled() && !event.getFrom().getWorld().equals(event.getTo().getWorld())) plugin.persist.setDirty(event.getFrom().getWorld());
	}
}
