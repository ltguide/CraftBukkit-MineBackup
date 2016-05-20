package ltguide.minebackup.configuration;

import ltguide.minebackup.Base;
import ltguide.minebackup.Debug;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Process;
import ltguide.minebackup.data.SourceFilenameFilter;
import ltguide.minebackup.utils.HttpUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.zip.Deflater;

public class Config extends Configuration {
	public final Set<String> loaded = new HashSet<String>();
	
	public Config(final Base instance) {
		super(instance);
		reload();
	}
	
	@Override
	public void reload() {
		super.reload();
		
		plugin.setDebug(getBoolean("debug"));
		
		check();
		loaded.clear();
	}
	
	@Override
	protected void migrate() {
		final HashMap<String, Object> map = new HashMap<String, Object>();
		
		if (migrate(5, 9, 3)) {
			if (Debug.ON) Debug.info("here comes timezone fix, root files, and broadcast_settings!");
			
			set("destination.timezone-offset", 0d);
			
			map.clear();
			map.put("copy", false);
			map.put("compress", false);
			map.put("exclude-folders", Arrays.asList(new String[] { "*" }));
			map.put("exclude-types", Arrays.asList(new String[] { "jar", "lck" }));
			
			set("others.root", createSection("others.root", map));
			
			map.clear();
			map.put("on_save", true);
			map.put("on_copy", false);
			map.put("on_compress", false);
			map.put("when_done", true);
			
			set("broadcast_settings", createSection("broadcast_settings", map));
		}
		
		if (migrate(5, 9)) {
			if (Debug.ON) Debug.info("here comes better dirt!");
			
			map.clear();
			map.put("enabled", getBoolean("start_covered_in_dirt"));
			map.put("delay", "2m");
			
			set("actions_on_startup", createSection("actions_on_startup", map));
			set("start_covered_in_dirt", null);
		}
		
		if (migrate(0, 5, 8)) {
			if (Debug.ON) Debug.info("here comes action broadcasts!");
			
			set("default_settings.broadcast", false);
			set("commands", null);
			set("messages", null);
		}
		
		if (migrate(0, 5, 7)) {
			if (Debug.ON) Debug.info("here comes ftp!");
			
			//set("start_covered_in_dirt", false);
			
			if (isSet("ftp.ftphost")) {
				map.clear();
				map.put("server", getString("ftphost") + ":" + getString("ftpport"));
				map.put("path", getString("ftptargetdir"));
				map.put("username", getString("ftpuser"));
				map.put("password", getString("ftppassword"));
				
				set("ftp", createSection("ftp", map));
			}
			else {
				set("default_actions.ftp", false);
				set("default_settings.ftp", "3:30");
				set("ftp", getDefaultSection().getConfigurationSection("ftp"));
			}
		}
		
		if (migrate(0, 5, 5)) {
			final List<String> types = getStringList("others.plugins.exclude-types");
			if (!types.contains("lck")) types.add("lck");
			
			set("others.plugins.exclude-types", types);
		}
		
		if (isSet("backup")) {
			plugin.warning("migrating config from v0.4.8.1+");
			
			final String oldLevel = getString("compression.level");
			int level = Deflater.BEST_COMPRESSION;
			if ("BEST_SPEED".equals(oldLevel)) level = Deflater.BEST_SPEED;
			else if ("NO_COMPRESSION".equals(oldLevel)) level = Deflater.NO_COMPRESSION;
			
			final int interval = getInt("time.interval", 3600);
			final String format = getString("backup.format", "%Y-%M-%D_%H-%m-%S");
			
			final String action = getBoolean("compression.enabled", false) ? "compress" : "copy";
			
			for (final String world : getStringList("worlds")) {
				set("worlds." + world + ".save", true);
				set("worlds." + world + "." + action, true);
			}
			
			if (getBoolean("options.backup-plugins", false)) set("others.plugins." + action, true);
			
			set("default_settings.compression_level", level);
			set("default_settings.compress", interval);
			set("default_settings.copy", interval);
			set("default_settings.keep", (int) Math.floor(86400 / interval));
			set("directories.destination", getString("backup.dir", "minebackup"));
			set("destination.format", format.startsWith("%W/") ? format.substring(3) : format);
			set("debug", getBoolean("options.debug", false));
			
			set("compression", null);
			//set("messages.backup-ended", null);
			//set("messages.backup-started", null);
			//set("messages.backup-started-user", null);
			//set("messages.enabled", null);
			set("backup", null);
			set("time", null);
			set("options", null);
		}
	}
	
	private void check() {
		final ConfigurationSection actions = getConfigurationSection("default_actions");
		final ConfigurationSection defaults = getConfigurationSection("default_settings");
		for (final String key : ((MineBackup) plugin).actions) {
			fixBoolean(actions, key);
			defaults.set(key, getTime(defaults, key));
		}
		
		fixIntRange(defaults, "compression_level", 0, 9);
		fixIntRange(defaults, "keep", 1, 168);
		
		fixBoolean(defaults, "broadcast");
		
		set("destination.timezone-offset", getDouble("destination.timezone-offset", 0));
		
		final ConfigurationSection directories = getConfigurationSection("directories");
		for (final String key : directories.getKeys(false))
			fixSeparator(directories, key);
	}
	
