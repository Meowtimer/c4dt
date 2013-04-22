package net.arctics.clonk.c4script;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.parser.Problem;

import org.eclipse.core.resources.IFile;

public class Marker {
	public final Problem code;
	public final int start, end;
	public final int severity;
	public final Object[] args;
	public final Declaration cf;
	public final ASTNode reporter;
	public final IFile scriptFile;
	public final Declaration container;
	public Marker(IASTPositionProvider positionProvider, Problem code, ASTNode node, int start, int end, int severity, Object[] args) {
		super();
		this.code = code;
		this.start = start;
		this.end = end;
		this.severity = severity;
		this.args = args;

		this.cf = node != null ? node.parentOfType(Declaration.class) : null;
		this.reporter = node;
		this.scriptFile = positionProvider.file();
		this.container = positionProvider.container();
	}
	@Override
	public String toString() {
		return String.format("%s @(%s)", code.toString(), reporter.toString()); //$NON-NLS-1$
	}
}