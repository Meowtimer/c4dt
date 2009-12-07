package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

public class ClonkContextInformation implements IContextInformation {

	private String contextDisplayString;
	private Image image;
	private String informationDisplayString;
	private int parmIndex;

	public ClonkContextInformation(String contextDisplayString, Image image,
            String informationDisplayString, int parmIndex) {
	    super();
	    this.contextDisplayString = contextDisplayString;
	    this.image = image;
	    this.informationDisplayString = informationDisplayString;
	    this.parmIndex = parmIndex;
    }
	
	public int getParmIndex() {
		return parmIndex;
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
