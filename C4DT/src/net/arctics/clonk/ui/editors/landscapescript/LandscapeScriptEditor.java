package net.arctics.clonk.ui.editors.landscapescript;

import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.landscapescript.LandscapeScriptLexer;
import net.arctics.clonk.landscapescript.LandscapeScriptParser;
import net.arctics.clonk.ui.editors.ClonkContentOutlinePage;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.navigator.ClonkPreviewView;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;


public class LandscapeScriptEditor extends ClonkTextEditor {
	
	private LandscapeScript script;
	private boolean parsed;
	
	public LandscapeScriptEditor() {
		super();
		ColorManager colorManager = new ColorManager();
		setSourceViewerConfiguration(new LandscapeScriptSourceViewerConfiguration(getPreferenceStore(), colorManager,this));
	}

	private void reparse() {
		if (!parsed) {
			String documentText = getDocumentProvider().getDocument(getEditorInput()).get();
			CharStream charStream = new ANTLRStringStream(documentText);
			LandscapeScriptLexer lexer = new LandscapeScriptLexer(charStream);
			CommonTokenStream tokenStream = new CommonTokenStream();
			tokenStream.setTokenSource(lexer);
			LandscapeScriptParser parser = new LandscapeScriptParser(script, tokenStream);
			script.clear();
			parser.parse();
			parsed = true;
			try {
				ClonkPreviewView view = (ClonkPreviewView) UI.findViewInActivePage(getSite(), ClonkPreviewView.ID);
				if (view != null) {
					IStructuredSelection sel = Utilities.as(view.getSelectionOfInterest(), IStructuredSelection.class);
					IFile file = Utilities.fileEditedBy(this);
					if (
							script != null && script.engine() != null && script.engine().settings().supportsEmbeddedUtilities &&
							sel != null && sel.getFirstElement().equals(file)
					)
						view.schedulePreviewUpdaterJob();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public LandscapeScript script() {
		if (script == null)
			script = new LandscapeScript(Utilities.fileEditedBy(this));
		return script;
		
	}
	
	@Override
	public LandscapeScript structure() {
		LandscapeScript result = script();
		reparse();
		return result;
	}
	
	public void silentReparse() {
		IFile file = script().file();
		script().setFile(null);
		try {
			reparse();
		}
		finally {
			script().setFile(file);
		}
	}
	
	@Override
	public void refreshOutline() {
		if (outlinePage != null) {
			outlinePage.setInput(structure());
			super.refreshOutline();
		}
	}
	
	@Override
	public ClonkContentOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new ClonkContentOutlinePage();
			outlinePage.setEditor(this);
		}
		return super.getOutlinePage();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		getDocumentProvider().getDocument(getEditorInput()).addDocumentListener(new IDocumentListener() {

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				parsed = false;
			}
			
		});
	}
	
}
