package ltguide.base;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Debug {
	public static final boolean ON = false;
	private static Logger logger;

	public static void init(final JavaPlugin instance) {
		logger = instance.getServer().getLogger();
	}

	public static void info(final String msg) {
		logger.info("# " + msg);
	}
}
