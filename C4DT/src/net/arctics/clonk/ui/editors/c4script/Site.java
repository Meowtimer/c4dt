package net.arctics.clonk.ui.editors.c4script;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.FunctionFragmentParser;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Markers;

class Site {
	final IDocument document;
	final Script script;
	final IMarker marker;
	final Problem problem;
	final IRegion expressionRegion0;
	final Function func;
	final int tabIndentation;
	final FunctionFragmentParser parser;
	final ASTNode offendingExpression;
	final Statement topLevel;
	final IRegion expressionRegion;
	final ReplacementsList replacements;
	Replacement addRemoveReplacement() {
		final Replacement result = replacements.add(
			Messages.ClonkQuickAssistProcessor_Remove,
			new ReplacementStatement("", expressionRegion, document, expressionRegion.getOffset(), func.bodyLocation().getOffset()), //$NON-NLS-1$
			false, true
		);
		return result;
	}
	Site(final List<ICompletionProposal> proposals, final IDocument document, final Script script, final Position position, final IMarker marker) {
		this.document = document;
		this.script = script;
		this.marker = marker;
		this.problem = Markers.problem(marker);
		this.expressionRegion0 = new SourceLocation(marker.getAttribute(IMarker.CHAR_START, 0),  marker.getAttribute(IMarker.CHAR_END, 0));
		if (expressionRegion0.getOffset() == -1 || script == null || document == null) {
			throw new IllegalStateException();
		}
		this.func = script.funcAt(position.getOffset());
		this.tabIndentation = BufferedScanner.indentationOfStringAtPos(document.get(),
			func.bodyLocation().getOffset()+expressionRegion0.getOffset(), BufferedScanner.TABINDENTATIONMODE);
		final ExpressionLocator<Site> locator = new ExpressionLocator<Site>(position.getOffset()-func.bodyLocation().start());
		this.func.traverse(locator, this);
		this.parser = new FunctionFragmentParser(document, script, func, null);
		this.offendingExpression = locator.expressionAtRegion();
		this.topLevel = offendingExpression != null ? offendingExpression.parent(Statement.class) : null;
		this.expressionRegion = offendingExpression != null && topLevel != null ? offendingExpression.absolute() : expressionRegion0;
		if (offendingExpression == null || topLevel == null) {
			throw new IllegalStateException();
		}
		this.replacements = new ReplacementsList(offendingExpression, proposals);
	}
	void commitReplacements() {
		replacements.stream().map(replacement -> {
			final String replacementAsString = "later"; //$NON-NLS-1$
			final IRegion r = replacement.regionToBeReplacedSpecifiedByReplacementExpression
				? new Region(
					func.bodyLocation().getOffset() + replacement.replacementExpression().start(),
					replacement.replacementExpression().getLength()
				)
				: new Region(
					expressionRegion.getOffset(),
					replacement.replacementExpression() instanceof Statement
						? topLevel.getLength()
						: expressionRegion.getLength()
				);
			return new ParameterizedProposal(
				null, replacementAsString, r.getOffset(), r.getLength(),
				replacementAsString.length(), null, replacement.title(), null, null, null, null,
				replacement, tabIndentation, parser, func
			);
		}).forEach(replacement -> replacements.existingList.add(replacement));
	}
}