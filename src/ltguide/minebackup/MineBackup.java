package ltguide.minebackup;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;

import ltguide.debug.Debug;
import ltguide.minebackup.data.Command;
import ltguide.minebackup.data.Config;
import ltguide.minebackup.data.Persist;
import ltguide.minebackup.listeners.MineBackupCommandListener;
import ltguide.minebackup.listeners.MineBackupPlayerListener;
import ltguide.minebackup.listeners.MineBackupWorldListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MineBackup extends JavaPlugin {
	private int processId = -1;
	private int dropboxId = -1;
	private final TaskProcess process = new TaskProcess(this);
	private final TaskDropbox dropbox = new TaskDropbox(this);
	private final Set<String> working = new HashSet<String>();
	public Config config;
	public Persist persist;
	
	public ClassLoader getClazzLoader() {
		return getClassLoader();
	}
	
	@Override public void onDisable() {
		for (final World world : Bukkit.getWorlds())
			world.setAutoSave(true);
		
		if (persist != null) persist.save();
	}
	
	@Override public void onEnable() {
		for (final World world : Bukkit.getWorlds())
			world.setAutoSave(false);
		
		getCommand("minebackup").setExecutor(new MineBackupCommandListener(this));
		
		final MineBackupPlayerListener playerListener = new MineBackupPlayerListener(this);
		final PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.WORLD_SAVE, new MineBackupWorldListener(this), Priority.Monitor, this);
		
		persist = new Persist(this);
		config = new Config(this);
		
		log("v" + getDescription().getVersion() + " enabled");
		
		spawnProcess(60); // 60
		spawnDropbox();
	}
	
	public int spawnProcess() {
		return getServer().getScheduler().scheduleAsyncDelayedTask(this, process, 5 * 20L);
	}
	
	public void spawnProcess(int delay) {
		if (isWorking(process)) return;
		
		if (processId != -1) getServer().getScheduler().cancelTask(processId);
		
		if (delay == 0) {
			if (Debug.ON) ifDebug("spawnProcess(); delay 0 (quick mode)");
			process.setQuick(true);
			
			processId = spawnProcess();
		}
		else {
			if (delay == 60) delay -= Calendar.getInstance().get(Calendar.SECOND);
			if (Debug.ON) ifDebug("spawnProcess(); delaying " + delay);
			
			processId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, process.setQuick(false), delay * 20L, 60 * 20L);
		}
	}
	
	public void spawnDropbox() {
		if (isWorking(dropbox)) return;
		if (Debug.ON) ifDebug("attempting to spawnDropbox()");
		
		if (dropboxRunning()) getServer().getScheduler().cancelTask(dropboxId);
		
		if (!config.hasDropboxAction() || !dropbox.hasDropboxAuth()) return;
		if (Debug.ON) ifDebug("spawnDropbox()");
		
		dropboxId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, dropbox, 30 * 20L, 300 * 20L);
	}
	
	public Future<Boolean> callSync(final String action, final World world) {
		return getServer().getScheduler().callSyncMethod(this, new CallSync(this, action, world));
	}
	
	protected synchronized void setWorking(final Thread thread, final boolean working) {
		final String name = thread.getClass().getSimpleName();
		
		if (working) this.working.add(name);
		else this.working.remove(name);
		
		if (working == false && "TaskProcess".equals(name)) persist.save();
	}
	
	public synchronized boolean isWorking() {
		return working.size() > 0;
	}
	
	public synchronized boolean isWorking(final Thread thread) {
		return working.contains(thread.getClass().getSimpleName());
	}
	
	public void broadcast(final CommandSender sender, final Command command) {
		broadcast(sender, command.getBroadcast(), command.getMessage().toString(sender.getName()));
	}
	
	public void broadcast(final CommandSender sender, final String broadcast, final String message) {
		if (broadcast == null) send(sender, message);
		else getServer().broadcast(message, broadcast);
	}
	
	public void send(final CommandSender sender, final String msg) {
		if (sender instanceof Player) {
			sender.sendMessage(msg);
			log("->" + sender.getName() + " " + msg);
		}
		else logRaw(Level.INFO, msg);
	}
	
	protected void log(final String msg) {
		log(Level.INFO, msg);
	}
	
	public void log(final Level level, final String msg) {
		logRaw(level, "[" + getDescription().getName() + "] " + msg);
	}
	
	public void logRaw(final Level level, final String msg) {
		getServer().getLogger().log(level, ChatColor.stripColor(msg));
	}
	
	public void logException(final Exception e, final String msg) {
		log(Level.SEVERE, "---------------------------------------");
		if (!"".equals(msg)) log(Level.SEVERE, "DEBUG: " + msg);
		
		log(Level.SEVERE, e.toString());
		for (final StackTraceElement stack : e.getStackTrace())
			log(Level.SEVERE, "\t" + stack.toString());
		
		log(Level.SEVERE, "---------------------------------------");
	}
	
	public void debug(final String msg) {
		if (getConfig().getBoolean("debug")) log(msg);
	}
	
	public void ifDebug(final String msg) {
		if (Debug.ON) log("# " + msg);
	}
	
	public String duration(final long start) {
		return String.format("%.2fms", (System.nanoTime() - start) * 1e-6);
	}
	
	public boolean dropboxRunning() {
		return dropboxId != -1;
	}
	
	public void fillProcessQueue() {
		process.checkQueue(true);
	}
	
	public void reload() {
		config.reload();
		persist.reload();
		process.reload();
		spawnDropbox();
	}
}
