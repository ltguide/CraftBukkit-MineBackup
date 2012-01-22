package ltguide.base.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;

import ltguide.base.exceptions.HttpException;

public class HttpUtils {
	public static String responseGet(final String url) throws HttpException {
		return new Scanner(get(url)).useDelimiter("\\A").next();
	}
	
	public static InputStream get(final String url) throws HttpException {
		return getResponse(setupConnection(url));
	}
	
	public static InputStream put(final String url, final String auth, final File file) throws HttpException {
		final HttpURLConnection connection = setupConnection(url);
		connection.setRequestProperty("Authorization", auth);
		
		try {
			final long length = file.length();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setFixedLengthStreamingMode((int) length);
			connection.setRequestProperty("Content-Length", String.valueOf(length));
			connection.setRequestProperty("Content-Encoding", "application/octet-stream");
			
			final InputStream inStream = new FileInputStream(file);
			final OutputStream outStream = connection.getOutputStream();
			try {
				final byte[] buf = new byte[4096];
				int len;
				
				while ((len = inStream.read(buf)) > -1)
					if (len > 0) outStream.write(buf, 0, len);
			}
			finally {
				inStream.close();
			}
			
			return getResponse(connection);
		}
		catch (final IOException e) {
			throw new HttpException(e);
		}
	}
	
	public static HttpURLConnection setupConnection(final String url) throws HttpException {
		try {
			final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			
			return connection;
		}
		catch (final IOException e) {
			throw new HttpException(e);
		}
	}
	
	public static InputStream getResponse(final HttpURLConnection connection) throws HttpException {
		try {
			final int code = connection.getResponseCode();
			if (code != 200 && code != 304) throw new HttpException(code);
			
			return connection.getInputStream();
		}
		catch (final IOException e) {
			throw new HttpException(e);
		}
	}
	
	public static String urlencode(final Map<String, String> params) {
		final StringBuilder sb = new StringBuilder();
		for (final Map.Entry<String, String> param : params.entrySet()) {
			sb.append("&");
			sb.append(param.getKey());
			sb.append("=");
			sb.append(encode(param.getValue()));
		}
		
		return sb.substring(1);
	}
	
	public static String createAuth(final SortedMap<String, String> params, final String signature) {
		final StringBuilder sb = new StringBuilder("OAuth ");
		
		for (final Map.Entry<String, String> param : params.entrySet()) {
			sb.append(param.getKey());
			sb.append("=\"");
			sb.append(encode(param.getValue()));
			sb.append("\", ");
		}
		
		sb.append("oauth_signature=\"");
		sb.append(signature);
		sb.append("\"");
		
		return sb.toString();
	}
	
	public static String encode(final String string) {
		try {
			return URLEncoder.encode(string, "UTF-8").replace("+", "%20").replace("*", "%2A");
		}
		catch (final Exception e) {
			return "";
		}
	}
}
