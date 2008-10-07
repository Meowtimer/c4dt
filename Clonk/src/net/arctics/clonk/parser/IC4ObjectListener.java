package net.arctics.clonk.parser;

public interface IC4ObjectListener {

	public void fieldAdded(C4Object obj, C4Field field);
	public void fieldRemoved(C4Object obj, C4Field field);
	public void fieldChanged(C4Object obj, C4Field field);
	
}
