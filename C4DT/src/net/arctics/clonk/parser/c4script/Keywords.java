package net.arctics.clonk.parser.c4script;

/**
 * Keywords of C4Script
 */
public interface Keywords {

	public static final String Func = "func"; //$NON-NLS-1$

	public static final String Private = "private"; //$NON-NLS-1$
	public static final String Protected = "protected"; //$NON-NLS-1$
	public static final String Public = "public"; //$NON-NLS-1$
	public static final String Global = "global"; //$NON-NLS-1$
	public static final String Const = "const"; //$NON-NLS-1$

	public static final String If = "if"; //$NON-NLS-1$
	public static final String Else = "else"; //$NON-NLS-1$
	public static final String While = "while"; //$NON-NLS-1$
	public static final String Do = "do"; //$NON-NLS-1$
	public static final String For = "for"; //$NON-NLS-1$
	public static final String In = "in"; //$NON-NLS-1$
	public static final String Return = "return"; //$NON-NLS-1$
	public static final String Break = "break"; //$NON-NLS-1$
	public static final String Continue = "continue"; //$NON-NLS-1$
	public static final String Inherited = "inherited"; //$NON-NLS-1$
	public static final String SafeInherited = "_inherited"; //$NON-NLS-1$

	public static final String Image = "Image"; //$NON-NLS-1$
	public static final String Condition = "Condition"; //$NON-NLS-1$

	/** Static variable scope */
	public static final String GlobalNamed = "static"; //$NON-NLS-1$
	public static final String LocalNamed = "local"; //$NON-NLS-1$
	public static final String VarNamed = "var"; //$NON-NLS-1$

	public static final String True = "true"; //$NON-NLS-1$
	public static final String False = "false"; //$NON-NLS-1$

	public static final String DefinitionFunc = "Definition"; //$NON-NLS-1$
}