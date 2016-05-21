package ltguide.minebackup.data;

import java.util.Comparator;

public class Process implements Comparable<Process> {
	private final String type;
	private final String name;
	private final String action;
	private long next;
	
	public Process(final String type, final String name, final String action, final long next) {
		this.type = type;
		this.name = name;
		this.action = action;
		setNext(next);
	}
	
	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAction() {
		return action;
	}
	
	public long getNext() {
		return next;
	}
	
	public void setNext(final long next) {
		this.next = next;
	}
	
	public Action valueOfAction() {
		return Action.valueOf(action.toUpperCase());
	}
	
	@Override
	public int compareTo(final Process process) {
		return (int) (getNext() - process.getNext());
	}
	
	public static Comparator<Process> comparator = new Comparator<Process>() {
		@Override
		public int compare(final Process process1, final Process process2) {
			return process1.compareTo(process2);
		}
	};
}
