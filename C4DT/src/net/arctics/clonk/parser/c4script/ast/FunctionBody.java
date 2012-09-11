package net.arctics.clonk.parser.c4script.ast;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;

/**
 * The body of a {@link Function}
 * @author madeen
 */
public class FunctionBody extends BunchOfStatements {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Function owner;
	private transient boolean postLoaded;
	public Function owner() {
		return owner;
	}
	public FunctionBody(Function owner, List<Statement> statements) {
		super(statements);
		this.owner = owner;
		this.postLoaded = true;
	}
	@Override
	public Declaration owningDeclaration() {
		return owner;
	}
	@Override
	public void postLoad(ExprElm parent, DeclarationObtainmentContext root) {
		if (postLoaded)
			return;
		postLoaded = true;
		super.postLoad(parent, root);
	}
}
