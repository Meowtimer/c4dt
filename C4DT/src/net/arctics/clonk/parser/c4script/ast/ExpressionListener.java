package net.arctics.clonk.parser.c4script.ast;

import java.util.List;


public abstract class ExpressionListener implements IExpressionListener {
	@Override
	public void endTypeInferenceBlock(List<IStoredTypeInformation> typeInfos) {
		// don't care
	}
}