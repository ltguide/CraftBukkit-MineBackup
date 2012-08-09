package ltguide.base.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ltguide.base.Base;
import ltguide.base.exceptions.CommandException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Command {
	private final Base plugin;
	private final String name;
	private final String permission;
	private final String message;
	private final String syntax;
	private final boolean usesTarget;
	private final String description;
	private final String broadcast;
	private CommandSender sender;
	private List<String> args;
	private Player target = null;
	private boolean isSubCommand = true;
	
	public Command(final Base instance, final ICommand command, final String description, final Object group) {
		plugin = instance;
		
		name = command.name();
		permission = plugin.getName().toLowerCase() + "." + command.permission();
		message = command.message();
		syntax = command.syntax();
		usesTarget = command.usesTarget();
		this.description = plugin.colorize(description);
		broadcast = plugin.findGroup(group);
	}
	
	public String getMessage(final Object... args) {
		return plugin.getMessage(message, args);
	}
	
	public String getSyntax(final String label) {
		if (isSubCommand) return String.format("/%s %s %s", label, name, syntax);
		return String.format("/%s %s", label, syntax);
	}
	
	public void setCommand() {
		isSubCommand = false;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getBroadcast() {
		return broadcast;
	}
	
	public CommandSender getSender() {
		return sender;
	}
	
	public void setSender(final CommandSender sender) {
		this.sender = sender;
		if (sender instanceof Player) setTarget((Player) sender);
	}
	
	public List<String> getArgs() {
		return args;
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
	
	public boolean hasPermission() {
		return hasPermission(sender);
	}
	
	public boolean hasPermission(final CommandSender sender) {
		return "".equals(permission) || plugin.hasPermission(sender, permission);
	}
	
	public void sendInfo(final CommandSender sender, final String label) {
		if (hasPermission(sender) && !"".equals(getDescription())) plugin.send(sender, plugin.getMessage("SYNTAX", getSyntax(label), getDescription()));
	}
	
	public Command init(final CommandSender sender, final String label, final String[] list) throws CommandException {
		setSender(sender);
		
		if (!hasPermission()) throw new CommandException(plugin.getMessage("PERMISSION"));
		
		args = new ArrayList<String>(Arrays.asList(list));
		args.remove(0);
		
		if (usesTarget) {
			if (args.size() > 0) {
				boolean foundAt = false;
				for (final String arg : args)
					if (arg.startsWith("@")) {
						foundAt = true;
						
						findPlayer(arg.substring(1));
						
						args.remove(arg);
						break;
					}
				
				if (!foundAt) {
					findPlayer(args.get(0));
					args.remove(0);
				}
			}
			
			if (target == null) throw new CommandException(plugin.getMessage("TARGET_REQUIRED"));
		}
		
		if (getRequiredArgs() > args.size()) throw new CommandException(plugin.getMessage("SYNTAX", getSyntax(label), getDescription()));
		
		return this;
	}
	
	private void findPlayer(final String name) throws CommandException {
		final List<Player> matches = sender.getServer().matchPlayer(name);
		if (matches.size() == 0) setTarget(null);
		else if (matches.size() == 1) setTarget(matches.get(0));
		else throw new CommandException(plugin.getMessage("TARGET_RESULTS", plugin.joinPlayers(matches)));
	}
}
