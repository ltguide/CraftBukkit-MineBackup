package ltguide.minebackup.data;

import ltguide.base.data.IMessage;

public enum Messages implements IMessage {
	TARGET_NONE(true),
	TARGET_REQUIRED(true),
	SYNTAX(false),
	PERMISSION(false),
	BUSY(true),
	BACKUP_NEXT(true),
	BACKUP_NOW(true),
	BACKUP_SOON(true),
	BACKUP_UPLOAD(true),
	BACKUP_DONE(true),
	STATUS(true),
	STATUS_ACTION(false),
	STATUS_TIME_NONE(false),
	STATUS_TIME_OVER(false),
	STATUS_TIME_UNDER(false),
	STATUS_NOTE(true),
	ACTION_STARTING(true),
	ACTION_COMPRESS(false),
	ACTION_COPY(false),
	ACTION_SAVE(false),
	ACTION_DONE(true),
	DROPBOX(true),
	RELOAD(true),
	USAGE_MEMORY(true),
	USAGE_WORLD(true);
	
	private boolean usesPrefix;
	
	Messages(final boolean usesPrefix) {
		this.usesPrefix = usesPrefix;
	}
	
	@Override
	public boolean usesPrefix() {
		return usesPrefix;
	}
}
