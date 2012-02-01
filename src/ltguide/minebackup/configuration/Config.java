package ltguide.minebackup.configuration;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.data.Command;
import ltguide.base.data.Configuration;
import ltguide.base.data.Message;
import ltguide.minebackup.data.Commands;
import ltguide.minebackup.data.Messages;
import ltguide.minebackup.data.Process;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.zip.Deflater;

public class Config extends Configuration {
	private final Set<String> loaded = new HashSet<String>();
    private ConfigurationSection ftpConfig;

	public Config(final JavaPlugin instance) {
		super(instance, "config.yml");

		loadConfig();

		if (upgradeConfig()) {
			options().copyDefaults(true);
			saveConfig();
		}

		checkConfig();
        ftpConfig = getConfigurationSection("ftp");

	}

	public void reload() {
		loadConfig();
		checkConfig();
        ftpConfig = getConfigurationSection("ftp");
		loaded.clear();
	}

	private boolean upgradeConfig() {
		if (Debug.ON) Debug.info("checking config version");
		if (isSet("version-nomodify")) {
			if (Debug.ON) Debug.info("version isSet");

			final String newVersion = plugin.getDescription().getVersion();
			final String oldVersion = getString("version-nomodify");
			if (newVersion.equals(oldVersion)) return false;

			Base.warning("migrating config from " + oldVersion);
			set("version-nomodify", newVersion);

			final int[] oldNum = getVersionInt(oldVersion);
			if (oldNum.length != 3) {
				Base.warning("\t\\ using default config (version tags altered)");
				return true;
			}

			if (versionCompare(oldNum, new int[] { 0, 5, 5 })) {
				final List<String> types = getStringList("others.plugins.exclude-types");
				types.add("lck");
				set("others.plugins.exclude-types", types);
			}
		}
		else if (isSet("backup")) {
			Base.warning("migrating config from v0.4.8.1+");

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
		else if (Debug.ON) Debug.info("using default config");

		return true;
	}

	private int[] getVersionInt(final String version) {
		final String[] split = version.split("\\.");
		final int[] num = new int[split.length];

		for (int i = 0; i < split.length; i++)
			try {
				num[i] = Integer.parseInt(split[i]);
			}
			catch (final NumberFormatException e) {
				num[i] = 0;
			}

		return num;
	}

	private boolean versionCompare(final int[] old, final int[] current) {
		for (int i = 0; i < current.length; i++)
			if (old[i] > current[i]) return false;

		return true;
	}

	private void checkConfig() {
		options().copyDefaults(true);
		Base.setDebug(getBoolean("debug"));

		final ConfigurationSection defaultSettings = getConfigurationSection("default_settings");
		for (final String key : Arrays.asList("save", "copy", "compress", "cleanup", "dropbox", "ftp"))
			defaultSettings.set(key, getTime(defaultSettings, key));

		fixIntRange(defaultSettings, "compression_level", 0, 9);
		fixIntRange(defaultSettings, "keep", 1, 168);

		for (final Messages messages : Messages.values())
			Message.setConfig(messages.name(), getString("messages." + messages.name().toLowerCase()));

		for (final Commands command : Commands.values()) {
			final String path = "commands." + command.name().toLowerCase();

			Command.setConfig(command.name(), getString(path + ".description"), getBroadcast(get(path + ".broadcast")));
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
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		boolean valid = false;
		final Object obj = cs.get(key);
		if (obj != null) {
			if (Debug.ON) Debug.info(" \\ " + obj + " (" + obj.getClass().getSimpleName() + ")");
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
			cs.set(key, getDefaultSection().getInt("default_settings." + key));
			sendWarning(cs, key, value + "; valid: " + min + "-" + max);
		}
	}

	private void sendWarning(final ConfigurationSection cs, final String key, final Object object) {
		Base.warning(" $ invalid config setting: " + cs.getCurrentPath() + "." + key + " (" + object + ")");
	}

	public boolean isLoaded(final String type, final String name) {
		return loaded.contains(type + "-" + name);
	}

	public boolean load(final String type, final String name) {
		if (isLoaded(type, name)) return false;
		loaded.add(type + "-" + name);

		Base.debug("loading config for " + type + "\\" + name);

		ConfigurationSection folderSettings = getConfigurationSection(type + "." + name);
		if (folderSettings == null) folderSettings = createSection(type + "." + name);

		final ConfigurationSection defaultSettings = getConfigurationSection("default_settings");

		for (final String key : defaultSettings.getKeys(false))
			if (folderSettings.contains(key)) {
				if (folderSettings.isBoolean(key)) folderSettings.set(key, folderSettings.getBoolean(key) ? defaultSettings.get(key) : 0);
				else folderSettings.set(key, getTime(folderSettings, key));
			}
			else folderSettings.set(key, !contains("default_actions." + key) || getBoolean("default_actions." + key) ? defaultSettings.get(key) : 0);

		if ("others".equals(type)) folderSettings.set("save", 0);
		//plugin.saveConfig(); //not for production (overwrites string-based times with integers)

		return true;
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

	private class SourceFilenameFilter implements FilenameFilter {
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
}
