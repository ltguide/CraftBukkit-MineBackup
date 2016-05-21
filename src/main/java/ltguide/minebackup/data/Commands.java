package ltguide.minebackup.data;

public enum Commands implements ICommand {
	NEXT("manual", Messages.BACKUP_NEXT, "", false),
	NOW("manual", Messages.BACKUP_NOW, "", false),
	SOON("manual", Messages.BACKUP_SOON, "", false),
	UPLOAD("manual", Messages.BACKUP_UPLOAD, "", false),
	STATUS("status", Messages.STATUS_NOTE, "", false),
	DROPBOX("dropbox", Messages.DROPBOX, "", false),
	DROPBOXTOKEN("dropbox", Messages.DROPBOX, "<authToken>", false),
	RELOAD("reload", Messages.RELOAD, "", false),
	USAGE("usage", null, "", false);
	
	private String permission;
	private String message;
	private String syntax;
	private boolean usesTarget;
	
	Commands(final String permission, final Messages message, final String syntax, final boolean usesTarget) {
		this.permission = permission;
		this.message = message == null ? null : message.name();
		this.syntax = syntax;
		this.usesTarget = usesTarget;
	}
	
	public static Commands get(final String[] args) {
		try {
			return valueOf(args[0].toUpperCase());
		}
		catch (final Exception e) {
			return null;
		}
	}
	
	@Override
	public String permission() {
		return permission;
	}
	
	@Override
	public String message() {
		return message;
	}
	
	@Override
	public String syntax() {
		return syntax;
	}
	
	@Override
	public boolean usesTarget() {
		return usesTarget;
	}
}
