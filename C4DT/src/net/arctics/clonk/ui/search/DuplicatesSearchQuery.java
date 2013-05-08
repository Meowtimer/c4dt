package net.arctics.clonk.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.ast.ASTComparisonDelegate;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.InitializationFunction;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.FunctionDescription;
import net.arctics.clonk.c4script.ast.Parenthesized;
import net.arctics.clonk.c4script.ast.ReturnStatement;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.preferences.ClonkPreferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;

/**
 * Query to find potential duplicates of functions.
 * @author madeen
 *
 */
public class DuplicatesSearchQuery extends SearchQuery {

	private final Map<String, List<Function>> functionsToBeChecked = new HashMap<String, List<Function>>();
	private final Set<Index> indexes = new HashSet<Index>();
	private final Map<Function, List<FindDuplicatesMatch>> detectedDupes = new HashMap<Function, List<FindDuplicatesMatch>>();
	private final ASTComparisonDelegate comparisonDelegate = new ASTComparisonDelegate(null) {
		private boolean irrelevant(ASTNode leftNode) {
			return leftNode instanceof Comment || leftNode instanceof FunctionDescription;
		}
		@Override
		public boolean ignoreLeftSubElement(ASTNode leftNode) { return irrelevant(leftNode); }
		@Override
		public boolean ignoreRightSubElement(ASTNode rightNode) { return irrelevant(rightNode); }
		@Override
		public boolean ignoreSubElementDifference(ASTNode left, ASTNode right) {
			if (left == null || right == null)
				return false;
			if (left instanceof Parenthesized)
				return ((Parenthesized)left).innerExpression().compare(right, this);
			if (right instanceof Parenthesized)
				return left.compare(((Parenthesized)right).innerExpression(), this);

			// ignore differing variable names if both variables are parameters at the same index in their respective functions
			if (left instanceof AccessVar && right instanceof AccessVar) {
				final AccessVar varA = (AccessVar)left;
				final AccessVar varB = (AccessVar)right;
				if (varA.declaration() instanceof Variable && varB.declaration() instanceof Variable) {
					final int parmA = ((Variable)varA.declaration()).parameterIndex();
					final int parmB = ((Variable)varB.declaration()).parameterIndex();
					if (parmA != -1 && parmA == parmB)
						return true;
				}
			}

			// ignore order of operands in binary associative operator expression
			if (left.parent() instanceof BinaryOp && right.parent() instanceof BinaryOp) {
				final BinaryOp opA = (BinaryOp) left.parent();
				final BinaryOp opB = (BinaryOp) right.parent();
				if (opA.operator() == opB.operator() && opA.operator().isAssociative())
					if (
						right == opB.leftSide() || right == opB.rightSide() &&
						left == opA.leftSide() || left == opA.rightSide()
					) {
						final ASTNode bCounterpart = opB.leftSide() == right ? opB.rightSide() : opB.leftSide();
						final ASTNode aCounterpart = opA.leftSide() == left ? opA.rightSide() : opA.leftSide();
						final ASTComparisonDelegate moi = this;
						final ASTComparisonDelegate proxy = new ASTComparisonDelegate(right) {
							@Override
							public boolean ignoreSubElementDifference(ASTNode left, ASTNode right) {
								if (left == aCounterpart || left == bCounterpart)
									return false;
								return moi.ignoreSubElementDifference(left, right);
							}
						};
						if (aCounterpart.compare(right, proxy) && left.compare(bCounterpart, proxy))
							return true;
					}
			}

			return false;
		}
	};

	public Map<Function, List<FindDuplicatesMatch>> getDetectedDupes() {
		return detectedDupes;
	}

	private DuplicatesSearchQuery() {}

	/**
	 * Return a new FindDuplicatesQuery that will operate on a list of functions.
	 * @param functions The function list
	 * @return The new query
	 */
	public static DuplicatesSearchQuery queryWithFunctions(List<Function> functions) {
		final DuplicatesSearchQuery result = new DuplicatesSearchQuery();
		result.fillFunctionMapWithFunctionList(functions);
		for (final List<Function> fnList : result.functionsToBeChecked.values())
			for (final Function f : fnList)
				for (final Index i : f.index().relevantIndexes())
					result.indexes.add(i);
		return result;
	}

