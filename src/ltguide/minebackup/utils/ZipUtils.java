package ltguide.minebackup.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for compressing Zip stripped/refactored/modified by ltguide
 * 
 * @author iubito (Sylvain Machefert)
 * @author ltguide (Matthew Churilla)
 */
public class ZipUtils {
	private static final int BUFFER_SIZE = 4 * 1024;
	
	public static void zipDir(final File srcDir, final File destFile, String prepend, final int level, final FilenameFilter filter) throws FileNotFoundException, IOException {
		destFile.getParentFile().mkdirs();
		final FileOutputStream outStream = new FileOutputStream(destFile);
		
		try {
			final CheckedOutputStream checkedOutStream = new CheckedOutputStream(outStream, new Adler32());
			try {
				final BufferedOutputStream bufOutStream = new BufferedOutputStream(checkedOutStream, BUFFER_SIZE);
				try {
					final ZipOutputStream zipOutStream = new ZipOutputStream(bufOutStream);
					try {
						zipOutStream.setLevel(level);
						
						if (prepend != null) {
							prepend += "/";
							zipOutStream.putNextEntry(new ZipEntry(prepend));
							zipOutStream.closeEntry();
						}
						else prepend = "";
						
						zipDir(srcDir, "", prepend, zipOutStream, filter);
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
				checkedOutStream.close();
			}
		}
		finally {
			outStream.close();
		}
	}
	
	private static void zipDir(final File srcDir, String currentDir, final String prepend, final ZipOutputStream zipOutStream, final FilenameFilter filter) throws FileNotFoundException, IOException {
		if (!"".equals(currentDir)) {
			currentDir += "/";
			zipOutStream.putNextEntry(new ZipEntry(prepend + currentDir));
			zipOutStream.closeEntry();
		}
		
		final File zipDir = new File(srcDir, currentDir);
		for (final String child : zipDir.list(filter)) {
			final File srcFile = new File(zipDir, child);
			
			if (srcFile.isDirectory()) zipDir(srcDir, currentDir + child, prepend, zipOutStream, filter);
			else {
				final ZipEntry zipEntry = new ZipEntry(prepend + currentDir + child);
				zipEntry.setTime(srcFile.lastModified());
				zipFile(srcFile, zipEntry, zipOutStream);
			}
		}
	}
	
	private static void zipFile(final File srcFile, final ZipEntry zipEntry, final ZipOutputStream zipOutStream) throws FileNotFoundException, IOException {
		final InputStream inStream = new FileInputStream(srcFile);
		try {
			final BufferedInputStream bufInStream = new BufferedInputStream(inStream, BUFFER_SIZE);
			try {
				zipOutStream.putNextEntry(zipEntry);
				
				final byte[] buf = new byte[BUFFER_SIZE];
				int len;
				
				while ((len = bufInStream.read(buf)) > -1)
					if (len > 0) zipOutStream.write(buf, 0, len);
				
				zipOutStream.closeEntry();
			}
			finally {
				bufInStream.close();
			}
		}
		finally {
			inStream.close();
		}
	}
	
}
