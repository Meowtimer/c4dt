package net.arctics.clonk.parser.c4script.ast;


import net.arctics.clonk.util.IConverter;

public abstract class Conf {
	
	// options
	public static boolean alwaysConvertObjectCalls = false;
	public static BraceStyleType braceStyle = BraceStyleType.NewLine;
	public static String indentString = "\t"; //$NON-NLS-1$

	public static void printIndent(ExprWriter builder, int indentDepth) {
		for (int i = 0; i < indentDepth; i++)
			builder.append(indentString); // FIXME: should be done according to user's preferences
	}

	static final IConverter<ExprElm, Object> EVALUATE_EXPR = new IConverter<ExprElm, Object>() {
		@Override
        public Object convert(ExprElm from) {
            try {
				return from != null ? from.evaluate() : null;
			} catch (ControlFlowException e) {
				return null;
			}
        }
	};

}
