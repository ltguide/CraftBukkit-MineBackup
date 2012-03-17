package ltguide.minebackup.data;

import ltguide.base.data.IMessage;

public enum Messages implements IMessage {
	BUSY(true),
	SYNTAX(false),
	PERMISSION(false),
	RELOAD(true),
	STATUS(true),
	STATUS_ACTION(false),
	STATUS_TIME_UNDER(false),
	STATUS_TIME_OVER(false),
	STATUS_TIME_NONE(false),
	STATUS_NOTE(true),
	BACKUP_NOW(true),
	BACKUP_SOON(true),
	BACKUP_NEXT(true),
	BACKUP_UPLOAD(true),
	BACKUP_DONE(true),
	ACTION_STARTING(true),
	ACTION_DONE(true),
	ACTION_SAVE(false),
	ACTION_COPY(false),
	ACTION_COMPRESS(false),
	DROPBOX(true),
	TARGET_NONE(true),
	TARGET_REQUIRED(true);
	
	private boolean usesPrefix;
	
	Messages(final boolean usesPrefix) {
		this.usesPrefix = usesPrefix;
	}
	
	@Override
	public boolean usesPrefix() {
		return usesPrefix;
	}
}
