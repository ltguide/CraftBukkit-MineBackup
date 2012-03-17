package ltguide.minebackup.configuration;

import ltguide.base.Base;
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
		/*
		if (versionCompare(5, 9)) {
			
		}
		*/
	}
}
