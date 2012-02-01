package ltguide.minebackup;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.exceptions.HttpException;
import ltguide.base.utils.HttpUtils;
import ltguide.minebackup.configuration.Config;

import java.io.File;

public class TaskFTP extends Thread {
	private final MineBackup plugin;
    private Config config;

	public TaskFTP(final MineBackup plugin) {
		this.plugin = plugin;
        config.g
		oauth.put("oauth_consumer_key", appKey);
		oauth.put("oauth_signature_method", "HMAC-SHA1");
		oauth.put("oauth_version", "1.0");
	}



	@Override public void run() {
		if (Debug.ON) Debug.info("TaskFTP run()");

		if (plugin.isWorking()) {
			Base.debug("not checking FTP upload queue because an action is in progress");
			return;
		}

		final String target = plugin.persist.getFTPUpload();
		if (target == null) {
			if (Debug.ON) Debug.info("FTP - but nothing in queue");
			return;
		}

		final File file = new File(target);
		if (!file.exists()) return;

		plugin.setWorking(this, true);
		Base.info(" * uploading " + target);

		final String path = HttpUtils.encode(target.substring(plugin.config.getDir("destination").length() + 1)).replace("%2F", "/").replace("%5C", "/");
		Base.startTime();

		try {
			HttpUtils.put("https://api-content.dropbox.com/1/files_put/sandbox/" + path, getAuth(path), file);
			Base.debug("  \\ upload done " + Base.stopTime());
		}
		catch (final HttpException e) {
			Base.logException(e, path);
		}

		plugin.setWorking(this, false);
	}


}
