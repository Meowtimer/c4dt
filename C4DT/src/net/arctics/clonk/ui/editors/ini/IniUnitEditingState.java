package net.arctics.clonk.ui.editors.ini;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.ui.editors.StructureEditingState;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;

public final class IniUnitEditingState extends StructureEditingState<IniTextEditor, IniUnit> {

	private static final List<IniUnitEditingState> list = new ArrayList<>();

	private boolean unitParsed;
	public int unitLocked;

	private final Timer reparseTimer = new Timer("Reparse Timer");
	private TimerTask reparseTask;

	public IniUnitEditingState() { super(); }

	@Override
	public void documentChanged(DocumentEvent event) {
		super.documentChanged(event);
		forgetUnitParsed();
		reparseTask = cancelTimerTask(reparseTask);
		reparseTimer.schedule(reparseTask = new TimerTask() {
			@Override
			public void run() {
				boolean foundClient = false;
				for (final IniTextEditor ed : editors) {
					if (!foundClient) {
						foundClient = true;
						ensureIniUnitUpToDate(ed);
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
	public static IniUnitEditingState addTo(IDocument document, IniUnit unit, IniTextEditor client)  {
		try {
			return addTo(list, IniUnitEditingState.class, document, unit, client);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public void forgetUnitParsed() {
		if (unitLocked == 0)
			unitParsed = false;
	}
	public boolean ensureIniUnitUpToDate(IniTextEditor editor) {
		if (!unitParsed) {
			unitParsed = true;
			final String newDocumentString = editor != null ? editor.getDocumentProvider().getDocument(editor.getEditorInput()).get() : document.get();
			structure.parser().reset(newDocumentString);
			try {
				structure.parser().parse(false, false);
			} catch (final ProblemException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}