package ltguide.minebackup.listeners;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Command;
import ltguide.minebackup.data.Message;
import ltguide.minebackup.exceptions.CommandException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MineBackupCommandListener implements CommandExecutor {
	private final MineBackup plugin;
	
	public MineBackupCommandListener(final MineBackup plugin) {
		this.plugin = plugin;
	}
	
	@Override public boolean onCommand(final CommandSender sender, final org.bukkit.command.Command c, final String label, final String[] args) {
		try {
			if (plugin.isWorking()) throw new CommandException(Message.BUSY);
			
			Command command;
			if (args.length == 0 || (command = Command.toValue(args[0])) == null) {
				for (final Command comm : Command.values())
					if (sender.hasPermission("minebackup." + comm.getPerm())) plugin.send(sender, Message.SYNTAX.toString(comm.getSyntax(label), comm.getText()));
				
				return true;
			}
			
			if (!sender.hasPermission("minebackup." + command.getPerm())) throw new CommandException(Message.PERMISSION);
			
			if (command.getRequired() > args.length - 1) throw new CommandException(Message.SYNTAX, command.getSyntax(label), command.getText());
			
			switch (command) {
				case STATUS:
					final long msecs = Calendar.getInstance().getTimeInMillis();
					
					for (final String name : plugin.config.getOthers())
						sendStatus(sender, msecs, "others", name);
					
					for (final World world : Bukkit.getWorlds())
						sendStatus(sender, msecs, "worlds", world.getName());
					
					plugin.send(sender, Message.STATUS_NOTE.toString());
					break;
				case NOW:
					plugin.broadcast(sender, command);
					plugin.spawnProcess(0);
					break;
				case SOON:
					plugin.fillProcessQueue();
					plugin.broadcast(sender, command);
					break;
				case NEXT:
					plugin.broadcast(sender, command);
					plugin.spawnProcess();
					break;
				case RELOAD:
					plugin.reload();
					plugin.broadcast(sender, command);
					break;
				case DROPBOX:
					plugin.persist.setDropboxAuth(args[1], args[2]);
					plugin.spawnDropbox();
					plugin.broadcast(sender, command);
					break;
			}
		}
		catch (final CommandException e) {
			plugin.send(sender, e.getMessage());
		}
		
		return true;
	}
	
	private void sendStatus(final CommandSender sender, final long msecs, final String type, final String name) {
		long interval;
		
		if (!plugin.persist.isLoaded(name)) return;
		
		final StringBuilder sb = new StringBuilder();
		for (final String action : Arrays.asList("save", "copy", "compress", "dropbox"))
			if ((interval = plugin.config.getInterval(type, name, action)) != 0) sb.append(Message.STATUS_ACTION.toString(action, getNext(msecs, type, name, action), getTime(interval)));
		
		plugin.send(sender, Message.STATUS.toString("worlds".equals(type) ? "World" : "Other", name, plugin.persist.isDirty(type, name), sb.toString()));
	}
	
	private String getNext(final long msecs, final String type, final String name, final String action) {
		long time = plugin.persist.getNext(type, name, action);
		if (time == 0L) return Message.STATUS_TIME_NONE.toString();
		
		time -= msecs;
		if (time > 0L) return Message.STATUS_TIME_UNDER.toString(getTime(time));
		
		time *= -1;
		return Message.STATUS_TIME_OVER.toString(getTime(time));
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
