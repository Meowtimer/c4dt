package net.arctics.clonk.c4script.ast.evaluate;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessVar;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class EvaluationContextProxy implements IEvaluationContext {
	private final IEvaluationContext base;
	public EvaluationContextProxy(IEvaluationContext base) { this.base = base; }
	@Override
	public Object valueForVariable(AccessVar access) { return base.valueForVariable(access); }
	@Override
	public Object[] arguments() { return base.arguments(); }
	@Override
	public Function function() { return base.function(); }
	@Override
	public Script script() { return base.script(); }
	@Override
	public int codeFragmentOffset() { return base.codeFragmentOffset(); }
	@Override
	public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) { base.reportOriginForExpression(expression, location, file); }
	@Override
	public Object cookie() { return null; }
}
