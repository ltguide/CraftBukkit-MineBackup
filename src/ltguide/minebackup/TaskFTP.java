package ltguide.minebackup;

import it.sauronsoftware.ftp4j_MB.*;
import ltguide.base.Base;
import ltguide.base.Debug;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;

public class TaskFTP extends Thread {
    private final MineBackup plugin;
    private ConfigurationSection ftpConfig;

    public TaskFTP(final MineBackup plugin) {
        this.plugin = plugin;

    }

// used ftp4j from http://www.sauronsoftware.it/projects/ftp4j/index.php


    @Override public void run() {
        ftpConfig = plugin.getConfig().getConfigurationSection("ftp");
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
        if (Debug.ON) Debug.info("FtpUser: " + ftpUser);
        if (ftpUser.isEmpty()) ftpConfigOK = false;
        String ftpHost = ftpConfig.getString("ftphost");
        if (Debug.ON) Debug.info("FtpHost: " + ftpHost);
        if (ftpHost.isEmpty()) ftpConfigOK = false;
        int ftpPort = ftpConfig.getInt("ftpport");
        if (Debug.ON) Debug.info("FtpPort: " +ftpPort);
        if (ftpPort == 0 ) ftpConfigOK = false;
        String ftpPW = ftpConfig.getString("ftppassword");
        if (Debug.ON) Debug.info("FtpPW: " + ftpPW);
        if (ftpPW.isEmpty()) ftpConfigOK = false;

        if (!ftpConfigOK){
            if (Debug.ON) Debug.info("FTP Configuration not correct");
            return;
        }
        String ftpTargetDir = ftpConfig.getString("ftptargetdir");
        if (Debug.ON) Debug.info("FtpTargetDir: " + ftpTargetDir);
        plugin.setWorking(this, true);
        Base.info(" * uploading " + target);

        if (ftpTargetDir.isEmpty()){
            ftpTargetDir = target.substring(plugin.config.getDir("destination").length() + 1).replace("%2F", "\\").replace("%5C", "\\");
        } else {
            ftpTargetDir = ftpTargetDir+ "\\" + target.substring(plugin.config.getDir("destination").length() + 1).replace("%2F", "\\").replace("%5C", "\\");
        }


        FTPClient ftp = new FTPClient();

        // lets try to connect
        try {
            if (ftpPort == 21) {
                ftp.connect(ftpHost);
            }   else {
                ftp.connect(ftpHost, ftpPort);
            }
            // now lets try to login
            ftp.login(ftpUser, ftpPW);
            // perhaps change the target directory
            if (!ftpTargetDir.isEmpty()){
                String[] dirs = ftpTargetDir.split("\\\\");
                for (String dir : dirs){
                    if (Debug.ON) Debug.info("Dir: " + dir);
                    if (!dir.contains(".zip")){
                        FTPFile[] list = ftp.list();
                        boolean createSubDir = true;
                        for (FTPFile subdir : list) {
                            if (Debug.ON) Debug.info("SubDir: "+subdir);
                            if (subdir.getName().equalsIgnoreCase(dir)){
                                createSubDir = false;
                            }
                        }
                        if (createSubDir){
                            ftp.createDirectory(dir);

                        }
                        ftp.changeDirectory(dir);
                    }
                }
            }
            // now upload the file
            ftp.upload(new java.io.File(target));
            Base.debug("  \\ upload done " + Base.stopTime());

            // finally closing the connection
            ftp.disconnect(true);

        } catch (IOException e) {
            Base.logException(e, "FTP IO Exception");
        } catch (FTPIllegalReplyException e) {
            Base.logException(e, "Illegal FTP Reply");
        } catch (FTPException e) {
            Base.logException(e, "FTP exception");
        } catch (FTPAbortedException e) {
            Base.logException(e, "FTP Transfer aborted");
        } catch (FTPDataTransferException e) {
            Base.logException(e, "FTP Transfer exception");
        } catch (FTPListParseException e) {
            Base.logException(e, "FTP Parse exception");
        }


        plugin.setWorking(this, false);
    }


}
