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

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate;
import net.arctics.clonk.parser.c4script.ast.Parenthesized;

/**
 * Query to find potential duplicates of functions.
 * @author madeen
 *
 */
public class FindDuplicatesQuery extends ClonkSearchQueryBase implements IASTComparisonDelegate {
	
	private Map<String, List<Function>> functionsToBeChecked = new HashMap<String, List<Function>>();
	private Set<ClonkIndex> indexes = new HashSet<ClonkIndex>();
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
				for (ClonkIndex i : f.getIndex().relevantIndexes())
					result.indexes.add(i);
		return result;
	}
	
	/**
	 * Return a new FindDuplicatesQuery that will operate on all functions contained in the passed scripts
	 * @param scripts The script list
	 * @return The new query
	 */
	public static FindDuplicatesQuery queryWithScripts(Iterable<ScriptBase> scripts) {
		FindDuplicatesQuery result = new FindDuplicatesQuery();
		List<Function> fns = new LinkedList<Function>();
		for (ScriptBase script : scripts) {
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
		detectedDupes.clear();
		Set<ClonkIndex> indexes = new HashSet<ClonkIndex>();
		Set<Function> deemedDuplicate = new HashSet<Function>();
		for (List<Function> fnList : functionsToBeChecked.values())
			for (Function f : fnList)
				for (ClonkIndex i : f.getIndex().relevantIndexes())
					indexes.add(i);
		for (Map.Entry<String, List<Function>> entry : functionsToBeChecked.entrySet()) {
			for (final Function function : entry.getValue()) {
				if (deemedDuplicate.contains(function))
					continue;
				for (ClonkIndex index : indexes) {
					List<Declaration> decs = index.getDeclarationMap().get(function.getName());
					if (decs == null)
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
							if (function.getCodeBlock().compare(block, this).isEqual()) {
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
		if (b instanceof Comment)
			return DifferenceHandling.IgnoreRightSide;
		if (a instanceof Comment)
			return DifferenceHandling.IgnoreLeftSide;
		// treat parenthesized expressions as equivalent to non-parenthesized ones
		if (a != null && b != null) {
			if (a instanceof Parenthesized)
				return ((Parenthesized)a).getInnerExpr().compare(b, this).isEqual() ? DifferenceHandling.EqualShortCircuited : DifferenceHandling.Differs;
			if (b instanceof Parenthesized)
				return a.compare(((Parenthesized)b).getInnerExpr(), this).isEqual() ? DifferenceHandling.EqualShortCircuited : DifferenceHandling.Differs;
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

}
