package ltguide.base.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import ltguide.base.Base;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.error.YAMLException;

public class Configuration extends YamlConfiguration {
	private final File file;
	protected final JavaPlugin plugin;
	
	public Configuration(final JavaPlugin instance, final String config) {
		plugin = instance;
		file = new File(plugin.getDataFolder(), config);
	}
	
	public void saveConfig() {
		try {
			save(file);
		}
		catch (final IOException e) {
			Base.logException(e, "could not save " + file);
		}
	}
	
	protected void loadConfig() {
		try {
			load(file);
		}
		catch (final FileNotFoundException e) {}
		catch (final IOException e) {
			Base.logException(e, "cannot load " + file);
		}
		catch (final InvalidConfigurationException e) {
			if (e.getCause() instanceof YAMLException) Base.severe("Config file " + file + " isn't valid! " + e.getCause());
			else if (e.getCause() == null || e.getCause() instanceof ClassCastException) Base.severe("Config file " + file + " isn't valid!");
			else Base.logException(e, "cannot load " + file + ": " + e.getCause().getClass());
		}
		
		final InputStream inStream = plugin.getResource(file.getName());
		if (inStream != null) setDefaults(YamlConfiguration.loadConfiguration(inStream));
	}
}
