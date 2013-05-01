package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.ASTNodeWithSubElementsArray;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StringUtil;

public class Tuple extends ASTNodeWithSubElementsArray {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public Tuple(ASTNode[] elms) {
		super(elms);		
	}

	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		StringUtil.writeBlock(output, "(", ")", ", ", ArrayUtil.iterable(elements));
	}

}