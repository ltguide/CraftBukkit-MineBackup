package ltguide.base.data;

import java.util.IllegalFormatException;

import ltguide.base.Base;

import org.bukkit.ChatColor;

public class Message {
	private final Base plugin;
	private final String name;
	private String text = "";
	
	public Message(final Base instance, final IMessage message, String string) {
		plugin = instance;
		
		name = message.name();
		if (string == null) string = name + " (not defined in configuration)";
		text = (message.usesPrefix() ? plugin.messagePrefix : "") + plugin.colorize(string);
	}
	
	public String getText() {
		return text;
	}
	
	public String getText(final Object... args) {
		try {
			return String.format(text, args);
		}
		catch (final IllegalFormatException e) {
			return ChatColor.RED + "Error in " + name + " translation! (" + e.getMessage() + ")";
		}
	}
}
