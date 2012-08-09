package ltguide.base;

import java.util.logging.Logger;

public final class Debug {
	public static final boolean ON = true; // change to false for release
	private static Logger logger = Logger.getLogger("Minecraft");
	
	public static void info(final String msg) {
		logger.info("# " + msg);
	}
	
	public static void warning(final String msg) {
		logger.warning(msg);
	}
}
