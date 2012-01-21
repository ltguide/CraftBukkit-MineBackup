package ltguide.base.data;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

import org.bukkit.ChatColor;

public class Message {
	private static Map<String, Message> messages = new HashMap<String, Message>();
	
	private final String name;
	private final boolean usesPrefix;
	private String text = "";
	private Object[] args;
	
	public Message(final String name, final boolean usesPrefix) {
		this.name = name;
		this.usesPrefix = usesPrefix;
	}
	
	public static void put(final Message handle) {
		messages.put(handle.name, handle);
	}
	
	public static void setConfig(final String name, final String text) {
		messages.put(name, messages.get(name).setText(text));
	}
	
	public String getText() {
		try {
			return String.format(((usesPrefix ? messages.get("PREFIX").text + " " : "") + text).replaceAll("(?i)&([0-F])", "\u00A7$1"), args);
		}
		catch (final IllegalFormatException e) {
			return ChatColor.RED + "Error in " + name + " translation! (" + e.getMessage() + ")";
		}
	}
	
	public String getText(final Object[] args) {
		return setArgs(args).getText();
	}
	
	private Message setText(final String text) {
		this.text = text;
		return this;
	}
	
	private Message setArgs(final Object[] args) {
		this.args = args;
		return this;
	}
	
	public static Message get(final String name) {
		return messages.get(name);
	}
	
	public static Message get(final String name, final Object... args) {
		return messages.get(name).setArgs(args);
	}
	
	public static String getText(final String name, final Object... args) {
		return messages.get(name).getText(args);
	}
}
