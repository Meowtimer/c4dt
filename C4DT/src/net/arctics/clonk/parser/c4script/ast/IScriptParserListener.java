package net.arctics.clonk.parser.c4script.ast;

import java.util.List;

import net.arctics.clonk.parser.c4script.C4ScriptParser;

public interface IScriptParserListener {
	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser);
	public void endTypeInferenceBlock(List<IStoredTypeInformation> typeInfos);
	public int minimumParsingRecursion();
}