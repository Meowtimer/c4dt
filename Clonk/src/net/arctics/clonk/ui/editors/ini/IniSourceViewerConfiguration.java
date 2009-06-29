package net.arctics.clonk.ui.editors.ini;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.inireader.Action;
import net.arctics.clonk.parser.inireader.Function;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.IClonkColorConstants;
import net.arctics.clonk.util.Predicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.RGB;

public class IniSourceViewerConfiguration extends ClonkSourceViewerConfiguration<IniTextEditor> {
	
	public static Pattern NO_ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)");
	public static Pattern ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)\\s*=\\s*(.*)\\s*");
	
	private class IniSourceHyperlinkPresenter extends DefaultHyperlinkPresenter {

		public IniSourceHyperlinkPresenter(IPreferenceStore store) {
			super(store);
		}
		
		public IniSourceHyperlinkPresenter(RGB color) {
			super(color);
		}
		
		@Override
		public void hideHyperlinks() {
			//getEditor().forgetUnitParsed();
			super.hideHyperlinks();
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			super.documentChanged(event);
		}
		
	}
	
	private class IniSourceHyperlinkDetector implements IHyperlinkDetector {
		
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			if (!getEditor().ensureIniUnitUpToDate())
				return null;
			try {
				IRegion lineRegion = textViewer.getDocument().getLineInformationOfOffset(region.getOffset());
				String line = textViewer.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
				Matcher m;
				IniSection section = getEditor().getIniUnit().sectionAtOffset(region.getOffset(), 0);
				if (section != null) {
					int relativeOffset = region.getOffset()-lineRegion.getOffset();
					if ((m = ASSIGN_PATTERN.matcher(line)).matches()) {
						boolean hoverOverAttrib = relativeOffset < m.start(2);
						String attrib = m.group(1);
						final String value = m.group(2);
						if (!hoverOverAttrib) {
							// link stuff on the value side
							IniDataEntry entry = section.getSectionData().getEntry(attrib);
							int linkStart = lineRegion.getOffset()+m.start(2), linkLen = value.length();
							if (entry != null) {
								Class<?> entryClass = entry.getEntryClass();
								C4Declaration declaration = null;
								if (entryClass == C4ID.class) {
									IResource r = Utilities.getEditingFile(getEditor());
									ClonkIndex index = Utilities.getIndex(r);
									declaration = index.getObjectNearestTo(r, C4ID.getID(value));
								}
								else if (entryClass == Function.class) {
									C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(Utilities.getEditingFile(getEditor()).getParent());
									if (obj != null) {
										declaration = obj.findFunction(value);
									}
								}
								else if (entryClass == IDArray.class) {
									IRegion idRegion = Utilities.wordRegionAt(line, relativeOffset);
									if (idRegion.getLength() == 4) {
										IResource r = Utilities.getEditingFile(getEditor());
										ClonkIndex index = Utilities.getIndex(r);
										declaration = index.getObjectNearestTo(r, C4ID.getID(line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength())));
										linkStart = lineRegion.getOffset()+idRegion.getOffset();
										linkLen = idRegion.getLength();
									}
								}
								else if (entryClass == Action.class) {
									declaration = getEditor().getIniUnit().sectionMatching(new Predicate<IniSection>() {
										public boolean test(IniSection object) {
											IniEntry entry = object.getEntry("Name");
											return (entry != null && entry.getValue().equals(value));
										}
									});
								}
								if (declaration != null) {
									return new IHyperlink[] {
										new ClonkHyperlink(new Region(linkStart, linkLen), declaration)
									};
								}
							}
						}
					}
				}
				return null;
			} catch (BadLocationException e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}
	
	private IniScanner scanner;
	
	@Override
	public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
		if (fPreferenceStore == null)
			return new IniSourceHyperlinkPresenter(new RGB(0, 0, 255));
		return new IniSourceHyperlinkPresenter(fPreferenceStore);
	}
	
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		try {
			return new IHyperlinkDetector[] {
				new IniSourceHyperlinkDetector()
			};
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public IniSourceViewerConfiguration(ColorManager colorManager, IniTextEditor textEditor) {
		super(colorManager, textEditor);
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(
			ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getDefCoreScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		return reconciler;
	}
	
	protected IniScanner getDefCoreScanner() {
		if (scanner == null) {
			scanner = new IniScanner(getColorManager());
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						getColorManager().getColor(IClonkColorConstants.DEFAULT))));
		}
		return scanner;
	}
	
	private ContentAssistant assistant;
	
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (assistant != null)
			return assistant;
		
		assistant = new ContentAssistant();
		IniCompletionProcessor processor = new IniCompletionProcessor(getEditor(), assistant);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.addCompletionListener(processor);
		assistant.install(sourceViewer);
		
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(Utilities.getEditingFile(getEditor()).getName() + " proposals");
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		return assistant;
	}
	
}
