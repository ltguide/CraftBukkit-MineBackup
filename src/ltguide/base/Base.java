package ltguide.base;

import java.util.logging.Level;
import java.util.logging.Logger;

import ltguide.base.data.Command;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Base {
	public static final int bufferSize = 4 * 1024;
	
	private static boolean debug = true;
	private static Logger logger;
	private static String name;
	private static Server server;
	private static long startTime;
	
	public static void init(final JavaPlugin instance) {
		server = instance.getServer();
		logger = server.getLogger();
		name = instance.getDescription().getName();
	}
	
	public static void setDebug(final boolean _debug) {
		debug = _debug;
	}
	
	public static void info(final String msg) {
		log(Level.INFO, msg);
	}
	
	public static void warning(final String msg) {
		log(Level.WARNING, msg);
	}
	
	public static void severe(final String msg) {
		log(Level.SEVERE, msg);
	}
	
	public static void log(final Level level, final String msg) {
		logRaw(level, "[" + name + "] " + msg);
	}
	
	private static void logRaw(final Level level, final String msg) {
		logger.log(level, ChatColor.stripColor(msg));
	}
	
	public static void logException(final Exception e, final String msg) {
		severe("---------------------------------------");
		if (!"".equals(msg)) severe("# " + msg);
		
		severe(e.toString());
		for (final StackTraceElement stack : e.getStackTrace())
			severe("\t" + stack.toString());
		
		severe("---------------------------------------");
	}
	
	public static void debug(final String msg) {
		if (debug || Debug.ON) Base.info(msg);
	}
	
	public static void send(final CommandSender sender, final String msg) {
		if (sender instanceof Player) {
			sender.sendMessage(msg);
			info("->" + sender.getName() + " " + msg);
		}
		else logRaw(Level.INFO, msg);
	}
	
	public static void broadcast(final CommandSender sender, final String permission, final String msg) {
		if (permission == null || "".equals(permission)) send(sender, msg);
		else server.broadcast(msg, permission);
	}
	
	public static void broadcast(final CommandSender sender, final Command command) {
		Base.broadcast(sender, command.getBroadcast(), command.getMessage(sender.getName()));
	}
	
	public static void startTime() {
		startTime = System.nanoTime();
	}
	
	public static String stopTime() {
		return stopTime(startTime);
	}
	
	public static String stopTime(final long startTime) {
		return String.format("%.2fms", (System.nanoTime() - startTime) * 1e-6);
	}
}
