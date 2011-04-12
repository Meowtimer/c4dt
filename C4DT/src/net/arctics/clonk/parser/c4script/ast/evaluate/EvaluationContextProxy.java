package net.arctics.clonk.parser.c4script.ast.evaluate;

import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class EvaluationContextProxy implements IEvaluationContext {

	private IEvaluationContext base;
	
	public EvaluationContextProxy(IEvaluationContext base) {
		this.base = base;
	}

	@Override
	public Object getValueForVariable(String varName) {
		return base.getValueForVariable(varName);
	}

	@Override
	public Object[] getArguments() {
		return base.getArguments();
	}

	@Override
	public Function getFunction() {
		return base.getFunction();
	}

	@Override
	public ScriptBase getScript() {
		return base.getScript();
	}

	@Override
	public int getCodeFragmentOffset() {
		return base.getCodeFragmentOffset();
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		base.reportOriginForExpression(expression, location, file);
	}

}
