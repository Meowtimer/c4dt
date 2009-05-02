/**
 * 
 */
package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class ClonkHyperlink implements IHyperlink {

	private final IRegion region;
	private C4Field target;
	
	/**
	 * @param region
	 * @param target
	 */
	public ClonkHyperlink(IRegion region, C4Field target) {
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
	
}