package ltguide.minebackup.data;

import java.util.IllegalFormatException;

import org.bukkit.ChatColor;

public enum Message {
	PREFIX(false),
	BUSY,
	SYNTAX(false),
	PERMISSION(false),
	RELOAD,
	STATUS,
	STATUS_ACTION(false),
	STATUS_TIME_UNDER(false),
	STATUS_TIME_OVER(false),
	STATUS_TIME_NONE(false),
	STATUS_NOTE,
	BACKUP_NOW,
	BACKUP_SOON,
	BACKUP_NEXT,
	BACKUP_DONE,
	DROPBOX;
	
	private boolean prefix;
	private String text = "";
	
	Message() {
		this(true);
	}
	
	Message(final boolean prefix) {
		this.prefix = prefix;
	}
	
	public boolean isPrefixed() {
		return prefix;
	}
	
	public String getText() {
		return (isPrefixed() ? PREFIX.toString() + " " : "") + text;
	}
	
	public void setText(final String text) {
		this.text = text;
	}
	
	@Override public String toString() {
		return getText().replaceAll("(?i)&([0-F])", "\u00A7$1");
	}
	
	public String toString(final Object... args) {
		try {
			return String.format(toString(), args);
		}
		catch (final IllegalFormatException e) {
			return ChatColor.RED + "Error in " + name() + " translation! (" + e.getMessage() + ")";
		}
	}
}
