package net.arctics.clonk.index;

import java.lang.reflect.Field;

import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;

/**
 * Holds references to common engine functions that some {@link AccessDeclaration#optimize(net.arctics.clonk.parser.c4script.C4ScriptParser)} implementations
 * might compare their declarations with to determine viable code transformations. All the public fields of this class will be set to refer to functions of the
 * same name from the engine this helper object is associated with.
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
	
	/**
	 * Functions used to dynamically call functions on an object (Call, PrivateCall, PublicCall)
	 */
	public final Function[] ObjectCallFunctions;
	/**
	 * Functions used to dynamically call functions on an object or in the {@link Scenario} script (GameCall, PrivateCall, ProtectedCall, Call)
	 */
	public final Function[] CallFunctions;

	/**
	 * Create new {@link CachedEngineDeclarations} for some {@link Engine}.
	 * Fields will be initialized by searching for respective functions and variables in the {@link Engine}. 
	 * @param engine The engine to create the object for
	 */
	public CachedEngineDeclarations(Engine engine) {
		this.engine = engine;
		try {
			for (Field f : CachedEngineDeclarations.class.getFields()) {
				String realName = f.getName();
				if (realName.startsWith("_"))
					realName = realName.substring(1);
				if (f.getType() == Function.class)
					f.set(this, this.f(realName));
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