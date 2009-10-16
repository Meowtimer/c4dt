package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.ClonkCore;

/**
 * Contains cached engine functions that may be used frequently.
 * FIXME: if engine index is modified/recreated stuff depending on those functions won't work properly
 */
public class CachedEngineFuncs {
	
	private static CachedEngineFuncs instance;
	
	private static C4Function f(String name) {
		return ClonkCore.getDefault().getEngineObject().findFunction(name);
	}
	
	public final C4Function Par = f("Par");
	public final C4Function Var = f("Var");
	public final C4Function GameCall = f("GameCall");
	public final C4Function LocalN = f("LocalN");
	public final C4Function ScheduleCall = f("ScheduleCall");
	public final C4Function PrivateCall = f("PrivateCall");
	public final C4Function ProtectedCall = f("ProtectedCall");
	public final C4Function Call = f("Call");
	public final C4Function DecVar = f("DecVar");
	public final C4Function IncVar = f("IncVar");
	public final C4Function SetVar = f("SetVar");
	public final C4Function AssignVar = f("AssignVar");
	public final C4Function SetLocal = f("SetLocal");
	public final C4Function SetCommand = f("SetCommand");
	public final C4Function AddCommand = f("AddCommand");
	public final C4Function AppendCommand = f("AppendCommand");
	public final C4Function ObjectCall = f("ObjectCall");
	
	public final C4Function[] ObjectCallFunctions = new C4Function[] {
		ObjectCall,
		ProtectedCall,
		PrivateCall
	};
	
	public final C4Function[] CallFunctions = new C4Function[] {
		GameCall, ScheduleCall, PrivateCall, ProtectedCall, Call
	};
	
	public static CachedEngineFuncs getInstance() {
		if (instance == null)
			instance = new CachedEngineFuncs();
		return instance;
	}
	
	public static void refreshInstance() {
		instance = null; // get new one when asked
	}

}