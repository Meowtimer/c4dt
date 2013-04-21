package net.arctics.clonk.c4script;

import java.util.Arrays;
import java.util.List;



public class BuiltInDefinitions {	
	public static final List<String> KEYWORDS = Arrays.asList(
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
	);
	
	public static final List<String> DECLARATORS = Arrays.asList(
		Keywords.Const,
		Keywords.Func,
		Keywords.Global,
		Keywords.LocalNamed,
		Keywords.Private,
		Keywords.Protected,
		Keywords.Public,
		Keywords.GlobalNamed,
		Keywords.VarNamed
	);
	
	public static final String[] SCRIPTOPERATORS = Operator.operatorNames(); 
}
