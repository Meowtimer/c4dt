package net.arctics.clonk.resource;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.resource.messages"; //$NON-NLS-1$
	public static String CleaningUp;
	public static String ClonkBuilder_CleaningProject;
	public static String ClonkBuilder_GatheringScripts;
	public static String ClonkBuilder_ParseCodeTask;
	public static String ClonkBuilder_ParseDeclarations;
	public static String ClonkBuilder_ParseDeclarationsTask;
	public static String ClonkBuilder_ParseFunctionCode;
	public static String ClonkBuilder_RenameRefactoringPrompt;
	public static String ClonkBuilder_RenameRefactoringTitle;
	public static String ClonkBuilder_SaveIndexFilesForParsedScripts;
	public static String ClonkBuilder_SavingScriptIndexFiles;
	public static String BuildProject;
	public static String ParsingLibraries;
	public static String SavingLibraries;
	public static String IndexProject;
	public static String ParseProject;
	public static String SavingData;
	public static String ExternalLibraryDoesNotExist;
	public static String ParsingLibs;
	public static String ReparseDependentScripts;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
