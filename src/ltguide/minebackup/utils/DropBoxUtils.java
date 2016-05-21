package ltguide.minebackup.utils;

import com.dropbox.core.*;
import ltguide.minebackup.MineBackup;

import java.io.File;
import java.util.Locale;

public class DropBoxUtils {

    public static final long MAX_DROPBOX_PUSH_SIZE = 150 * 1024 * 1024;

    private static final DropBoxUtils instance = new DropBoxUtils();
    private static final String APP_KEY = "jbx8av4f9v22nol";
    private static final String APP_SECRET = "";
    private static final String IDENTIFIER = "MineBackupV2";

    private DbxAppInfo appInfo;
    private DbxWebAuthNoRedirect webAuth;


    private DropBoxUtils() {
        appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
    }

    public static DropBoxUtils getInstance() {
        return instance;
    }

    /**
     * Starts authorisation for dropbox OAuth2.
     *
     * @return Returns the link to allows the access.
     */
    public String authoriseStart() {
        DbxRequestConfig requestConfig = new DbxRequestConfig(IDENTIFIER, Locale.getDefault().toString());
        webAuth = new DbxWebAuthNoRedirect(requestConfig, appInfo);

        return webAuth.start();
//        System.out.println("1. Go to " + authorizeUrl);
//        System.out.println("2. Click \"Allow\" (you might have to log in first).");
//        System.out.println("3. Copy the authorization code.");
//        System.out.print("Enter the authorization code here: ");
    }

    public boolean authoriseEnd(MineBackup plugin, String code) {

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

    public void upload(File file, String authToken) {
        if (file.length() > MAX_DROPBOX_PUSH_SIZE) {
            uploadChunked(file);
        } else {
            uploadPush(file);
        }
    }

    private void uploadPush(File file) {

    }

    private void uploadChunked(File file) {

    }
}
