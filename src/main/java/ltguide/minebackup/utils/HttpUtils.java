package ltguide.minebackup.utils;

import ltguide.minebackup.Base;
import ltguide.minebackup.exceptions.HttpException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class HttpUtils {


    public static InputStream get(final String url) throws HttpException {
        return getResponse(setupConnection(url));
    }

    public static void ftp(final String url, final File file) throws HttpException {
        try {
            send(new FileInputStream(file), new URL(url).openConnection().getOutputStream());
        } catch (final IOException e) {
            throw new HttpException(e);
        }

    }

    private static void send(final InputStream inStream, final OutputStream outStream) throws IOException {
        try {
            final byte[] buf = new byte[Base.bufferSize];
            int len;

            while ((len = inStream.read(buf)) > -1)
                if (len > 0) outStream.write(buf, 0, len);
        } finally {
            inStream.close();
            outStream.close();
        }
    }

    public static HttpURLConnection setupConnection(final String url) throws HttpException {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            return connection;
        } catch (final IOException e) {
            throw new HttpException(e);
        }
    }

    public static InputStream getResponse(final HttpURLConnection connection) throws HttpException {
        try {
            final int code = connection.getResponseCode();
            if (code != 200 && code != 304) throw new HttpException(code);

            return connection.getInputStream();
        } catch (final IOException e) {
            throw new HttpException(e);
        }
    }

    public static String encode(final String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20").replace("*", "%2A");
        } catch (final Exception e) {
            return "";
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
