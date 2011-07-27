package net.arctics.clonk.ui.editors;

import net.arctics.clonk.util.UI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

public class HyperlinkToResource implements IHyperlink {

	private IResource resource;
	private IRegion region;
	private IWorkbenchWindow workbenchWindow;
	
	public HyperlinkToResource(IResource resource, IRegion region, IWorkbenchWindow window) {
		super();
		this.resource = resource;
		this.region = region;
		this.workbenchWindow = window;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getTypeLabel() {
		return "Hyperlink To Project";
	}

	@Override
	public String getHyperlinkText() {
		return resource.getName();
	}

	@Override
	public void open() {
		UI.projectExplorer(workbenchWindow).selectReveal(new StructuredSelection(resource));
	}

}
