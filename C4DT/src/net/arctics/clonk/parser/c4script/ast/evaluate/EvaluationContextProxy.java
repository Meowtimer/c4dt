package net.arctics.clonk.parser.c4script.ast.evaluate;

import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class EvaluationContextProxy implements IEvaluationContext {

	private IEvaluationContext base;
	
	public EvaluationContextProxy(IEvaluationContext base) {
		this.base = base;
	}

	@Override
	public Object valueForVariable(String varName) {
		return base.valueForVariable(varName);
	}

	@Override
	public Object[] arguments() {
		return base.arguments();
	}

	@Override
	public Function function() {
		return base.function();
	}

	@Override
	public Script script() {
		return base.script();
	}

	@Override
	public int codeFragmentOffset() {
		return base.codeFragmentOffset();
	}

	@Override
	public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
		base.reportOriginForExpression(expression, location, file);
	}

}
