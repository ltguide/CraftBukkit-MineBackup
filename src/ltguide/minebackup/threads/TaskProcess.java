package ltguide.minebackup.threads;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import ltguide.base.Debug;
import ltguide.base.utils.DirUtils;
import ltguide.base.utils.ZipUtils;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Process;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class TaskProcess extends Thread {
	private final MineBackup plugin;
	private boolean quick;
	private final SortedSet<Process> queue = new TreeSet<Process>(Process.comparator);
	private long msecs;
	private long startTime;
	
	public TaskProcess(final MineBackup instance) {
		plugin = instance;
	}
	
	public void reload() {
		for (final Process process : queue) {
			process.setNext(0L);
			plugin.persist.setNext(process);
		}
		
		queue.clear();
	}
	
	public void checkQueue(final boolean fill) {
		msecs = Calendar.getInstance().getTimeInMillis();
		startTime = plugin.startTime();
		if (Debug.ON) Debug.info("checkQueue(); fill=" + fill + "; msecs=" + msecs);
		
		LinkedHashSet<String> actions = plugin.actions;
		if (fill) {
			reload();
			actions = new LinkedHashSet<String>(actions);
			actions.remove("dropbox");
			actions.remove("ftp");
		}
		
		for (final String name : plugin.config.getOthers())
			checkQueue(actions, fill, "others", name);
		
		for (final World world : Bukkit.getWorlds())
			checkQueue(actions, fill, "worlds", world.getName());
		
		if (queue.size() > 150) plugin.warning("The action queue has " + queue.size() + " items. You need to increase the interval between actions.");
	}
	
	private void checkQueue(final LinkedHashSet<String> actions, final boolean fill, final String type, final String name) {
		if (Debug.ON) Debug.info("checking " + name);
		
		long next = 0L;
		long interval;
		final boolean loaded = plugin.config.load(type, name);
		final boolean dirty = plugin.persist.isDirty(type, name) || fill;
		
		for (final String action : actions) {
			if ((interval = plugin.config.getInterval(type, name, action)) == 0) continue;
			
			next = plugin.persist.getNext(type, name, action);
			if (Debug.ON) Debug.info(" | " + action + " next=" + next + " interval=" + interval + " dirty=" + dirty + " loaded=" + loaded + " >msecs=" + (next > msecs));
			
			if (loaded) next = 0L;
			else if (next > msecs || !dirty && next == 0L) continue;
			
			final Process process = new Process(type, name, action, next);
			
			if (next == 0L) {
				if (fill) interval = 0;
				else if (interval < 0) interval = getNextExact(interval);
				
				process.setNext(msecs++ + interval);
			}
			
			queue.add(process);
			plugin.persist.setNext(process);
		}
		
		plugin.persist.setClean(type, name);
	}
	
	public void fillUploadQueue() {
		final HashSet<String> actions = new HashSet<String>(Arrays.asList("dropbox", "ftp"));
		
		for (final String name : plugin.config.getOthers())
			fillUploadQueue(actions, "others", name);
		
		for (final World world : Bukkit.getWorlds())
			fillUploadQueue(actions, "worlds", world.getName());
	}
	
	private void fillUploadQueue(final HashSet<String> actions, final String type, final String name) {
		for (final String action : actions)
			if (plugin.config.getInterval(type, name, action) != 0) process(new Process(type, name, action, 0L));
	}
	
	@Override
	public void run() {
		if (Debug.ON) Debug.info("TaskProcess run()");
		if (plugin.isWorking(this)) {
			plugin.info(" - TaskProcess already working - it's already been a minute?!");
			return;
		}
		
		plugin.setWorking(this, true);
		
		if (isQuick()) {
			runQuick();
			plugin.spawnProcess(60);
		}
		else runOnce();
	}
	
	private void runOnce() {
		checkQueue(false);
		
		if (queue.size() > 0) {
			final Process process = queue.first();
			if (msecs >= process.getNext()) {
				queue.remove(process);
				
				process(process);
				
				if (plugin.persist.isDirty(process)) {
					long interval = plugin.config.getInterval(process);
					if (interval < 0) interval = getNextExact(interval);
					
					process.setNext(msecs + interval);
					queue.add(process);
				}
				else process.setNext(0L);
				
				plugin.persist.setNext(process);
			}
		}
		else if (Debug.ON) Debug.info("runOnce() - but nothing in queue");
		if (Debug.ON) Debug.info("> total " + plugin.stopTime(startTime));
		
		plugin.setWorking(this, false);
	}
	
	private void runQuick() {
		checkQueue(true);
		
		for (final Process action : queue)
			process(action);
		
		reload();
		
		plugin.debug("> total " + plugin.stopTime(startTime));
		plugin.broadcast(null, plugin.getCmd("NOW").getBroadcast(), plugin.getMessage("BACKUP_DONE", ""));
		plugin.setWorking(this, false);
	}
	
	private boolean isQuick() {
		return quick;
	}
	
	public TaskProcess setQuick(final boolean quick) {
		this.quick = quick;
		return this;
	}
	
	private void process(final Process process) {
		if (Debug.ON) Debug.info("process queue: " + process.getName() + " " + process.getAction() + " @ " + process.getNext());
		
		boolean isBroadcast = false;
		final World world = plugin.getServer().getWorld(process.getName());
		
		if ("save".equals(process.getAction())) {
			if (world == null) return;
			
			isBroadcast = broadcast(process);
			logAction(" * saving %s\\%s", process);
			plugin.startTime();
			
			try {
				plugin.syncCall("save", world).get();
			}
			catch (final Exception e) {
				plugin.debug("process()->syncCall('save'): " + e.toString());
			}
			
			plugin.debug("  \\ done " + plugin.stopTime());
		}
		else if ("cleanup".equals(process.getAction())) plugin.persist.processKeep(process, null);
		else if ("dropbox".equals(process.getAction()) || "ftp".equals(process.getAction())) {
			if (plugin.hasAction(process.getAction()) && plugin.persist.addUpload(process.getAction(), process)) logAction(" * queuing " + process.getAction() + " upload of latest %s\\%s", process);
		}
		else if ("compress".equals(process.getAction()) || "copy".equals(process.getAction())) {
			final String format = getFormat(process.getName(), world);
			final String prepend = plugin.config.getDestPrepend() ? process.getName() : null;
			final FilenameFilter filter = plugin.config.getFilenameFilter(process);
			File target = null;
			final String backupDir = plugin.config.getDir("destination", process).getPath();
			final File sourceDir = plugin.config.getDir(process.getType(), process);
			
			if (!sourceDir.exists()) {
				plugin.warning(String.format("%% unable to %s %s (check path: %s)", process.getAction(), process.getName(), sourceDir.getPath()));
				return;
			}
			
			isBroadcast = broadcast(process);
			logAction(" * %3$sing %s\\%s", process);
			plugin.startTime();
			
			try {
				if ("copy".equals(process.getAction())) DirUtils.copyDir(sourceDir, target = new File(backupDir, format), prepend, filter);
				else ZipUtils.zipDir(sourceDir, target = new File(backupDir, format + ".zip"), prepend, plugin.config.getInt(process, "compression_level"), filter);
				
				plugin.debug("  \\ done " + plugin.stopTime());
				
				plugin.persist.processKeep(process, target);
			}
			catch (final Exception e) {
				plugin.info("  \\ failed");
				plugin.logException(e, process.getAction() + ": " + sourceDir + " -> " + target);
				
				DirUtils.delete(target);
				if (target.exists()) plugin.warning("unable to delete: " + target);
			}
		}
		
		if (isBroadcast) plugin.getServer().broadcastMessage(plugin.getMessage("ACTION_DONE"));
	}
	
	private boolean broadcast(final Process process) {
		if (plugin.config.getBoolean(process, "broadcast")) {
			plugin.getServer().broadcastMessage(plugin.getMessage("ACTION_STARTING", plugin.getMessage("ACTION_" + process.getAction().toUpperCase()), process.getType() + "\\" + process.getName()));
			
			try {
				sleep(5000);
			}
			catch (final Exception e) {
				plugin.debug("broadcast(process)->sleep(): " + e.toString());
			}
			
			return plugin.strings.getString("action_done") != null;
		}
		
		return false;
	}
	
	private void logAction(final String format, final Process process) {
		plugin.info(String.format(format, process.getType(), process.getName(), process.getAction()));
	}
	
	private String padZero(final int i) {
		return String.format("%02d", i).toString();
	}
	
	private String getFormat(final String name, final World world) {
		final Calendar calendar = Calendar.getInstance();
		final Map<String, String> formats = new HashMap<String, String>();
		formats.put("%Y", String.valueOf(calendar.get(Calendar.YEAR)));
		formats.put("%M", padZero(calendar.get(Calendar.MONTH) + 1));
		formats.put("%D", padZero(calendar.get(Calendar.DAY_OF_MONTH)));
		formats.put("%H", padZero(calendar.get(Calendar.HOUR_OF_DAY)));
		formats.put("%m", padZero(calendar.get(Calendar.MINUTE)));
		formats.put("%S", padZero(calendar.get(Calendar.SECOND)));
		
		if (world == null) {
			formats.put("%W", name);
			formats.put("%U", "0");
			formats.put("%s", "0");
		}
		else {
			formats.put("%W", world.getName());
			formats.put("%U", world.getUID().toString());
			formats.put("%s", String.valueOf(world.getSeed()));
		}
		
		String format = plugin.config.getDestFormat();
		for (final Entry<String, String> entry : formats.entrySet())
			format = format.replaceAll(entry.getKey(), entry.getValue());
		
		return format;
	}
	
	private long getNextExact(long interval) {
		final Calendar calendar = Calendar.getInstance();
		final long now = (calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60) * 1000;
		
		interval *= -1;
		if (interval > now) return interval - now;
		
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.add(Calendar.MILLISECOND, (int) interval);
		
		return calendar.getTimeInMillis() - msecs;
	}
}
