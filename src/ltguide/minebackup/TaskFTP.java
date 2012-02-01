package ltguide.minebackup;

import ltguide.base.Base;
import ltguide.base.Debug;
import ltguide.base.utils.FtpUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public class TaskFTP extends Thread {
    private final MineBackup plugin;
    private ConfigurationSection ftpConfig;

    public TaskFTP(final MineBackup plugin) {
        this.plugin = plugin;

    }



    @Override public void run() {
        ftpConfig = plugin.getConfig().getConfigurationSection("ftp");
        Base.info("FTP Task running");
        if (Debug.ON) Debug.info("TaskFTP run()");

        if (plugin.isWorking()) {
            Base.debug("not checking FTP upload queue because an action is in progress");
            return;
        }

        String target = plugin.persist.getFTPUpload();
        if (target == null) {
            if (Debug.ON) Debug.info("FTP - but nothing in queue");
            return;
        }

        final File file = new File(target);
        if (!file.exists()) return;
        boolean ftpConfigOK = true;
        String ftpUser = ftpConfig.getString("ftpuser");
        if (ftpUser.isEmpty()) ftpConfigOK = false;
        String ftpHost = ftpConfig.getString("ftphost");
        if (ftpHost.isEmpty()) ftpConfigOK = false;
        int ftpPort = ftpConfig.getInt("ftpport");
        if (ftpPort == 0 ) ftpConfigOK = false;
        String ftpPW = ftpConfig.getString("ftppassword");
        if (ftpPW.isEmpty()) ftpConfigOK = false;

        if (!ftpConfigOK){
            if (Debug.ON) Debug.info("FTP Configuration not correct");
            return;
        }

        String ftpTargetDir = ftpConfig.getString("ftptargetdir");

        plugin.setWorking(this, true);
        Base.info(" * uploading " + target);

        FtpUtils ftp = new FtpUtils();
        ftp.setHost(ftpHost+":"+Integer.toString(ftpPort));
        ftp.setUser(ftpUser);
        ftp.setPassword(ftpPW);

        if (ftp.connect()){
            String remoteTarget;
            if (ftpTargetDir.isEmpty()){
                remoteTarget = target;
            } else {
                remoteTarget = ftpTargetDir +"/"+target;
            }
            ftp.setRemoteFile(target);
            if (ftp.uploadFile(target))
                // display the message of success if uploaded
                Base.debug(ftp.getLastSuccessMessage ());
            else
                Base.debug(ftp.getLastErrorMessage ());
        }
        else {
            // Display any connection exception, if any
            Base.debug(ftp.getLastErrorMessage ());
        }

        plugin.setWorking(this, false);
    }


}
