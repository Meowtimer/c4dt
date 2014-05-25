package net.arctics.clonk.ui.editors;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.tri;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.arctics.clonk.builder.ClonkProjectNature;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class EditorUtil {
	public static <T extends IEditorPart> Stream<T> clonkTextEditors(final Class<T> c, final boolean restore) {
		return stream(PlatformUI.getWorkbench().getWorkbenchWindows())
			.flatMap(w -> stream(w.getPages()))
			.flatMap(p -> stream(p.getEditorReferences()))
			.map(e -> {
				final FileEditorInput i = tri(() -> as(e.getEditorInput(), FileEditorInput.class), PartInitException.class, Exception::printStackTrace);
				return i != null && ClonkProjectNature.get(i.getFile()) != null ? as(e.getEditor(restore), c) : null;
			})
			.filter(x -> x != null);
	}

	public static Stream<IEditorPart> editorPartsToBeSaved() {
		return clonkTextEditors(IEditorPart.class, false).filter(IEditorPart::isDirty);
	}
}
