package net.arctics.clonk.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IASTComparisonDelegate;

public class FindDuplicatesQuery extends ClonkSearchQueryBase implements IASTComparisonDelegate {
	
	private Map<String, Function> functionsToBeChecked = new HashMap<String, Function>();
	
	public FindDuplicatesQuery(Function... functions) {
		for (Function f : functions)
			functionsToBeChecked.put(f.getName(), f);
	}
	
	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		Set<ClonkIndex> indexes = new HashSet<ClonkIndex>();
		for (Function f : functionsToBeChecked.values())
			for (ClonkIndex i : f.getIndex().relevantIndexes())
				indexes.add(i);
		for (ClonkIndex index : indexes) {
			for (ScriptBase script : index.allScripts()) {
				for (Function f : script.functions()) {
					Function function = functionsToBeChecked.get(f.getName());
					if (function == null || function == f)
						continue;
					if (f.getName().equals(function.getName())) {
						Block block = f.getCodeBlock();
						switch (function.getCodeBlock().compare(block, this)) {
						case Equal: case IgnoreLeftSide: case IgnoreRightSide:
							result.addMatch(f, f.getScript());
							break;
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
		else if (a instanceof Comment)
			return DifferenceHandling.IgnoreLeftSide;
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
		for (Function f : functionsToBeChecked.values()) {
			if (builder.length() > 0)
				builder.append(", ");
			if (size-- == 0) {
				builder.append("...");
				break;
			} else
				builder.append(f.getName());
		}
		return String.format(Messages.FindDuplicatesQuery_Label, builder.toString());
	}

}
