package ltguide.base.data;

public interface ICommand {
	String name();
	
	String permission();
	
	String message();
	
	String syntax();
	
	boolean usesTarget();
}
