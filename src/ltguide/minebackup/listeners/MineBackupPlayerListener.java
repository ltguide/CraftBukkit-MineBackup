package ltguide.minebackup.listeners;

import ltguide.minebackup.MineBackup;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MineBackupPlayerListener extends PlayerListener {
	private final MineBackup plugin;
	
	public MineBackupPlayerListener(final MineBackup plugin) {
		this.plugin = plugin;
	}
	
	@Override public void onPlayerQuit(final PlayerQuitEvent event) {
		plugin.persist.setDirty(event.getPlayer().getWorld());
		plugin.persist.setDirty();
	}
	
	@Override public void onPlayerTeleport(final PlayerTeleportEvent event) {
		if (!event.isCancelled() && !event.getFrom().getWorld().equals(event.getTo().getWorld())) plugin.persist.setDirty(event.getFrom().getWorld());
	}
}
