package ltguide.minebackup.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.configuration.Configuration;
import ltguide.base.utils.DirUtils;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Process;
import ltguide.minebackup.data.Upload;

import org.bukkit.World;

public class Persist extends Configuration {
	private static final long maxDropboxSize = 180 * 1024 * 1024;
	
	public Persist(final Base instance) {
		super(instance, "persist.dat");
		reload();
	}
	
	@Override
	protected void migrate() {
		/*
		if (migrate(5, 9, 2)) {
			
		}
		*/
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
			return getBoolean(type + "." + name + ".dirty") || ("Server thread".equals(Thread.currentThread().getName()) ? hasPlayers(world) : ((MineBackup) plugin).syncCall("count", world).get());
		}
		catch (final Exception e) {
			plugin.debug("isDirty()->syncCall('count'): " + e.toString());
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
	
	public void processKeep(final Process process, final File target) {
		final int num = ((MineBackup) plugin).config.getInt(process, "keep");
		final String path = process.getType() + "." + process.getName() + ".keep";
		
		List<String> keep = getStringList(path);
		if (keep == null) keep = new ArrayList<String>();
		
		if (target != null) keep.add(target.getPath());
		
		if (keep.size() > num && (target == null || ((MineBackup) plugin).config.getInterval(process.getType(), process.getName(), "cleanup") == 0)) {
			plugin.debug(" * cleaning up " + process.getType() + "\\" + process.getName());
			plugin.startTime();
			
			while (keep.size() > num) {
				final String backup = keep.get(0);
				keep.remove(0);
				
				final File file = new File(backup);
				if (file.exists()) {
					plugin.debug(" | removing " + backup);
					
					DirUtils.delete(file);
				}
			}
			
			plugin.debug("  \\ done " + plugin.stopTime());
		}
		
		set(path, keep);
	}
	
	public boolean addUpload(final Process process) {
		final List<String> keep = getStringList(process.getType() + "." + process.getName() + ".keep");
		if (keep == null || keep.size() == 0) return false;
		
		Collections.reverse(keep);
		for (final String name : keep)
			if (name.endsWith(".zip")) return addUpload(process.getAction(), name);
			else if (Debug.ON) Debug.info("bad file for " + process.getAction() + ": " + name);
		
		return false;
	}
	
	private boolean addUpload(final String type, final String name) {
		final File file = new File(name);
		if (!file.exists()) return false;
		
		if ("dropbox".equals(type) && file.length() > maxDropboxSize) {
			plugin.warning(name + ": file size exceeds maximum allowed by the Dropbox API");
			return false;
		}
		
		List<Map<?, ?>> list = getMapList("upload");
		if (list == null) list = new ArrayList<Map<?, ?>>();
		
		final HashMap<String, String> upload = new HashMap<String, String>();
		upload.put("type", type);
		upload.put("name", name);
		
		list.add(upload);
		set("upload", list);
		
		return true;
	}
	
	public Upload getUpload() {
		final List<Map<?, ?>> list = getMapList("upload");
		if (list == null || list.size() == 0) return null;
		
		final Map<?, ?> upload = list.get(0);
		list.remove(0);
		set("upload", list);
		
		return new Upload(upload.get("type").toString(), upload.get("name").toString());
	}
	
	public String getDropboxAuth(final String key) {
		return getString("dropbox.auth." + key);
	}
	
	public void setDropboxAuth(final String key, final String secret) {
		set("dropbox.auth.key", key);
		set("dropbox.auth.secret", secret);
		save();
	}
}
