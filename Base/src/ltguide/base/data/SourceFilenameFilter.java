package ltguide.base.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class SourceFilenameFilter implements FilenameFilter {
	private final List<String> folders;
	private final List<String> types;
	private boolean allFolders = false;
	
	public SourceFilenameFilter(final List<String> folders, final List<String> types) throws IllegalArgumentException {
		if (folders == null && types == null) throw new IllegalArgumentException();
		
		this.folders = folders;
		this.types = types;
		
		if (folders != null && folders.contains("*")) allFolders = true;
	}
	
	@Override
	public boolean accept(final File dir, final String name) {
		final String path = dir.getName().toLowerCase();
		final String nameLower = name.toLowerCase();
		
		if (allFolders) {
			if (!".".equals(path) || new File(dir, name).isDirectory()) return false;
		}
		else for (final String folder : folders)
			if (path.indexOf(folder) == 0) return false;
		
		for (final String type : types)
			if (nameLower.endsWith(type)) return false;
		
		return true;
	}
}
