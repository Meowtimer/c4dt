package net.arctics.clonk.parser;

public class Pair<First, Second> {
	private First first;
	private Second second;
	public Pair(First first, Second second) {
		super();
		this.first = first;
		this.second = second;
	}
	public First getFirst() {
		return first;
	}
	public void setFirst(First first) {
		this.first = first;
	}
	public Second getSecond() {
		return second;
	}
	public void setSecond(Second second) {
		this.second = second;
	}
}
