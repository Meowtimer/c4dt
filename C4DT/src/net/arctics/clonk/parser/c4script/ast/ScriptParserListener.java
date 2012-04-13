package net.arctics.clonk.parser.c4script.ast;

import java.util.List;


public abstract class ScriptParserListener implements IScriptParserListener {
	@Override
	public void endTypeInferenceBlock(List<ITypeInfo> typeInfos) {
		// don't care
	}
	@Override
	public int minimumParsingRecursion() {
		return 1;
	}
}