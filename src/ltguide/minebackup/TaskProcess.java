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

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.data.Message;
import ltguide.base.utils.DirUtils;
import ltguide.base.utils.ZipUtils;
import ltguide.minebackup.data.Commands;
import ltguide.minebackup.data.Process;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class TaskProcess extends Thread {
	private final MineBackup plugin;
	private boolean quick;
	private final SortedSet<Process> queue = new TreeSet<Process>(Process.comparator);
	private long msecs;
	private long startTime;
	
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
		startTime = Base.startTime();
		if (Debug.ON) Debug.info("checkQueue(); fill=" + fill + "; msecs=" + msecs);
		
		List<String> actions = Arrays.asList("save", "copy", "compress", "cleanup", "dropbox");
		if (fill) {
			reload();
			actions = new ArrayList<String>(actions);
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
		
		final boolean load = plugin.config.load(type, name);
		
		if (fill || load || plugin.persist.isDirty(type, name)) {
			if (Debug.ON) Debug.info("checking " + name);
			
			for (final String action : actions)
				if ((interval = plugin.config.getInterval(type, name, action)) != 0 && ((next = plugin.persist.getNext(type, name, action)) < msecs || load)) {
					if (Debug.ON) Debug.info(" | " + action + " time=" + next + " interval=" + interval);
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
		if (Debug.ON) Debug.info("TaskProcess run()");
		if (plugin.isWorking(this)) {
			Base.info(" - TaskProcess already working - it's already been a minute?!");
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
			
			if (Debug.ON) Debug.info("+ total " + Base.stopTime(startTime));
		}
		else if (Debug.ON) Debug.info("p \\ but nothing in queue");
		
		plugin.setWorking(this, false);
	}
	
	private void runQuick() {
		checkQueue(true);
		
		for (final Process action : queue)
			process(action);
		
		reload();
		
		Base.debug("+ total " + Base.stopTime(startTime));
		Base.broadcast(null, Commands.NOW.handle.getBroadcast(), Message.getText("BACKUP_DONE"));
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
		if (Debug.ON) Debug.info("process queue: " + process.getName() + " " + process.getAction() + " @ " + process.getNext());
		
		final World world = plugin.getServer().getWorld(process.getName());
		
		if ("save".equals(process.getAction())) {
			if (world == null) return;
			
			Base.info(" * saving " + process.getName());
			Base.startTime();
			
			try {
				plugin.callSync("save", world).get();
			}
			catch (final Exception e) {
				Base.logException(e, "");
			}
			
			Base.debug("\t\\ done " + Base.stopTime());
		}
		else if ("cleanup".equals(process.getAction())) plugin.persist.processKeep(process, null);
		else if ("dropbox".equals(process.getAction())) {
			if (plugin.dropboxRunning() && plugin.persist.addDropboxUpload(process)) Base.info(" * queuing upload of " + process.getName());
		}
		else {
			final String format = getFormat(process.getName(), world);
			final String backupDir = plugin.config.getDir("destination", process).getPath();
			final File sourceDir = plugin.config.getDir(process.getType(), process);
			final String prepend = plugin.config.getDestPrepend() ? process.getName() : null;
			final FilenameFilter filter = plugin.config.getFilenameFilter(process);
			File target = null;
			
			Base.info(" * " + process.getAction() + "ing " + process.getName());
			Base.startTime();
			
			try {
				if ("copy".equals(process.getAction())) DirUtils.copyDir(sourceDir, target = new File(backupDir, format), prepend, filter);
				else ZipUtils.zipDir(sourceDir, target = new File(backupDir, format + ".zip"), prepend, plugin.config.getInt(process, "compression_level"), filter);
				
				Base.debug("\t\\ done " + Base.stopTime());
				
				plugin.persist.processKeep(process, target);
			}
			catch (final Exception e) {
				Base.info("\t\\ failed");
				Base.logException(e, process.getAction() + ": " + sourceDir + " -> " + target);
				
				DirUtils.delete(target);
				if (target.exists()) Base.warning("unable to delete: " + target);
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