	/**
	 * Return a new FindDuplicatesQuery that will operate on all functions contained in the passed scripts
	 * @param scripts The script list
	 * @return The new query
	 */
	public static DuplicatesSearchQuery queryWithScripts(Iterable<Script> scripts) {
		final DuplicatesSearchQuery result = new DuplicatesSearchQuery();
		final List<Function> fns = new LinkedList<Function>();
		for (final Script script : scripts) {
			fns.addAll(script.functions());
			result.indexes.add(script.index());
		}
		result.fillFunctionMapWithFunctionList(fns);
		return result;
	}

	private void fillFunctionMapWithFunctionList(List<Function> functions) {
		for (final Function f : functions) {
			if (f.body() == null || f instanceof InitializationFunction)
				continue;
			List<Function> list = functionsToBeChecked.get(f.name());
			if (list == null) {
				list = new LinkedList<Function>();
				functionsToBeChecked.put(f.name(), list);
			}
			list.add(f);
		}
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) throws OperationCanceledException {
		final boolean ignoreSimpleFunctions = ClonkPreferences.toggle(ClonkPreferences.IGNORE_SIMPLE_FUNCTION_DUPES, false);

		detectedDupes.clear();
		final Set<Index> indexes = new HashSet<Index>();
		final Set<Function> deemedDuplicate = new HashSet<Function>();
		for (final List<Function> fnList : functionsToBeChecked.values())
			for (final Function f : fnList)
				for (final Index i : f.index().relevantIndexes())
					indexes.add(i);
		for (final Map.Entry<String, List<Function>> entry : functionsToBeChecked.entrySet())
			for (final Function function : entry.getValue()) {
				for (final Index index : indexes)
					index.loadScriptsContainingDeclarationsNamed(function.name());
				if (deemedDuplicate.contains(function))
					continue;
				final Block functionCodeBlock = function.body();
				// ignore simple return functions
				if (ignoreSimpleFunctions)
					if (functionCodeBlock == null || functionCodeBlock.statements().length == 1 && functionCodeBlock.statements()[0] instanceof ReturnStatement)
						continue;
				for (final Index index : indexes) {
					final List<Declaration> decs = index.snapshotOfDeclarationsNamed(function.name());
					if (decs == null)
						continue;
					if (!decs.contains(function)) // happens when a newly-parsed function is not already added to the declaration map
						continue;
					for (final Declaration d : decs)
						if (d instanceof Function) {
							final Function otherFn = (Function) d;
							if (deemedDuplicate.contains(d))
								continue;
							if (function == otherFn)
								continue;
							final Block block = otherFn.body();
							if (block == null)
								continue; // -.-
							if (functionCodeBlock.compare(block, comparisonDelegate)) {
								List<FindDuplicatesMatch> dupes = detectedDupes.get(function);
								if (dupes == null) {
									dupes = new LinkedList<FindDuplicatesMatch>();
									detectedDupes.put(function, dupes);
								}
								final FindDuplicatesMatch match = new FindDuplicatesMatch(otherFn.script(), otherFn.start(), otherFn.getLength(), function, otherFn);
								dupes.add(match);
								result.addMatch(match);
								deemedDuplicate.add(otherFn);
							}
						}
				}
			}
		return Status.OK_STATUS;
	}

	@Override
	public String getLabel() {
		final StringBuilder builder = new StringBuilder(30);
		int size = 3;
		for (final String f : functionsToBeChecked.keySet()) {
			if (builder.length() > 0)
				builder.append(", ");
			if (size-- == 0) {
				builder.append("...");
				break;
			} else
				builder.append(f);
		}
		return String.format(Messages.FindDuplicatesQuery_Label, builder.toString());
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		return NO_MATCHES;
	}

	@Override
	public IFile getFile(Object element) {
		return null;
	}

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		return false;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		return null;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (result == null)
			result = new FindDuplicatesSearchResult(this);
		return result;
	}

}
