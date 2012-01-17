package ltguide.minebackup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DirUtils {
	
	public static void delete(final File target) {
		if (target.isDirectory()) for (final File child : target.listFiles())
			delete(child);
		
		target.delete();
	}
	
	public static void copyDir(final File srcDir, File destDir, final String prepend, final FilenameFilter filter) throws FileNotFoundException, IOException {
		if (prepend != null) destDir = new File(destDir, prepend);
		destDir.mkdirs();
		
		for (final String child : srcDir.list(filter)) {
			final File src = new File(srcDir, child);
			final File dest = new File(destDir, child);
			
			if (src.isDirectory()) copyDir(src, dest, null, filter);
			else copyFile(src, dest);
		}
	}
	
	public static void copyFile(final File srcFile, final File destFile) throws FileNotFoundException, IOException {
		final InputStream inStream = new FileInputStream(srcFile);
		final OutputStream outStream = new FileOutputStream(destFile);
		try {
			final byte[] buf = new byte[4096];
			int len;
			
			while ((len = inStream.read(buf)) > -1)
				if (len > 0) outStream.write(buf, 0, len);
		}
		finally {
			inStream.close();
			outStream.close();
		}
	}
}
