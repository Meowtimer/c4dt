package net.arctics.clonk.c4script.ast.evaluate;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ControlFlowException;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.AccessVar;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class EvaluationContextProxy implements IEvaluationContext {
	private final IEvaluationContext base;
	public EvaluationContextProxy(final IEvaluationContext base) { this.base = base; }
	@Override
	public IVariable variable(final AccessVar access, final Object obj) throws ControlFlowException { return base.variable(access, null); }
	@Override
	public Object[] arguments() { return base.arguments(); }
	@Override
	public Function function() { return base.function(); }
	@Override
	public Script script() { return base.script(); }
	@Override
	public int codeFragmentOffset() { return base.codeFragmentOffset(); }
	@Override
	public void reportOriginForExpression(final ASTNode expression, final IRegion location, final IFile file) { base.reportOriginForExpression(expression, location, file); }
	@Override
	public Object self() { return null; }
}
