package net.arctics.clonk.index;

import java.lang.reflect.Field;

import net.arctics.clonk.parser.c4script.Function;

/**
 * Contains cached engine functions that may be used frequently.
 */
public class CachedEngineDeclarations {

	private final Engine engine;
	
	private Function f(String name) {
		return engine.findFunction(name);
	}
	
	public Function Par;
	public Function Var;
	public Function Local;
	public Function GameCall;
	public Function LocalN;
	public Function PrivateCall;
	public Function ProtectedCall;
	public Function Call;
	public Function DecVar;
	public Function IncVar;
	public Function SetVar;
	public Function AssignVar;
	public Function SetLocal;
	public Function SetCommand;
	public Function AddCommand;
	public Function AppendCommand;
	public Function ObjectCall;
	public Function GetID;
	public Object   This; // this as variable name not allowed so exclude this var -.-
	
	public final Function[] ObjectCallFunctions;
	
	public final Function[] CallFunctions;

	public CachedEngineDeclarations(Engine engine) {
		this.engine = engine;
		try {
			for (Field f : CachedEngineDeclarations.class.getFields()) {
				if (f.getType() == Function.class) {
					f.set(this, this.f(f.getName()));
				}
			}
			This = this.f("this"); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
		}
		CallFunctions = new Function[] {
			GameCall, PrivateCall, ProtectedCall, Call
		};
		ObjectCallFunctions = new Function[] {
			ObjectCall,
			ProtectedCall,
			PrivateCall
		};
	}

}