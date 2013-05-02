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
	private final Function owner;
	public FunctionBody(Function owner, List<ASTNode> statements) {
		super(statements);
		this.owner = owner;
	}
	public FunctionBody(Function owner, ASTNode... statements) {
		super(statements);
		this.owner = owner;
	}
	@Override
	public Function owner() { return owner; }
	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		if (owner != null && owner.isOldStyle())
			super.doPrint(builder, depth);
		else
			printBlock(statements(), builder, depth);
	}
	@Override
	public IRegion absolute() {
		if (owner != null && !owner.isOldStyle())
			return new Region(owner.bodyLocation().start()-1, owner.bodyLocation().getLength()+2);
		else
			return super.absolute();
	}
}
