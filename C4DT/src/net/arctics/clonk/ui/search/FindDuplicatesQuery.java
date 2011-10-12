package net.arctics.clonk.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.FunctionDescription;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;
import net.arctics.clonk.parser.c4script.ast.ReturnStatement;
import net.arctics.clonk.parser.c4script.ast.Wildcard;
import net.arctics.clonk.preferences.ClonkPreferences;

/**
 * Query to find potential duplicates of functions.
 * @author madeen
 *
 */
public class FindDuplicatesQuery extends ClonkSearchQueryBase implements IASTComparisonDelegate {
	
	private Map<String, List<Function>> functionsToBeChecked = new HashMap<String, List<Function>>();
	private Set<Index> indexes = new HashSet<Index>();
	private Map<Function, List<FindDuplicatesMatch>> detectedDupes = new HashMap<Function, List<FindDuplicatesMatch>>();
	
	public Map<Function, List<FindDuplicatesMatch>> getDetectedDupes() {
		return detectedDupes;
	}
	
	private FindDuplicatesQuery() {}
	
	/**
	 * Return a new FindDuplicatesQuery that will operate on a list of functions.
	 * @param functions The function list
	 * @return The new query
	 */
	public static FindDuplicatesQuery queryWithFunctions(List<Function> functions) {
		FindDuplicatesQuery result = new FindDuplicatesQuery();
		result.fillFunctionMapWithFunctionList(functions);
		for (List<Function> fnList : result.functionsToBeChecked.values())
			for (Function f : fnList)
				for (Index i : f.getIndex().relevantIndexes())
					result.indexes.add(i);
		return result;
	}
	
	/**
	 * Return a new FindDuplicatesQuery that will operate on all functions contained in the passed scripts
	 * @param scripts The script list
	 * @return The new query
	 */
	public static FindDuplicatesQuery queryWithScripts(Iterable<Script> scripts) {
		FindDuplicatesQuery result = new FindDuplicatesQuery();
		List<Function> fns = new LinkedList<Function>();
		for (Script script : scripts) {
			fns.addAll(script.functions());
			result.indexes.add(script.getIndex());
		}
		result.fillFunctionMapWithFunctionList(fns);
		return result;
	}

	private void fillFunctionMapWithFunctionList(List<Function> functions) {
		for (Function f : functions) {
			if (f.getCodeBlock() == null)
				continue;
			List<Function> list = functionsToBeChecked.get(f.getName());
			if (list == null) {
				list = new LinkedList<Function>();
				functionsToBeChecked.put(f.getName(), list);
			}
			list.add(f);
		}
	}
	
	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		boolean ignoreSimpleFunctions = ClonkPreferences.getPreferenceToggle(ClonkPreferences.IGNORE_SIMPLE_FUNCTION_DUPES, false);
		
