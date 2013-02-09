package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.ParserErrorCode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IRegion;

public class Marker {
	public ParserErrorCode code;
	public int start, end;
	public int severity;
	public Object[] args;

	private final Declaration cf;
	private final ASTNode reporter;
	private final IFile scriptFile;
	private final Declaration container;

	public Marker(IASTPositionProvider positionProvider, ParserErrorCode code, ASTNode node, int start, int end, int severity, Object[] args) {
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
	public IMarker deploy() {
		IMarker result = code.createMarker(scriptFile, container, Core.MARKER_C4SCRIPT_ERROR, start, end, severity, reporter, args);
		if (cf != null && result != null)
			ParserErrorCode.setDeclarationTag(result, cf.makeNameUniqueToParent());
		IRegion exprLocation = reporter;
		if (exprLocation != null)
			ParserErrorCode.setExpressionLocation(result, exprLocation);
		return result;
	}
	@Override
	public String toString() {
		return String.format("%s @(%s)", code.toString(), reporter.toString()); //$NON-NLS-1$
	}
}