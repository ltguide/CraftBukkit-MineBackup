package ltguide.minebackup.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.Deflater;

import ltguide.debug.Debug;
import ltguide.minebackup.MineBackup;

import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
	private final MineBackup plugin;
	private FileConfiguration config;
	
	public Config(final MineBackup plugin) {
		this.plugin = plugin;
		config = plugin.getConfig();
		
		if (upgradeConfig()) {
			config.options().copyDefaults(true);
			plugin.saveConfig();
		}
		
		checkConfig();
	}
	
	public void reload() {
		plugin.reloadConfig();
		config = plugin.getConfig();
		
		checkConfig();
	}
	
	private boolean upgradeConfig() {
		if (Debug.ON) plugin.ifDebug("checking config version");
		if (config.isSet("version-nomodify")) {
			if (Debug.ON) plugin.ifDebug("version isSet");
			final String version = plugin.getDescription().getVersion();
			final String oldVersion = config.getString("version-nomodify");
			
			if (version.equals(oldVersion)) return false;
			if (Debug.ON) plugin.ifDebug("upgrading config");
			config.set("version-nomodify", version);
			
			//if (oldVersion == "0.x") {
			//	//upgrade
			//}
		}
		else if (config.isSet("backup")) {
			plugin.log(Level.WARNING, "migrating config from v0.4.8.1+");
			
			final String oldLevel = config.getString("compression.level");
			int level = Deflater.BEST_COMPRESSION;
			if ("BEST_SPEED".equals(oldLevel)) level = Deflater.BEST_SPEED;
			else if ("NO_COMPRESSION".equals(oldLevel)) level = Deflater.NO_COMPRESSION;
			
			final int interval = config.getInt("time.interval", 3600);
			final String format = config.getString("backup.format", "%Y-%M-%D_%H-%m-%S");
			
			final String action = config.getBoolean("compression.enabled", false) ? "compress" : "copy";
			
			for (final String world : config.getStringList("worlds")) {
				config.set("worlds." + world + ".save", true);
				config.set("worlds." + world + "." + action, true);
			}
			
			if (config.getBoolean("options.backup-plugins", false)) config.set("others.plugins." + action, true);
			
			config.set("default_settings.compression_level", level);
			config.set("default_settings.compress", interval);
			config.set("default_settings.copy", interval);
			config.set("default_settings.keep", (int) Math.floor(86400 / interval));
			config.set("directories.destination", config.getString("backup.dir", "minebackup"));
			config.set("destination.format", format.startsWith("%W/") ? format.substring(3) : format);
			config.set("debug", config.getBoolean("options.debug", false));
			
			config.set("compression", null);
			config.set("messages.backup-ended", null);
			config.set("messages.backup-started", null);
			config.set("messages.backup-started-user", null);
			config.set("messages.enabled", null);
			config.set("backup", null);
			config.set("time", null);
			config.set("options", null);
		}
		else if (Debug.ON) plugin.debug("using default config");
		
		return true;
	}
	
	private void checkConfig() {
		config.options().copyDefaults(true);
		
		final ConfigurationSection defaultSettings = config.getConfigurationSection("default_settings");
		for (final String key : Arrays.asList("save", "copy", "compress", "dropbox"))
			defaultSettings.set(key, getTime(defaultSettings, key));
		
		fixIntRange(defaultSettings, "compression_level", 0, 9);
		fixIntRange(defaultSettings, "keep", 1, 168);
		
		for (final Message message : Message.values())
			message.setText(config.getString("messages." + message.name().toLowerCase()));
		
		for (final Command command : Command.values()) {
			final String path = "commands." + command.name().toLowerCase();
			
			command.setText(config.getString(path + ".description"));
			command.setBroadcast(getBroadcast(config.get(path + ".broadcast")));
		}
	}
	
	private String getBroadcast(final Object object) {
		if (object == null) return null;
		
		if (object instanceof Boolean) {
			if ((Boolean) object) return Server.BROADCAST_CHANNEL_USERS;
			
			return null;
		}
		
		final String group = String.valueOf(object);
		
		if ("users".equalsIgnoreCase(group)) return Server.BROADCAST_CHANNEL_USERS;
		if ("admins".equalsIgnoreCase(group)) return Server.BROADCAST_CHANNEL_ADMINISTRATIVE;
		
		return "minebackup." + group;
	}
	
	private boolean isValidTime(final ConfigurationSection cs, final String key) {
		if (Debug.ON) plugin.ifDebug("checking " + cs.getCurrentPath() + "." + key);
		boolean valid = false;
		final Object obj = cs.get(key);
		if (obj != null) {
			if (Debug.ON) plugin.ifDebug(" \\ " + obj + " (" + obj.getClass().getSimpleName() + ")");
			if (obj instanceof Integer) valid = (Integer) obj > -1;
			else if (obj instanceof String) valid = ((String) obj).matches("0|[1-9]\\d*[smhd]|(?:[0-1]?\\d|2[0-4]):[0-5][0-9]");
		}
		
		if (!valid) sendWarning(cs, key, obj);
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
	
	private void fixIntRange(final ConfigurationSection cs, final String key, final int min, final int max) {
		final int value = cs.getInt(key);
		if (value < min || value > max) {
			cs.set(key, config.getDefaultSection().getInt("default_settings." + key));
			sendWarning(cs, key, value + "; valid: " + min + "-" + max);
		}
	}
	
	private void sendWarning(final ConfigurationSection cs, final String key, final Object object) {
		plugin.log(Level.WARNING, " $ invalid config setting: " + cs.getCurrentPath() + "." + key + " (" + object + ")");
	}
	
	protected void cascadeConfig(final String type, final String name) {
		plugin.debug(" - cascading config for " + type + "\\" + name);
		ConfigurationSection folderSettings = config.getConfigurationSection(type + "." + name);
		if (folderSettings == null) folderSettings = config.createSection(type + "." + name);
		
		final ConfigurationSection defaultActions = config.getConfigurationSection("default_actions");
		final ConfigurationSection defaultSettings = config.getConfigurationSection("default_settings");
		
		for (final String key : defaultSettings.getKeys(false))
			if (folderSettings.contains(key)) {
				if (folderSettings.isBoolean(key)) folderSettings.set(key, folderSettings.getBoolean(key) ? defaultSettings.get(key) : 0);
				else folderSettings.set(key, getTime(folderSettings, key));
			}
			else folderSettings.set(key, !defaultActions.contains(key) || defaultActions.getBoolean(key) ? defaultSettings.get(key) : 0);
		
		if ("others".equals(type)) folderSettings.set("save", null);
		//plugin.saveConfig(); //not for production (overwrites string-based times with integers)
	}
	
	public long getInterval(final Process process) {
		return getInterval(process.getType(), process.getName(), process.getAction());
	}
	
	public long getInterval(final String type, final String name, final String key) {
		return config.getInt(type + "." + name + "." + key, 0) * 1000L;
	}
	
	protected List<String> getFilter(final Process process, final String key) {
		return config.getStringList(process.getType() + "." + process.getName() + ".exclude-" + key);
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
		return new File(config.getString("directories." + dir)).getPath();
	}
	
	public File getDir(final String dir, final Process process) {
		return new File(config.getString("directories." + dir), process.getName());
	}
	
	public String getDestFormat() {
		return config.getString("destination.format");
	}
	
	public boolean getDestPrepend() {
		return config.getBoolean("destination.prepend-world");
	}
	
	public int getInt(final Process process, final String key) {
		return config.getInt(process.getType() + "." + process.getName() + "." + key, 0);
	}
	
	public Set<String> getOthers() {
		return config.getConfigurationSection("others").getKeys(false);
	}
	
	class SourceFilenameFilter implements FilenameFilter {
		private final List<String> folders;
		private final List<String> types;
		
		public SourceFilenameFilter(final List<String> folders, final List<String> types) throws IllegalArgumentException {
			if (folders == null && types == null) throw new IllegalArgumentException();
			
			this.folders = folders;
			this.types = types;
		}
		
		@Override public boolean accept(final File dir, String name) {
			final String path = dir.getName().toLowerCase();
			name = name.toLowerCase();
			
			for (final String folder : folders)
				if (path.indexOf(folder) == 0) return false;
			
			for (final String type : types)
				if (name.endsWith(type)) return false;
			
			return true;
		}
		
	}
	
	public boolean hasDropboxAction() {
		if (config.getBoolean("default_actions.dropbox", false)) return true;
		
		for (final String type : Arrays.asList("worlds", "others")) {
			final ConfigurationSection section = config.getConfigurationSection(type);
			for (final String key : section.getKeys(false)) {
				final Object object = section.get(key + ".dropbox");
				if (object != null && (!(object instanceof Boolean) || (Boolean) object == true)) return true;
			}
		}
		
		return false;
	}
}
