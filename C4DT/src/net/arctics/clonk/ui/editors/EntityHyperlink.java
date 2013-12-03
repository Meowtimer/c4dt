package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.DeclarationLocation;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.index.DocumentedFunction;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.ProjectResource;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A hyperlink that stores a reference to the hyperlinked Clonk declaration
 */
@SuppressWarnings("restriction")
public class EntityHyperlink implements IHyperlink {

	private final IRegion region;
	protected Collection<? extends IIndexEntity> targets;

	public EntityHyperlink(final IRegion region, final IIndexEntity target) {
		super();
		this.region = region;
		this.targets = ArrayUtil.list(target);
	}
	
	public EntityHyperlink(final IRegion region, final Collection<? extends IIndexEntity> targets) {
		this.region = region;
		this.targets = targets;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getHyperlinkText() {
		for (final IIndexEntity t : targets)
			return t.name();
		return "hyperlink to ?"; //$NON-NLS-1$
	}

	@Override
	public String getTypeLabel() {
		return Messages.ClonkHyperlink_Label;
	}

	@Override
	public void open() {
		if (this.targets.size() == 1)
			openTarget(this.targets.iterator().next(), true);
		else
			chooseDeclarations();
	}

	private void chooseDeclarations() {
		final EntityChooser chooser = new EntityChooser(Messages.ClonkHyperlink_ChooseLinkTargetTitle, Core.instance().getWorkbench().getActiveWorkbenchWindow().getShell(), this.targets);
		chooser.run();
	}

	public static void openTarget(IIndexEntity target, final boolean activateEditor) {
		try {
			if (target instanceof DeclarationLocation)
				target = ((DeclarationLocation)target).declaration();
			final DocumentedFunction documentedFunction = as(target, DocumentedFunction.class);
			if (documentedFunction != null && documentedFunction.originInfo() != null) {
				final IFileStore sourceFile = EFS.getLocalFileSystem().fromLocalFile(
					new File(documentedFunction.engine().settings().repositoryPath, documentedFunction.originInfo())
				);
				final IEditorPart editor = IDE.openEditor(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
					new FileStoreEditorInput(sourceFile),
					EditorsUI.DEFAULT_TEXT_EDITOR_ID
				);
				if (editor instanceof ITextEditor)
					((ITextEditor)editor).selectAndReveal(documentedFunction.start(), documentedFunction.getLength());
			}
			else if (target instanceof Declaration) {
				final Declaration dec = (Declaration)target;
				if (StructureTextEditor.openDeclaration(dec, activateEditor) == null)
					// can't open editor so try something else like opening up a documentation page in the browser
					if (dec.isEngineDeclaration())
						openDocumentationForFunction(dec.name(), dec.engine());
			}
			else if (target instanceof ProjectResource) {
				final CommonNavigator nav = UI.projectExplorer();
				nav.setFocus();
				nav.selectReveal(new StructuredSelection(((ProjectResource)target).resource()));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	private static WeakReference<IWebBrowser> internalBrowser = new WeakReference<IWebBrowser>(null);

	public static void openDocumentationForFunction(final String functionName, final Engine engine) throws PartInitException, MalformedURLException {
		final String docURLTemplate = Function.documentationURLForFunction(functionName, engine);
		final URL url = new URL(String.format(
			docURLTemplate,
			functionName, ClonkPreferences.languagePref().toLowerCase()
		));
		openURL(url);
	}

	public static void openURL(final URL url) throws PartInitException {
		final IWorkbenchBrowserSupport support = WorkbenchBrowserSupport.getInstance();
		IWebBrowser browser;
		if (Core.instance().getPreferenceStore().getBoolean(ClonkPreferences.OPEN_EXTERNAL_BROWSER) || !support.isInternalWebBrowserAvailable())
			browser = support.getExternalBrowser();
		else {
			browser = internalBrowser.get();
			if (browser == null)
				internalBrowser = new WeakReference<IWebBrowser>(browser = support.createBrowser(null));
		}
		if (browser != null)
			browser.openURL(url);
	}

	public IRegion region() {
		return region;
	}

	public IIndexEntity target() {
		return targets.iterator().next();
	}
	
	public Collection<? extends IIndexEntity> targets() {
		return targets;
	}

}