package net.arctics.clonk.ui.editors;

import java.net.MalformedURLException;
import java.net.URL;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.preferences.ClonkPreferences;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;

/**
 * A hyperlink that stores a reference to the hyperlinked Clonk declaration
 */
@SuppressWarnings("restriction")
public class ClonkHyperlink implements IHyperlink {

	private final IRegion region;
	protected C4Declaration target;

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
		return Messages.ClonkHyperlink_Label;
	}

	public void open() {
		try {
			if (ClonkTextEditor.openDeclaration(target) == null) {
				// can't open editor so try something else like opening up a documentation page in the browser
				if (target.isEngineDeclaration()) {
					openDocumentationForFunction(target.getName(), target.getEngine());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void openDocumentationForFunction(String functionName, C4Engine engine) throws PartInitException, MalformedURLException {
		String docURLTemplate = C4Function.getDocumentationURL(functionName, engine);
		IWorkbenchBrowserSupport support = WorkbenchBrowserSupport.getInstance();
		IWebBrowser browser;
		if (!ClonkCore.getDefault().getPreferenceStore().getBoolean(ClonkPreferences.OPEN_EXTERNAL_BROWSER) && support.isInternalWebBrowserAvailable()) {
			browser = support.createBrowser(null);
		}
		else {
			browser = support.getExternalBrowser();
		}
		if (browser != null)
			browser.openURL(new URL(String.format(
				docURLTemplate,
				functionName, ClonkPreferences.getLanguagePref().toLowerCase()
			)));
	}

	public IRegion getRegion() {
		return region;
	}

	public C4Declaration getTarget() {
		return target;
	}

}