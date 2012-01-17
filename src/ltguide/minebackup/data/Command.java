package ltguide.minebackup.data;

public enum Command {
	STATUS("status", Message.STATUS_NOTE),
	NOW("manual", Message.BACKUP_NOW),
	SOON("manual", Message.BACKUP_SOON),
	NEXT("manual", Message.BACKUP_NEXT),
	RELOAD("reload", Message.RELOAD),
	DROPBOX("dropbox", Message.DROPBOX, "<key> <secret>");
	
	private final String permission;
	private final Message message;
	private final String syntax;
	private String text;
	private String broadcast;
	
	Command(final String permission, final Message message) {
		this(permission, message, "");
	}
	
	Command(final String permission, final Message message, final String syntax) {
		this.permission = permission;
		this.message = message;
		this.syntax = syntax;
	}
	
	public static Command toValue(final String text) {
		try {
			return valueOf(text.toUpperCase());
		}
		catch (final Exception e) {
			return null;
		}
	}
	
	public String getPerm() {
		return permission;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public String getSyntax(final String label) {
		return String.format("/%s %s %s", label, name().toLowerCase(), syntax);
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(final String text) {
		this.text = text;
	}
	
	public String getBroadcast() {
		return broadcast;
	}
	
	public void setBroadcast(final String broadcast) {
		this.broadcast = broadcast;
	}
	
	@Override public String toString() {
		return getText().replaceAll("(?i)&([0-F])", "\u00A7$1");
	}
	
	public int getRequired() {
		return syntax.length() - syntax.replace("<", "").length();
	}
	
}
