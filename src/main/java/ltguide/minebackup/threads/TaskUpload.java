package ltguide.minebackup.threads;

import ltguide.minebackup.Debug;
import ltguide.minebackup.MineBackup;
import ltguide.minebackup.data.Upload;
import ltguide.minebackup.exceptions.HttpException;
import ltguide.minebackup.utils.HttpUtils;
import sun.net.ftp.FtpLoginException;

import javax.naming.AuthenticationException;
import java.io.File;
import java.io.FileNotFoundException;

public class TaskUpload extends Thread {
    private final MineBackup plugin;
    private boolean quick;
    private long startTime;
    private String ftpString;

    public TaskUpload(final MineBackup instance) {
        plugin = instance;

    }

    public boolean hasAuth(final String type) {
        if ("dropbox".equals(type)) {
            final String authToken = plugin.persist.getDropboxToken();
            return authToken != null;
        }

        return (ftpString = plugin.config.getFTPAuth()) != null;
    }

    @Override
    public void run() {
        if (Debug.ON) Debug.info("TaskUpload run() " + isQuick());
        if (plugin.isWorking()) {
            plugin.debug("not checking upload queue because an action is in progress; will try again shortly");
            plugin.spawnUpload(90);
            return;
        }

        plugin.setWorking(this, true);
        startTime = plugin.startTime();

        if (isQuick()) {
            runQuick();
            plugin.spawnUpload(90);
        } else runOnce();

    }

    private void runOnce() {
        upload(plugin.persist.getUpload());

        plugin.setWorking(this, false);
    }

    private void runQuick() {
        Upload upload;
        while ((upload = plugin.persist.getUpload()) != null)
            upload(upload);

        plugin.debug("> total " + plugin.stopTime(startTime));
        plugin.setWorking(this, false);
    }

    private boolean isQuick() {
        return quick;
    }

    public TaskUpload setQuick(final boolean quick) {
        this.quick = quick;
        return this;
    }

    private void upload(final Upload upload) {
        if (upload == null || !plugin.hasAction(upload.getType())) return;

        final File file = new File(upload.getName());
        if (!file.exists()) {
            if (Debug.ON) Debug.info("failed uploading " + file + " (doesnt exist)");
            return;
        }

        plugin.info(" * " + upload.getType() + " uploading " + upload.getName());
        plugin.startTime();
        final String path = HttpUtils.encode(upload.getName().substring(plugin.config.getDir("destination").getPath().length() + 1)).replace("%2F", "/").replace("%5C", "/");

        try {
            if ("dropbox".equals(upload.getType())) {
                plugin.dropBoxUtils.upload(file, plugin.persist.getDropboxToken());
            } else {
                HttpUtils.ftp(ftpString + path + ";type=i,mkd=survival", file);
            }

            plugin.debug("  \\ upload done " + plugin.stopTime());
        } catch (final HttpException e) {
            if (e.getCause() instanceof FtpLoginException) plugin.warning("% failed to login to ftp server");
            else if (e.getCause() instanceof FileNotFoundException)
                plugin.warning("% failed to upload to ftp server: " + e.getCause().getMessage());
            else plugin.warning("% " + e.getMessage());
        } catch (AuthenticationException e) {
            plugin.warning("No dropbox authentication available!");
        }
    }
}
