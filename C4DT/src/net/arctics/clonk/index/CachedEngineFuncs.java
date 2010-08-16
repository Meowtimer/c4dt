package net.arctics.clonk.index;

import java.lang.reflect.Field;

import net.arctics.clonk.parser.c4script.C4Function;

/**
 * Contains cached engine functions that may be used frequently.
 */
public class CachedEngineFuncs {

	private final C4Engine engine;
	
	private C4Function f(String name) {
		return engine.findFunction(name);
	}
	
	public C4Function Par;
	public C4Function Var;
	public C4Function Local;
	public C4Function GameCall;
	public C4Function LocalN;
	public C4Function PrivateCall;
	public C4Function ProtectedCall;
	public C4Function Call;
	public C4Function DecVar;
	public C4Function IncVar;
	public C4Function SetVar;
	public C4Function AssignVar;
	public C4Function SetLocal;
	public C4Function SetCommand;
	public C4Function AddCommand;
	public C4Function AppendCommand;
	public C4Function ObjectCall;
	public C4Function GetID;
	public Object     This; // this as variable name not allowed so exclude this var -.-
	
	public final C4Function[] ObjectCallFunctions;
	
	public final C4Function[] CallFunctions;

	public CachedEngineFuncs(C4Engine engine) {
		this.engine = engine;
		try {
			for (Field f : CachedEngineFuncs.class.getFields()) {
				if (f.getType() == C4Function.class) {
					f.set(this, this.f(f.getName()));
				}
			}
			This = this.f("this"); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
		}
		CallFunctions = new C4Function[] {
			GameCall, PrivateCall, ProtectedCall, Call
		};
		ObjectCallFunctions = new C4Function[] {
			ObjectCall,
			ProtectedCall,
			PrivateCall
		};
	}

}