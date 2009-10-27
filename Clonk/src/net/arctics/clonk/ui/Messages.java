package net.arctics.clonk.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.messages"; //$NON-NLS-1$
	public static String Project_NewDependencyDesc;
	public static String Project_ShowDependencies;
	public static String Project_Dependencies;
	public static String Project_NewDependency;
	public static String Engine_NewParameter;
	public static String Engine_EditVariable;
	public static String Engine_AddFunction;
	public static String Engine_AddFunctionDesc;
	public static String Engine_AddVariable;
	public static String Engine_AddVariableDesc;
	public static String Engine_Edit;
	public static String Engine_EditDesc;
	public static String Engine_NameTitle;
	public static String Engine_Delete;
	public static String Engine_DeleteDesc;
	public static String Engine_Save;
	public static String Engine_SaveTitle;
	public static String Engine_NoRepository;
	public static String Engine_NoRepositoryDesc;
	public static String Engine_ImportFromRepoDesc;
	public static String Engine_ImportFromRepo;
	public static String Engine_ReloadDesc;
	public static String Engine_Reload;
	public static String Engine_TypeTitle;
	public static String Engine_ScopeTitle;
	public static String Engine_EditFunction;
	public static String Engine_ReturnTypeTitle;
	public static String Engine_DescriptionTitle;
	public static String OpenObjectDialog_Empty;
	public static String OpenObjectDialog_Searching;
	public static String SimpleConfirmDialog_Yes;
	public static String SimpleConfirmDialog_ConfirmMessage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
