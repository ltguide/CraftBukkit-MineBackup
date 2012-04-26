package ltguide.minebackup.configuration;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.configuration.StringsConfiguration;
import ltguide.minebackup.data.Commands;
import ltguide.minebackup.data.Messages;

public class Strings extends StringsConfiguration {
	public Strings(final Base instance) {
		super(instance, Messages.values(), Commands.values());
		reload();
	}
	
	@Override
	protected void migrate() {
		if (migrate(5, 9, 3)) {
			if (Debug.ON) Debug.info("adding usage command!");
			
			set("messages.usage_memory", "&f%s&e MiB allocated&6; &f%s&e MiB free");
			set("messages.usage_world", "&f%s&6: &f%s&e chunks&6; &f%s&e entities");
			set("commands.usage.description", "Display the server memory and chunk/entities per world");
		}
		
		if (migrate(5, 9, 1)) {
			if (Debug.ON) Debug.info("changing upload command description!");
			
			set("messages.backup_upload", "All upload actions will start momentarily. (%s)");
			set("commands.upload.description", "Triggers all upload actions.");
		}
	}
}
