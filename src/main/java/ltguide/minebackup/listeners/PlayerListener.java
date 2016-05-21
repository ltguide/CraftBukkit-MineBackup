package ltguide.minebackup.listeners;

import ltguide.minebackup.MineBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {
	private final MineBackup plugin;
	
	public PlayerListener(final MineBackup instance) {
		plugin = instance;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		plugin.persist.setDirty(event.getPlayer().getWorld());
		plugin.persist.setDirty();
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) plugin.persist.setDirty(event.getFrom().getWorld());
	}
}
