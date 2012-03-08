package ltguide.minebackup.data;

import java.util.Arrays;

import ltguide.base.data.Command;
import ltguide.base.data.IEnum;
import ltguide.base.exceptions.CommandException;

import org.bukkit.command.CommandSender;

public enum Commands implements IEnum {
	STATUS("status", Messages.STATUS_NOTE, "", false),
	NOW("manual", Messages.BACKUP_NOW, "", false),
	SOON("manual", Messages.BACKUP_SOON, "", false),
	NEXT("manual", Messages.BACKUP_NEXT, "", false),
	RELOAD("reload", Messages.RELOAD, "", false),
	DROPBOX("dropbox", Messages.DROPBOX, "<key> <secret>", false);
	
	public Command handle;
	
	Commands(final String permission, final Messages messages, final String syntax, final boolean usesTarget) {
		handle = new Command(name(), "minebackup." + permission, messages.handle, syntax, usesTarget);
		Command.put(handle);
	}
	
	public static Commands get(final CommandSender sender, final String label, final String[] args) throws CommandException {
		try {
			final Commands commands = valueOf(args[0].toUpperCase());
			commands.handle.init(sender, label, Arrays.asList(args));
			return commands;
		}
		catch (final CommandException e) {
			throw e;
		}
		catch (final Exception e) {
			for (final Commands commands : Commands.values())
				commands.handle.sendSyntax(sender, label);
			
			return null;
		}
	}
}
