/**
 * 
 */
package net.arctics.clonk.ui.editors;

import net.arctics.clonk.parser.C4Declaration;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class ClonkHyperlink implements IHyperlink {

	private final IRegion region;
	protected C4Declaration target;
	
	/**
	 * @param region
	 * @param target
	 */
	public ClonkHyperlink(IRegion region, C4Declaration target) {
		super();
		this.region = region;
		this.target = target;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getHyperlinkText() {
		return target.getName();
	}

	public String getTypeLabel() {
		return "Clonk Hyperlink";
	}

	public void open() {
		try {
			ClonkTextEditor.openDeclaration(target);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public IRegion getRegion() {
    	return region;
    }

	public C4Declaration getTarget() {
    	return target;
    }
	
}