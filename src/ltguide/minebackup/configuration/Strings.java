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
		if (migrate(5, 9, 1)) {
			if (Debug.ON) Debug.info("changing upload command description!");
			
			set("messages.backup_upload", "All upload actions will start momentarily. (%s)");
			set("commands.upload.description", "Triggers all upload actions.");
		}
	}
}
