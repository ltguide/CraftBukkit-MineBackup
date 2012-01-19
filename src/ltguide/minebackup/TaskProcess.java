package ltguide.minebackup;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import ltguide.debug.Debug;
import ltguide.minebackup.data.Command;
import ltguide.minebackup.data.Message;
import ltguide.minebackup.data.Process;
import ltguide.minebackup.utils.DirUtils;
import ltguide.minebackup.utils.ZipUtils;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class TaskProcess extends Thread {
	private final MineBackup plugin;
	private boolean quick;
	private final SortedSet<Process> queue = new TreeSet<Process>(Process.comparator);
	private long msecs;
	
	public TaskProcess(final MineBackup plugin) {
		this.plugin = plugin;
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
		if (Debug.ON) plugin.ifDebug("checkQueue(); fill=" + fill);
		
		final List<String> actions = new ArrayList<String>(Arrays.asList("save", "copy", "compress", "dropbox"));
		if (fill) {
			reload();
			actions.remove("dropbox");
		}
		
		for (final String name : plugin.config.getOthers())
			checkQueue(actions, fill, "others", name);
		
		for (final World world : Bukkit.getWorlds())
			checkQueue(actions, fill, "worlds", world.getName());
	}
	
	private void checkQueue(final List<String> actions, final boolean fill, final String type, final String name) {
		long next = 0L;
		long interval;
		final boolean load = plugin.persist.load(name, type);
		
		if (fill || plugin.persist.isDirty(type, name) || load) {
			if (Debug.ON) plugin.ifDebug("checking " + name);
			
			for (final String action : actions)
				if ((interval = plugin.config.getInterval(type, name, action)) != 0 && ((next = plugin.persist.getNext(type, name, action)) < msecs || load)) {
					if (Debug.ON) plugin.ifDebug(" | " + action + " time=" + next + " interval=" + interval);
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
	}
	
	@Override public void run() {
		if (Debug.ON) plugin.ifDebug("TaskProcess run()");
		if (plugin.isWorking(this)) {
			plugin.log(" - TaskProcess already working - it's already been a minute?!");
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
		if (Debug.ON) plugin.ifDebug("runOnce() @ " + msecs);
		
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
		else if (Debug.ON) plugin.ifDebug("p \\ but nothing in queue");
		
		plugin.setWorking(this, false);
	}
	
	private void runQuick() {
		checkQueue(true);
		
		for (final Process action : queue)
			process(action);
		
		reload();
		
		plugin.broadcast(null, Command.NOW.getBroadcast(), Message.BACKUP_DONE.toString());
		plugin.setWorking(this, false);
	}
	
	private boolean isQuick() {
		return quick;
	}
	
	protected TaskProcess setQuick(final boolean quick) {
		this.quick = quick;
		return this;
	}
	
	private void process(final Process process) {
		if (Debug.ON) plugin.ifDebug("process queue: " + process.getName() + " " + process.getAction() + " @ " + process.getNext());
		
		final World world = plugin.getServer().getWorld(process.getName());
		
		if ("save".equals(process.getAction())) {
			if (world == null) return;
			plugin.log(" * saving " + process.getName());
			
			final long start = System.nanoTime();
			
			try {
				plugin.callSync("save", world).get();
			}
			catch (final Exception e) {
				plugin.logException(e, "");
			}
			
			plugin.debug("\t\\ done " + plugin.duration(start));
		}
		else if ("dropbox".equals(process.getAction())) {
			if (plugin.dropboxRunning() && plugin.persist.addDropboxUpload(process)) plugin.log(" * queuing upload of " + process.getName());
		}
		else {
			plugin.log(" * " + process.getAction() + "ing " + process.getName());
			
			final String format = getFormat(process.getName(), world);
			final String backupDir = plugin.config.getDir("destination", process).getPath();
			final File sourceDir = plugin.config.getDir(process.getType(), process);
			final String prepend = plugin.config.getDestPrepend() ? process.getName() : null;
			final FilenameFilter filter = plugin.config.getFilenameFilter(process);
			File target = null;
			final long start = System.nanoTime();
			
			try {
				if ("copy".equals(process.getAction())) DirUtils.copyDir(plugin, sourceDir, target = new File(backupDir, format), prepend, filter);
				else ZipUtils.zipDir(plugin, sourceDir, target = new File(backupDir, format + ".zip"), prepend, plugin.config.getInt(process, "compression_level"), filter);
				
				plugin.debug("\t\\ done " + plugin.duration(start));
				
				plugin.persist.addKeep(process, target);
			}
			catch (final Exception e) {
				plugin.log("\t\\ failed");
				plugin.logException(e, process.getAction() + ": " + sourceDir + " -> " + target);
				
				DirUtils.delete(target);
				if (target.exists()) plugin.log(Level.WARNING, "unable to delete: " + target);
			}
		}
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
