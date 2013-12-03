package net.arctics.clonk.c4script.ast;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Function;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * The body of a {@link Function}
 * @author madeen
 */
public class FunctionBody extends BunchOfStatements {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public FunctionBody(final Function owner, final List<ASTNode> statements) {
		super(statements);
		this.parent = owner;
	}
	public FunctionBody(final Function owner, final ASTNode... statements) {
		super(statements);
		this.parent = owner;
	}
	@Override
	public Function owner() { return (Function)parent; }
	@Override
	public void doPrint(final ASTNodePrinter builder, final int depth) {
		final Function owner = owner();
		if (owner != null && owner.isOldStyle())
			super.doPrint(builder, depth);
		else
			printBlock(statements(), builder, depth);
	}
	@Override
	public IRegion absolute() {
		final Function owner = owner();
		if (owner != null && !owner.isOldStyle())
			return new Region(owner.bodyLocation().start()-1, owner.bodyLocation().getLength()+2);
		else
			return super.absolute();
	}
}
