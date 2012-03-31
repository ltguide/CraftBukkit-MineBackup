package ltguide.minebackup.data;

public class Upload {
	private final String type;
	private final String name;
	
	public Upload(final String type, final String name) {
		this.type = type;
		this.name = name;
	}
	
	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
