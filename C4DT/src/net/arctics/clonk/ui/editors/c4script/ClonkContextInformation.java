package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

public class ClonkContextInformation implements IContextInformation {

	private String contextDisplayString;
	private Image image;
	private String informationDisplayString;
	private int parmIndex;
	private int parmsStart, parmsEnd;

	public ClonkContextInformation(String contextDisplayString, Image image,
		String informationDisplayString, int parmIndex, int parmsStart, int parmsEnd) {
	    super();
	    this.contextDisplayString = contextDisplayString;
	    this.image = image;
	    this.informationDisplayString = informationDisplayString;
	    this.parmIndex = parmIndex;
	    this.parmsStart = parmsStart;
	    this.parmsEnd = parmsEnd;
    }

	public boolean valid(int offset) {
		//System.out.println(String.format("%d %d %d", parmsStart, offset, parmsEnd));
		return parmIndex != -1 && offset >= parmsStart && offset <= parmsEnd;
	}
	
	public ClonkContextInformation() {
		this.parmIndex = -1;
	}

	public int getParmIndex() {
		return parmIndex;
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
	
}
