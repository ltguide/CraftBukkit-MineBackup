package ltguide.minebackup.listeners;

import java.util.Calendar;

import ltguide.base.Base;
import ltguide.minebackup.MineBackup;

import org.bukkit.event.world.WorldSaveEvent;

public class WorldListener extends org.bukkit.event.world.WorldListener {
	private final MineBackup plugin;
	private long msecs = 0;
	
	public WorldListener(final MineBackup plugin) {
		this.plugin = plugin;
	}
	
	@Override public void onWorldSave(final WorldSaveEvent event) {
		if (plugin.isEnabled() && !plugin.isWorking()) {
			final long current = Calendar.getInstance().getTimeInMillis();
			if (current - msecs > 5000) {
				msecs = current;
				Base.warning("WorldSave detected. If a person triggered it then ignore this message. However, if another world save plugin is responsible, then you should uninstall it before it saves while a backup is in progress.");
			}
		}
	}
}
