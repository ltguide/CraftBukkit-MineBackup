package ltguide.minebackup;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.minebackup.configuration.Config;
import ltguide.minebackup.configuration.Persist;
import ltguide.minebackup.listeners.CommandListener;
import ltguide.minebackup.listeners.PlayerListener;
import ltguide.minebackup.listeners.WorldListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

public class MineBackup extends JavaPlugin {
	private int processId = -1;
	private int dropboxId = -1;
    private int ftpId = -1;
	private final TaskProcess process = new TaskProcess(this);
	private final TaskDropbox dropbox = new TaskDropbox(this);
    private final TaskFTP ftp = new TaskFTP(this);
	private final Set<String> working = new HashSet<String>();
	public Config config;
	public Persist persist;

	@Override public void onDisable() {
		for (final World world : Bukkit.getWorlds())
			world.setAutoSave(true);

		if (persist != null) persist.saveConfig();
	}

	@Override public void onEnable() {
		if (Debug.ON) Debug.init(this);
		Base.init(this);
		persist = new Persist(this);
		config = new Config(this);

		for (final World world : Bukkit.getWorlds())
			world.setAutoSave(false);

		getCommand("minebackup").setExecutor(new CommandListener(this));

		final PlayerListener playerListener = new PlayerListener(this);
		final PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.WORLD_SAVE, new WorldListener(this), Priority.Monitor, this);

		Base.info("v" + getDescription().getVersion() + " enabled");

		spawnProcess(60);
		spawnDropbox();
        spawnFTP();
	}

	public void reload() {
		config.reload();
		persist.reload();
		process.reload();
		spawnDropbox();
        spawnFTP();
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

	public void spawnDropbox() {
		if (isWorking(dropbox)) return;
		if (Debug.ON) Debug.info("attempting to spawnDropbox()");

		if (dropboxRunning()) getServer().getScheduler().cancelTask(dropboxId);

		if (!config.hasAction("dropbox") || !dropbox.hasDropboxAuth()) return;
		if (Debug.ON) Debug.info("spawnDropbox()");

		dropboxId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, dropbox, 30 * 20L, 300 * 20L);
	}
    public void spawnFTP() {
    		if (isWorking(ftp)) return;
    		if (Debug.ON) Debug.info("attempting to spawnFTP()");

    		if (ftpRunning()) getServer().getScheduler().cancelTask(ftpId);

    		if (!config.hasAction("ftp")) return;
    		if (Debug.ON) Debug.info("spawnFTP()");

    		ftpId = getServer().getScheduler().scheduleAsyncRepeatingTask(this, ftp, 30 * 20L, 300 * 20L);
    	}



	public Future<Boolean> callSync(final String action, final World world) {
		return getServer().getScheduler().callSyncMethod(this, new CallSync(this, action, world));
	}

	protected synchronized void setWorking(final Thread thread, final boolean working) {
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

	public boolean dropboxRunning() {
		return dropboxId != -1;
	}

    public boolean ftpRunning() {
        return ftpId != -1;
    }
}
