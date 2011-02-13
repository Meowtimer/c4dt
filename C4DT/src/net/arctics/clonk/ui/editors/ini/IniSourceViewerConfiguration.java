package net.arctics.clonk.ui.editors.ini;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.inireader.Action;
import net.arctics.clonk.parser.inireader.CategoriesArray;
import net.arctics.clonk.parser.inireader.DefinitionPack;
import net.arctics.clonk.parser.inireader.FuncRefEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IconSpec;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ClonkColorConstants;
import net.arctics.clonk.ui.editors.HyperlinkToResource;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.ui.PlatformUI;

public class IniSourceViewerConfiguration extends ClonkSourceViewerConfiguration<IniTextEditor> {
	
	public static Pattern NO_ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)"); //$NON-NLS-1$
	public static Pattern ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)\\s*=\\s*(.*)\\s*"); //$NON-NLS-1$
	
	private IniScanner scanner;
	
	private static class IniSourceHyperlinkPresenter extends DefaultHyperlinkPresenter {

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
		
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
			if (!getEditor().ensureIniUnitUpToDate())
				return null;
			try {
				IRegion lineRegion = textViewer.getDocument().getLineInformationOfOffset(region.getOffset());
				String line = textViewer.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
				Matcher m;
				IniSection section = getEditor().getIniUnit().sectionAtOffset(region.getOffset());
				if (section != null && section.getSectionData() != null) {
					int relativeOffset = region.getOffset()-lineRegion.getOffset();
					if ((m = ASSIGN_PATTERN.matcher(line)).matches()) {
						boolean hoverOverAttrib = relativeOffset < m.start(2);
						String attrib = m.group(1);
						final String value = m.group(2);
						if (!hoverOverAttrib) {
							// link stuff on the value side
							IniDataBase dataItem = section.getSectionData().getEntry(attrib);
							int linkStart = lineRegion.getOffset()+m.start(2), linkLen = value.length();
							if (dataItem instanceof IniDataEntry) {
								IniDataEntry entry = (IniDataEntry) dataItem;
								Class<?> entryClass = entry.getEntryClass();
								Declaration declaration = null;
								if (entryClass == ID.class) {
									IResource r = Utilities.getEditingFile(getEditor());
									ClonkIndex index = Utilities.getIndex(r);
									declaration = index.getObjectNearestTo(r, ID.getID(value));
								}
								else if (entryClass == FuncRefEntry.class) {
									ProjectDefinition obj = ProjectDefinition.objectCorrespondingTo(Utilities.getEditingFile(getEditor()).getParent());
									if (obj != null) {
										declaration = obj.findFunction(value);
									}
								}
								else if (entryClass == IDArray.class) {
									IRegion idRegion = Utilities.wordRegionAt(line, relativeOffset);
									IResource r = Utilities.getEditingFile(getEditor());
									ClonkIndex index = Utilities.getIndex(r);
									String id = line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength());
									if (index.getEngine() != null && index.getEngine().acceptsId(id)) {
										declaration = index.getObjectNearestTo(r, ID.getID(line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength())));
										linkStart = lineRegion.getOffset()+idRegion.getOffset();
										linkLen = idRegion.getLength();
									}
								}
								else if (entryClass == Action.class) {
									declaration = getEditor().getIniUnit().sectionMatching(new IPredicate<IniSection>() {
										public boolean test(IniSection object) {
											IniItem entry = object.getSubItem("Name"); //$NON-NLS-1$
											return (entry instanceof IniEntry && ((IniEntry)entry).getValue().equals(value));
										}
									});
								}
								else if (entryClass == CategoriesArray.class || entryClass == IntegerArray.class) {
									IRegion idRegion = Utilities.wordRegionAt(line, relativeOffset);
									if (idRegion.getLength() > 0) {
										declaration = getEditor().getIniUnit().getEngine().findVariable(line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength()));
										linkStart = lineRegion.getOffset()+idRegion.getOffset();
										linkLen = idRegion.getLength();
									}
								}
								else if (entryClass == DefinitionPack.class) {
									ClonkIndex projIndex = ProjectDefinition.objectCorrespondingTo(Utilities.getEditingFile(getEditor()).getParent()).getIndex();
									List<ClonkIndex> indexes = projIndex.relevantIndexes();
									for (ClonkIndex index : indexes) {
										if (index instanceof ProjectIndex) {
											ProjectIndex pi = (ProjectIndex) index;
											try {
												for (IResource res : pi.getProject().members()) {
													if (res instanceof IContainer && C4Group.getGroupType(res.getName()) == C4GroupType.DefinitionGroup) {
														if (res.getName().equals(value)) {
															return new IHyperlink[] {
																new HyperlinkToResource(res, new Region(linkStart, linkLen), PlatformUI.getWorkbench().getActiveWorkbenchWindow())
															};
														}
													}
												}
											} catch (CoreException e) {
												e.printStackTrace();
											}	
										}
									}
								}
								else if (entryClass == IconSpec.class) {
									String firstPart = value.split(":")[0];
									IResource r = Utilities.getEditingFile(getEditor());
									ClonkIndex index = Utilities.getIndex(r);
									declaration = index.getObjectNearestTo(r, ID.getID(firstPart));
								}
								else if (entryClass == String.class) {
									DeclarationRegion reg = StringTbl.getEntryForLanguagePref(value, 0, relativeOffset, getEditor().getIniUnit(), true);
									if (reg != null) {
										declaration = reg.getDeclaration();
										linkStart += reg.getRegion().getOffset();
										linkLen = reg.getRegion().getLength();
									}
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
				//e.printStackTrace(); oh well
				return null;
			}
		}
		
	}
	
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
	
	public IniSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, IniTextEditor textEditor) {
		super(store, colorManager, textEditor);
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getDefCoreScanner(getEditor().getIniUnit().getEngine()));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		return reconciler;
	}
	
	protected IniScanner getDefCoreScanner(Engine engine) {
		if (scanner == null) {
			scanner = new IniScanner(getColorManager(), engine);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						getColorManager().getColor(ClonkColorConstants.getColor("DEFAULT"))))); //$NON-NLS-1$
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
		assistant.setStatusMessage(Utilities.getEditingFile(getEditor()).getName() + " proposals"); //$NON-NLS-1$
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		return assistant;
	}
	
}
