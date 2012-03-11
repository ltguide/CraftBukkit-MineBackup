package ltguide.minebackup.listeners;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import ltguide.base.exceptions.CommandException;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
					plugin.fillProcessQueue();
					plugin.broadcast(sender);
					break;
				case NEXT:
					plugin.broadcast(sender);
					plugin.spawnProcess();
					break;
				case RELOAD:
					plugin.reload();
					plugin.broadcast(sender);
					break;
				case DROPBOX:
					plugin.persist.setDropboxAuth(args[1], args[2]);
					plugin.spawnUpload();
					plugin.broadcast(sender);
					break;
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
			if ((interval = plugin.config.getInterval(type, name, action)) != 0) sb.append(plugin.getMessage("STATUS_ACTION", action, getNext(msecs, type, name, action), getTime(interval)));
		
		plugin.send(sender, plugin.getMessage("STATUS", "worlds".equals(type) ? "World" : "Other", name, plugin.persist.isDirty(type, name), sb.toString()));
	}
	
	private String getNext(final long msecs, final String type, final String name, final String action) {
		long time = plugin.persist.getNext(type, name, action);
		if (time == 0L) return plugin.getMessage("STATUS_TIME_NONE", "");
		
		time -= msecs;
		if (time > 0L) return plugin.getMessage("STATUS_TIME_UNDER", getTime(time));
		
		time *= -1;
		return plugin.getMessage("STATUS_TIME_OVER", getTime(time));
	}
	
	private String getTime(final long time) {
		int secs = (int) (time / 1000);
		final StringBuilder sb = new StringBuilder();
		
		if (secs < 0) {
			secs *= -1;
			sb.append(secs / 3600);
			sb.append(":");
			sb.append(secs % 3600 / 60);
			return sb.toString();
		}
		
		int c;
		final Map<String, Integer> map = new TreeMap<String, Integer>();
		map.put("d", 86400);
		map.put("h", 3600);
		map.put("m", 60);
		map.put("s", 1);
		
		for (final Map.Entry<String, Integer> entry : map.entrySet())
			if ((c = secs / entry.getValue()) > 0 || sb.length() > 0) {
				secs %= entry.getValue();
				sb.append(c);
				sb.append(entry.getKey());
				
				if (secs == 0) break;
			}
		
		if (sb.length() == 0) sb.append("0s");
		
		return sb.toString();
	}
}
