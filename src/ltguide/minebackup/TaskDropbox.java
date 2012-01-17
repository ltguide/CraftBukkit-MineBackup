package ltguide.minebackup;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import ltguide.debug.Debug;
import ltguide.minebackup.exceptions.DropboxException;
import ltguide.minebackup.utils.HttpUtils;

public class TaskDropbox extends Thread {
	private final MineBackup plugin;
	private String userSecret;
	SortedMap<String, String> oauth = new TreeMap<String, String>();
	
	final private static String appKey = "l5uwj4soziwf5de";
	
	public TaskDropbox(final MineBackup plugin) {
		this.plugin = plugin;
		
		oauth.put("oauth_consumer_key", appKey);
		oauth.put("oauth_signature_method", "HMAC-SHA1");
		oauth.put("oauth_version", "1.0");
	}
	
	public boolean hasDropboxAuth() {
		final String userKey = plugin.persist.getDropboxAuth("key");
		oauth.put("oauth_token", userKey);
		
		return userKey != null && (userSecret = plugin.persist.getDropboxAuth("secret")) != null;
	}
	
	@Override public void run() {
		if (Debug.ON) plugin.ifDebug("TaskDropbox run()");
		
		if (plugin.isWorking()) {
			plugin.debug(" - TaskDropbox not checking upload queue because something else is already in progress");
			return;
		}
		
		final String target = plugin.persist.getDropboxUpload();
		if (target == null) {
			if (Debug.ON) plugin.ifDebug("d \\ but nothing in queue");
			return;
		}
		
		plugin.setWorking(this, true);
		plugin.log(" * upload " + target);
		
		final String path = HttpUtils.encode(target.substring(plugin.config.getDir("destination").length() + 1)).replace("%2F", "/").replace("%5C", "/");
		final long start = System.nanoTime();
		
		try {
			HttpUtils.put("https://api-content.dropbox.com/1/files_put/sandbox/" + path, getSignature(path), new File(target));
			plugin.debug("\t\\ upload done " + plugin.duration(start));
		}
		catch (final DropboxException e) {
			plugin.logException(e, path);
		}
		
		plugin.setWorking(this, false);
	}
	
	public String getSignature(final String url) throws DropboxException {
		final long time = Calendar.getInstance().getTimeInMillis();
		oauth.put("oauth_nonce", String.valueOf(new Random(time).nextLong()));
		oauth.put("oauth_timestamp", String.valueOf(time / 1000));
		
		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("url", url);
		params.put("params", HttpUtils.urlencode(oauth));
		params.put("oauth_token_secret", userSecret);
		
		return HttpUtils.createSignature(oauth, HttpUtils.responseGet("https://minebackup-dropbox.appspot.com/sign?" + HttpUtils.urlencode(params)));
	}
}
