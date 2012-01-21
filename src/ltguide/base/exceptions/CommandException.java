package ltguide.base.exceptions;

import ltguide.base.data.Message;

public class CommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public CommandException(final Message message) {
		super(message.getText());
	}
	
	public CommandException(final Message message, final Object... args) {
		super(message.getText(args));
	}
}