package net.arctics.clonk.ui.editors;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;

public class ClonkContextInformation implements IContextInformation, IContextInformationExtension {

	private String contextDisplayString;
	private Image image;
	private String informationDisplayString;
	private int parmIndex;
	private int parmsStart, parmsEnd;
	private int parmCount;
	
	@Override
	public String toString() {
		return String.format("%d %d", parmsStart, parmsEnd); //$NON-NLS-1$
	}
	
	@Override
	public boolean equals(Object obj) {
		//System.out.println("ClonkContextInformation.equals called");
		if (obj instanceof ClonkContextInformation) {
			ClonkContextInformation info = (ClonkContextInformation) obj;
			return parmsStart == info.parmsStart; // similar enough :o
		}
		return false;
	}
	
	public ClonkContextInformation(String contextDisplayString, Image image,
		String informationDisplayString, int parmIndex, int parmsStart, int parmsEnd, int parmCount) {
	    super();
	    this.contextDisplayString = contextDisplayString;
	    this.image = image;
	    this.informationDisplayString = informationDisplayString;
	    this.parmIndex = parmIndex;
	    this.parmsStart = parmsStart;
	    this.parmsEnd = parmsEnd;
	    this.parmCount = parmCount;
    }

	public boolean valid(int offset) {
		//System.out.println(String.format("%d %d %d", parmsStart, offset, parmsEnd));
		return parmIndex != -1 && offset >= parmsStart;// && offset <= parmsEnd; <- does the trick!1
	}
	
	public ClonkContextInformation() {
		this.parmIndex = -1;
	}

	public int getParmIndex() {
		return parmIndex;
	}

	public int getParmCount() {
		return parmCount;
	}
	
	public int getParmsStart() {
		return parmsStart;
	}

	public int getParmsEnd() {
		return parmsEnd;
	}

	@Override
    public String getContextDisplayString() {
	    return contextDisplayString;
    }

	@Override
    public Image getImage() {
	    return image;
    }

	@Override
    public String getInformationDisplayString() {
	    return informationDisplayString;
    }

	@Override
	public int getContextInformationPosition() {
		return parmsStart; 
	}
	
}
