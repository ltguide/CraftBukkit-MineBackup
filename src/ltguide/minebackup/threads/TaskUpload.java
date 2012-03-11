package ltguide.minebackup.threads;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import ltguide.base.Debug;
import ltguide.base.exceptions.HttpException;
import ltguide.base.utils.HttpUtils;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Upload;

public class TaskUpload extends Thread {
	private final MineBackup plugin;
	private String userSecret;
	SortedMap<String, String> oauth = new TreeMap<String, String>();
	private String ftpString;
	
	final private static String appKey = "l5uwj4soziwf5de";
	
	public TaskUpload(final MineBackup instance) {
		plugin = instance;
		
		oauth.put("oauth_consumer_key", appKey);
		oauth.put("oauth_signature_method", "HMAC-SHA1");
		oauth.put("oauth_version", "1.0");
	}
	
	public boolean hasAuth(final String type) {
		if ("dropbox".equals(type)) {
			final String userKey = plugin.persist.getDropboxAuth("key");
			oauth.put("oauth_token", userKey);
			
			return userKey != null && (userSecret = plugin.persist.getDropboxAuth("secret")) != null;
		}
		
		return (ftpString = plugin.config.getFTPAuth()) != null;
	}
	
	@Override
	public void run() {
		if (Debug.ON) Debug.info("TaskUpload run()");
		
		if (plugin.isWorking()) {
			plugin.debug("not checking upload queue because an action is in progress");
			return;
		}
		
		final Upload upload = plugin.persist.getUpload();
		if (upload == null) {
			if (Debug.ON) Debug.info("Upload - but nothing in queue");
			return;
		}
		
		if (!plugin.hasAction(upload.getType())) return;
		
		final File file = new File(upload.getName());
		if (!file.exists()) return;
		
		plugin.setWorking(this, true);
		plugin.info(" * " + upload.getType() + " uploading " + upload.getName());
		plugin.startTime();
		final String path = HttpUtils.encode(upload.getName().substring(plugin.config.getDir("destination").length() + 1)).replace("%2F", "/").replace("%5C", "/");
		
		try {
			if ("dropbox".equals(upload.getType())) HttpUtils.put("https://api-content.dropbox.com/1/files_put/sandbox/" + path, getAuthString(path), file);
			else HttpUtils.ftp(ftpString + path + ";type=i,mkd=survival", file);
			
			plugin.debug("  \\ upload done " + plugin.stopTime());
		}
		catch (final HttpException e) {
			plugin.logException(e, path);
		}
		
		plugin.setWorking(this, false);
	}
	
	public String getAuthString(final String url) throws HttpException {
		final long time = Calendar.getInstance().getTimeInMillis();
		oauth.put("oauth_nonce", String.valueOf(new Random(time).nextLong()));
		oauth.put("oauth_timestamp", String.valueOf(time / 1000));
		
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("url", url);
		params.put("params", HttpUtils.urlencode(oauth));
		params.put("oauth_token_secret", userSecret);
		
		return HttpUtils.createAuth(oauth, HttpUtils.responseGet("https://minebackup-dropbox.appspot.com/sign?" + HttpUtils.urlencode(params)));
	}
}
