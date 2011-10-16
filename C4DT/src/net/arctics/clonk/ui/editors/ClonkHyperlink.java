package net.arctics.clonk.ui.editors;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Declaration.DeclarationLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.actions.c4script.DeclarationChooser;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;

/**
 * A hyperlink that stores a reference to the hyperlinked Clonk declaration
 */
@SuppressWarnings("restriction")
public class ClonkHyperlink implements IHyperlink {

	private final IRegion region;
	protected Declaration target;

	public ClonkHyperlink(IRegion region, Declaration target) {
		super();
		this.region = region;
		this.target = target;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getHyperlinkText() {
		return target.name();
	}

	public String getTypeLabel() {
		return Messages.ClonkHyperlink_Label;
	}

	public void open() {
		try {
			DeclarationLocation[] locations = target.getDeclarationLocations();
			if (locations.length == 1)
				ClonkTextEditor.openDeclaration(locations[0].getDeclaration());
			else
				new DeclarationChooser(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new HashSet<DeclarationLocation>(Arrays.asList(locations))).run();
			if (ClonkTextEditor.openDeclaration(target) == null) {
				// can't open editor so try something else like opening up a documentation page in the browser
				if (target.isEngineDeclaration()) {
					openDocumentationForFunction(target.name(), target.getEngine());
				}
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
		if (ClonkCore.getDefault().getPreferenceStore().getBoolean(ClonkPreferences.OPEN_EXTERNAL_BROWSER) || !support.isInternalWebBrowserAvailable()) {
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

	public IRegion getRegion() {
		return region;
	}

	public Declaration getTarget() {
		return target;
	}

}