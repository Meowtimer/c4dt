package net.arctics.clonk.c4script.ast;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.parser.c4script.ast.messages"; //$NON-NLS-1$
	public static String AccessDeclaration_TypesSporting;
	public static String PropListExpression_ProplistsFeature;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
