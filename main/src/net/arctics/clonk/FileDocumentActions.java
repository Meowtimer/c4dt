package net.arctics.clonk;

import static java.lang.String.format;
import static java.lang.System.out;
import static net.arctics.clonk.util.Utilities.synchronizing;

import java.util.function.Function;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public final class FileDocumentActions {
	
	/** Provider used by the plug-in to provide text of documents */
	static final TextFileDocumentProvider provider = new TextFileDocumentProvider();

	/**
	 * Perform action on a document obtained from a file.
	 * @param file The file/storage object to obtain the document from
	 * @param action Action to be performed on the document
	 * @param save Whether the action is expected to mutate the document. If true, the document will be saved back to the file after performing the action.
	 * @return The result of the action
	 */
	public static <T> T performActionOnFileDocument(final IStorage file, final Function<IDocument, T> action, final boolean save) {
		final IDocument document = synchronizing(provider, () -> {
			try {
				provider.connect(file);
				return provider.getDocument(file);
			} catch (final CoreException e) {
				e.printStackTrace();
				return null;
			}
		});
		if (document == null) {
			return null;
		}
		try {
			final T result = action.apply(document);
			if (save) {
				synchronized (provider) {
					try {
						//textFileDocumentProvider.setEncoding(document, textFileDocumentProvider.getDefaultEncoding());
						provider.saveDocument(Core.NPM, file, document, true);
					} catch (final CoreException e) {
						out.println(format("Failed to save %s: %s", file.getFullPath(), e.getMessage()));
					}
				}
			}
			return result;
		} finally {
			synchronized (provider) {
				provider.disconnect(file);
			}
		}
	}
}