package ltguide.minebackup.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ltguide.debug.Debug;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.utils.DirUtils;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Persist {
	private final MineBackup plugin;
	private final FileConfiguration config;
	private final File file;
	private final Set<String> loaded = new HashSet<String>();
	
	public Persist(final MineBackup plugin) {
		this.plugin = plugin;
		file = new File(plugin.getDataFolder(), "persist.dat");
		config = YamlConfiguration.loadConfiguration(file);
	}
	
	public void save() {
		try {
			config.save(file);
		}
		catch (final IOException e) {
			plugin.logException(e, "could not save persist.dat");
		}
	}
	
	public boolean load(final String name, final String type) {
		if (isLoaded(name)) return false;
		loaded.add(name);
		if (Debug.ON) plugin.ifDebug("loaded " + type + ": " + name);
		
		plugin.config.cascadeConfig(type, name);
		
		return true;
	}
	
	public boolean isLoaded(final String name) {
		return loaded.contains(name);
	}
	
	public void reload() {
		loaded.clear();
	}
	
	public void setDirty(final World world) {
		config.set("worlds." + world.getName() + ".dirty", true);
	}
	
	public void setDirty() {
		for (final String name : plugin.config.getOthers())
			config.set("others." + name + ".dirty", true);
	}
	
	public void setClean(final String type, final String name) {
		config.set(type + "." + name + ".dirty", false);
	}
	
	public boolean isDirty(final Process process) {
		return isDirty(process.getType(), process.getName());
	}
	
	public boolean isDirty(final String type, final String name) {
		synchronized (plugin.synch) {
			if ("others".equals(type)) return config.getBoolean("others." + name + ".dirty") || plugin.getServer().getOnlinePlayers().length > 0;
			
			final World world = plugin.getServer().getWorld(name);
			return world != null && (config.getBoolean("worlds." + world.getName() + ".dirty") || world.getPlayers().size() > 0);
		}
	}
	
	public long getNext(final String type, final String name, final String action) {
		return config.getLong(type + "." + name + "." + action + ".next", 0);
	}
	
	public void setNext(final Process process) {
		if (Debug.ON) plugin.ifDebug(" \\ " + process.getType() + "." + process.getName() + "." + process.getAction() + ".next=" + process.getNext());
		config.set(process.getType() + "." + process.getName() + "." + process.getAction() + ".next", process.getNext());
	}
	
	public void addKeep(final Process process, final File target) {
		final int num = plugin.config.getInt(process, "keep");
		final String path = process.getType() + "." + process.getName() + ".keep";
		
		List<String> keep = config.getStringList(path);
		if (keep == null) keep = new ArrayList<String>();
		
		keep.add(target.getPath());
		
		while (keep.size() > num) {
			final String backup = keep.get(0);
			keep.remove(0);
			
			plugin.debug(" * deleting " + backup);
			DirUtils.delete(new File(backup));
		}
		
		config.set(path, keep);
	}
	
	public boolean addDropboxUpload(final Process process) {
		final List<String> keep = config.getStringList(process.getType() + "." + process.getName() + ".keep");
		if (keep == null || keep.size() == 0) return false;
		
		Collections.reverse(keep);
		String target = null;
		for (final String name : keep)
			if (name.endsWith(".zip")) {
				target = name;
				break;
			}
			else if (Debug.ON) plugin.ifDebug("not .zip: " + name);
		
		if (target == null) return false;
		
		List<String> upload = config.getStringList("dropbox.upload");
		if (upload == null) upload = new ArrayList<String>();
		
		upload.add(target);
		config.set("dropbox.upload", upload);
		
		return true;
	}
	
	public String getDropboxUpload() {
		final List<String> upload = config.getStringList("dropbox.upload");
		if (upload == null || upload.size() == 0) return null;
		
		final String first = upload.get(0);
		upload.remove(0);
		config.set("dropbox.upload", upload);
		
		return first;
	}
	
	public String getDropboxAuth(final String key) {
		return config.getString("dropbox.auth." + key);
	}
	
	public void setDropboxAuth(final String key, final String secret) {
		config.set("dropbox.auth.key", key);
		config.set("dropbox.auth.secret", secret);
		save();
	}
}
