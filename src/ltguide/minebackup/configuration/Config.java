package ltguide.minebackup.configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.configuration.Configuration;
import ltguide.base.utils.HttpUtils;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Process;

import org.bukkit.configuration.ConfigurationSection;

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
		if (Debug.ON) Debug.info("Config migrate()");
		
		if (versionCompare(0, 5, 8)) {
			if (Debug.ON) Debug.info("here comes action broadcasts!");
			set("default_settings.broadcast", false);
			set("commands", null);
			set("messages", null);
		}
		
		if (versionCompare(0, 5, 7)) {
			if (Debug.ON) Debug.info("here comes ftp!");
			
			set("start_covered_in_dirt", false);
			
			if (isSet("ftp.ftphost")) {
				final HashMap<String, String> ftp = new HashMap<String, String>();
				ftp.put("server", getString("ftphost") + ":" + getString("ftpport"));
				ftp.put("path", getString("ftptargetdir"));
				ftp.put("username", getString("ftpuser"));
				ftp.put("password", getString("ftppassword"));
				
				set("ftp", createSection("ftp", ftp));
			}
			else {
				set("default_actions.ftp", false);
				set("default_settings.ftp", "3:30");
				set("ftp", getDefaultSection().getConfigurationSection("ftp"));
			}
		}
		
		if (versionCompare(0, 5, 5)) {
			final List<String> types = getStringList("others.plugins.exclude-types");
			types.add("lck");
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
			set("messages.backup-ended", null);
			set("messages.backup-started", null);
			set("messages.backup-started-user", null);
			set("messages.enabled", null);
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
	}
	
	private boolean isValidTime(final ConfigurationSection cs, final String key) {
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		boolean valid = false;
		final Object obj = cs.get(key);
		if (obj != null) {
			if (Debug.ON) Debug.info(" \\ " + obj + " (" + obj.getClass().getSimpleName() + ")");
			if (obj instanceof Integer) valid = (Integer) obj > -1;
			else if (obj instanceof String) valid = ((String) obj).matches("0|[1-9]\\d*[smhd]|(?:[0-1]?\\d|2[0-4]):[0-5][0-9]");
		}
		
		if (!valid) plugin.configWarning(cs, key, obj);
		return valid;
	}
	
	private int getTime(final ConfigurationSection cs, final String key) {
		if (!isValidTime(cs, key)) return 0;
		
		final Object obj = cs.get(key);
		if (obj instanceof String) {
			final String str = (String) obj;
			if ("0".equals(str)) return 0;
			
			if (str.contains(":")) {
				final String[] parts = str.split(":");
				return 0 - Integer.parseInt(parts[0]) * 3600 - Integer.parseInt(parts[1]) * 60;
			}
			
			int time = Integer.parseInt(str.substring(0, str.length() - 1));
			if (time < 1) return 0;
			
			final String scale = str.substring(str.length() - 1);
			if ("m".equals(scale)) time *= 60;
			else if ("h".equals(scale)) time *= 3600;
			else if ("d".equals(scale)) time *= 86400;
			
			return time;
		}
		
		return (Integer) obj;
	}
	
	public boolean isLoaded(final String type, final String name) {
		return loaded.contains(type + "-" + name);
	}
	
	public boolean load(final String type, final String name) {
		if (isLoaded(type, name)) return false;
		loaded.add(type + "-" + name);
		
		plugin.debug("loading config for " + type + "\\" + name);
		
		ConfigurationSection settings = getConfigurationSection(type + "." + name);
		if (settings == null) settings = createSection(type + "." + name);
		
		final ConfigurationSection defaults = getConfigurationSection("default_settings");
		
		for (final String key : defaults.getKeys(false)) {
			final boolean isAction = contains("default_actions." + key);
			
			if (settings.contains(key)) {
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
		}
		
		if ("others".equals(type)) settings.set("save", 0);
		//plugin.saveConfig(); //not for production (overwrites string-based times with integers)
		
		return getBoolean("start_covered_in_dirt");
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
	
	public String getDir(final String dir) {
		return new File(getString("directories." + dir)).getPath();
	}
	
	public File getDir(final String dir, final Process process) {
		return new File(getString("directories." + dir), process.getName());
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
			for (final String key : section.getKeys(false)) {
				final Object object = section.get(key + "." + action);
				if (object != null && (!(object instanceof Boolean) || (Boolean) object == true)) return true;
			}
		}
		
		return false;
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
	
	private class SourceFilenameFilter implements FilenameFilter {
		private final List<String> folders;
		private final List<String> types;
		
		public SourceFilenameFilter(final List<String> folders, final List<String> types) throws IllegalArgumentException {
			if (folders == null && types == null) throw new IllegalArgumentException();
			
			this.folders = folders;
			this.types = types;
		}
		
		@Override
		public boolean accept(final File dir, String name) {
			final String path = dir.getName().toLowerCase();
			name = name.toLowerCase();
			
			for (final String folder : folders)
				if (path.indexOf(folder) == 0) return false;
			
			for (final String type : types)
				if (name.endsWith(type)) return false;
			
			return true;
		}
		
	}
}
