package net.arctics.clonk.ui.editors;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectResource;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * A hyperlink that stores a reference to the hyperlinked Clonk declaration
 */
@SuppressWarnings("restriction")
public class ClonkHyperlink implements IHyperlink {

	private final IRegion region;
	protected Set<IIndexEntity> targets;

	public ClonkHyperlink(IRegion region, IIndexEntity target) {
		super();
		this.region = region;
		this.targets = ArrayUtil.set(target);
	}
	
	public ClonkHyperlink(IRegion region, Set<IIndexEntity> targets) {
		this.region = region;
		this.targets = targets;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getHyperlinkText() {
		for (IIndexEntity t : targets)
			return t.name();
		return "hyperlink to ?";
	}

	@Override
	public String getTypeLabel() {
		return Messages.ClonkHyperlink_Label;
	}

	@Override
	public void open() {
		if (this.targets.size() == 1) {
			openTarget(this.targets.iterator().next(), true);
		}
		else
			chooseDeclarations();
	}

	private void chooseDeclarations() {
		EntityChooser chooser = new EntityChooser(ClonkCore.instance().getWorkbench().getActiveWorkbenchWindow().getShell(), this.targets);
		chooser.run();
	}

	public static void openTarget(IIndexEntity target, boolean activateEditor) {
		try {
			if (target instanceof Declaration) {
				Declaration dec = (Declaration)target;
				if (ClonkTextEditor.openDeclaration(dec, activateEditor) == null) {
					// can't open editor so try something else like opening up a documentation page in the browser
					if (dec.isEngineDeclaration()) {
						openDocumentationForFunction(dec.name(), dec.engine());
					}
				}
			}
			else if (target instanceof ProjectResource) {
				CommonNavigator nav = UI.projectExplorer();
				nav.setFocus();
				nav.selectReveal(new StructuredSelection(((ProjectResource)target).resource()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static WeakReference<IWebBrowser> internalBrowser = new WeakReference<IWebBrowser>(null);

	public static void openDocumentationForFunction(String functionName, Engine engine) throws PartInitException, MalformedURLException {
		String docURLTemplate = Function.getDocumentationURL(functionName, engine);
		IWorkbenchBrowserSupport support = WorkbenchBrowserSupport.getInstance();
		IWebBrowser browser;
		if (ClonkCore.instance().getPreferenceStore().getBoolean(ClonkPreferences.OPEN_EXTERNAL_BROWSER) || !support.isInternalWebBrowserAvailable()) {
			browser = support.getExternalBrowser();
		}
		else {
			browser = internalBrowser.get();
			if (browser == null) {
				internalBrowser = new WeakReference<IWebBrowser>(browser = support.createBrowser(null));
			}
		}
		if (browser != null) {
			browser.openURL(new URL(String.format(
				docURLTemplate,
				functionName, ClonkPreferences.getLanguagePref().toLowerCase()
			)));
		}
	}

	public IRegion region() {
		return region;
	}

	public IIndexEntity target() {
		return targets.iterator().next();
	}
	
	public Set<IIndexEntity> targets() {
		return targets;
	}

}