package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.eq;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.util.Collections;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.c4script.C4ScriptParser;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;

final class StaticTypingMigrationJob extends TypingMigrationJob {
	private final ProjectSettings settings;
	private final C4ScriptParser[] parsers;

	StaticTypingMigrationJob(String name, ClonkProjectNature nature, ProjectSettings settings, C4ScriptParser[] parsers) {
		super(name, nature);
		this.settings = settings;
		this.parsers = parsers;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Static Typing Migration", parsers.length);
		runWithoutAutoBuild(new Runnable() { @Override public void run() {
			for (final C4ScriptParser parser : parsers) {
				if (parser != null && parser.script() != null && parser.script().scriptFile() != null)
					insertTypeAnnotations(parser);
				monitor.worked(1);
			}
		}});
		settings.concludeTypingMigration();
		nature.saveSettings();
		return Status.OK_STATUS;
	}

	private void insertTypeAnnotations(final C4ScriptParser parser) {
		Core.instance().performActionsOnFileDocument(parser.script().scriptFile(), new IDocumentAction<Object>() {
			@Override
			public Object run(IDocument document) {
				final StringBuilder builder = new StringBuilder(document.get());
				final List<TypeAnnotation> annotations = parser.typeAnnotations();
				Collections.sort(annotations);
				for (int i = annotations.size()-1; i >= 0; i--) {
					final TypeAnnotation annot = annotations.get(i);
					if (annot.type() == null && annot.target() != null) {
						/*System.out.println(String.format(
							"typeable: %s type: %s enviro: %s",
							annot.typeable().name(),
							annot.typeable().type().typeName(false),
							builder.substring(annot.start()-7, annot.end()+7)
						));*/
						builder.delete(annot.start(), annot.end());
						builder.insert(annot.start(), " ");
						IType type = annot.target().type();
						if (eq(type, PrimitiveType.UNKNOWN))
							type = PrimitiveType.ANY;
						builder.insert(annot.start(), type.typeName(false));
					}
				}
				document.set(builder.toString());
				return null;
			}
		}, true);
	}
}