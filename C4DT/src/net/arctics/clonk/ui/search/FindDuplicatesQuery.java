package net.arctics.clonk.ui.search;

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
import net.arctics.clonk.resource.ClonkProjectNature;

public class FindDuplicatesQuery extends ClonkSearchQuery implements IASTComparisonDelegate {
	
	public FindDuplicatesQuery(Function function, ClonkProjectNature project) {
		super(function, project);
	}
	
	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		Function function = (Function) declaration;
		Block functionBlock = function.getCodeBlock();
		for (ClonkIndex index : function.getIndex().relevantIndexes()) {
			for (ScriptBase script : index.allScripts()) {
				for (Function f : script.functions()) {
					if (f == function)
						continue;
					Block block = f.getCodeBlock();
					switch (functionBlock.compare(block, this)) {
					case Equal: case IgnoreLeftSide: case IgnoreRightSide:
						result.addMatch(f);
						break;
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
		return String.format(Messages.FindDuplicatesQuery_Label, declaration.getName());
	}

}
