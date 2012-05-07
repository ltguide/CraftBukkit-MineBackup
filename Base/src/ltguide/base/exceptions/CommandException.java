package ltguide.base.exceptions;

public class CommandException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public CommandException(final String text) {
		super(text);
	}
}
