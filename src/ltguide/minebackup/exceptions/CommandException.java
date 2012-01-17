package ltguide.minebackup.exceptions;

import ltguide.minebackup.data.Message;

public class CommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public CommandException(final Message message) {
		super(message.toString());
	}
	
	public CommandException(final Message message, final Object... args) {
		super(message.toString(args));
	}
}