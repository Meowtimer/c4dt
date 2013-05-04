package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.util.Collections;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.c4script.C4ScriptParser;
import net.arctics.clonk.c4script.typing.TypeAnnotation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;

final class DynamicTypingMigrationJob extends TypingMigrationJob {
	private final C4ScriptParser[] parsers;
	private final ProjectSettings settings;

	DynamicTypingMigrationJob(ClonkProjectNature nature, String name, C4ScriptParser[] parsers, ProjectSettings settings) {
		super(name, nature);
		this.parsers = parsers;
		this.settings = settings;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Dynamic Typing Migration", parsers.length);
		runWithoutAutoBuild(new Runnable() { @Override public void run() {
			for (final C4ScriptParser parser : parsers) {
				if (parser != null && parser.script() != null && parser.script().scriptFile() != null)
					removeTypeAnnotations(parser);
				monitor.worked(1);
			}
		}});
		settings.concludeTypingMigration();
		nature.saveSettings();
		return Status.OK_STATUS;
	}

	private void removeTypeAnnotations(final C4ScriptParser parser) {
		if (parser.typeAnnotations() == null)
			return;
		Core.instance().performActionsOnFileDocument(parser.script().scriptFile(), new IDocumentAction<Object>() {
			@Override
			public Object run(IDocument document) {
				final StringBuilder builder = new StringBuilder(document.get());
				final List<TypeAnnotation> annotations = parser.typeAnnotations();
				Collections.sort(annotations);
				for (int i = annotations.size()-1; i >= 0; i--) {
					final TypeAnnotation annot = annotations.get(i);
					int end = annot.end();
					if (end < builder.length() && Character.isWhitespace(builder.charAt(end)))
						end++;
					builder.delete(annot.start(), end);
				}
				document.set(builder.toString());
				return null;
			}
		}, true);
	}
}