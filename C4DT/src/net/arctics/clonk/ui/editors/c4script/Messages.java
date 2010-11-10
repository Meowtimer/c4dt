package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.c4script.messages"; //$NON-NLS-1$
	public static String C4ScriptCompletionProcessor_AllCompletions;
	public static String C4ScriptCompletionProcessor_LocalCompletions;
	public static String C4ScriptCompletionProcessor_Engine;
	public static String C4ScriptCompletionProcessor_ObjectCompletions;
	public static String C4ScriptCompletionProcessor_Callback;
	public static String C4ScriptCompletionProcessor_ProjectFiles;
	public static String C4ScriptCompletionProcessor_PressToShowCycle;
	public static String C4ScriptCompletionProcessor_ExternalLibraries;
	public static String C4ScriptCompletionProcessor_EngineFunctions;
	public static String C4ScriptCompletionProcessor_ShownData;
	public static String C4ScriptCompletionProcessor_NoParameters;
	public static String C4ScriptCompletionProcessor_NoProposalsAvailable;
	public static String C4ScriptSourceViewerConfiguration_StandardProposals;
	public static String C4ScriptSourceViewerConfiguration_PressTabOrClick;
	public static String ClonkQuickAssistProcessor_AddMissingSemicolon;
	public static String ClonkQuickAssistProcessor_CommentOutStatement;
	public static String ClonkQuickAssistProcessor_ConvertComparisonToAssignment;
	public static String ClonkQuickAssistProcessor_ConvertToReturn;
	public static String ClonkQuickAssistProcessor_ConvertToVarDeclaration;
	public static String ClonkQuickAssistProcessor_FailedToFix;
	public static String ClonkQuickAssistProcessor_QuoteExpression;
	public static String ClonkQuickAssistProcessor_RemoveBrackets;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
