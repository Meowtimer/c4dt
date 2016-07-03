package net.arctics.clonk.ui.editors;

import net.arctics.clonk.util.UI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.navigator.CommonNavigator;

public class HyperlinkToResource implements IHyperlink {

	private final IResource resource;
	private final IRegion region;
	private final IWorkbenchWindow workbenchWindow;
	
	public HyperlinkToResource(final IResource resource, final IRegion region, final IWorkbenchWindow window) {
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
		final CommonNavigator nav = UI.projectExplorer(workbenchWindow);
		nav.setFocus();
		nav.selectReveal(new StructuredSelection(resource));
	}

}