	public boolean isLoaded(final String type, final String name) {
		return loaded.contains(type + "-" + name);
	}
	
	public boolean load(final String type, final String name) {
		if (isLoaded(type, name)) return false;
		loaded.add(type + "-" + name);
		
		ConfigurationSection settings = getConfigurationSection(type + "." + name);
		if (settings == null) {
			settings = createSection(type + "." + name);
			plugin.debug("Using default configuration for " + type + "\\" + name);
		}
		else plugin.debug("Loading configuration for " + type + "\\" + name);
		
		final ConfigurationSection defaults = getConfigurationSection("default_settings");
		
		final Set<String> keys = defaults.getKeys(false);
		if ("others".equals(type)) {
			keys.remove("save");
			settings.set("save", 0);
			fixSeparator(settings, "path");
		}
		
		for (final String key : keys) {
			final boolean isAction = contains("default_actions." + key);
			
			if (settings.isSet(key)) {
				if (isAction) {
					if (settings.isBoolean(key)) settings.set(key, settings.getBoolean(key) ? defaults.get(key) : 0);
					else settings.set(key, getTime(settings, key));
				}
				else if (defaults.isBoolean(key)) settings.set(key, settings.getBoolean(key, false));
				else settings.set(key, settings.getInt(key, 0));
			}
			else if (isAction) settings.set(key, getBoolean("default_actions." + key) ? defaults.get(key) : 0);
			else if (defaults.isBoolean(key)) settings.set(key, defaults.getBoolean(key));
			else settings.set(key, defaults.getInt(key));
			
			plugin.debug(" - " + key + ": " + settings.get(key));
		}
		
		checkMapFormat(getDir(type, name));
		
		return getBoolean("actions_on_startup.enabled");
	}
	
	private void checkMapFormat(final File dir) {
		final List<String> files = new ArrayList<String>();
		
		if (new File(dir, "level.dat_mcr").exists()) files.add("level.dat_mcr");
		
		for (String path : Arrays.asList("", "DIM-1", "DIM1")) {
			path += File.separator + "region";
			if (new File(dir + File.separator + path, "r.0.0.mcr").exists()) files.add("*.mcr files from " + path);
		}
		
		if (files.size() > 0) plugin.warning(String.format("%% detected old map format files (%s); once backed up, remove %s", dir.getPath(), plugin.joinString(files.toArray(), " and ")));
	}
	
	public long getInterval(final Process process) {
		return getInterval(process.getType(), process.getName(), process.getAction());
	}
	
	public long getInterval(final String type, final String name, final String key) {
		return getInt(type + "." + name + "." + key, 0) * 1000L;
	}
	
	protected List<String> getFilter(final Process process, final String key) {
		return getStringList(process.getType() + "." + process.getName() + ".exclude-" + key);
	}
	
	public FilenameFilter getFilenameFilter(final Process process) {
		try {
			return new SourceFilenameFilter(getFilter(process, "folders"), getFilter(process, "types"));
		}
		catch (final IllegalArgumentException e) {
			return null;
		}
	}
	
	public File getDir(final String dir) {
		return new File(getString("directories." + dir));
	}
	
	public File getDir(final String dir, final Process process) {
		if ("others".equals(dir)) {
			if ("root".equals(process.getName())) return getDir(dir);
			
			final String path = getString(process.getType() + "." + process.getName() + ".path", "");
			if (!"".equals(path)) return getDir(dir, path); //.replaceAll("\\", "/")
		}
		
		return getDir(dir, process.getName());
	}
	
	public File getDir(final String dir, final String name) {
		return new File(getString("directories." + dir), name);
	}
	
	public String getDestFormat() {
		return getString("destination.format");
	}
	
	public boolean getDestPrepend() {
		return getBoolean("destination.prepend-world");
	}
	
	public boolean getBoolean(final Process process, final String key) {
		return getBoolean(process.getType() + "." + process.getName() + "." + key, false);
	}
	
	public int getInt(final Process process, final String key) {
		return getInt(process.getType() + "." + process.getName() + "." + key, 0);
	}
	
	public Set<String> getOthers() {
		return getConfigurationSection("others").getKeys(false);
	}
	
	public boolean hasAction(final String action) {
		if (getBoolean("default_actions." + action, false)) return true;
		
		for (final String type : Arrays.asList("worlds", "others")) {
			final ConfigurationSection section = getConfigurationSection(type);
			for (final String key : section.getKeys(false))
				if (getTime(section, key + "." + action) != 0) return true;
		}
		
		return false;
	}
	
	public int getStartupDelay() {
		final ConfigurationSection cs = getConfigurationSection("actions_on_startup");
		if (!cs.getBoolean("enabled")) return 0;
		
		return getTime(cs, "delay");
	}
	
	public String getFTPAuth() {
		try {
			return String.format("ftp://%s:%s@%s/%s/", getFTPString("username"), getFTPString("password"), getFTPString("server").replace("%3A", ":"), getFTPString("path").replace("%2F", "/").replace("%5C", "/"));
		}
		catch (final Exception e) {
			return null;
		}
	}
	
	private String getFTPString(final String key) throws Exception {
		final String value = getString("ftp." + key);
		if (value == null) throw new Exception();
		return HttpUtils.encode(value);
	}
}
