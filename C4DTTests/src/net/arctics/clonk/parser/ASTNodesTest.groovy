package net.arctics.clonk.parser

import net.arctics.clonk.TestBase
import net.arctics.clonk.c4script.Operator
import net.arctics.clonk.c4script.ast.AccessVar
import net.arctics.clonk.c4script.ast.BinaryOp
import net.arctics.clonk.c4script.ast.Block
import net.arctics.clonk.c4script.ast.BreakStatement
import net.arctics.clonk.c4script.ast.CallDeclaration
import net.arctics.clonk.c4script.ast.IntegerLiteral
import net.arctics.clonk.c4script.ast.SimpleStatement
import net.arctics.clonk.c4script.ast.StringLiteral
import net.arctics.clonk.c4script.ast.True
import net.arctics.clonk.c4script.ast.UnaryOp
import net.arctics.clonk.c4script.ast.UnaryOp.Placement
import net.arctics.clonk.c4script.ast.WhileStatement
import org.junit.Assert
import org.junit.Test

class ASTNodesTest extends TestBase {
	@Test
	public void testASTPrinting() {
		final Block b = new Block(new SimpleStatement(new BinaryOp(Operator.Assign,
				new AccessVar("i"), new IntegerLiteral(50))),
				new SimpleStatement(new UnaryOp(Operator.Increment,
						Placement.Prefix, new AccessVar("i"))),
				new WhileStatement(new True(), new Block(
						new SimpleStatement(new CallDeclaration("Log",
								new StringLiteral("Test"))),
						new BreakStatement())))
		def ref =
"""{
	i = 50;
	++i;
	while (true)
	{
		Log("Test");
		break;
	}
}"""
		Assert.assertTrue(b.printed().equals(ref))
	}
}
