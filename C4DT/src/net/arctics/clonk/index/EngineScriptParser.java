package net.arctics.clonk.index;

import java.net.URL;

import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.util.LineNumberObtainer;

import org.eclipse.core.resources.IFile;

final class EngineScriptParser extends ScriptParser {
	private final URL url;
	private final LineNumberObtainer lno;
	private boolean firstMessage = true;
	EngineScriptParser(String engineScript, Script script, IFile scriptFile, URL url) {
		super(engineScript, script, scriptFile);
		this.url = url;
		this.lno = new LineNumberObtainer(engineScript);
	}
	@Override
	public void marker(Problem code,
		int errorStart, int errorEnd, int flags,
		int severity, Object... args) throws ProblemException {
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
	protected Function newFunction(String nameWillBe) { return new EngineFunction(); }
	@Override
	public Variable newVariable(String varName, Scope scope) { return new EngineVariable(varName, scope); }
}