package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * new <protoype> { ... } expression as syntactic sugar for { Prototype = <prototype>, ... }
 * @author madeen
 *
 */
public class NewProplist extends PropListExpression {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	/**
	 * Create new NewProplist expression. The passed {@link ProplistDeclaration} will get its {@link ProplistDeclaration#implicitPrototype()} set to the {@code prototypeExpression} parameter.
	 * @param declaration The declaration representing the proplist block
	 * @param prototypeExpression The prototype expression
	 */
	public NewProplist(ProplistDeclaration declaration, ASTNode prototypeExpression) {
		super(declaration.withImplicitProtoype(prototypeExpression));
	}
	
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append(Keywords.New);
		output.append(" ");
		definedDeclaration().implicitPrototype().print(output, depth);
		output.append(" ");
		super.doPrint(output, depth);
	}
	
	@Override
	public ASTNode[] subElements() {
		ASTNode[] result = new ASTNode[1+components().size()];
		result[0] = definedDeclaration().implicitPrototype();
		int i = 1;
		for (Variable c : components())
			result[i++] = c.initializationExpression();
		return result;
	}

}
