package net.arctics.clonk.ui.editors;

import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.ui.editors.c4script.Messages;
import net.arctics.clonk.util.UI;

import org.eclipse.swt.graphics.Image;

public class PrimitiveTypeProposal extends DeclarationProposal {
	static final Image KEYWORD_IMG = UI.imageForPath("icons/keyword.png"); //$NON-NLS-1$
	public PrimitiveTypeProposal(ProposalsSite site, PrimitiveType t) {
		super(null, null, t.scriptName(), site.offset, site.prefix != null ? site.prefix.length() : 0 , t.scriptName().length(),
				KEYWORD_IMG , t.scriptName(), null, null, Messages.C4ScriptCompletionProcessor_Engine, site);
	}
}
