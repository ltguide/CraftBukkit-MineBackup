package ltguide.minebackup.exceptions;

@SuppressWarnings("serial") public class DropboxException extends Exception {
	
	protected DropboxException() {
		super();
	}
	
	public DropboxException(final String message) {
		super(message);
	}
	
	public DropboxException(final Exception e) {
		super(e);
	}
	
	public DropboxException(final int code) {
		super(getMsg(code));
	}
	
	public static String getMsg(final int code) {
		switch (code) {
			case 401:
				return "UNAUTHORIZED: Dropbox configuration needs to be set again.";
			case 500:
				return "INTERNAL_SERVER_ERROR: Try again later.";
			case 502:
				return "BAD_GATEWAY: Try again later.";
			case 503:
				return "SERVICE_UNAVAILABLE: Try again later.";
			default:
				return "Misc HTTP error. (" + code + ")";
		}
	}
}
