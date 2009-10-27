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
	
	public final C4Function Par = f("Par"); //$NON-NLS-1$
	public final C4Function Var = f("Var"); //$NON-NLS-1$
	public final C4Function GameCall = f("GameCall"); //$NON-NLS-1$
	public final C4Function LocalN = f("LocalN"); //$NON-NLS-1$
	public final C4Function ScheduleCall = f("ScheduleCall"); //$NON-NLS-1$
	public final C4Function PrivateCall = f("PrivateCall"); //$NON-NLS-1$
	public final C4Function ProtectedCall = f("ProtectedCall"); //$NON-NLS-1$
	public final C4Function Call = f("Call"); //$NON-NLS-1$
	public final C4Function DecVar = f("DecVar"); //$NON-NLS-1$
	public final C4Function IncVar = f("IncVar"); //$NON-NLS-1$
	public final C4Function SetVar = f("SetVar"); //$NON-NLS-1$
	public final C4Function AssignVar = f("AssignVar"); //$NON-NLS-1$
	public final C4Function SetLocal = f("SetLocal"); //$NON-NLS-1$
	public final C4Function SetCommand = f("SetCommand"); //$NON-NLS-1$
	public final C4Function AddCommand = f("AddCommand"); //$NON-NLS-1$
	public final C4Function AppendCommand = f("AppendCommand"); //$NON-NLS-1$
	public final C4Function ObjectCall = f("ObjectCall"); //$NON-NLS-1$
	
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