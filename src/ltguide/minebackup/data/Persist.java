package ltguide.minebackup.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.data.Configuration;
import ltguide.base.utils.DirUtils;
import ltguide.minebackup.MineBackup;

import org.bukkit.World;

public class Persist extends Configuration {
	private static final long maxDropboxSize = 180 * 1024 * 1024;
	
	public Persist(final MineBackup instance) {
		super(instance, "persist.dat");
		
		loadConfig();
	}
	
	public void reload() {
		loadConfig();
	}
	
	public void setDirty(final World world) {
		set("worlds." + world.getName() + ".dirty", true);
	}
	
	public void setDirty() {
		for (final String name : ((MineBackup) plugin).config.getOthers())
			set("others." + name + ".dirty", true);
	}
	
	public void setClean(final String type, final String name) {
		set(type + "." + name + ".dirty", false);
	}
	
	public boolean isDirty(final Process process) {
		return isDirty(process.getType(), process.getName());
	}
	
	public boolean isDirty(final String type, final String name) {
		World world = null;
		
		if ("worlds".equals(type) && (world = plugin.getServer().getWorld(name)) == null) return false;
		
		try {
			return getBoolean(type + "." + name + ".dirty") || ("Server thread".equals(Thread.currentThread().getName()) ? hasPlayers(world) : ((MineBackup) plugin).callSync("count", world).get());
		}
		catch (final Exception e) {
			Base.logException(e, "");
			return false;
		}
	}
	
	public boolean hasPlayers(final World world) {
		if (world != null) return world.getPlayers().size() > 0;
		
		return plugin.getServer().getOnlinePlayers().length > 0;
	}
	
	public long getNext(final String type, final String name, final String action) {
		return getLong(type + "." + name + "." + action + ".next", 0);
	}
	
	public void setNext(final Process process) {
		if (Debug.ON) Debug.info(" \\ " + process.getType() + "." + process.getName() + "." + process.getAction() + ".next=" + process.getNext());
		set(process.getType() + "." + process.getName() + "." + process.getAction() + ".next", process.getNext());
	}
	
	public void addKeep(final Process process, final File target) {
		final int num = ((MineBackup) plugin).config.getInt(process, "keep");
		final String path = process.getType() + "." + process.getName() + ".keep";
		
		List<String> keep = getStringList(path);
		if (keep == null) keep = new ArrayList<String>();
		
		keep.add(target.getPath());
		
		if (keep.size() > num) {
			Base.debug(" * deleting old backups");
			Base.startTime();

			while (keep.size() > num) {
				final String backup = keep.get(0);
				keep.remove(0);
				
				final File file = new File(backup);
				if (file.exists()) {
					Base.debug(" | " + backup);
					
					DirUtils.delete(file);
				}
			}
			
			Base.debug("\t\\ done " + Base.stopTime());
		}
		
		set(path, keep);
	}
	
	public boolean addDropboxUpload(final Process process) {
		final List<String> keep = getStringList(process.getType() + "." + process.getName() + ".keep");
		if (keep == null || keep.size() == 0) return false;
		
		Collections.reverse(keep);
		for (final String name : keep)
			if (name.endsWith(".zip")) return addDropboxUpload(name);
			else if (Debug.ON) Debug.info("bad file for dropbox: " + name);
		
		return false;
	}
	
	private boolean addDropboxUpload(final String name) {
		final File file = new File(name);
		if (!file.exists()) return false;
		
		if (file.length() > maxDropboxSize) {
			Base.warning(name + ": file size exceeds maximum allowed by the API");
			return false;
		}
		
		List<String> upload = getStringList("dropbox.upload");
		if (upload == null) upload = new ArrayList<String>();
		
		upload.add(name);
		set("dropbox.upload", upload);
		
		return true;
	}
	
	public String getDropboxUpload() {
		final List<String> upload = getStringList("dropbox.upload");
		if (upload == null || upload.size() == 0) return null;
		
		final String first = upload.get(0);
		upload.remove(0);
		set("dropbox.upload", upload);
		
		return first;
	}
	
	public String getDropboxAuth(final String key) {
		return getString("dropbox.auth." + key);
	}
	
	public void setDropboxAuth(final String key, final String secret) {
		set("dropbox.auth.key", key);
		set("dropbox.auth.secret", secret);
		saveConfig();
	}
}
