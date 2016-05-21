package ltguide.minebackup.utils;

import com.dropbox.core.*;
import ltguide.minebackup.MineBackup;

import javax.naming.AuthenticationException;
import java.io.*;
import java.util.Locale;

public class DropBoxUtils {

    public static final long MAX_DROPBOX_PUSH_SIZE = 150 * 1024 * 1024;

    private static final String APP_KEY = "jbx8av4f9v22nol";
    private static final String APP_SECRET = "";
    private static final String IDENTIFIER = "MineBackupV2";
    private static final int DROPBOX_UPLOAD_CHUNK_SIZE = 2 * 1024 * 1024;

    private DbxAppInfo appInfo;
    private DbxWebAuthNoRedirect webAuth;
    private DbxRequestConfig requestConfig;
    private MineBackup plugin;

    public DropBoxUtils(MineBackup plugin) {
        this.plugin = plugin;
        this.appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
        this.requestConfig = new DbxRequestConfig(IDENTIFIER, Locale.getDefault().toString());
    }

    /**
     * Starts authorisation for dropbox OAuth2.
     *
     * @return Returns the link to allows the access.
     */
    public String authoriseStart() {
        webAuth = new DbxWebAuthNoRedirect(requestConfig, appInfo);

        return webAuth.start();
//        System.out.println("1. Go to " + authorizeUrl);
//        System.out.println("2. Click \"Allow\" (you might have to log in first).");
//        System.out.println("3. Copy the authorization code.");
//        System.out.print("Enter the authorization code here: ");
    }

    public boolean authoriseEnd(String code) {

        if (webAuth == null) {
            return false;
        }

        DbxAuthFinish authFinish;
        try {
            authFinish = webAuth.finish(code);
        } catch (DbxException ex) {
            plugin.logException(ex, "Error in DbxWebAuth.start: " + ex.getMessage());
            return false;
        }

        // Save auth information to output file.
        plugin.persist.setDropboxToken(authFinish.accessToken);

        return true;
    }

    public void upload(File file, String authToken) throws AuthenticationException {
        if (authToken == null) {
            throw new AuthenticationException("Invalid authtoken");
        }

        DbxClient dbxClient = new DbxClient(requestConfig, authToken);
        String path = file.getPath().substring(((String) plugin.config.get("directories.destination")).length());

        try {
            if (file.length() > MAX_DROPBOX_PUSH_SIZE) {
                uploadChunked(dbxClient, file, path);
            } else {
                uploadPush(dbxClient, file, path);
            }

            plugin.info("Uploaded " + file.getName() + " to dropbox.");
        } catch (DbxException ex) {
            plugin.logException(ex, "Error uploading to Dropbox");
        } catch (IOException ex) {
            plugin.logException(ex, "Error reading from file \"" + file);
        }
    }

    private void uploadPush(DbxClient dbxClient, File file, String path) throws IOException, DbxException {
        try (InputStream in = new FileInputStream(file)) {
            dbxClient.uploadFile(path, DbxWriteMode.add(), file.length(), in);
        }
    }

    private void uploadChunked(DbxClient dbxClient, File file, String path) throws IOException, DbxException {

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        byte[] fileBuffer = new byte[DROPBOX_UPLOAD_CHUNK_SIZE];
        raf.read(fileBuffer, 0, DROPBOX_UPLOAD_CHUNK_SIZE);

        String uploadId = dbxClient.chunkedUploadFirst(fileBuffer);
        float lastPercent = 0;
        for (int currentPosition = DROPBOX_UPLOAD_CHUNK_SIZE; currentPosition < file.length(); ) {
            int length = currentPosition + DROPBOX_UPLOAD_CHUNK_SIZE < file.length() ? DROPBOX_UPLOAD_CHUNK_SIZE : (int) (file.length() - currentPosition);
            raf.read(fileBuffer, 0, length);
            dbxClient.chunkedUploadAppend(uploadId, currentPosition, fileBuffer);
            currentPosition += length;

            float percentDone = (float) currentPosition / file.length();
            if (percentDone - lastPercent > 0.10) {
                lastPercent = percentDone;
                plugin.info(String.format("%.0f of file uploaded.", percentDone * 100));
            }
        }

        dbxClient.chunkedUploadFinish(path, DbxWriteMode.add(), uploadId);
    }
}
