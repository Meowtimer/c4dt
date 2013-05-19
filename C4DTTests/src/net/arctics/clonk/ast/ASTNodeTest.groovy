package net.arctics.clonk.ast;

import net.arctics.clonk.c4script.ast.AccessVar
import net.arctics.clonk.c4script.ast.BinaryOp
import net.arctics.clonk.c4script.ast.Block
import net.arctics.clonk.c4script.ast.BreakStatement
import net.arctics.clonk.c4script.ast.CallDeclaration
import net.arctics.clonk.c4script.ast.CallExpr
import net.arctics.clonk.c4script.ast.IntegerLiteral
import net.arctics.clonk.c4script.ast.SimpleStatement
import net.arctics.clonk.c4script.ast.StringLiteral
import net.arctics.clonk.c4script.ast.True
import net.arctics.clonk.c4script.ast.Tuple
import net.arctics.clonk.c4script.ast.UnaryOp
import net.arctics.clonk.c4script.ast.WhileStatement
import net.arctics.clonk.c4script.ast.UnaryOp.Placement
import net.arctics.clonk.c4script.Operator

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class ASTNodeTest {
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
	@Test
	public void testCompare() {
		def ast1 = new Block(
			new SimpleStatement(new CallDeclaration("Log", new StringLiteral("There are many bodies, but this one's mine"))),
			new WhileStatement(
				new BinaryOp(Operator.Smaller, new AccessVar("x"), new IntegerLiteral(10)),
				new Block(
					new SimpleStatement(new CallDeclaration("CreateObject",
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20)),
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20))
					)),
					new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("x"))
				)
			)
		)

		def ast2 = new Block(
			new SimpleStatement(new CallDeclaration("Log", new StringLiteral("There are many bodies, but this one's mine"))),
			new WhileStatement(
				new BinaryOp(Operator.Smaller, new AccessVar("x"), new IntegerLiteral(10)),
				new Block(
					new SimpleStatement(new CallDeclaration("CreateObject",
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20)),
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20))
					)),
					new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("x"))
				)
			)
		)

		def ast3 = new Block(
			new SimpleStatement(new CallDeclaration("Log", new StringLiteral("There are many bodies, but this one's mine"))),
			new WhileStatement(
				new BinaryOp(Operator.Smaller, new AccessVar("x"), new IntegerLiteral(10)),
				new Block(
					new SimpleStatement(new CallDeclaration("CreateObject",
						new BinaryOp(Operator.Add, new AccessVar("x"), new IntegerLiteral(20)),
						new BinaryOp(Operator.Add, new AccessVar("x"), new IntegerLiteral(20))
					)),
					new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("x"))
				)
			)
		)

		Assert.assertTrue(ast1.compare(ast2, new ASTComparisonDelegate(ast1)))
		Assert.assertFalse(ast1.compare(ast3, new ASTComparisonDelegate(ast1)))
	}

	@Test
	public void testMatch() {
		def ast1 = ASTNodeMatcher.prepareForMatching(new Block(
			new SimpleStatement(new CallDeclaration("Log", new Placeholder("msg"))),
			new Sequence(
				new Placeholder("while:WhileStatement"),
				new CallExpr(
					new Placeholder(":BinaryOp"),
					new Block(
						new SimpleStatement(new CallDeclaration("CreateObject",
							new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20)),
							new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20))
						)),
						new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("x"))
					)
				)
			)
		))

		def ast2 = new Block(
			new SimpleStatement(new CallDeclaration("Log", new StringLiteral("There are many bodies, but this one's mine"))),
			new WhileStatement(
				new BinaryOp(Operator.Smaller, new AccessVar("x"), new IntegerLiteral(10)),
				new Block(
					new SimpleStatement(new CallDeclaration("CreateObject",
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20)),
						new BinaryOp(Operator.Multiply, new AccessVar("x"), new IntegerLiteral(20))
					)),
					new UnaryOp(Operator.Increment, Placement.Postfix, new AccessVar("x"))
				)
			)
		)

		def match = ast1.match(ast2)

		Assert.assertNotNull(match)
		Assert.assertTrue(match.containsKey("msg"))
		Assert.assertTrue(match["msg"][0].equals(new StringLiteral("There are many bodies, but this one's mine")))
	}
}
