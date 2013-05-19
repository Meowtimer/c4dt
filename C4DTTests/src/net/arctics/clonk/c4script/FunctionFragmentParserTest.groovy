package net.arctics.clonk.c4script

import org.junit.Test

import static org.junit.Assert.*
import net.arctics.clonk.DefinitionInfo
import net.arctics.clonk.TestBase
import net.arctics.clonk.c4script.ScriptParserTest.Setup
import net.arctics.clonk.c4script.ast.AccessVar
import net.arctics.clonk.c4script.ast.BinaryOp
import net.arctics.clonk.c4script.ast.Block
import net.arctics.clonk.c4script.ast.CallDeclaration
import net.arctics.clonk.c4script.ast.IntegerLiteral
import net.arctics.clonk.c4script.ast.SimpleStatement
import net.arctics.clonk.c4script.ast.StringLiteral
import net.arctics.clonk.c4script.ast.UnaryOp
import net.arctics.clonk.c4script.ast.WhileStatement
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.parser.Markers

public class FunctionFragmentParserTest extends TestBase {
	@Test
	public void testUpdate() {

		def ast = new Block(
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

		def setup = new Setup(new DefinitionInfo(name: 'Obj', source:
"""
local x;
func Func() {${ast.printed()}}
"""))
		setup.parser.parse()
		setup.script.deriveInformation()

		def func = setup.script.findLocalFunction("Func", false)

		FunctionFragmentParser parser = new FunctionFragmentParser(
			documentMock(setup.script.source().contentsAsString()),
			setup.script, func, new Markers()
		)
	}
}
