package ltguide.base.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.utils.DirUtils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.error.YAMLException;

public class Configuration extends YamlConfiguration {
	private final File file;
	private final String name;
	private boolean migrate;
	protected final Base plugin;
	protected int[] oldVersion;
	
	public Configuration(final Base instance) {
		this(instance, "config.yml");
	}
	
	public Configuration(final Base instance, final String fileName) {
		plugin = instance;
		name = fileName;
		file = new File(plugin.getDataFolder(), fileName);
	}
	
	public void reload() {
		load();
	}
	
	protected void load() {
		boolean loaded = false;
		try {
			load(file);
			loaded = true;
		}
		catch (final FileNotFoundException e) {}
		catch (final IOException e) {
			plugin.logException(e, "cannot load " + file);
		}
		catch (final InvalidConfigurationException e) {
			if (e.getCause() instanceof YAMLException) plugin.severe("Config file " + file + " isn't valid! \n" + e.getCause());
			else if (e.getCause() == null || e.getCause() instanceof ClassCastException) plugin.severe("Config file " + file + " isn't valid!");
			else plugin.logException(e, "cannot load " + file + ": " + e.getCause().getClass());
			
			plugin.info("Saving a backup of " + name + " to " + backup("invalid"));
		}
		
		final InputStream inStream = plugin.getResource(file.getName());
		if (inStream != null) {
			setDefaults(YamlConfiguration.loadConfiguration(inStream));
			
			if (!loaded) plugin.info("Writing default " + name + " to " + file);
		}
		
		if (!loaded) {
			options().copyDefaults(true);
			save();
		}
		else upgrade();
	}
	
	public File backup(final String prefix) {
		try {
			final File destFile = new File(plugin.getDataFolder() + File.separator + "old_configurations", prefix + "-" + name);
			
			destFile.getParentFile().mkdirs();
			DirUtils.copyFile(file, destFile);
			
			return destFile;
		}
		catch (final Exception e) {
			plugin.logException(e, "failed to copy " + name);
			return null;
		}
	}
	
	private void upgrade() {
		if (Debug.ON) Debug.info("upgrade() " + file);
		
		String old = "UNKNOWN";
		if (isSet("version-nomodify")) old = getString("version-nomodify");
		
		if (plugin.getDescription().getVersion().equals(old)) return;
		
		oldVersion = getVersionInt(old);
		
		migrate = false;
		migrate();
		
		if (migrate) plugin.warning("Migrating " + name + " from version " + old + " (backup: " + backup(old) + ")");
		
		save();
	}
	
	protected void migrate() {
		if (Debug.ON) Debug.info("Configuration migrate()");
	}
	
	protected boolean migrate(final int... compare) {
		for (int i = 0; i < compare.length && i < oldVersion.length; i++)
			if (oldVersion[i] > compare[i]) return false;
		
		if (compare.length < oldVersion.length) return false;
		
		return migrate = true;
	}
	
	public void save() {
		try {
			set("version-nomodify", plugin.getDescription().getVersion());
			save(file);
		}
		catch (final IOException e) {
			plugin.logException(e, "could not save " + file);
		}
	}
	
	protected int[] getVersionInt(final String version) {
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
	
	protected void fixIntRange(final ConfigurationSection cs, final String key, final int min, final int max) {
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		
		if (Debug.ON) {
			final Object obj = cs.get(key);
			Debug.info(" \\ " + obj + " (" + (obj == null ? obj : obj.getClass().getSimpleName()) + ")");
		}
		
		final int value = cs.getInt(key);
		if (value < min || value > max) {
			cs.set(key, getDefaultSection().getInt(cs.getCurrentPath() + "." + key));
			plugin.configWarning(cs, key, value + "; valid: " + min + "-" + max);
		}
	}
	
	protected void fixBoolean(final ConfigurationSection cs, final String key) {
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		
		if (Debug.ON) {
			final Object obj = cs.get(key);
			Debug.info(" \\ " + obj + " (" + (obj == null ? obj : obj.getClass().getSimpleName()) + ")");
		}
		
		if (!cs.isBoolean(key)) {
			plugin.configWarning(cs, key, cs.get(key) + "; valid: true/false");
			cs.set(key, getDefaultSection().getBoolean(cs.getCurrentPath() + "." + key));
		}
	}
	
	protected void fixSeparator(final ConfigurationSection cs, final String key) {
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		
		if (File.separatorChar == '\\') return;
		
		final String str = cs.getString(key);
		if (str == null) return;
		
		final String value = str.replace("\\", "/");
		
		if (!value.equals(str)) {
			plugin.configWarning(cs, key, str + "; valid: " + value);
			cs.set(key, value);
		}
	}
	
	protected int getTime(final ConfigurationSection cs, final String key) {
		if (Debug.ON) Debug.info("checking " + cs.getCurrentPath() + "." + key);
		
		Object obj = cs.get(key);
		if (obj != null) {
			if (Debug.ON) Debug.info(" \\ " + obj + " (" + obj.getClass().getSimpleName() + ")");
			
			boolean valid = false;
			if (obj instanceof String) valid = ((String) obj).matches("0|[1-9]\\d*[smhd]|(?:[0-1]?\\d|2[0-4]):[0-5][0-9]");
			else valid = obj instanceof Boolean || obj instanceof Integer;
			
			if (!valid) {
				plugin.configWarning(cs, key, obj);
				obj = null;
			}
		}
		
		return plugin.getTime(obj);
	}
}
