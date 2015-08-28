package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.AppendableBackedNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.ui.editors.DeclarationProposal;
import net.arctics.clonk.ui.editors.ProposalsSite;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.util.UI;

public final class ParameterizedProposal extends DeclarationProposal {
	private final Replacement replacement;
	private final int tabIndentation;
	private final ScriptParser parser;
	private final Function func;

	ParameterizedProposal(final Declaration declaration,
		final String replacementString, final int replacementOffset,
		final int replacementLength, final int cursorPosition, final Image image,
		final String displayString, final IContextInformation contextInformation,
		final String additionalProposalInfo, final String postInfo,
		final ProposalsSite site,
		final Replacement replacement, final int tabIndentation,
		final ScriptParser parser, final Function func
	) {
		super(declaration, declaration, replacementString, replacementOffset,
			replacementLength, cursorPosition, image, displayString,
			contextInformation, additionalProposalInfo, postInfo,
			site);
		this.replacement = replacement;
		this.tabIndentation = tabIndentation;
		this.parser = parser;
		this.func = func;
	}

	public boolean createdFrom(final Replacement other) {
		return this.replacement.equals(other);
	}

	@Override
	public void apply(final ITextViewer viewer, final char trigger, final int stateMask, final int offset) {
		this.apply(viewer.getDocument());
	}

	@Override
	public void apply(final IDocument document) {
		replacement.performAdditionalActionsBeforeDoingReplacements();
		final ASTNode replacementExpr = replacement.replacementExpression();
		if (replacementExpr != ASTNode.NULL_EXPR) {
			for (final ASTNode spec : replacement.specifiable())
				if (spec instanceof AccessDeclaration) {
					final AccessDeclaration accessDec = (AccessDeclaration) spec;
					final String s = UI.input(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						Messages.ClonkQuickAssistProcessor_SpecifyValue,
						String.format(Messages.ClonkQuickAssistProcessor_SpecifyFormat, accessDec.name()), accessDec.name(),
						newText -> {
							if (!ScriptQuickAssistProcessor.validIdentifierPattern.matcher(newText).matches())
								return String.format(Messages.ClonkQuickAssistProcessor_NotAValidFunctionName, newText);
							else
								return null;
						}
					);
					if (s != null)
						accessDec.setName(s);
				}
			try {
				final Tidy tidy = new Tidy(parser.script(), parser.script().strictLevel());
				this.replacementString = tidy.tidy(replacement.replacementExpression()).printed(tabIndentation+1);
			} catch (final CloneNotSupportedException e) {
				e.printStackTrace();
			}
		} else {
			// don't replace expression
			replacementString = ""; //$NON-NLS-1$
			replacementLength = 0;
		}
		cursorPosition = replacementString.length();
		super.apply(document);

		for (final Declaration dec : replacement.additionalDeclarations()) {
			final StringBuilder builder = new StringBuilder(50);
			dec.print(new AppendableBackedNodePrinter(builder), 0);
			builder.append("\n"); //$NON-NLS-1$
			builder.append("\n"); //$NON-NLS-1$
			try {
				document.replace(func.header().getOffset(), 0, builder.toString());
			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
		}

		final ScriptEditingState listener = StructureEditingState.existing(ScriptEditingState.class, parser.script());
		if (listener != null)
			listener.scheduleReparsing(false);
	}

	public void runOnMarker(final IMarker marker) {
		Core.instance().performActionsOnFileDocument((IFile)marker.getResource(), document -> {
			replacementOffset = marker.getAttribute(IMarker.CHAR_START, replacementOffset);
			replacementLength = marker.getAttribute(IMarker.CHAR_END, replacementOffset+replacementLength)-replacementOffset;
			apply(document);
			return null;
		}, true);
	}

	public Replacement replacement() { return replacement; }
	@Override
	public boolean requiresDocumentReparse() { return replacement.additionalDeclarations().size() > 0; }
}

