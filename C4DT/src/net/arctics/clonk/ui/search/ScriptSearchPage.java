package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.search.ui.IReplacePage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class ScriptSearchPage extends DialogPage implements ISearchPage, IReplacePage {

	public static final String PREF_TEMPLATE_TEXT = ScriptSearchPage.class.getSimpleName()+".templateText"; //$NON-NLS-1$
	public static final String PREF_REPLACEMENT_TEXT = ScriptSearchPage.class.getSimpleName()+".replacementText"; //$NON-NLS-1$
	public static final String PREF_SCOPE = ScriptSearchPage.class.getSimpleName()+".scope"; //$NON-NLS-1$
	public static final String PREF_RECENTS = ScriptSearchPage.class.getSimpleName()+".recents"; //$NON-NLS-1$

	private static final String RECENTS_SEPARATOR = "<>"; //$NON-NLS-1$

	private Text templateText;
	private Text replacementText;
	private ISearchPageContainer container;
	private ComboViewer recentsCombo;

	public ScriptSearchPage() {}
	public ScriptSearchPage(String title) { super(title); }
	public ScriptSearchPage(String title, ImageDescriptor image) { super(title, image); }

	@Override
	public void createControl(Composite parent) {
		createTextFields(parent);
		readConfiguration();
	}

	public static class Recent {
		public String template;
		public String replacement;
		public Recent(String template, String replacement) {
			super();
			this.template = template;
			this.replacement = replacement;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Recent) {
				final Recent r = (Recent)obj;
				return eq(template, r.template) && eq(replacement, r.replacement);
			}
			else
				return false;
		}
		@Override
		public String toString() {
			return template;
		}
	}

	public static final int MAX_RECENTS = 20;

	private void addRecent() {
		final Recent recent = new Recent(templateText.getText(), replacementText.getText());
		final ArrayList<Recent> list = new ArrayList<Recent>(Arrays.asList((Recent[])recentsCombo.getInput()));
		final int ndx = list.indexOf(recent);
		if (ndx != -1)
			list.remove(ndx);
		list.add(recent);
		while (list.size() > MAX_RECENTS)
			list.remove(0);
		recentsCombo.setInput(list.toArray(new Recent[list.size()]));
		final IPreferenceStore prefs = Core.instance().getPreferenceStore();
		prefs.setValue(PREF_RECENTS, StringUtil.blockString("", "", "", map(list, new IConverter<Recent, String>() {
			@Override
			public String convert(Recent recent) {
				return recent.template + RECENTS_SEPARATOR + recent.replacement + RECENTS_SEPARATOR;
			}
		})));
	}

	private void readConfiguration() {
		final IPreferenceStore prefs = Core.instance().getPreferenceStore();
		templateText.setText(defaulting(prefs.getString(PREF_TEMPLATE_TEXT), "")); //$NON-NLS-1$
		replacementText.setText(defaulting(prefs.getString(PREF_REPLACEMENT_TEXT), "")); //$NON-NLS-1$

		//prefs.setValue(PREF_RECENTS, "");
		final String s = prefs.getString(PREF_RECENTS);
		final String[] recentsRaw = s != null ? s.split(Pattern.quote(RECENTS_SEPARATOR)) : new String[0];
		final Recent[] recents = new Recent[recentsRaw.length/2+recentsRaw.length%2];
		for (int i = 0; i < recents.length; i++)
			recents[i] = new Recent(recentsRaw[i*2], i*2+1 < recentsRaw.length ? recentsRaw[i*2+1] : ""); //$NON-NLS-1$
		recentsCombo.setInput(recents);
	}

	private void writeConfiguration() {
		final IPreferenceStore prefs = Core.instance().getPreferenceStore();
		prefs.setValue(PREF_TEMPLATE_TEXT, templateText.getText());
		prefs.setValue(PREF_REPLACEMENT_TEXT, replacementText.getText());
		prefs.setValue(PREF_SCOPE, container.getSelectedScope());
	}

	@Override
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	private void createTextFields(Composite parent) {
		final Composite ctrl = new Composite(parent, SWT.NONE);
		setControl(ctrl);
		final GridLayout gl_ctrl = new GridLayout(2, false);
		ctrl.setLayout(gl_ctrl);

		final Label recentTemplatesLabel = new Label(ctrl, SWT.LEFT);
		recentTemplatesLabel.setText(Messages.C4ScriptSearchPage_Recents);

		final Combo recentsCombo = new Combo(ctrl, SWT.READ_ONLY);
		final GridData gd_presetCombo = new GridData(GridData.FILL_HORIZONTAL);
		gd_presetCombo.widthHint = 568;
		recentsCombo.setLayoutData(gd_presetCombo);
		this.recentsCombo = new ComboViewer(recentsCombo);
		this.recentsCombo.setContentProvider(new ArrayContentProvider());
		this.recentsCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final ISelection sel = event.getSelection();
				if (!sel.isEmpty()) {
					final Recent r = (Recent)((IStructuredSelection)sel).getFirstElement();
					templateText.setText(r.template);
					replacementText.setText(r.replacement);
				}
			}
		});

		final Label templateLabel = new Label(ctrl, SWT.LEFT);
		templateLabel.setText(Messages.C4ScriptSearchPage_Template);
		templateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));

		templateText = new Text(ctrl, SWT.BORDER|SWT.MULTI);
		final GridData gd_templateText = new GridData(GridData.FILL_BOTH);
		//gd_templateText.widthHint = 527;
		gd_templateText.heightHint = 80;
		templateText.setLayoutData(gd_templateText);

		final Label replacementLabel = new Label(ctrl, SWT.LEFT);
		replacementLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		replacementLabel.setText(Messages.C4ScriptSearchPage_Replacement);

		replacementText = new Text(ctrl, SWT.BORDER|SWT.MULTI);
		final GridData gd_replacementText = new GridData(GridData.FILL_BOTH);
		gd_replacementText.heightHint = 80;
		replacementText.setLayoutData(gd_replacementText);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		container.setActiveEditorCanProvideScopeSelection(true);
		container.setPerformActionEnabled(true);
		templateText.setFocus();
		templateText.selectAll();
	}

	private ASTSearchQuery newQuery() {
		final ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		final ISelection sel = selectionService.getSelection();
		final Set<Script> scope = new HashSet<Script>();
		final IResourceVisitor scopeVisitor = new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				final Script script = Script.get(resource, true);
				if (script != null)
					scope.add(script);
				return true;
			}
		};
		switch (container.getSelectedScope()) {
		case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
			for (final ClonkProjectNature nature : map(iterable(container.getSelectedProjectNames()), new IConverter<String, ClonkProjectNature>() {
				@Override
				public ClonkProjectNature convert(String from) {
					return ClonkProjectNature.get(from);
				}
			}))
				if (nature != null)
					nature.index().allScripts(new Sink<Script>() {
						@Override
						public void receivedObject(Script item) {
							scope.add(item);
						}
					});
			break;
		case ISearchPageContainer.SELECTION_SCOPE:
			final IFileEditorInput input = as(container.getActiveEditorInput(), IFileEditorInput.class);
			IStructuredSelection ssel;
			if (input != null)
				ssel = new StructuredSelection(input.getFile());
			else
				ssel = as(sel, IStructuredSelection.class);
			if (ssel != null)
				for (final Object s : ssel.toArray())
					if (s instanceof IResource)
						try {
							((IResource)s).accept(scopeVisitor);
						} catch (final CoreException e1) {
							e1.printStackTrace();
						}
			break;
		case ISearchPageContainer.WORKSPACE_SCOPE:
			for (final IProject proj : ClonkProjectNature.clonkProjectsInWorkspace()) {
				final ClonkProjectNature nature = ClonkProjectNature.get(proj);
				if (nature != null)
					nature.index().allScripts(new Sink<Script>() {
						@Override
						public void receivedObject(Script item) {
							scope.add(item);
						}
					});
			}
			break;
		case ISearchPageContainer.WORKING_SET_SCOPE:
			for (final IWorkingSet workingSet : container.getSelectedWorkingSets())
				for (final IAdaptable a : workingSet.getElements()) {
					final IResource res = (IResource)a.getAdapter(IResource.class);
					if (res != null)
						try {
							res.accept(scopeVisitor);
						} catch (final CoreException e) {
							e.printStackTrace();
						}
				}
			break;
		default:
			return null;
		}
		try {
			return new ASTSearchQuery(templateText.getText(), replacementText.getText(), scope);
		} catch (final ParsingException e) {
			e.printStackTrace();
			MessageDialog.openError(this.getShell(), e.parser().bufferSequence(0).toString(), e.getMessage());
			return null;
		}
	}

	@Override
	public boolean performAction() {
		addRecent();
		NewSearchUI.runQueryInBackground(newQuery());
		return true;
	}

	@Override
	public boolean performReplace() {
		addRecent();
		final ASTSearchQuery query = newQuery();
		final IStatus status= NewSearchUI.runQueryInForeground(container.getRunnableContext(), query);
		if (status.matches(IStatus.CANCEL))
			return false;

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				final ISearchResultViewPart view = NewSearchUI.activateSearchResultView();
				if (view != null) {
					final SearchResultPage page = as(view.getActivePage(), SearchResultPage.class);
					if (page != null) {
						final SearchResult result = (SearchResult)page.getInput();
						for (final Object element : result.getElements()) {
							final Script script = as(element, Script.class);
							if (script == null)
								continue;
							final Match[] matches = result.getMatches(element);
							final List<ASTNode> replacements = new LinkedList<ASTNode>();
							for (final Match m : matches)
								if (m instanceof ASTSearchQuery.Match) {
									final ASTSearchQuery.Match qm = (ASTSearchQuery.Match) m;
									final ASTNode replacement = query.replacement();
									ASTNode repl = replacement.transform(qm.subst(), null);
									if (repl == replacement)
										repl = repl.clone();
									repl.setLocation(qm.getOffset(), qm.getOffset()+qm.getLength());
									repl.setParent(qm.matched().parent());
									replacements.add(repl);
								}
							script.saveNodes(replacements, false);
						}
					}
				}
			}
		});
		return true;
	}

	@Override
	public void setContainer(ISearchPageContainer container) { this.container = container; }
}
