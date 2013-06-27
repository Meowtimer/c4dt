package net.arctics.clonk.ui.editors.ini;

import static net.arctics.clonk.util.Utilities.wordRegionAt;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.ini.Action;
import net.arctics.clonk.ini.CategoriesValue;
import net.arctics.clonk.ini.DefinitionPack;
import net.arctics.clonk.ini.FunctionEntry;
import net.arctics.clonk.ini.IDArray;
import net.arctics.clonk.ini.IconSpec;
import net.arctics.clonk.ini.IniData.IniDataBase;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.ini.IniUnitParser;
import net.arctics.clonk.ini.IniUnitWithNamedSections;
import net.arctics.clonk.ini.IntegerArray;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner.ScannerPerEngine;
import net.arctics.clonk.ui.editors.HyperlinkToResource;
import net.arctics.clonk.ui.editors.StructureEditingState;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public final class IniUnitEditingState extends StructureEditingState<IniTextEditor, IniUnit> {
	private boolean unitParsed;
	public int unitLocked;

	private final Timer timer = new Timer("Reparse Timer");
	private TimerTask reparseTask;

	@Override
	public void cleanupAfterRemoval() {
		if (timer != null)
			timer.cancel();
		super.cleanupAfterRemoval();
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		super.documentChanged(event);
		forgetUnitParsed();
		reparseTask = cancelTimerTask(reparseTask);
		timer.schedule(reparseTask = new TimerTask() {
			@Override
			public void run() {
				boolean foundClient = false;
				for (final IniTextEditor ed : editors) {
					if (!foundClient) {
						foundClient = true;
						ensureIniUnitUpToDate();
					}
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							ed.updateFoldingStructure();
						}
					});
				}
			}
		}, 700);
	}
	public void forgetUnitParsed() {
		if (unitLocked == 0)
			unitParsed = false;
	}
	public boolean ensureIniUnitUpToDate() {
		if (!unitParsed) {
			unitParsed = true;
			final String newDocumentString = document.get();
			final IniUnitParser parser = new IniUnitParser(structure);
			parser.reset(newDocumentString);
			try {
				parser.parseBuffer(false);
			} catch (final ProblemException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public void lockUnit() {
		unitLocked++;
	}

	public void unlockUnit() {
		unitLocked--;
	}

	public static Pattern NO_ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)"); //$NON-NLS-1$
	public static Pattern ASSIGN_PATTERN = Pattern.compile("\\s*([A-Za-z_0-9]*)\\s*=\\s*(.*)\\s*"); //$NON-NLS-1$
	private static final ScannerPerEngine<IniScanner> SCANNERS = new ScannerPerEngine<IniScanner>(IniScanner.class);
	private ContentAssistant assistant;
	private static class IniSourceHyperlinkPresenter extends DefaultHyperlinkPresenter {
		public IniSourceHyperlinkPresenter(IPreferenceStore store) { super(store); }
		public IniSourceHyperlinkPresenter(RGB color) { super(color); }
		@Override
		public void hideHyperlinks() { super.hideHyperlinks(); }
		@Override
		public void documentChanged(DocumentEvent event) { super.documentChanged(event); }
	}
	private class IniSourceHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
			if (!ensureIniUnitUpToDate())
				return null;
			try {
				final IRegion lineRegion = textViewer.getDocument().getLineInformationOfOffset(region.getOffset());
				final String line = textViewer.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
				Matcher m;
				final IniSection section = structure().sectionAtOffset(region.getOffset());
				if (section != null && section.definition() != null) {
					final int relativeOffset = region.getOffset()-lineRegion.getOffset();
					if ((m = ASSIGN_PATTERN.matcher(line)).matches()) {
						final boolean hoverOverAttrib = relativeOffset < m.start(2);
						final String attrib = m.group(1);
						final String value = m.group(2);
						if (!hoverOverAttrib) {
							// link stuff on the value side
							final IniDataBase dataItem = section.definition().entryForKey(attrib);
							int linkStart = lineRegion.getOffset()+m.start(2), linkLen = value.length();
							if (dataItem instanceof IniEntryDefinition) {
								final IniEntryDefinition entry = (IniEntryDefinition) dataItem;
								final Class<?> entryClass = entry.entryClass();
								Declaration declaration = null;
								if (entryClass == IDLiteral.class) {
									final IResource r = structure().file();
									final Index index = ProjectIndex.fromResource(r);
									declaration = index.definitionNearestTo(r, ID.get(value));
								}
								else if (entryClass == FunctionEntry.class) {
									final Definition obj = Definition.definitionCorrespondingToFolder(structure().file().getParent());
									if (obj != null)
										declaration = obj.findFunction(value);
								}
								else if (entryClass == IDArray.class) {
									final IRegion idRegion = wordRegionAt(line, relativeOffset);
									final IResource r = structure().file();
									final Index index = ProjectIndex.fromResource(r);
									final String id = line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength());
									if (index.engine() != null && index.engine().acceptsId(id)) {
										declaration = index.definitionNearestTo(r, ID.get(line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength())));
										linkStart = lineRegion.getOffset()+idRegion.getOffset();
										linkLen = idRegion.getLength();
									}
								}
								else if (entryClass == Action.class) {
									final IniUnitWithNamedSections iniUnit = (IniUnitWithNamedSections) structure();
									declaration = iniUnit.sectionMatching(iniUnit.nameMatcherPredicate(value));
								}
								else if (entryClass == CategoriesValue.class || entryClass == IntegerArray.class) {
									final IRegion idRegion = wordRegionAt(line, relativeOffset);
									if (idRegion.getLength() > 0) {
										declaration = structure().engine().findVariable(line.substring(idRegion.getOffset(), idRegion.getOffset()+idRegion.getLength()));
										linkStart = lineRegion.getOffset()+idRegion.getOffset();
										linkLen = idRegion.getLength();
									}
								}
								else if (entryClass == DefinitionPack.class) {
									final Index projIndex = ProjectIndex.fromResource(structure().file().getParent()).index();
									final List<Index> indexes = projIndex.relevantIndexes();
									for (final Index index : indexes)
										if (index instanceof ProjectIndex) {
											final ProjectIndex pi = (ProjectIndex) index;
											final IPath path = Path.fromPortableString(value.replaceAll("\\\\", "/"));
											final IResource res = pi.nature().getProject().findMember(path);
											if (res instanceof IContainer)
												return new IHyperlink[] {
													new HyperlinkToResource(res, new Region(linkStart, linkLen), PlatformUI.getWorkbench().getActiveWorkbenchWindow())
												};
										}
								}
								else if (entryClass == IconSpec.class) {
									final String firstPart = value.split(":")[0];
									final IResource r = structure().file();
									final Index index = ProjectIndex.fromResource(r);
									declaration = index.definitionNearestTo(r, ID.get(firstPart));
								}
								else if (entryClass == String.class) {
									final EntityRegion reg = StringTbl.entryForLanguagePref(value, 0, relativeOffset, structure(), true);
									if (reg != null) {
										declaration = reg.entityAs(Declaration.class);
										linkStart += reg.region().getOffset();
										linkLen = reg.region().getLength();
									}
								}

								if (declaration != null)
									return new IHyperlink[] {
										new ClonkHyperlink(new Region(linkStart, linkLen), declaration)
									};
							}
						}
					}
				}
				return null;
			} catch (final BadLocationException e) {
				//e.printStackTrace(); oh well, happens
				return null;
			} catch (final NullPointerException e) {
				// ignore, due to file being at unusual location
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
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public IniUnitEditingState(IPreferenceStore store) { super(store); }
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();

		final DefaultDamagerRepairer dr = new DefaultDamagerRepairer(SCANNERS.get(structure().engine()));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}
	@Override
	protected ContentAssistant createAssistant() {
		assistant = new ContentAssistant();
		final IniCompletionProcessor processor = new IniCompletionProcessor(this);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.addCompletionListener(processor);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(structure().file().getName() + " proposals"); //$NON-NLS-1$
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		assistant.enableColoredLabels(true);
		return assistant;
	}
}