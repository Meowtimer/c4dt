package net.arctics.clonk.parser.c4script;



public class BuiltInDefinitions {	
	public static final String[] KEYWORDS = new String[] {
		Keywords.Break,
		Keywords.Continue,
		Keywords.Else,
		Keywords.For,
		Keywords.If,
		Keywords.Return,
		Keywords.While,
		Keywords.In,
		Keywords.Do,
		Keywords.New,
		Keywords.Nil,
		Keywords.False,
		Keywords.True
	};
	
	public static final String[] DECLARATORS = new String[] {
		Keywords.Const,
		Keywords.Func,
		Keywords.Global,
		Keywords.LocalNamed,
		Keywords.Private,
		Keywords.Protected,
		Keywords.Public,
		Keywords.GlobalNamed,
		Keywords.VarNamed
	};
	
	public static final String[] DIRECTIVES = Directive.arrayOfDirectiveStrings();
	public static final String[] SCRIPTOPERATORS = Operator.arrayOfOperatorNames(); 
}
