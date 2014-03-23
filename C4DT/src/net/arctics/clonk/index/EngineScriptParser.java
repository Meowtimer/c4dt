package net.arctics.clonk.index;

import java.net.URL;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeAnnotation;
import net.arctics.clonk.util.LineNumberObtainer;

import org.eclipse.core.resources.IFile;

final class EngineScriptParser extends ScriptParser {
	private final URL url;
	private final LineNumberObtainer lno;
	private boolean firstMessage = true;
	EngineScriptParser(final String engineScript, final Script script, final IFile scriptFile, final URL url) {
		super(engineScript, script, scriptFile);
		this.url = url;
		this.lno = new LineNumberObtainer(engineScript);
	}
	@Override
	public void marker(final Problem code,
		final int errorStart, final int errorEnd, final int flags,
		final int severity, final Object... args) throws ProblemException {
		if (firstMessage) {
			firstMessage = false;
			System.out.println("Messages while parsing " + url.toString()); //$NON-NLS-1$
		}
		System.out.println(String.format(
			"%s @(%d, %d)", //$NON-NLS-1$
			code.makeErrorString(args),
			lno.obtainLineNumber(errorStart),
			lno.obtainCharNumberInObtainedLine()
		));
		super.marker(code, errorStart, errorEnd, flags, severity, args);
	}
	@Override
	protected TypeAnnotation typeAnnotation(final int s, final int e, final IType type) {
		// undo authority boost -.-
		return super.typeAnnotation(s, e, type instanceof PrimitiveType.Unified ? ((PrimitiveType.Unified)type).base() : type);
	}
	@Override
	protected Function newFunction(final String nameWillBe) { return new EngineFunction(); }
	@Override
	public Variable newVariable(final String varName, final Scope scope) { return new EngineVariable(scope, varName); }
}