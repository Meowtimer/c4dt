package net.arctics.clonk.c4script;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StreamUtil.stringFromInputStream;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.Utilities.attempt;
import net.arctics.clonk.DefinitionInfo;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.TestBase;
import net.arctics.clonk.c4script.ScriptParserTest.Setup;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.Block;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.ast.UnaryOp;
import net.arctics.clonk.c4script.ast.UnaryOp.Placement;
import net.arctics.clonk.c4script.ast.WhileStatement;
import net.arctics.clonk.parser.Markers;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class FunctionFragmentParserTest extends TestBase {
	@Test
	public void testUpdate() {

		final Block ast = new Block(
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
		);

		final Setup setup = new Setup(new DefinitionInfo("Obj", blockString("", "", "\n", stream(new String[] {"local x;", "func Func() {${ast.printed()}}"}))));
		try {
			setup.parser.parse();
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
		setup.script.deriveInformation();

		final Function func = setup.script.findLocalFunction("Func", false);

		final FunctionFragmentParser parser = new FunctionFragmentParser(
			documentMock(attempt(() -> stringFromInputStream(setup.script.source().getContents()), CoreException.class, Exception::printStackTrace)),
			setup.script, func, new Markers()
		);
	}
}
