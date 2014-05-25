package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.arctics.clonk.builder.ClonkProjectNature;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class EditorUtil {
	public static Iterable<IEditorPart> clonkTextEditors(final boolean restore) {
		return clonkTextEditors(IEditorPart.class, restore);
	}
	@SuppressWarnings("unchecked")
	public static <T extends IEditorPart> Iterable<T> clonkTextEditors(final Class<T> c, final boolean restore) {
		final List<T> editors = new ArrayList<T>();
		for (final IWorkbenchWindow w : PlatformUI.getWorkbench().getWorkbenchWindows())
			for (final IWorkbenchPage p : w.getPages())
				for (final IEditorReference e : p.getEditorReferences()) {
					FileEditorInput input;
					try {
						input = as(e.getEditorInput(), FileEditorInput.class);
					} catch (final PartInitException e1) {
						e1.printStackTrace();
						continue;
					}
					if (input != null && ClonkProjectNature.get(input.getFile()) != null) {
						final IEditorPart part = e.getEditor(restore);
						if (c.isInstance(part))
							editors.add((T)part);
					}
				}
		return editors;
	}
	public static Stream<IEditorPart> editorPartsToBeSaved() {
		return StreamSupport.stream(clonkTextEditors(false).spliterator(), false).filter(item -> item.isDirty());
	}
}
