package ltguide.minebackup;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.minebackup.configuration.Config;
import ltguide.minebackup.configuration.Persist;
import ltguide.minebackup.listeners.CommandListener;
import ltguide.minebackup.listeners.PlayerListener;
import ltguide.minebackup.listeners.WorldListener;
import ltguide.minebackup.threads.SyncCall;
import ltguide.minebackup.threads.TaskProcess;
import ltguide.minebackup.threads.TaskUpload;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class MineBackup extends Base {
	private int processId = -1;
	private int uploadId = -1;
	private final TaskProcess process = new TaskProcess(this);
	private final TaskUpload upload = new TaskUpload(this);
	private final HashSet<String> working = new HashSet<String>();
	public Config config;
	public Persist persist;
	public LinkedHashSet<String> actions = new LinkedHashSet<String>(Arrays.asList("save", "copy", "compress", "cleanup"));
	
	@Override
	public void onDisable() {
		debug("Forcing world save back ON.");
		
		for (final World world : Bukkit.getWorlds())
			world.setAutoSave(true);
		
		if (persist != null) persist.saveConfig();
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
		
		persist = new Persist(this);
		config = new Config(this);
		
		if (!config.hasAction("save")) warning("You have NOT enabled any worlds to be automatically saved. This plugin needs to control world saving to prevent backup corruption.");
		else for (final World world : Bukkit.getWorlds())
			world.setAutoSave(false);
		
		new CommandListener(this);
		new PlayerListener(this);
		new WorldListener(this);
		
		spawnProcess(60);
		spawnUpload();
	}
	
	public void reload() {
		config.reload();
		persist.reload();
		process.reload();
		spawnUpload();
	}
	
	public void fillProcessQueue() {
		process.checkQueue(true);
	}
	
	public int spawnProcess() {
		return getServer().getScheduler().scheduleAsyncDelayedTask(this, process, 5 * 20L);
	}
	
	public void spawnProcess(int delay) {
		if (isWorking(process)) return;
		
		if (processId != -1) getServer().getScheduler().cancelTask(processId);
		
		if (delay == 0) {
			if (Debug.ON) Debug.info("spawnProcess(); delay 0 (quick mode)");
			process.setQuick(true);
			
			processId = spawnProcess();
		}
		else {
			if (delay == 60) delay -= Calendar.getInstance().get(Calendar.SECOND);
			if (Debug.ON) Debug.info("spawnProcess(); delaying " + delay);
			
			processId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, process.setQuick(false), delay * 20L, 60 * 20L);
		}
	}
	
	public void spawnUpload() {
		if (isWorking(upload)) return;
		
		if (uploadId != -1) getServer().getScheduler().cancelTask(uploadId);
		
		checkUpload("dropbox");
		checkUpload("ftp");
		if (actions.size() == 4) return;
		
		if (Debug.ON) Debug.info("spawnUpload()");
		uploadId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, upload, 30 * 20L, 300 * 20L);
	}
	
	public Future<Boolean> syncCall(final String action, final World world) {
		return getServer().getScheduler().callSyncMethod(this, new SyncCall(this, action, world));
	}
	
	public synchronized void setWorking(final Thread thread, final boolean working) {
		final String name = thread.getClass().getSimpleName();
		
		if (working) this.working.add(name);
		else this.working.remove(name);
		
		if (working == false && "TaskProcess".equals(name)) persist.saveConfig();
	}
	
	public synchronized boolean isWorking() {
		return working.size() > 0;
	}
	
	public synchronized boolean isWorking(final Thread thread) {
		return working.contains(thread.getClass().getSimpleName());
	}
	
	private void checkUpload(final String type) {
		if (Debug.ON) Debug.info("checkUpload(\"" + type + "\")");
		
		if (config.hasAction(type) && upload.hasAuth(type)) actions.add(type);
		else actions.remove(type);
	}
	
	public boolean hasAction(final String action) {
		return actions.contains(action);
	}
}