		detectedDupes.clear();
		Set<Index> indexes = new HashSet<Index>();
		Set<Function> deemedDuplicate = new HashSet<Function>();
		for (List<Function> fnList : functionsToBeChecked.values())
			for (Function f : fnList)
				for (Index i : f.getIndex().relevantIndexes())
					indexes.add(i);
		for (Map.Entry<String, List<Function>> entry : functionsToBeChecked.entrySet()) {
			for (final Function function : entry.getValue()) {
				for (Index index : indexes)
					index.loadScriptsContainingDeclarationsNamed(function.getName());
				if (deemedDuplicate.contains(function))
					continue;
				Block functionCodeBlock = function.getCodeBlock();
				// ignore simple return functions
				if (ignoreSimpleFunctions)
					if (functionCodeBlock == null || functionCodeBlock.getStatements().length == 1 && functionCodeBlock.getStatements()[0] instanceof ReturnStatement)
						continue;
				for (Index index : indexes) {
					List<Declaration> decs = index.snapshotOfDeclarationsNamed(function.getName());
					if (decs == null)
						continue;
					if (!decs.contains(function)) // happens when a newly-parsed function is not already added to the declaration map
						continue;
					for (Declaration d : decs) {
						if (d instanceof Function) {
							final Function otherFn = (Function) d;
							if (deemedDuplicate.contains(d))
								continue;
							if (function == otherFn)
								continue;
							Block block = otherFn.getCodeBlock();
							if (block == null)
								continue; // -.-
							if (functionCodeBlock.compare(block, this).isEqual()) {
								List<FindDuplicatesMatch> dupes = detectedDupes.get(function);
								if (dupes == null) {
									dupes = new LinkedList<FindDuplicatesMatch>();
									detectedDupes.put(function, dupes);
								}
								FindDuplicatesMatch match = new FindDuplicatesMatch(otherFn.getScript(), otherFn.getLocation().getOffset(), otherFn.getLocation().getLength(), function, otherFn);
								dupes.add(match);
								result.addMatch(match);
								deemedDuplicate.add(otherFn);
							}
						}
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public DifferenceHandling differs(ExprElm a, ExprElm b, Object what) {
		// ignore deviating subelements length since there might be comments in there that will be ignored
		if (what == SUBELEMENTS_LENGTH)
			return DifferenceHandling.IgnoreLeftSide; // either left or right.. doesn't matter
		// ignore comments on both sides
		if (b instanceof Comment || b instanceof FunctionDescription)
			return DifferenceHandling.IgnoreRightSide;
		if (a instanceof Comment || a instanceof FunctionDescription)
			return DifferenceHandling.IgnoreLeftSide;
		if (a != null && b != null) {
			
			// treat parenthesized expressions as equivalent to non-parenthesized ones
			if (a instanceof Parenthesized)
				return ((Parenthesized)a).innerExpression().compare(b, this).isEqual() ? DifferenceHandling.EqualShortCircuited : DifferenceHandling.Differs;
			if (b instanceof Parenthesized)
				return a.compare(((Parenthesized)b).innerExpression(), this).isEqual() ? DifferenceHandling.EqualShortCircuited : DifferenceHandling.Differs;
			
			// ignore differing variable names if both variables are parameters at the same index in their respective functions
			if (a instanceof AccessVar && b instanceof AccessVar) {
				AccessVar varA = (AccessVar)a;
				AccessVar varB = (AccessVar)b;
				if (varA.getDeclaration() instanceof Variable && varB.getDeclaration() instanceof Variable) {
					int parmA = ((Variable)varA.getDeclaration()).parameterIndex();
					int parmB = ((Variable)varB.getDeclaration()).parameterIndex();
					if (parmA != -1 && parmA == parmB)
						return DifferenceHandling.EqualShortCircuited;
				}
			}
			
			// ignore order of operands in binary associative operator expression 
			if (a.getParent() instanceof BinaryOp && b.getParent() instanceof BinaryOp) {
				BinaryOp opA = (BinaryOp) a.getParent();
				BinaryOp opB = (BinaryOp) b.getParent();
				if (opA.operator() == opB.operator() && opA.operator().isAssociative()) {
					if (
						b == opB.leftSide() || b == opB.rightSide() &&
						a == opA.leftSide() || a == opA.rightSide()
					) {
						final ExprElm bCounterpart = opB.leftSide() == b ? opB.rightSide() : opB.leftSide();
						final ExprElm aCounterpart = opA.leftSide() == a ? opA.rightSide() : opA.leftSide();
						IASTComparisonDelegate proxy = new IASTComparisonDelegate() {
							@Override
							public DifferenceHandling differs(ExprElm _a, ExprElm _b, Object what) {
								// fuck off, recursion
								if (_a == aCounterpart || _a == bCounterpart)
									return DifferenceHandling.Differs;
								return FindDuplicatesQuery.this.differs(_a, _b, what);
							}
							@Override
							public boolean optionEnabled(Option option) {
								return FindDuplicatesQuery.this.optionEnabled(option);
							}
							@Override
							public void wildcardMatched(Wildcard wildcard, ExprElm expression) {
								FindDuplicatesQuery.this.wildcardMatched(wildcard, expression);
							}
						};
						if (aCounterpart.compare(b, proxy).isEqual() && a.compare(bCounterpart, proxy).isEqual())
							return DifferenceHandling.EqualShortCircuited;
					}
				}
			}
		}
		return DifferenceHandling.Differs;
	}

	@Override
	public boolean optionEnabled(Option option) {
		return false;
	}
	
	@Override
	public String getLabel() {
		StringBuilder builder = new StringBuilder(30);
		int size = 3;
		for (String f : functionsToBeChecked.keySet()) {
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

	@Override
	public void wildcardMatched(Wildcard wildcard, ExprElm expression) {
	}

}
