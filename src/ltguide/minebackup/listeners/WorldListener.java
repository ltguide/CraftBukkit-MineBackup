package ltguide.minebackup.listeners;

import java.util.Calendar;

import ltguide.minebackup.MineBackup;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

public class WorldListener implements Listener {
	private final MineBackup plugin;
	private long msecs = 0;
	
	public WorldListener(final MineBackup instance) {
		plugin = instance;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldSave(final WorldSaveEvent event) {
		if (plugin.isEnabled() && !plugin.isWorking()) {
			final long current = Calendar.getInstance().getTimeInMillis();
			if (current - msecs > 5000) {
				msecs = current;
				plugin.warning("WorldSave detected. If a person triggered it then ignore this message. However, if another world save plugin is responsible, then you should uninstall it before it saves while a backup is in progress.");
			}
		}
	}
}
