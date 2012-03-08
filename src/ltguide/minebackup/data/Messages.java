package ltguide.minebackup.data;

import ltguide.base.data.IEnum;
import ltguide.base.data.Message;

public enum Messages implements IEnum {
	PREFIX(false),
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
	BACKUP_DONE(true),
	DROPBOX(true),
	TARGET_NONE(true),
	TARGET_REQUIRED(true);
	
	public Message handle;
	
	Messages(final boolean usesPrefix) {
		handle = new Message(name(), usesPrefix);
		Message.put(handle);
	}
}
