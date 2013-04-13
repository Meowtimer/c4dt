package net.arctics.clonk.parser.c4script.ast;

import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * The body of a {@link Function}
 * @author madeen
 */
public class FunctionBody extends BunchOfStatements {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final Function owner;
	public Function owner() { return owner; }
	public FunctionBody(Function owner, List<ASTNode> statements) {
		super(statements);
		this.owner = owner;
	}
	public FunctionBody(Function owner, ASTNode... statements) {
		super(statements);
		this.owner = owner;
	}
	@Override
	public Declaration owningDeclaration() {
		return owner;
	}
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
