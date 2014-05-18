package net.arctics.clonk.c4script.ast;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.util.StringUtil;

import org.junit.Test;

public class PropListExpressionTest {
	@Test
	public void testLongPropListParameterPrinting() {
		final PropListExpression expr = new PropListExpression(new ProplistDeclaration(Arrays.asList(
			new Variable(Scope.LOCAL, "longIdentifierIsLong", new IntegerLiteral(Integer.MAX_VALUE)),
			new Variable(Scope.LOCAL, "anotherOneIsEvenLonger", new StringLiteral("asdasdasdasdasdasdasdasdasd"))
		)));
		final CallDeclaration call = new CallDeclaration("SomeFunction", expr);
		assertEquals(
			StringUtil.join("\n", 
				"SomeFunction",
				"(",
				"	{",
				"		longIdentifierIsLong: 2147483647,",
				"		anotherOneIsEvenLonger: \"asdasdasdasdasdasdasdasdasd\"",
				"	}",
				")"
			),
			call.printed()
		);
	}
}
