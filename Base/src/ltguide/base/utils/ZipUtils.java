package ltguide.base.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ltguide.base.Base;
import ltguide.base.Debug;

public class ZipUtils {
	private static FilenameFilter filter;
	private static String prepend;
	private static ZipOutputStream zipOutStream;
	
	public static void zipDir(final File srcDir, final File destFile, String prepend, final int level, final FilenameFilter filter) throws FileNotFoundException, IOException {
		destFile.getParentFile().mkdirs();
		final FileOutputStream outStream = new FileOutputStream(destFile);
		
		try {
			final BufferedOutputStream bufOutStream = new BufferedOutputStream(outStream, Base.bufferSize);
			try {
				zipOutStream = new ZipOutputStream(bufOutStream);
				try {
					zipOutStream.setLevel(level);
					
					if (prepend != null) {
						prepend += "/";
						zipOutStream.putNextEntry(new ZipEntry(prepend));
						zipOutStream.closeEntry();
					}
					else prepend = "";
					
					ZipUtils.prepend = prepend;
					ZipUtils.filter = filter;
					
					zipDir(srcDir, "");
				}
				finally {
					zipOutStream.close();
				}
			}
			finally {
				bufOutStream.close();
			}
		}
		finally {
			outStream.close();
		}
	}
	
	private static void zipDir(final File srcDir, String currentDir) throws IOException {
		if (!"".equals(currentDir)) {
			currentDir += "/";
			zipOutStream.putNextEntry(new ZipEntry(prepend + currentDir));
			zipOutStream.closeEntry();
		}
		
		final File zipDir = new File(srcDir, currentDir);
		for (final String child : zipDir.list(filter)) {
			final File srcFile = new File(zipDir, child);
			
			if (srcFile.isDirectory()) zipDir(srcDir, currentDir + child);
			else zipFile(srcFile, prepend + currentDir + child);
		}
	}
	
	private static void zipFile(final File srcFile, final String entry) throws IOException {
		final InputStream inStream = new FileInputStream(srcFile);
		try {
			final ZipEntry zipEntry = new ZipEntry(entry);
			zipEntry.setTime(srcFile.lastModified());
			zipOutStream.putNextEntry(zipEntry);
			
			final byte[] buf = new byte[Base.bufferSize];
			int len;
			
			try {
				while ((len = inStream.read(buf)) > -1)
					if (len > 0) zipOutStream.write(buf, 0, len);
			}
			catch (final IOException e) {
				Debug.warning(" | unable to add file: " + srcFile);
			}
			finally {
				zipOutStream.closeEntry();
			}
		}
		finally {
			inStream.close();
		}
	}
}
