package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.filter;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.util.IPredicate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class EditorUtil {
	public static Iterable<IEditorPart> clonkTextEditors(boolean restore) {
		return clonkTextEditors(IEditorPart.class, restore);
	}
	@SuppressWarnings("unchecked")
	public static <T extends IEditorPart> Iterable<T> clonkTextEditors(Class<T> c, boolean restore) {
		List<T> editors = new ArrayList<T>();
		for (IWorkbenchWindow w : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage p : w.getPages()) {
				for (IEditorReference e : p.getEditorReferences()) {
					FileEditorInput input;
					try {
						input = as(e.getEditorInput(), FileEditorInput.class);
					} catch (PartInitException e1) {
						e1.printStackTrace();
						continue;
					}
					if (input != null && ClonkProjectNature.get(input.getFile()) != null) {
						IEditorPart part = e.getEditor(restore);
						if (c.isInstance(part)) {
							editors.add((T)part);
						}
					}
				}
			}
		}
		return editors;
	}
	public static Iterable<IEditorPart> editorPartsToBeSaved() {
		return filter(clonkTextEditors(false), new IPredicate<IEditorPart>() {
			@Override
			public boolean test(IEditorPart item) {
				return item.isDirty();
			}
		});
	}
}
