package net.arctics.clonk.parser.c4script;



public class BuiltInDefinitions {
	public static final String[] OBJECT_CALLBACKS = new String[] {
		"Activate", //$NON-NLS-1$
		"ActivateEntrance", //$NON-NLS-1$
		"AttachTargetLost", //$NON-NLS-1$
		"BuildNeedsMaterial", //$NON-NLS-1$
		"CalcBuyValue", //$NON-NLS-1$
		"CalcDefValue", //$NON-NLS-1$
		"CalcSellValue", //$NON-NLS-1$
		"CalcValue", //$NON-NLS-1$
		"CatchBlow", //$NON-NLS-1$
		"Collection", //$NON-NLS-1$
		"Collection2", //$NON-NLS-1$
		"Completion", //$NON-NLS-1$
		"Construction", //$NON-NLS-1$
		"ContainedLeft", //$NON-NLS-1$
		"ContainedRight", //$NON-NLS-1$
		"ContainedUp", //$NON-NLS-1$
		"ContainedDown", //$NON-NLS-1$
		"ContainedDig", //$NON-NLS-1$
		"ContainedThrow", //$NON-NLS-1$
		"ContainedUpdate", //$NON-NLS-1$
		"ContainedLeftSingle", //$NON-NLS-1$
		"ContainedRightSingle", //$NON-NLS-1$
		"ContainedUpSingle", //$NON-NLS-1$
		"ContainedDownSingle", //$NON-NLS-1$
		"ContainedDigSingle", //$NON-NLS-1$
		"ContainedThrowSingle", //$NON-NLS-1$
		"ContainedLeftDouble", //$NON-NLS-1$
		"ContainedRightDouble", //$NON-NLS-1$
		"ContainedUpDouble", //$NON-NLS-1$
		"ContainedDownDouble", //$NON-NLS-1$
		"ContainedDigDouble", //$NON-NLS-1$
		"ContainedThrowDouble", //$NON-NLS-1$
		"ControlCommand", //$NON-NLS-1$
		"ControlCommandFinished", //$NON-NLS-1$
		"ControlContents", //$NON-NLS-1$
		"ControlTransfer", //$NON-NLS-1$
		"ControlLeft", //$NON-NLS-1$
		"ControlUp", //$NON-NLS-1$
		"ControlRight", //$NON-NLS-1$
		"ControlDown", //$NON-NLS-1$
		"ControlDig", //$NON-NLS-1$
		"ControlThrow", //$NON-NLS-1$
		"ControlSpecial", //$NON-NLS-1$
		"ControlWheelUp", //$NON-NLS-1$
		"ControlWheelDown", //$NON-NLS-1$
		"ControlLeftSingle", //$NON-NLS-1$
		"ControlUpSingle", //$NON-NLS-1$
		"ControlRightSingle", //$NON-NLS-1$
		"ControlDownSingle", //$NON-NLS-1$
		"ControlDigSingle", //$NON-NLS-1$
		"ControlThrowSingle", //$NON-NLS-1$
		"ControlSpecialSingle", //$NON-NLS-1$
		"ControlLeftDouble", //$NON-NLS-1$
		"ControlUpDouble", //$NON-NLS-1$
		"ControlRightDouble", //$NON-NLS-1$
		"ControlDownDouble", //$NON-NLS-1$
		"ControlDigDouble", //$NON-NLS-1$
		"ControlThrowDouble", //$NON-NLS-1$
		"ControlSpecialDouble", //$NON-NLS-1$
		"ControlLeftReleased", //$NON-NLS-1$
		"ControlUpReleased", //$NON-NLS-1$
		"ControlRightReleased", //$NON-NLS-1$
		"ControlDownReleased", //$NON-NLS-1$
		"ControlDigReleased", //$NON-NLS-1$
		"ControlThrowReleased", //$NON-NLS-1$
		"ControlUpdate", //$NON-NLS-1$
		"CrewSelection", //$NON-NLS-1$
		"Damage", //$NON-NLS-1$
		"Death", //$NON-NLS-1$
		"DeepBreath", //$NON-NLS-1$
		"Departure", //$NON-NLS-1$
		"Destruction", //$NON-NLS-1$
		"Ejection", //$NON-NLS-1$
		"Entrance", //$NON-NLS-1$
		"Get", //$NON-NLS-1$
		"GetObject2Drop", //$NON-NLS-1$
		"Grab", //$NON-NLS-1$
		"GrabLost", //$NON-NLS-1$
		"Hit", //$NON-NLS-1$
		"Hit2", //$NON-NLS-1$
		"Hit3", //$NON-NLS-1$
		"Incineration", //$NON-NLS-1$
		"IncinerationEx", //$NON-NLS-1$
		"Initialize", //$NON-NLS-1$
		"InitializePlayer", //$NON-NLS-1$
		"IsFulfilled", //$NON-NLS-1$
		"LiftTop", //$NON-NLS-1$
		"LineBreak", //$NON-NLS-1$
		"MenuQueryCancel", //$NON-NLS-1$
		"OnMenuSelection", //$NON-NLS-1$
		"Purchase", //$NON-NLS-1$
		"Put", //$NON-NLS-1$
		"QueryCatchBlow", //$NON-NLS-1$
		"Recruitment", //$NON-NLS-1$
		"RejectCollect", //$NON-NLS-1$
		"RejectEntrance", //$NON-NLS-1$
		"Sale", //$NON-NLS-1$
		"Selection", //$NON-NLS-1$
		"SellTo", //$NON-NLS-1$
		"Stuck", //$NON-NLS-1$
		"UpdateTransferZone" //$NON-NLS-1$
	};
	
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
		Keywords.Nil
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
