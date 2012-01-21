package ltguide.base.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ltguide.base.Base;
import ltguide.base.exceptions.CommandException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Command {
	private static Map<String, Command> commands = new HashMap<String, Command>();
	
	private final String name;
	private final String permission;
	private final Message message;
	private final String syntax;
	private final boolean usesTarget;
	private String help;
	private String broadcast;
	private CommandSender sender;
	private String label;
	private List<String> args;
	private Player target = null;
	
	public Command(final String name, final String permission, final Message message, final String syntax, final boolean usesTarget) {
		this.name = name;
		this.permission = permission;
		this.message = message;
		this.syntax = syntax;
		this.usesTarget = usesTarget;
	}
	
	public static void put(final Command handle) {
		commands.put(handle.name, handle);
	}
	
	public static void setConfig(final String name, final String text, final String permission) {
		commands.put(name, commands.get(name).setHelp(text).setBroadcast(permission));
	}
	
	public String getMessage(final Object... args) {
		return message.getText(args);
	}
	
	public String getSyntax() {
		return String.format("/%s %s %s", label, name, syntax);
	}
	
	public String getHelp() {
		return help.replaceAll("(?i)&([0-F])", "\u00A7$1");
	}
	
	public Command setHelp(final String text) {
		help = text;
		return this;
	}
	
	public String getBroadcast() {
		return broadcast;
	}
	
	public Command setBroadcast(final String permission) {
		broadcast = permission;
		return this;
	}
	
	public CommandSender getSender() {
		return sender;
	}
	
	public void setSender(final CommandSender sender) {
		this.sender = sender;
		if (sender instanceof Player) setTarget((Player) sender);
	}
	
	public Player getTarget() {
		return target;
	}
	
	public void setTarget(final Player target) {
		this.target = target;
	}
	
	public int getRequiredArgs() {
		return syntax.length() - syntax.replace("<", "").length();
	}
	
	public void sendSyntax(final CommandSender sender, final String _label) {
		label = _label;
		if (sender.hasPermission(permission)) Base.send(sender, Message.getText("SYNTAX", getSyntax(), getHelp()));
	}
	
	public Command init(final CommandSender sender, final String _label, final List<String> list) throws CommandException {
		if (!sender.hasPermission(permission)) throw new CommandException(Message.get("PERMISSION"));
		
		setSender(sender);
		label = _label;
		
		args = new ArrayList<String>(list);
		args.remove(0);
		
		if (args.size() > 0) for (final String option : args)
			if (option.startsWith("@")) {
				final List<Player> matches = sender.getServer().matchPlayer(option.substring(1));
				if (matches.size() == 1) setTarget(matches.get(0));
				else throw new CommandException(Message.get("TARGET_NONE"));
				
				args.remove(option);
				break;
			}
		
		if (usesTarget && target == null) throw new CommandException(Message.get("TARGET_REQUIRED"));
		
		if (getRequiredArgs() > args.size()) throw new CommandException(Message.get("SYNTAX", getSyntax(), getHelp()));
		
		return this;
	}
}
