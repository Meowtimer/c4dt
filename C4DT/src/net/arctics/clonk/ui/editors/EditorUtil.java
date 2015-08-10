package net.arctics.clonk.ui.editors;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.attempt;

import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import net.arctics.clonk.builder.ClonkProjectNature;

public class EditorUtil {
	public static <T extends IEditorPart> Stream<T> clonkTextEditors(final Class<T> c, final boolean restore) {

		final Function<IEditorReference, T> eclipsesJavaCompilerCannotDeal = editorReference -> {
			final FileEditorInput editorInput = attempt(
				() -> as(editorReference.getEditorInput(), FileEditorInput.class),
				PartInitException.class, Exception::printStackTrace
			);
			return editorInput != null && ClonkProjectNature.get(editorInput.getFile()) != null
				? as(editorReference.getEditor(restore), c) : null;
		};

		return stream(PlatformUI.getWorkbench().getWorkbenchWindows())
			.flatMap(w -> stream(w.getPages()))
			.flatMap(p -> stream(p.getEditorReferences()))
			.map(eclipsesJavaCompilerCannotDeal)
			.filter(x -> x != null);
	}

	public static Stream<IEditorPart> editorPartsToBeSaved() {
		return clonkTextEditors(IEditorPart.class, false).filter(IEditorPart::isDirty);
	}
}
