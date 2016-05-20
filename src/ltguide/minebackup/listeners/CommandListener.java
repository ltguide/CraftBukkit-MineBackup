package ltguide.minebackup.listeners;

import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Commands;
import ltguide.minebackup.exceptions.CommandException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Calendar;

public class CommandListener implements CommandExecutor {
	private final MineBackup plugin;
	private long msecs;
	
	public CommandListener(final MineBackup instance) {
		plugin = instance;
		
		plugin.getCommand("minebackup").setExecutor(this);
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, final org.bukkit.command.Command c, final String label, final String[] args) {
		try {
			if (plugin.isWorking()) throw new CommandException(plugin.getMessage("BUSY"));
			
			final Commands commands = Commands.get(args);
			if (commands == null) return plugin.sendCommands(sender, label);
			
			plugin.initCommand(commands, sender, label, args);
			
			switch (commands) {
				case STATUS:
					msecs = Calendar.getInstance().getTimeInMillis();
					
					for (final String name : plugin.config.getOthers())
						sendStatus(sender, "others", name);
					
					for (final World world : Bukkit.getWorlds())
						sendStatus(sender, "worlds", world.getName());
					
					plugin.send(sender, plugin.getMessage("STATUS_NOTE"));
					break;
				case NOW:
					plugin.broadcast(sender);
					plugin.spawnProcess(0);
					break;
				case SOON:
					plugin.fillProcessQueue(1);
					plugin.broadcast(sender);
					break;
				case NEXT:
					plugin.broadcast(sender);
					plugin.spawnProcess();
					break;
				case UPLOAD:
					plugin.broadcast(sender);
					plugin.spawnUpload(0);
					break;
				case RELOAD:
					plugin.reload();
					plugin.broadcast(sender);
					break;
				case DROPBOX:
					plugin.persist.setDropboxAuth(args[1], args[2]);
					plugin.spawnUpload(90);
					plugin.broadcast(sender);
					break;
				case USAGE:
					final Runtime runtime = Runtime.getRuntime();
					final long total = runtime.totalMemory() / 1024 / 1024;
					final long max = runtime.maxMemory() / 1024 / 1024;
					plugin.send(sender, plugin.getMessage("USAGE_MEMORY", total == max ? total : total + "/" + max, runtime.freeMemory() / 1024 / 1024));
					
					for (final World world : Bukkit.getWorlds())
						plugin.send(sender, plugin.getMessage("USAGE_WORLD", world.getName(), world.getLoadedChunks().length, world.getEntities().size()));
					break;
				default:
			}
		}
		catch (final CommandException e) {
			plugin.send(sender, e.getMessage());
		}
		
		return true;
	}
	
	private void sendStatus(final CommandSender sender, final String type, final String name) {
		long interval;
		
		if (!plugin.config.isLoaded(type, name)) return;
		
		final StringBuilder sb = new StringBuilder();
		for (final String action : plugin.actions)
			if (plugin.hasAction(action) && (interval = plugin.config.getInterval(type, name, action)) != 0) sb.append(plugin.getMessage("STATUS_ACTION", action, getNext(msecs, type, name, action), plugin.convertMilli2Time(interval)));
		
		plugin.send(sender, plugin.getMessage("STATUS", "worlds".equals(type) ? "World" : "Other", name, plugin.persist.isDirty(type, name), sb.toString()));
	}
	
	private String getNext(final long msecs, final String type, final String name, final String action) {
		long time = plugin.persist.getNext(type, name, action);
		if (time == 0L) return plugin.getMessage("STATUS_TIME_NONE", "");
		
		time -= msecs;
		if (time > 0L) return plugin.getMessage("STATUS_TIME_UNDER", plugin.convertMilli2Time(time));
		
		time *= -1;
		return plugin.getMessage("STATUS_TIME_OVER", plugin.convertMilli2Time(time));
	}
}
