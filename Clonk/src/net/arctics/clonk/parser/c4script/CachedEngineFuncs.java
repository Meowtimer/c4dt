package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.ClonkCore;

/**
 * Contains cached engine functions that may be used frequently.
 */
public class CachedEngineFuncs {
	
	private static C4Function f(String name) {
		return ClonkCore.getDefault().getEngineObject().findFunction(name);
	}
	
	public static final C4Function Par = f("Par");
	public static final C4Function Var = f("Var");
	public static final C4Function GameCall = f("GameCall");
	public static final C4Function LocalN = f("LocalN");
	public static final C4Function ScheduleCall = f("ScheduleCall");
	public static final C4Function PrivateCall = f("PrivateCall");
	public static final C4Function ProtectedCall = f("ProtectedCall");
	public static final C4Function Call = f("Call");
	public static final C4Function DecVar = f("DecVar");
	public static final C4Function IncVar = f("IncVar");
	public static final C4Function SetVar = f("SetVar");
	public static final C4Function AssignVar = f("AssignVar");
	public static final C4Function SetLocal = f("SetLocal");
	
	public static final C4Function[] CallFunctions = new C4Function[] {
		GameCall, ScheduleCall, PrivateCall, ProtectedCall, Call
	};

}