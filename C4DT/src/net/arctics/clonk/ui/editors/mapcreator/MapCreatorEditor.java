package net.arctics.clonk.ui.editors.mapcreator;

import net.arctics.clonk.parser.mapcreator.MapCreator;
import net.arctics.clonk.parser.mapcreator.MapCreatorLexer;
import net.arctics.clonk.parser.mapcreator.MapCreatorParser;
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


public class MapCreatorEditor extends ClonkTextEditor {
	
	private MapCreator mapCreator;
	private boolean parsed;
	
	public MapCreatorEditor() {
		super();
		ColorManager colorManager = new ColorManager();
		setSourceViewerConfiguration(new MapCreatorSourceViewerConfiguration(getPreferenceStore(), colorManager,this));
	}

	private void reparse() {
		if (!parsed) {
			String documentText = getDocumentProvider().getDocument(getEditorInput()).get();
			CharStream charStream = new ANTLRStringStream(documentText);
			MapCreatorLexer lexer = new MapCreatorLexer(charStream);
			CommonTokenStream tokenStream = new CommonTokenStream();
			tokenStream.setTokenSource(lexer);
			MapCreatorParser parser = new MapCreatorParser(mapCreator, tokenStream);
			mapCreator.clear();
			parser.parse();
			parsed = true;
			try {
				ClonkPreviewView view = (ClonkPreviewView) UI.findViewInActivePage(getSite(), ClonkPreviewView.ID);
				if (view != null) {
					IStructuredSelection sel = Utilities.as(view.getSelectionOfInterest(), IStructuredSelection.class);
					IFile file = Utilities.fileEditedBy(this);
					if (
							mapCreator != null && mapCreator.engine() != null && mapCreator.engine().settings().supportsEmbeddedUtilities &&
							sel != null && sel.getFirstElement().equals(file)
					) {
						view.schedulePreviewUpdaterJob();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public MapCreator mapCreator() {
		if (mapCreator == null)
			mapCreator = new MapCreator(Utilities.fileEditedBy(this));
		return mapCreator;
		
	}
	
	@Override
	public MapCreator topLevelDeclaration() {
		MapCreator result = mapCreator();
		reparse();
		return result;
	}
	
	public void silentReparse() {
		IFile file = mapCreator().file();
		mapCreator().setFile(null);
		try {
			reparse();
		}
		finally {
			mapCreator().setFile(file);
		}
	}
	
	@Override
	public void refreshOutline() {
		if (outlinePage != null) {
			outlinePage.setInput(topLevelDeclaration());
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
