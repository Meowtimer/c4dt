package net.arctics.clonk.landscapescript;

public enum Unit {
	Percent,
	Pixels;
	
	public static Unit parse(final String px) {
		if (px.equals("px")) //$NON-NLS-1$
			return Pixels;
		if (px.equals("%")) //$NON-NLS-1$
			return Percent;
		return Pixels;
	}
	@Override
	public String toString() {
	    switch (this) {
	    case Percent:
	    	return "%"; //$NON-NLS-1$
	    case Pixels:
	    	return "px"; //$NON-NLS-1$
	    default:
	    	return super.toString();
	    }
	}
}